package com.minecraftin.clone.world;

import com.minecraftin.clone.config.GameConfig;
import com.minecraftin.clone.engine.TextureAtlas;
import com.minecraftin.clone.util.FloatArrayBuilder;

// 負責把 Chunk 內的方塊資料轉成可用來繪製的頂點資料。
// 輸出格式必須與 WorldRenderer 建立 Mesh 時的 attribute layout 以及 world.vert 保持一致。
public final class ChunkMesher {

    // 每個頂點包含 8 個 float：x、y、z、u、v、面陰影*AO、天空光、方塊光。
    public static final int STRIDE_FLOATS = 8;

    // 先把所有面存起來，方便後面直接遍歷。
    private static final Face[] FACES = Face.values();

    // 四個 AO 等級對應的亮度倍率；0 是被兩側完全夾住的最暗角落。
    private static final float[] AO_LEVELS = { 0.55f, 0.70f, 0.85f, 1.0f };

    // 每個面四個頂點的位置偏移（a、b、c、d 各 3 個 0/1 值），順序對應 Face 的 enum 順序。
    private static final int[][] FACE_POSITIONS = {
            { 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0 }, // NORTH
            { 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1 }, // SOUTH
            { 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0 }, // WEST
            { 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 1 }, // EAST
            { 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1 }, // UP
            { 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 0 }, // DOWN
    };

    // 每個面在平面上的兩個切線軸；角落取樣沿這兩個軸位移。
    private static final int[][] FACE_TANGENT1 = {
            { 1, 0, 0 }, { 1, 0, 0 }, { 0, 0, 1 }, { 0, 0, 1 }, { 1, 0, 0 }, { 1, 0, 0 },
    };
    private static final int[][] FACE_TANGENT2 = {
            { 0, 1, 0 }, { 0, 1, 0 }, { 0, 1, 0 }, { 0, 1, 0 }, { 0, 0, 1 }, { 0, 0, 1 },
    };

    // 每個頂點在兩個切線軸上的角落方向（-1 或 +1），與 FACE_POSITIONS 的頂點順序對齊。
    private static final int[][] FACE_SU = {
            { 1, -1, -1, 1 },   // NORTH
            { -1, 1, 1, -1 },   // SOUTH
            { -1, 1, 1, -1 },   // WEST
            { 1, -1, -1, 1 },   // EAST
            { -1, 1, 1, -1 },   // UP
            { -1, -1, 1, 1 },   // DOWN
    };
    private static final int[][] FACE_SV = {
            { -1, -1, 1, 1 },   // NORTH
            { -1, -1, 1, 1 },   // SOUTH
            { -1, -1, 1, 1 },   // WEST
            { -1, -1, 1, 1 },   // EAST
            { -1, -1, 1, 1 },   // UP
            { -1, 1, 1, -1 },   // DOWN
    };

    // 依 AO 對角線方向選擇三角形頂點順序；預先配置，避免每個外露面都建立 int[]。
    private static final int[] QUAD_ORDER_NORMAL = { 0, 1, 2, 2, 3, 0 };
    private static final int[] QUAD_ORDER_FLIPPED = { 1, 2, 3, 3, 0, 1 };

    // Chunk meshing 分成不透明與半透明兩組，讓 renderer 能先寫入不透明深度，再混合水與玻璃。
    public record MeshData(float[] opaqueVertices, float[] translucentVertices) {
    }

    // 這個類別只提供靜態方法，不需要建立物件。
    private ChunkMesher() {
    }

    // 根據 Chunk 內容建立對應的頂點資料；只輸出外露面，內部相鄰面會被省略以降低頂點數。
    public static MeshData build(Chunk chunk, World world, TextureAtlas atlas) {
        FloatArrayBuilder opaqueVertices = new FloatArrayBuilder(16384);
        FloatArrayBuilder translucentVertices = new FloatArrayBuilder(4096);
        LightSampler sampler = new LightSampler(chunk, world);

        // FaceScratch 屬於單次 build 呼叫；即使 meshing 在背景執行緒跑，也不會跨 Chunk 共用。
        FaceScratch scratch = new FaceScratch();

        // 取得這個 Chunk 在世界中的起始座標。
        int worldMinX = chunk.worldMinX();
        int worldMinZ = chunk.worldMinZ();

        // 逐一檢查 Chunk 內每一個方塊。
        for (int y = 0; y < GameConfig.CHUNK_HEIGHT; y++) {
            for (int z = 0; z < GameConfig.CHUNK_SIZE; z++) {
                for (int x = 0; x < GameConfig.CHUNK_SIZE; x++) {
                    BlockType block = chunk.get(x, y, z);

                    // 空氣不需要繪製，直接跳過。
                    if (block == BlockType.AIR) {
                        continue;
                    }

                    int worldX = worldMinX + x;
                    int worldZ = worldMinZ + z;
                    FloatArrayBuilder targetVertices = block.isTranslucent() ? translucentVertices : opaqueVertices;

                    if (!block.isFullCube()) {
                        // 非完整方塊直接依 renderBoxes 畫出簡化幾何，不做鄰面裁切，避免薄片或樓梯缺面。
                        addBlockBoxes(targetVertices, sampler, block, x, y, z, worldX, worldZ, atlas);
                        continue;
                    }

                    // 檢查這個方塊的六個面，判斷哪些面需要被畫出來。
                    for (Face face : FACES) {
                        int neighborX = x + face.dx();
                        int neighborY = y + face.dy();
                        int neighborZ = z + face.dz();
                        BlockType neighbor = neighborBlock(chunk, world,
                                worldX + face.dx(), neighborY, worldZ + face.dz(),
                                neighborX, neighborZ);

                        // 如果這個面不需要顯示，就跳過；跨 Chunk 時仍用 peekBlock 避免意外生成新 Chunk。
                        if (!shouldRenderFace(block, neighbor)) {
                            continue;
                        }

                        // 取得這個面的貼圖範圍。
                        int tile = block.tileForFace(face);
                        float u0 = atlas.u0(tile);
                        float v0 = atlas.v0(tile);
                        float u1 = atlas.u1(tile);
                        float v1 = atlas.v1(tile);

                        // 把這個面加入頂點資料中，含平滑光照與 AO。
                        addFace(targetVertices, sampler, x, y, z, worldX, worldZ, face, u0, v0, u1, v1, scratch);
                    }
                }
            }
        }

        return new MeshData(opaqueVertices.toArray(), translucentVertices.toArray());
    }

    // 同一個 Chunk 內的鄰格直接讀取，只有跨 Chunk 邊界時才透過 World.peekBlock 查詢。
    private static BlockType neighborBlock(Chunk chunk, World world, int worldX, int y, int worldZ, int localX,
            int localZ) {
        if (y < 0) {
            return BlockType.BEDROCK;
        }
        if (y >= GameConfig.CHUNK_HEIGHT) {
            return BlockType.AIR;
        }
        if (localX >= 0 && localX < GameConfig.CHUNK_SIZE && localZ >= 0 && localZ < GameConfig.CHUNK_SIZE) {
            return chunk.get(localX, y, localZ);
        }
        return world.peekBlock(worldX, y, worldZ);
    }

    // 判斷目前方塊的某個面是否需要被繪製。
    private static boolean shouldRenderFace(BlockType current, BlockType neighbor) {
        // 鄰居是空氣時，這個面一定要畫出來。
        if (neighbor == BlockType.AIR) {
            return true;
        }

        // 水（含流動水）只在旁邊不是水時才繪製面，避免相鄰水格中間產生多餘透明面。
        if (current.isWaterBlock()) {
            return !neighbor.isWaterBlock();
        }

        // 鄰居不是完整方塊時，仍要繪製完整方塊的面，避免門、柵欄、樓梯旁出現缺面。
        if (!neighbor.isFullCube()) {
            return true;
        }

        // 透明方塊彼此相鄰時，如果是同一種方塊，就不畫中間那個面。
        if (current.isTransparent() && neighbor == current) {
            return false;
        }

        // 鄰居是透明方塊時，這個面需要顯示。
        return neighbor.isTransparent();
    }

    // 完整方塊的面：對四個頂點做 Minecraft 式平滑光照取樣（鄰面四格平均）與 AO 計算。
    private static void addFace(
            FloatArrayBuilder out,
            LightSampler sampler,
            int x,
            int y,
            int z,
            int worldX,
            int worldZ,
            Face face,
            float u0,
            float v0,
            float u1,
            float v1,
            FaceScratch scratch) {
        int faceIndex = face.ordinal();
        int[] positions = FACE_POSITIONS[faceIndex];
        int[] t1 = FACE_TANGENT1[faceIndex];
        int[] t2 = FACE_TANGENT2[faceIndex];
        int[] suList = FACE_SU[faceIndex];
        int[] svList = FACE_SV[faceIndex];

        // 面前方那一格是光照取樣的基準。
        int baseX = worldX + face.dx();
        int baseY = y + face.dy();
        int baseZ = worldZ + face.dz();
        float faceShade = face.light();

        float[] shade = scratch.shade;
        float[] sky = scratch.sky;
        float[] blockLight = scratch.blockLight;
        int baseSky = sampler.sky(baseX, baseY, baseZ);
        int baseBlock = sampler.block(baseX, baseY, baseZ);

        for (int i = 0; i < 4; i++) {
            int su = suList[i];
            int sv = svList[i];
            int o1x = t1[0] * su;
            int o1y = t1[1] * su;
            int o1z = t1[2] * su;
            int o2x = t2[0] * sv;
            int o2y = t2[1] * sv;
            int o2z = t2[2] * sv;

            boolean side1 = sampler.occludes(baseX + o1x, baseY + o1y, baseZ + o1z);
            boolean side2 = sampler.occludes(baseX + o2x, baseY + o2y, baseZ + o2z);
            boolean corner = sampler.occludes(baseX + o1x + o2x, baseY + o1y + o2y, baseZ + o1z + o2z);

            int ao = (side1 && side2) ? 0 : 3 - ((side1 ? 1 : 0) + (side2 ? 1 : 0) + (corner ? 1 : 0));
            shade[i] = faceShade * AO_LEVELS[ao];

            // 平滑光照：取角落周圍四格的平均；被不透明方塊佔住的格子改用面前那格的值，避免過度變暗。
            int sumSky = baseSky;
            int sumBlock = baseBlock;

            sumSky += side1 ? baseSky : sampler.sky(baseX + o1x, baseY + o1y, baseZ + o1z);
            sumBlock += side1 ? baseBlock : sampler.block(baseX + o1x, baseY + o1y, baseZ + o1z);
            sumSky += side2 ? baseSky : sampler.sky(baseX + o2x, baseY + o2y, baseZ + o2z);
            sumBlock += side2 ? baseBlock : sampler.block(baseX + o2x, baseY + o2y, baseZ + o2z);
            sumSky += corner ? baseSky : sampler.sky(baseX + o1x + o2x, baseY + o1y + o2y, baseZ + o1z + o2z);
            sumBlock += corner ? baseBlock : sampler.block(baseX + o1x + o2x, baseY + o1y + o2y, baseZ + o1z + o2z);

            sky[i] = sumSky / (4.0f * LightEngine.MAX_LIGHT);
            blockLight[i] = sumBlock / (4.0f * LightEngine.MAX_LIGHT);
        }

        // 四個頂點的位置與 UV；a=(u0,v1)、b=(u1,v1)、c=(u1,v0)、d=(u0,v0)。
        float[] px = scratch.px;
        float[] py = scratch.py;
        float[] pz = scratch.pz;
        for (int i = 0; i < 4; i++) {
            px[i] = x + positions[i * 3];
            py[i] = y + positions[i * 3 + 1];
            pz[i] = z + positions[i * 3 + 2];
        }
        float[] u = scratch.u;
        float[] v = scratch.v;
        u[0] = u0;
        u[1] = u1;
        u[2] = u1;
        u[3] = u0;
        v[0] = v1;
        v[1] = v1;
        v[2] = v0;
        v[3] = v0;

        // 依角落亮度選擇對角線，避免 AO 在四邊形內插時出現方向性條紋。
        boolean flip = shade[0] + shade[2] < shade[1] + shade[3];
        int[] order = flip ? QUAD_ORDER_FLIPPED : QUAD_ORDER_NORMAL;

        for (int index : order) {
            out.add(px[index], py[index], pz[index], u[index], v[index],
                    shade[index], sky[index], blockLight[index]);
        }
    }

    // 將同一個非完整方塊的多個局部盒轉成 mesh，例如樓梯兩盒、柵欄三盒。
    private static void addBlockBoxes(FloatArrayBuilder out, LightSampler sampler, BlockType block, float x, float y,
            float z, int worldX, int worldZ, TextureAtlas atlas) {
        // 非完整方塊用自身格子的光照，不做 AO；火把因為自身格子有光，會自然顯得明亮。
        float skyLight = sampler.sky(worldX, (int) y, worldZ) / (float) LightEngine.MAX_LIGHT;
        float blockLight = sampler.block(worldX, (int) y, worldZ) / (float) LightEngine.MAX_LIGHT;

        for (BlockBounds bounds : block.renderBoxes()) {
            addBox(out, block, x, y, z, bounds, atlas, skyLight, blockLight);
        }
    }

    // render box 使用 Chunk 本地座標加上局部 AABB，材質仍依原方塊的六面貼圖規則取得。
    private static void addBox(FloatArrayBuilder out, BlockType block, float x, float y, float z,
            BlockBounds bounds, TextureAtlas atlas, float skyLight, float blockLight) {
        float minX = x + bounds.minX();
        float minY = y + bounds.minY();
        float minZ = z + bounds.minZ();
        float maxX = x + bounds.maxX();
        float maxY = y + bounds.maxY();
        float maxZ = z + bounds.maxZ();

        for (Face face : FACES) {
            int tile = block.tileForFace(face);
            float u0 = atlas.u0(tile);
            float v0 = atlas.v0(tile);
            float u1 = atlas.u1(tile);
            float v1 = atlas.v1(tile);
            addBoxFace(out, face, minX, minY, minZ, maxX, maxY, maxZ, u0, v0, u1, v1,
                    face.light(), skyLight, blockLight);
        }
    }

    // 和完整方塊的 addFace 相同輸出 6 個頂點，但座標來自 render box 的 min/max。
    // 這讓樓梯、門板、柵欄等非完整方塊能共用同一套 Mesh/shader attribute 格式。
    private static void addBoxFace(
            FloatArrayBuilder out,
            Face face,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            float u0,
            float v0,
            float u1,
            float v1,
            float shade,
            float skyLight,
            float blockLight) {
        switch (face) {
            case NORTH -> addQuad(out, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ,
                    u0, v0, u1, v1, shade, skyLight, blockLight);
            case SOUTH -> addQuad(out, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ,
                    u0, v0, u1, v1, shade, skyLight, blockLight);
            case WEST -> addQuad(out, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ,
                    u0, v0, u1, v1, shade, skyLight, blockLight);
            case EAST -> addQuad(out, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ,
                    u0, v0, u1, v1, shade, skyLight, blockLight);
            case UP -> addQuad(out, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ,
                    u0, v0, u1, v1, shade, skyLight, blockLight);
            case DOWN -> addQuad(out, minX, minY, minZ, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ,
                    u0, v0, u1, v1, shade, skyLight, blockLight);
        }
    }

    // 把四邊形拆成兩個三角形，加入頂點資料。
    private static void addQuad(
            FloatArrayBuilder out,
            float ax, float ay, float az,
            float bx, float by, float bz,
            float cx, float cy, float cz,
            float dx, float dy, float dz,
            float u0, float v0, float u1, float v1,
            float shade, float skyLight, float blockLight) {
        // 第一個三角形
        out.add(ax, ay, az, u0, v1, shade, skyLight, blockLight);
        out.add(bx, by, bz, u1, v1, shade, skyLight, blockLight);
        out.add(cx, cy, cz, u1, v0, shade, skyLight, blockLight);

        // 第二個三角形
        out.add(cx, cy, cz, u1, v0, shade, skyLight, blockLight);
        out.add(dx, dy, dz, u0, v0, shade, skyLight, blockLight);
        out.add(ax, ay, az, u0, v1, shade, skyLight, blockLight);
    }

    // 單次 Chunk meshing 內重複使用的小型暫存，避免每個外露面配置多個 4 格陣列。
    private static final class FaceScratch {
        private final float[] shade = new float[4];
        private final float[] sky = new float[4];
        private final float[] blockLight = new float[4];
        private final float[] px = new float[4];
        private final float[] py = new float[4];
        private final float[] pz = new float[4];
        private final float[] u = new float[4];
        private final float[] v = new float[4];
    }

    // 集中處理光照與遮蔽查詢；Chunk 內的格子走快速路徑，跨界才透過 World。
    private static final class LightSampler {
        private final Chunk chunk;
        private final World world;
        private final int minX;
        private final int minZ;

        private LightSampler(Chunk chunk, World world) {
            this.chunk = chunk;
            this.world = world;
            this.minX = chunk.worldMinX();
            this.minZ = chunk.worldMinZ();
        }

        // AO 只把「完整且不透明」的方塊視為遮蔽，樹葉與玻璃不會造成假陰影。
        private boolean occludes(int worldX, int y, int worldZ) {
            BlockType block;
            int localX = worldX - minX;
            int localZ = worldZ - minZ;
            if (localX >= 0 && localX < GameConfig.CHUNK_SIZE && localZ >= 0 && localZ < GameConfig.CHUNK_SIZE) {
                block = chunk.get(localX, y, localZ);
            } else {
                block = world.peekBlock(worldX, y, worldZ);
            }
            return block.isFullCube() && block.isOpaque();
        }

        private int sky(int worldX, int y, int worldZ) {
            if (y >= GameConfig.CHUNK_HEIGHT) {
                return LightEngine.MAX_LIGHT;
            }
            if (y < 0) {
                return 0;
            }
            int localX = worldX - minX;
            int localZ = worldZ - minZ;
            if (localX >= 0 && localX < GameConfig.CHUNK_SIZE && localZ >= 0 && localZ < GameConfig.CHUNK_SIZE) {
                return chunk.skyLight(localX, y, localZ);
            }
            return world.skyLightAt(worldX, y, worldZ);
        }

        private int block(int worldX, int y, int worldZ) {
            if (y < 0 || y >= GameConfig.CHUNK_HEIGHT) {
                return 0;
            }
            int localX = worldX - minX;
            int localZ = worldZ - minZ;
            if (localX >= 0 && localX < GameConfig.CHUNK_SIZE && localZ >= 0 && localZ < GameConfig.CHUNK_SIZE) {
                return chunk.blockLight(localX, y, localZ);
            }
            return world.blockLightAt(worldX, y, worldZ);
        }
    }
}

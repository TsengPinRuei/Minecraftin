package com.minecraftin.clone.world;

import com.minecraftin.clone.config.GameConfig;
import com.minecraftin.clone.engine.TextureAtlas;
import com.minecraftin.clone.util.FloatArrayBuilder;

// 負責把 Chunk 內的方塊資料轉成可用來繪製的頂點資料。
// 輸出格式必須與 WorldRenderer 建立 Mesh 時的 attribute layout 以及 world.vert 保持一致。
public final class ChunkMesher {

    // 每個頂點包含 6 個 float：x、y、z、u、v、light。
    public static final int STRIDE_FLOATS = 6;

    // 先把所有面存起來，方便後面直接遍歷。
    private static final Face[] FACES = Face.values();

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
                        addBlockBoxes(targetVertices, block, x, y, z, atlas);
                        continue;
                    }

                    // 檢查這個方塊的六個面，判斷哪些面需要被畫出來。
                    for (Face face : FACES) {
                        BlockType neighbor = world.peekBlock(worldX + face.dx(), y + face.dy(), worldZ + face.dz());

                        // 如果這個面不需要顯示，就跳過；peekBlock 可避免 meshing 時意外生成新 Chunk。
                        if (!shouldRenderFace(block, neighbor)) {
                            continue;
                        }

                        // 取得這個面的貼圖範圍。
                        int tile = block.tileForFace(face);
                        float u0 = atlas.u0(tile);
                        float v0 = atlas.v0(tile);
                        float u1 = atlas.u1(tile);
                        float v1 = atlas.v1(tile);

                        // 把這個面加入頂點資料中。
                        addFace(targetVertices, x, y, z, face, u0, v0, u1, v1, face.light());
                    }
                }
            }
        }

        return new MeshData(opaqueVertices.toArray(), translucentVertices.toArray());
    }

    // 判斷目前方塊的某個面是否需要被繪製。
    private static boolean shouldRenderFace(BlockType current, BlockType neighbor) {
        // 鄰居是空氣時，這個面一定要畫出來。
        if (neighbor == BlockType.AIR) {
            return true;
        }

        // 水只在旁邊不是水時才繪製面，避免相鄰水格中間產生多餘透明面。
        if (current == BlockType.WATER) {
            return neighbor != BlockType.WATER;
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

    // 根據面向，決定這個面的四個角落座標；頂點順序需和目前關閉背面剔除的渲染策略相容。
    private static void addFace(
            FloatArrayBuilder out,
            float x,
            float y,
            float z,
            Face face,
            float u0,
            float v0,
            float u1,
            float v1,
            float light) {
        switch (face) {
            case NORTH -> addQuad(out, x + 1, y, z, x, y, z, x, y + 1, z, x + 1, y + 1, z, u0, v0, u1, v1, light);
            case SOUTH ->
                addQuad(out, x, y, z + 1, x + 1, y, z + 1, x + 1, y + 1, z + 1, x, y + 1, z + 1, u0, v0, u1, v1, light);
            case WEST -> addQuad(out, x, y, z, x, y, z + 1, x, y + 1, z + 1, x, y + 1, z, u0, v0, u1, v1, light);
            case EAST ->
                addQuad(out, x + 1, y, z + 1, x + 1, y, z, x + 1, y + 1, z, x + 1, y + 1, z + 1, u0, v0, u1, v1, light);
            case UP ->
                addQuad(out, x, y + 1, z, x + 1, y + 1, z, x + 1, y + 1, z + 1, x, y + 1, z + 1, u0, v0, u1, v1, light);
            case DOWN -> addQuad(out, x, y, z, x, y, z + 1, x + 1, y, z + 1, x + 1, y, z, u0, v0, u1, v1, light);
        }
    }

    private static void addBlockBoxes(FloatArrayBuilder out, BlockType block, float x, float y, float z,
            TextureAtlas atlas) {
        for (BlockBounds bounds : block.renderBoxes()) {
            addBox(out, block, x, y, z, bounds, atlas);
        }
    }

    private static void addBox(FloatArrayBuilder out, BlockType block, float x, float y, float z,
            BlockBounds bounds, TextureAtlas atlas) {
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
            addBoxFace(out, face, minX, minY, minZ, maxX, maxY, maxZ, u0, v0, u1, v1, face.light());
        }
    }

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
            float light) {
        switch (face) {
            case NORTH -> addQuad(out, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ,
                    u0, v0, u1, v1, light);
            case SOUTH -> addQuad(out, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ,
                    u0, v0, u1, v1, light);
            case WEST -> addQuad(out, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ,
                    u0, v0, u1, v1, light);
            case EAST -> addQuad(out, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ,
                    u0, v0, u1, v1, light);
            case UP -> addQuad(out, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ,
                    u0, v0, u1, v1, light);
            case DOWN -> addQuad(out, minX, minY, minZ, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ,
                    u0, v0, u1, v1, light);
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
            float light) {
        // 第一個三角形
        putVertex(out, ax, ay, az, u0, v1, light);
        putVertex(out, bx, by, bz, u1, v1, light);
        putVertex(out, cx, cy, cz, u1, v0, light);

        // 第二個三角形
        putVertex(out, cx, cy, cz, u1, v0, light);
        putVertex(out, dx, dy, dz, u0, v0, light);
        putVertex(out, ax, ay, az, u0, v1, light);
    }

    // 加入一個頂點的資料。
    private static void putVertex(FloatArrayBuilder out, float x, float y, float z, float u, float v, float light) {
        out.add(x, y, z, u, v, light);
    }
}

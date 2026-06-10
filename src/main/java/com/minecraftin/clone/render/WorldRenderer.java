package com.minecraftin.clone.render;

import com.minecraftin.clone.config.GameConfig;
import com.minecraftin.clone.engine.Camera;
import com.minecraftin.clone.engine.Mesh;
import com.minecraftin.clone.engine.ShaderProgram;
import com.minecraftin.clone.engine.TextureAtlas;
import com.minecraftin.clone.util.FloatArrayBuilder;
import com.minecraftin.clone.world.BlockType;
import com.minecraftin.clone.world.Chunk;
import com.minecraftin.clone.world.ChunkMesher;
import com.minecraftin.clone.world.ChunkPos;
import com.minecraftin.clone.world.Face;
import com.minecraftin.clone.world.RaycastHit;
import com.minecraftin.clone.world.World;
import com.minecraftin.clone.util.Noise;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.lwjgl.opengl.GL33C.*;

// 負責繪製整個世界，包括區塊內容與被瞄準方塊的外框。
// 這裡也擁有所有與世界渲染相關的 OpenGL 資源，Game 結束時必須呼叫 close()。
public final class WorldRenderer implements AutoCloseable {

    // 白天、夜晚與日落的天空顏色；實際背景色依時間在這些顏色之間漸變。
    private static final Vector3f DAY_SKY_COLOR = new Vector3f(0.56f, 0.74f, 0.95f);
    private static final Vector3f NIGHT_SKY_COLOR = new Vector3f(0.015f, 0.025f, 0.07f);
    private static final Vector3f SUNSET_SKY_COLOR = new Vector3f(0.98f, 0.52f, 0.30f);

    // 夜晚的月光下限；天空光不會暗於這個倍率，與 Minecraft 的夜間亮度曲線類似。
    private static final float MOONLIGHT_FLOOR = 0.13f;

    // 霧效範圍綁定渲染距離，讓最遠一圈 Chunk 完全沒入霧中，像 Minecraft 一樣由霧中浮現而不是突然出現。
    private static final float FOG_FAR = GameConfig.RENDER_DISTANCE_CHUNKS * GameConfig.CHUNK_SIZE;
    private static final float FOG_NEAR = FOG_FAR * 0.6f;

    // 太陽與月亮：方形天體繞世界 X-Y 平面旋轉，是 Minecraft 天空的招牌造型。
    private static final float SKY_BODY_DISTANCE = 420.0f;
    private static final float SUN_HALF_SIZE = 36.0f;
    private static final float MOON_HALF_SIZE = 24.0f;
    private static final Vector3f SUN_COLOR = new Vector3f(1.0f, 0.97f, 0.82f);
    private static final Vector3f MOON_COLOR = new Vector3f(0.88f, 0.90f, 0.98f);

    // 雲層：固定高度的平面白雲，按格子由噪聲決定形狀，緩慢向 +X 漂移。
    private static final float CLOUD_Y = 150.0f;
    private static final float CLOUD_CELL_SIZE = 12.0f;
    private static final int CLOUD_RADIUS_CELLS = 30;
    private static final float CLOUD_DRIFT_SPEED = 0.7f;
    private static final Vector3f CLOUD_COLOR = new Vector3f(1.0f, 1.0f, 1.0f);
    private static final float CLOUD_ALPHA = 0.72f;

    // 選取方塊外框的顏色。
    private static final Vector3f SELECTION_COLOR = new Vector3f(0.03f, 0.03f, 0.03f);

    // 選取框用實際幾何體加粗，避免部分 OpenGL 驅動忽略 glLineWidth。
    private static final float SELECTION_OUTLINE_PADDING = 0.003f;
    private static final float SELECTION_OUTLINE_THICKNESS = 0.0092f;

    // 破壞方塊時噴出的碎屑數量與物理參數；創造模式仍保留 Minecraft 式的瞬間破壞回饋。
    private static final int BREAK_PARTICLES_PER_BLOCK = 28;
    private static final int MAX_BREAK_PARTICLES = 512;
    private static final float BREAK_PARTICLE_GRAVITY = 5.2f;

    // 方塊材質圖集。
    private final TextureAtlas atlas;

    // 繪製世界方塊時使用的 shader。
    private final ShaderProgram worldShader;

    // 繪製線框時使用的 shader。
    private final ShaderProgram lineShader;

    // 儲存每個 Chunk 對應的不透明/半透明 mesh，避免每幀都重新建立。
    private final Map<ChunkPos, ChunkMeshes> chunkMeshes = new HashMap<>();

    // 記錄目前畫面中可見的 Chunk。
    private final Set<ChunkPos> visibleChunks = new HashSet<>();

    // 記錄目前畫面中可見的 Chunk 順序；透明 pass 會用距離排序。
    private final List<VisibleChunk> visibleChunkOrder = new ArrayList<>();

    // 投影矩陣。
    private final Matrix4f projection = new Matrix4f();

    // 視角矩陣。
    private final Matrix4f view = new Matrix4f();

    // 模型矩陣。
    private final Matrix4f model = new Matrix4f();

    // 被選取方塊的外框 mesh。
    private final Mesh selectionMesh;

    // 方塊破壞碎屑共用一個動態 mesh，每幀依目前粒子位置重建。
    private final Mesh breakParticleMesh;
    private final List<BreakParticle> breakParticles = new ArrayList<>();
    private final Random particleRandom = new Random();
    private boolean breakParticleMeshDirty;

    // 太陽與月亮共用的 mesh，每幀依時間角度重建（頂點數極少）。
    private final Mesh skyBodyMesh;

    // 雲層 mesh；只有相機跨過雲格或漂移超過一格時才重建。
    private final Mesh cloudMesh;
    private int cachedCloudCellX = Integer.MIN_VALUE;
    private int cachedCloudCellZ = Integer.MIN_VALUE;
    private float cloudDrift;

    // 視錐剔除：只繪製鏡頭可見的 Chunk，但不影響 mesh 快取的保留範圍。
    private final FrustumIntersection frustum = new FrustumIntersection();
    private final Matrix4f projViewMatrix = new Matrix4f();

    // 目前的天空顏色與天空光倍率，由晝夜循環每幀更新。
    private final Vector3f skyColor = new Vector3f(DAY_SKY_COLOR);
    private float dayLight = 1.0f;

    // 快取上一次被選到的方塊座標。
    // 若目標沒變，就不需要重建外框資料。
    private int lastSelectionX = Integer.MIN_VALUE;
    private int lastSelectionY = Integer.MIN_VALUE;
    private int lastSelectionZ = Integer.MIN_VALUE;

    // 建立渲染器需要的資源。
    public WorldRenderer() {
        atlas = new TextureAtlas();
        worldShader = new ShaderProgram("/shaders/world.vert", "/shaders/world.frag");
        lineShader = new ShaderProgram("/shaders/line.vert", "/shaders/line.frag");

        // 一開始先建立空的選取框 mesh，之後有需要再更新內容。
        selectionMesh = new Mesh(new float[0], GL_TRIANGLES, 3);
        breakParticleMesh = new Mesh(new float[0], GL_TRIANGLES, 3, 2, 3);
        skyBodyMesh = new Mesh(new float[0], GL_TRIANGLES, 3);
        cloudMesh = new Mesh(new float[0], GL_TRIANGLES, 3);
    }

    // 推進方塊破壞碎屑的位置與生命週期；Game loop 每幀呼叫一次。
    public void update(float deltaSeconds) {
        if (deltaSeconds <= 0.0f) {
            return;
        }

        // 雲層持續漂移；使用累積時間而不是世界時間，避免跨日重置時雲突然跳回。
        cloudDrift += CLOUD_DRIFT_SPEED * deltaSeconds;

        if (breakParticles.isEmpty()) {
            return;
        }

        Iterator<BreakParticle> iterator = breakParticles.iterator();
        while (iterator.hasNext()) {
            BreakParticle particle = iterator.next();
            particle.life += deltaSeconds;

            if (particle.life >= particle.lifetime) {
                iterator.remove();
                continue;
            }

            particle.velocityY -= BREAK_PARTICLE_GRAVITY * deltaSeconds;
            particle.x += particle.velocityX * deltaSeconds;
            particle.y += particle.velocityY * deltaSeconds;
            particle.z += particle.velocityZ * deltaSeconds;
        }

        breakParticleMeshDirty = true;
    }

    // 產生使用被破壞方塊貼圖的小方塊碎屑；亮度取被破壞位置周圍最亮的一格。
    public void spawnBlockBreakEffect(World world, BlockType block, int x, int y, int z) {
        if (block == BlockType.AIR) {
            return;
        }

        float skyLight = neighborMaxSkyLight(world, x, y, z) / (float) 15;
        float blockLight = neighborMaxBlockLight(world, x, y, z) / (float) 15;

        while (breakParticles.size() + BREAK_PARTICLES_PER_BLOCK > MAX_BREAK_PARTICLES) {
            breakParticles.remove(0);
        }

        Face[] faces = Face.values();
        for (int i = 0; i < BREAK_PARTICLES_PER_BLOCK; i++) {
            float px = x + 0.18f + particleRandom.nextFloat() * 0.64f;
            float py = y + 0.18f + particleRandom.nextFloat() * 0.64f;
            float pz = z + 0.18f + particleRandom.nextFloat() * 0.64f;

            float dx = px - (x + 0.5f);
            float dy = py - (y + 0.5f);
            float dz = pz - (z + 0.5f);
            float length = Math.max(0.001f, (float) Math.sqrt(dx * dx + dy * dy + dz * dz));
            float burst = 0.95f + particleRandom.nextFloat() * 0.75f;

            float vx = dx / length * burst + (particleRandom.nextFloat() - 0.5f) * 0.55f;
            float vy = dy / length * burst + 1.05f + particleRandom.nextFloat() * 0.55f;
            float vz = dz / length * burst + (particleRandom.nextFloat() - 0.5f) * 0.55f;

            Face face = faces[particleRandom.nextInt(faces.length)];
            int tile = block.tileForFace(face);
            float tileU0 = atlas.u0(tile);
            float tileV0 = atlas.v0(tile);
            float tileU1 = atlas.u1(tile);
            float tileV1 = atlas.v1(tile);
            float patchU = (tileU1 - tileU0) * 0.25f;
            float patchV = (tileV1 - tileV0) * 0.25f;
            int patchX = particleRandom.nextInt(4);
            int patchY = particleRandom.nextInt(4);

            float u0 = tileU0 + patchU * patchX;
            float v0 = tileV0 + patchV * patchY;
            float u1 = u0 + patchU;
            float v1 = v0 + patchV;

            float size = 0.070f + particleRandom.nextFloat() * 0.055f;
            float lifetime = 0.42f + particleRandom.nextFloat() * 0.28f;
            breakParticles.add(new BreakParticle(px, py, pz, vx, vy, vz, size, lifetime, u0, v0, u1, v1,
                    skyLight, blockLight));
        }

        breakParticleMeshDirty = true;
    }

    // 取被破壞方塊六個鄰格中最亮的天空光，當作碎屑的亮度來源。
    private int neighborMaxSkyLight(World world, int x, int y, int z) {
        int max = world.skyLightAt(x, y, z);
        max = Math.max(max, world.skyLightAt(x + 1, y, z));
        max = Math.max(max, world.skyLightAt(x - 1, y, z));
        max = Math.max(max, world.skyLightAt(x, y + 1, z));
        max = Math.max(max, world.skyLightAt(x, y - 1, z));
        max = Math.max(max, world.skyLightAt(x, y, z + 1));
        max = Math.max(max, world.skyLightAt(x, y, z - 1));
        return max;
    }

    private int neighborMaxBlockLight(World world, int x, int y, int z) {
        int max = world.blockLightAt(x, y, z);
        max = Math.max(max, world.blockLightAt(x + 1, y, z));
        max = Math.max(max, world.blockLightAt(x - 1, y, z));
        max = Math.max(max, world.blockLightAt(x, y + 1, z));
        max = Math.max(max, world.blockLightAt(x, y - 1, z));
        max = Math.max(max, world.blockLightAt(x, y, z + 1));
        max = Math.max(max, world.blockLightAt(x, y, z - 1));
        return max;
    }

    // 繪製整個場景。
    public void render(World world, Camera camera, int width, int height, RaycastHit selection) {
        // 依世界時間更新天空顏色與天空光倍率。
        updateDayCycle(world.timeOfDay());

        // 設定 OpenGL 視窗範圍。
        glViewport(0, 0, width, height);

        // 清除畫面與深度緩衝，並設定天空底色。
        glClearColor(skyColor.x, skyColor.y, skyColor.z, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // 建立透視投影矩陣。
        projection.identity()
                .perspective(
                        (float) Math.toRadians(GameConfig.FOV_DEGREES),
                        (float) width / Math.max(height, 1),
                        GameConfig.NEAR_PLANE,
                        GameConfig.FAR_PLANE);

        // 由相機更新視角矩陣，並更新視錐剔除平面。
        camera.viewMatrix(view);
        projViewMatrix.set(projection).mul(view);
        frustum.set(projViewMatrix);

        // 先畫天空（太陽月亮），讓之後的地形可以遮住它們；再畫世界、雲與選取外框。
        renderSkyBodies(camera, world.timeOfDay());
        renderChunks(world, camera);
        renderBreakParticles(camera);
        renderClouds(world, camera);
        renderSelectionOutline(selection);
    }

    // 依時間計算天空顏色與天空光倍率：白天亮藍、夜晚深藍，日出日落帶橘色。
    private void updateDayCycle(float timeOfDay) {
        // 太陽仰角：0 是日出、0.25 是正午、0.5 是日落。
        float elevation = (float) Math.sin(timeOfDay * Math.PI * 2.0);

        // 地平線附近用較窄的窗格做平滑過渡，形成數十秒的晨昏。
        float daylight = clamp((elevation + 0.15f) / 0.30f, 0.0f, 1.0f);
        dayLight = MOONLIGHT_FLOOR + (1.0f - MOONLIGHT_FLOOR) * daylight;

        skyColor.set(NIGHT_SKY_COLOR).lerp(DAY_SKY_COLOR, daylight);

        // 太陽貼近地平線時混入日落色。
        float sunsetStrength = clamp(1.0f - Math.abs(elevation) / 0.15f, 0.0f, 1.0f);
        skyColor.lerp(SUNSET_SKY_COLOR, sunsetStrength * 0.45f);
    }

    // 繪製 Minecraft 式的方形太陽與月亮；不寫入深度，之後的地形會自然遮住它們。
    private void renderSkyBodies(Camera camera, float timeOfDay) {
        float angle = (float) (timeOfDay * Math.PI * 2.0);
        float sunX = (float) Math.cos(angle);
        float sunY = (float) Math.sin(angle);

        FloatArrayBuilder out = new FloatArrayBuilder(96);
        Vector3f cameraPosition = camera.position();

        boolean sunVisible = sunY > -0.12f;
        boolean moonVisible = -sunY > -0.12f;

        if (sunVisible) {
            addSkyBodyQuad(out, cameraPosition, sunX, sunY, SUN_HALF_SIZE);
        }
        if (moonVisible) {
            addSkyBodyQuad(out, cameraPosition, -sunX, -sunY, MOON_HALF_SIZE);
        }

        if (out.isEmpty()) {
            return;
        }

        // 太陽與月亮各 6 個頂點；用同一個 mesh 分兩次上傳會互相覆蓋，因此一次上傳、分段上色。
        lineShader.use();
        lineShader.setMat4("uProjection", projection);
        lineShader.setMat4("uView", view);
        model.identity();
        lineShader.setMat4("uModel", model);

        glDepthMask(false);

        if (sunVisible && moonVisible) {
            // 兩者都可見時拆成兩個批次，讓太陽與月亮可以用不同顏色。
            float[] vertices = out.toArray();
            float[] sunVertices = new float[18];
            float[] moonVertices = new float[18];
            System.arraycopy(vertices, 0, sunVertices, 0, 18);
            System.arraycopy(vertices, 18, moonVertices, 0, 18);

            skyBodyMesh.update(sunVertices, 3);
            lineShader.setVec3("uColor", SUN_COLOR);
            lineShader.setFloat("uAlpha", 1.0f);
            skyBodyMesh.draw();

            skyBodyMesh.update(moonVertices, 3);
            lineShader.setVec3("uColor", MOON_COLOR);
            skyBodyMesh.draw();
        } else {
            skyBodyMesh.update(out.toArray(), 3);
            lineShader.setVec3("uColor", sunVisible ? SUN_COLOR : MOON_COLOR);
            lineShader.setFloat("uAlpha", 1.0f);
            skyBodyMesh.draw();
        }

        glDepthMask(true);
    }

    // 在指定方向產生一個面向相機的方形天體。
    private void addSkyBodyQuad(FloatArrayBuilder out, Vector3f cameraPosition, float dirX, float dirY,
            float halfSize) {
        float centerX = cameraPosition.x + dirX * SKY_BODY_DISTANCE;
        float centerY = cameraPosition.y + dirY * SKY_BODY_DISTANCE;
        float centerZ = cameraPosition.z;

        // 太陽軌道在 X-Y 平面上，因此 Z 軸與軌道切線方向構成貼面的兩個軸。
        float tangentX = dirY * halfSize;
        float tangentY = -dirX * halfSize;

        float ax = centerX - tangentX;
        float ay = centerY - tangentY;
        float az = centerZ - halfSize;
        float bx = centerX + tangentX;
        float by = centerY + tangentY;
        float bz = centerZ - halfSize;
        float cx = centerX + tangentX;
        float cy = centerY + tangentY;
        float cz = centerZ + halfSize;
        float dx = centerX - tangentX;
        float dy = centerY - tangentY;
        float dz = centerZ + halfSize;

        out.add(ax, ay, az);
        out.add(bx, by, bz);
        out.add(cx, cy, cz);
        out.add(cx, cy, cz);
        out.add(dx, dy, dz);
        out.add(ax, ay, az);
    }

    // 繪製平面雲層；雲格由噪聲決定，整層隨時間向 +X 漂移。
    private void renderClouds(World world, Camera camera) {
        // 在「雲空間」（扣掉漂移量的座標系）建立 mesh，漂移由 model 矩陣處理。
        float cloudSpaceX = camera.position().x - cloudDrift;
        int cellX = (int) Math.floor(cloudSpaceX / CLOUD_CELL_SIZE);
        int cellZ = (int) Math.floor(camera.position().z / CLOUD_CELL_SIZE);

        if (cellX != cachedCloudCellX || cellZ != cachedCloudCellZ) {
            rebuildCloudMesh(world.seed(), cellX, cellZ);
            cachedCloudCellX = cellX;
            cachedCloudCellZ = cellZ;
        }

        lineShader.use();
        lineShader.setMat4("uProjection", projection);
        lineShader.setMat4("uView", view);
        model.identity().translate(cloudDrift, 0.0f, 0.0f);
        lineShader.setMat4("uModel", model);
        lineShader.setVec3("uColor", CLOUD_COLOR);
        lineShader.setFloat("uAlpha", CLOUD_ALPHA);

        glDepthMask(false);
        cloudMesh.draw();
        glDepthMask(true);
    }

    private void rebuildCloudMesh(long seed, int centerCellX, int centerCellZ) {
        FloatArrayBuilder out = new FloatArrayBuilder(16384);

        for (int dz = -CLOUD_RADIUS_CELLS; dz <= CLOUD_RADIUS_CELLS; dz++) {
            for (int dx = -CLOUD_RADIUS_CELLS; dx <= CLOUD_RADIUS_CELLS; dx++) {
                int cellX = centerCellX + dx;
                int cellZ = centerCellZ + dz;

                // 噪聲門檻決定雲的覆蓋率與團狀分布。
                if (Noise.fbm2(cellX * 0.17f, cellZ * 0.17f, 3, 2.0f, 0.5f, seed ^ 0x434C4F5544L) <= 0.12f) {
                    continue;
                }

                float minX = cellX * CLOUD_CELL_SIZE;
                float minZ = cellZ * CLOUD_CELL_SIZE;
                float maxX = minX + CLOUD_CELL_SIZE;
                float maxZ = minZ + CLOUD_CELL_SIZE;

                out.add(minX, CLOUD_Y, minZ);
                out.add(maxX, CLOUD_Y, minZ);
                out.add(maxX, CLOUD_Y, maxZ);
                out.add(maxX, CLOUD_Y, maxZ);
                out.add(minX, CLOUD_Y, maxZ);
                out.add(minX, CLOUD_Y, minZ);
            }
        }

        cloudMesh.update(out.toArray(), 3);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    // 繪製目前可見的所有 Chunk。
    // Mesh 快取以 ChunkPos 為 key，只有 Chunk dirty 或首次可見時才重建頂點資料。
    private void renderChunks(World world, Camera camera) {
        Vector3f cameraPosition = camera.position();

        worldShader.use();
        worldShader.setMat4("uProjection", projection);
        worldShader.setMat4("uView", view);
        worldShader.setVec3("uFogColor", skyColor);
        worldShader.setVec3("uCameraPos", cameraPosition);
        worldShader.setFloat("uFogNear", FOG_NEAR);
        worldShader.setFloat("uFogFar", FOG_FAR);
        worldShader.setFloat("uDayLight", dayLight);
        worldShader.setInt("uAtlas", 0);

        atlas.bind(0);

        // 根據相機位置找出目前所在的 Chunk。
        int centerChunkX = Math.floorDiv((int) Math.floor(cameraPosition.x), GameConfig.CHUNK_SIZE);
        int centerChunkZ = Math.floorDiv((int) Math.floor(cameraPosition.z), GameConfig.CHUNK_SIZE);

        int viewDistance = GameConfig.RENDER_DISTANCE_CHUNKS;
        int maxDistSq = viewDistance * viewDistance;

        visibleChunks.clear();
        visibleChunkOrder.clear();

        // 掃描玩家周圍一定距離內的 Chunk。
        for (int dz = -viewDistance; dz <= viewDistance; dz++) {
            for (int dx = -viewDistance; dx <= viewDistance; dx++) {
                // 用圓形範圍，而不是完整正方形範圍。
                if (dx * dx + dz * dz > maxDistSq) {
                    continue;
                }

                int chunkX = centerChunkX + dx;
                int chunkZ = centerChunkZ + dz;

                // 只渲染已載入的 Chunk。
                Chunk chunk = world.getChunkIfLoaded(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }

                ChunkPos key = new ChunkPos(chunkX, chunkZ);
                visibleChunks.add(key);

                // 視錐外的 Chunk 不建 mesh 也不繪製；保留在 visibleChunks 中避免快取被剪掉。
                int worldMinX = chunkX * GameConfig.CHUNK_SIZE;
                int worldMinZ = chunkZ * GameConfig.CHUNK_SIZE;
                if (!frustum.testAab(worldMinX, 0.0f, worldMinZ,
                        worldMinX + GameConfig.CHUNK_SIZE, GameConfig.CHUNK_HEIGHT,
                        worldMinZ + GameConfig.CHUNK_SIZE)) {
                    continue;
                }

                visibleChunkOrder.add(new VisibleChunk(key, chunkDistanceSq(key, cameraPosition.x, cameraPosition.z)));

                ChunkMeshes meshes = chunkMeshes.get(key);

                // 如果這個 Chunk 還沒有 mesh，或 mesh 已過期，就重新建立。
                if (meshes == null || chunk.isMeshDirty()) {
                    ChunkMesher.MeshData meshData = ChunkMesher.build(chunk, world, atlas);

                    if (meshes == null) {
                        // 頂點格式為位置 3、UV 2、光照 3（面陰影*AO、天空光、方塊光）。
                        meshes = new ChunkMeshes(
                                new Mesh(meshData.opaqueVertices(), GL_TRIANGLES, 3, 2, 3),
                                new Mesh(meshData.translucentVertices(), GL_TRIANGLES, 3, 2, 3));
                        chunkMeshes.put(key, meshes);
                    } else {
                        meshes.opaque.update(meshData.opaqueVertices(), ChunkMesher.STRIDE_FLOATS);
                        meshes.translucent.update(meshData.translucentVertices(), ChunkMesher.STRIDE_FLOATS);
                    }

                    chunk.clearMeshDirty();
                }

                // 把 Chunk 放到它在世界中的正確位置再繪製。
                model.identity().translate(chunk.worldMinX(), 0.0f, chunk.worldMinZ());
                worldShader.setMat4("uModel", model);
                meshes.opaque.draw();
            }
        }

        // 半透明材質後畫，並由遠到近排序；不寫入深度，避免玻璃/水先畫到深度後讓後面的透明面消失。
        visibleChunkOrder.sort((a, b) -> Float.compare(b.distanceSq(), a.distanceSq()));
        glDepthMask(false);
        for (VisibleChunk visibleChunk : visibleChunkOrder) {
            ChunkPos key = visibleChunk.pos();
            ChunkMeshes meshes = chunkMeshes.get(key);
            if (meshes == null) {
                continue;
            }

            model.identity().translate(key.x() * GameConfig.CHUNK_SIZE, 0.0f, key.z() * GameConfig.CHUNK_SIZE);
            worldShader.setMat4("uModel", model);
            meshes.translucent.draw();
        }
        glDepthMask(true);

        // 把這一幀看不到的 Chunk mesh 釋放掉，減少顯示卡資源占用；World 仍保留 Chunk 方塊資料。
        pruneChunkMeshes(visibleChunks);
    }

    // 用 Chunk 中心到相機的水平距離排序透明 pass；這是便宜近似，不做每個透明面的精確排序。
    private float chunkDistanceSq(ChunkPos pos, float cameraX, float cameraZ) {
        float chunkCenterX = pos.x() * GameConfig.CHUNK_SIZE + GameConfig.CHUNK_SIZE * 0.5f;
        float chunkCenterZ = pos.z() * GameConfig.CHUNK_SIZE + GameConfig.CHUNK_SIZE * 0.5f;
        float dx = chunkCenterX - cameraX;
        float dz = chunkCenterZ - cameraZ;
        return dx * dx + dz * dz;
    }

    // 刪除不在目前可見範圍內的 Chunk mesh。
    private void pruneChunkMeshes(Set<ChunkPos> visibleChunks) {
        Iterator<Map.Entry<ChunkPos, ChunkMeshes>> iterator = chunkMeshes.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<ChunkPos, ChunkMeshes> entry = iterator.next();

            if (visibleChunks.contains(entry.getKey())) {
                continue;
            }

            entry.getValue().close();
            iterator.remove();
        }
    }

    // 繪製被瞄準方塊的外框。
    private void renderSelectionOutline(RaycastHit hit) {
        // 如果目前沒有瞄準任何方塊，就清掉快取後直接結束。
        if (hit == null) {
            lastSelectionX = Integer.MIN_VALUE;
            lastSelectionY = Integer.MIN_VALUE;
            lastSelectionZ = Integer.MIN_VALUE;
            return;
        }

        // 只有在瞄準的方塊改變時，才重新建立外框資料。
        if (hit.x() != lastSelectionX || hit.y() != lastSelectionY || hit.z() != lastSelectionZ) {
            float[] vertices = buildWireCube(hit.x(), hit.y(), hit.z());
            selectionMesh.update(vertices, 3);

            lastSelectionX = hit.x();
            lastSelectionY = hit.y();
            lastSelectionZ = hit.z();
        }

        lineShader.use();
        lineShader.setMat4("uProjection", projection);
        lineShader.setMat4("uView", view);

        model.identity();
        lineShader.setMat4("uModel", model);
        lineShader.setVec3("uColor", SELECTION_COLOR);
        lineShader.setFloat("uAlpha", 1.0f);

        selectionMesh.draw();
    }

    // 繪製方塊破壞碎屑；共用世界 shader，讓碎屑也有霧效、面亮度與 atlas 貼圖。
    private void renderBreakParticles(Camera camera) {
        if (breakParticleMeshDirty) {
            updateBreakParticleMesh();
        }

        if (breakParticles.isEmpty()) {
            return;
        }

        worldShader.use();
        worldShader.setMat4("uProjection", projection);
        worldShader.setMat4("uView", view);
        worldShader.setVec3("uFogColor", skyColor);
        worldShader.setVec3("uCameraPos", camera.position());
        worldShader.setFloat("uFogNear", FOG_NEAR);
        worldShader.setFloat("uFogFar", FOG_FAR);
        worldShader.setFloat("uDayLight", dayLight);
        worldShader.setInt("uAtlas", 0);
        atlas.bind(0);

        model.identity();
        worldShader.setMat4("uModel", model);
        breakParticleMesh.draw();
    }

    private void updateBreakParticleMesh() {
        if (breakParticles.isEmpty()) {
            breakParticleMesh.update(new float[0], ChunkMesher.STRIDE_FLOATS);
            breakParticleMeshDirty = false;
            return;
        }

        FloatArrayBuilder out = new FloatArrayBuilder(breakParticles.size() * 216);
        for (BreakParticle particle : breakParticles) {
            addParticleCube(out, particle);
        }
        breakParticleMesh.update(out.toArray(), ChunkMesher.STRIDE_FLOATS);
        breakParticleMeshDirty = false;
    }

    private void addParticleCube(FloatArrayBuilder out, BreakParticle particle) {
        float half = particle.size * 0.5f;
        float minX = particle.x - half;
        float minY = particle.y - half;
        float minZ = particle.z - half;
        float maxX = particle.x + half;
        float maxY = particle.y + half;
        float maxZ = particle.z + half;

        addParticleFace(out, Face.NORTH, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ,
                particle);
        addParticleFace(out, Face.SOUTH, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ,
                particle);
        addParticleFace(out, Face.WEST, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ,
                particle);
        addParticleFace(out, Face.EAST, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ,
                particle);
        addParticleFace(out, Face.UP, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ,
                particle);
        addParticleFace(out, Face.DOWN, minX, minY, minZ, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ,
                particle);
    }

    private void addParticleFace(
            FloatArrayBuilder out,
            Face face,
            float ax, float ay, float az,
            float bx, float by, float bz,
            float cx, float cy, float cz,
            float dx, float dy, float dz,
            BreakParticle particle) {
        float shade = face.light();
        putParticleVertex(out, ax, ay, az, particle.u0, particle.v1, shade, particle);
        putParticleVertex(out, bx, by, bz, particle.u1, particle.v1, shade, particle);
        putParticleVertex(out, cx, cy, cz, particle.u1, particle.v0, shade, particle);
        putParticleVertex(out, cx, cy, cz, particle.u1, particle.v0, shade, particle);
        putParticleVertex(out, dx, dy, dz, particle.u0, particle.v0, shade, particle);
        putParticleVertex(out, ax, ay, az, particle.u0, particle.v1, shade, particle);
    }

    private void putParticleVertex(FloatArrayBuilder out, float x, float y, float z, float u, float v, float shade,
            BreakParticle particle) {
        out.add(x, y, z, u, v, shade, particle.skyLight, particle.blockLight);
    }

    // 建立包住一個方塊的粗邊框頂點資料。
    private float[] buildWireCube(int x, int y, int z) {
        // 稍微向外擴一點，避免和方塊表面重疊時閃爍。
        float minX = x - SELECTION_OUTLINE_PADDING;
        float minY = y - SELECTION_OUTLINE_PADDING;
        float minZ = z - SELECTION_OUTLINE_PADDING;
        float maxX = x + 1.0f + SELECTION_OUTLINE_PADDING;
        float maxY = y + 1.0f + SELECTION_OUTLINE_PADDING;
        float maxZ = z + 1.0f + SELECTION_OUTLINE_PADDING;
        float half = SELECTION_OUTLINE_THICKNESS * 0.5f;

        FloatArrayBuilder out = new FloatArrayBuilder(1600);

        // X 軸方向邊。
        addEdgeBox(out, minX - half, minY - half, minZ - half, maxX + half, minY + half, minZ + half);
        addEdgeBox(out, minX - half, minY - half, maxZ - half, maxX + half, minY + half, maxZ + half);
        addEdgeBox(out, minX - half, maxY - half, minZ - half, maxX + half, maxY + half, minZ + half);
        addEdgeBox(out, minX - half, maxY - half, maxZ - half, maxX + half, maxY + half, maxZ + half);

        // Y 軸方向邊。
        addEdgeBox(out, minX - half, minY - half, minZ - half, minX + half, maxY + half, minZ + half);
        addEdgeBox(out, maxX - half, minY - half, minZ - half, maxX + half, maxY + half, minZ + half);
        addEdgeBox(out, minX - half, minY - half, maxZ - half, minX + half, maxY + half, maxZ + half);
        addEdgeBox(out, maxX - half, minY - half, maxZ - half, maxX + half, maxY + half, maxZ + half);

        // Z 軸方向邊。
        addEdgeBox(out, minX - half, minY - half, minZ - half, minX + half, minY + half, maxZ + half);
        addEdgeBox(out, maxX - half, minY - half, minZ - half, maxX + half, minY + half, maxZ + half);
        addEdgeBox(out, minX - half, maxY - half, minZ - half, minX + half, maxY + half, maxZ + half);
        addEdgeBox(out, maxX - half, maxY - half, minZ - half, maxX + half, maxY + half, maxZ + half);

        return out.toArray();
    }

    // 用一個細長長方體代表線段，組成不依賴 glLineWidth 的粗選取框。
    private void addEdgeBox(FloatArrayBuilder out, float minX, float minY, float minZ, float maxX, float maxY,
            float maxZ) {
        addQuad(out, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ);
        addQuad(out, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ);
        addQuad(out, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ);
        addQuad(out, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ);
        addQuad(out, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ);
        addQuad(out, minX, minY, minZ, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ);
    }

    private void addQuad(FloatArrayBuilder out,
            float ax, float ay, float az,
            float bx, float by, float bz,
            float cx, float cy, float cz,
            float dx, float dy, float dz) {
        addVertex(out, ax, ay, az);
        addVertex(out, bx, by, bz);
        addVertex(out, cx, cy, cz);
        addVertex(out, cx, cy, cz);
        addVertex(out, dx, dy, dz);
        addVertex(out, ax, ay, az);
    }

    private void addVertex(FloatArrayBuilder out, float x, float y, float z) {
        out.add(x, y, z);
    }

    // 釋放所有渲染資源。
    @Override
    public void close() {
        for (ChunkMeshes meshes : chunkMeshes.values()) {
            meshes.close();
        }
        chunkMeshes.clear();

        selectionMesh.close();
        breakParticleMesh.close();
        skyBodyMesh.close();
        cloudMesh.close();
        worldShader.close();
        lineShader.close();
        atlas.close();
    }

    // 可見 Chunk 與它到相機的距離快取，避免透明排序時重複計算同一個距離。
    private record VisibleChunk(ChunkPos pos, float distanceSq) {
    }

    private static final class BreakParticle {
        private float x;
        private float y;
        private float z;
        private final float velocityX;
        private float velocityY;
        private final float velocityZ;
        private final float size;
        private final float lifetime;
        private final float u0;
        private final float v0;
        private final float u1;
        private final float v1;
        private final float skyLight;
        private final float blockLight;
        private float life;

        private BreakParticle(float x, float y, float z, float velocityX, float velocityY, float velocityZ,
                float size, float lifetime, float u0, float v0, float u1, float v1,
                float skyLight, float blockLight) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.velocityZ = velocityZ;
            this.size = size;
            this.lifetime = lifetime;
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
            this.skyLight = skyLight;
            this.blockLight = blockLight;
        }
    }

    // 同一個 Chunk 的不透明與半透明 mesh 綁在一起管理，避免剪裁時只釋放其中一個。
    private static final class ChunkMeshes implements AutoCloseable {
        private final Mesh opaque;
        private final Mesh translucent;

        private ChunkMeshes(Mesh opaque, Mesh translucent) {
            this.opaque = opaque;
            this.translucent = translucent;
        }

        @Override
        public void close() {
            opaque.close();
            translucent.close();
        }
    }
}

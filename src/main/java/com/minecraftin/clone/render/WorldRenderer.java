package com.minecraftin.clone.render;

import com.minecraftin.clone.config.GameConfig;
import com.minecraftin.clone.engine.Camera;
import com.minecraftin.clone.engine.Mesh;
import com.minecraftin.clone.engine.ShaderProgram;
import com.minecraftin.clone.engine.TextureAtlas;
import com.minecraftin.clone.world.Chunk;
import com.minecraftin.clone.world.ChunkMesher;
import com.minecraftin.clone.world.ChunkPos;
import com.minecraftin.clone.world.RaycastHit;
import com.minecraftin.clone.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.opengl.GL33C.*;

// 負責繪製整個世界，包括區塊內容與被瞄準方塊的外框。
public final class WorldRenderer implements AutoCloseable {

    // 天空背景顏色。
    private static final Vector3f SKY_COLOR = new Vector3f(0.56f, 0.74f, 0.95f);

    // 選取方塊外框的顏色。
    private static final Vector3f SELECTION_COLOR = new Vector3f(0.03f, 0.03f, 0.03f);

    // 方塊材質圖集。
    private final TextureAtlas atlas;

    // 繪製世界方塊時使用的 shader。
    private final ShaderProgram worldShader;

    // 繪製線框時使用的 shader。
    private final ShaderProgram lineShader;

    // 儲存每個 Chunk 對應的 mesh，避免每幀都重新建立。
    private final Map<ChunkPos, Mesh> chunkMeshes = new HashMap<>();

    // 記錄目前畫面中可見的 Chunk。
    private final Set<ChunkPos> visibleChunks = new HashSet<>();

    // 投影矩陣。
    private final Matrix4f projection = new Matrix4f();

    // 視角矩陣。
    private final Matrix4f view = new Matrix4f();

    // 模型矩陣。
    private final Matrix4f model = new Matrix4f();

    // 被選取方塊的外框 mesh。
    private final Mesh selectionMesh;

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
        selectionMesh = new Mesh(new float[0], GL_LINES, 3);
    }

    // 繪製整個場景。
    public void render(World world, Camera camera, int width, int height, RaycastHit selection) {
        // 設定 OpenGL 視窗範圍。
        glViewport(0, 0, width, height);

        // 清除畫面與深度緩衝，並設定天空底色。
        glClearColor(SKY_COLOR.x, SKY_COLOR.y, SKY_COLOR.z, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // 建立透視投影矩陣。
        projection.identity()
                .perspective(
                        (float) Math.toRadians(GameConfig.FOV_DEGREES),
                        (float) width / Math.max(height, 1),
                        GameConfig.NEAR_PLANE,
                        GameConfig.FAR_PLANE);

        // 由相機更新視角矩陣。
        camera.viewMatrix(view);

        // 先畫世界，再畫選取外框。
        renderChunks(world, camera);
        renderSelectionOutline(selection);
    }

    // 繪製目前可見的所有 Chunk。
    private void renderChunks(World world, Camera camera) {
        worldShader.use();
        worldShader.setMat4("uProjection", projection);
        worldShader.setMat4("uView", view);
        worldShader.setVec3("uFogColor", SKY_COLOR);
        worldShader.setVec3("uCameraPos", camera.position());
        worldShader.setFloat("uFogNear", 70.0f);
        worldShader.setFloat("uFogFar", 250.0f);
        worldShader.setInt("uAtlas", 0);

        atlas.bind(0);

        // 根據相機位置找出目前所在的 Chunk。
        int centerChunkX = Math.floorDiv((int) Math.floor(camera.position().x), GameConfig.CHUNK_SIZE);
        int centerChunkZ = Math.floorDiv((int) Math.floor(camera.position().z), GameConfig.CHUNK_SIZE);

        int viewDistance = GameConfig.RENDER_DISTANCE_CHUNKS;
        int maxDistSq = viewDistance * viewDistance;

        visibleChunks.clear();

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

                Mesh mesh = chunkMeshes.get(key);

                // 如果這個 Chunk 還沒有 mesh，或 mesh 已過期，就重新建立。
                if (mesh == null || chunk.isMeshDirty()) {
                    float[] vertices = ChunkMesher.build(chunk, world, atlas);

                    if (mesh == null) {
                        // 頂點格式為位置 3、UV 2、光照 1。
                        mesh = new Mesh(vertices, GL_TRIANGLES, 3, 2, 1);
                        chunkMeshes.put(key, mesh);
                    } else {
                        mesh.update(vertices, ChunkMesher.STRIDE_FLOATS);
                    }

                    chunk.clearMeshDirty();
                }

                // 把 Chunk 放到它在世界中的正確位置再繪製。
                model.identity().translate(chunk.worldMinX(), 0.0f, chunk.worldMinZ());
                worldShader.setMat4("uModel", model);
                mesh.draw();
            }
        }

        // 把這一幀看不到的 Chunk mesh 釋放掉，減少資源占用。
        pruneChunkMeshes(visibleChunks);
    }

    // 刪除不在目前可見範圍內的 Chunk mesh。
    private void pruneChunkMeshes(Set<ChunkPos> visibleChunks) {
        Iterator<Map.Entry<ChunkPos, Mesh>> iterator = chunkMeshes.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<ChunkPos, Mesh> entry = iterator.next();

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

        // 稍微加粗線條，讓外框更清楚。
        glLineWidth(2.2f);
        selectionMesh.draw();
        glLineWidth(1.0f);
    }

    // 建立包住一個方塊的線框頂點資料。
    private float[] buildWireCube(int x, int y, int z) {
        // 稍微向外擴一點，避免和方塊表面重疊時閃爍。
        float minX = x - 0.0015f;
        float minY = y - 0.0015f;
        float minZ = z - 0.0015f;
        float maxX = x + 1.0015f;
        float maxY = y + 1.0015f;
        float maxZ = z + 1.0015f;

        return new float[] {
                // 底面四條邊
                minX, minY, minZ, maxX, minY, minZ,
                maxX, minY, minZ, maxX, minY, maxZ,
                maxX, minY, maxZ, minX, minY, maxZ,
                minX, minY, maxZ, minX, minY, minZ,

                // 上面四條邊
                minX, maxY, minZ, maxX, maxY, minZ,
                maxX, maxY, minZ, maxX, maxY, maxZ,
                maxX, maxY, maxZ, minX, maxY, maxZ,
                minX, maxY, maxZ, minX, maxY, minZ,

                // 四條垂直邊
                minX, minY, minZ, minX, maxY, minZ,
                maxX, minY, minZ, maxX, maxY, minZ,
                maxX, minY, maxZ, maxX, maxY, maxZ,
                minX, minY, maxZ, minX, maxY, maxZ
        };
    }

    // 釋放所有渲染資源。
    @Override
    public void close() {
        for (Mesh mesh : chunkMeshes.values()) {
            mesh.close();
        }
        chunkMeshes.clear();

        selectionMesh.close();
        worldShader.close();
        lineShader.close();
        atlas.close();
    }
}
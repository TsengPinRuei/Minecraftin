package com.minecraftin.clone.render;

import com.minecraftin.clone.config.GameConfig;
import com.minecraftin.clone.engine.Camera;
import com.minecraftin.clone.engine.Mesh;
import com.minecraftin.clone.engine.ShaderProgram;
import com.minecraftin.clone.engine.TextureAtlas;
import com.minecraftin.clone.util.FloatArrayBuilder;
import com.minecraftin.clone.world.Chunk;
import com.minecraftin.clone.world.ChunkMesher;
import com.minecraftin.clone.world.ChunkPos;
import com.minecraftin.clone.world.RaycastHit;
import com.minecraftin.clone.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.opengl.GL33C.*;

// 負責繪製整個世界，包括區塊內容與被瞄準方塊的外框。
// 這裡也擁有所有與世界渲染相關的 OpenGL 資源，Game 結束時必須呼叫 close()。
public final class WorldRenderer implements AutoCloseable {

    // 天空背景顏色。
    private static final Vector3f SKY_COLOR = new Vector3f(0.56f, 0.74f, 0.95f);

    // 選取方塊外框的顏色。
    private static final Vector3f SELECTION_COLOR = new Vector3f(0.03f, 0.03f, 0.03f);

    // 選取框用實際幾何體加粗，避免部分 OpenGL 驅動忽略 glLineWidth。
    private static final float SELECTION_OUTLINE_PADDING = 0.003f;
    private static final float SELECTION_OUTLINE_THICKNESS = 0.0092f;

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
    // Mesh 快取以 ChunkPos 為 key，只有 Chunk dirty 或首次可見時才重建頂點資料。
    private void renderChunks(World world, Camera camera) {
        Vector3f cameraPosition = camera.position();

        worldShader.use();
        worldShader.setMat4("uProjection", projection);
        worldShader.setMat4("uView", view);
        worldShader.setVec3("uFogColor", SKY_COLOR);
        worldShader.setVec3("uCameraPos", cameraPosition);
        worldShader.setFloat("uFogNear", 70.0f);
        worldShader.setFloat("uFogFar", 250.0f);
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
                visibleChunkOrder.add(new VisibleChunk(key, chunkDistanceSq(key, cameraPosition.x, cameraPosition.z)));

                ChunkMeshes meshes = chunkMeshes.get(key);

                // 如果這個 Chunk 還沒有 mesh，或 mesh 已過期，就重新建立。
                if (meshes == null || chunk.isMeshDirty()) {
                    ChunkMesher.MeshData meshData = ChunkMesher.build(chunk, world, atlas);

                    if (meshes == null) {
                        // 頂點格式為位置 3、UV 2、光照 1。
                        meshes = new ChunkMeshes(
                                new Mesh(meshData.opaqueVertices(), GL_TRIANGLES, 3, 2, 1),
                                new Mesh(meshData.translucentVertices(), GL_TRIANGLES, 3, 2, 1));
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

        selectionMesh.draw();
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
        worldShader.close();
        lineShader.close();
        atlas.close();
    }

    // 可見 Chunk 與它到相機的距離快取，避免透明排序時重複計算同一個距離。
    private record VisibleChunk(ChunkPos pos, float distanceSq) {
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

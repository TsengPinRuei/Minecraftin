// 說明：宣告此檔案所屬的套件。
package com.minecraftin.clone.render;

// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.config.GameConfig;
// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.engine.Camera;
// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.engine.Mesh;
// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.engine.ShaderProgram;
// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.engine.TextureAtlas;
// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.world.Chunk;
// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.world.ChunkMesher;
// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.world.ChunkPos;
// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.world.RaycastHit;
// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.world.World;
// 說明：匯入後續會使用到的型別或函式。
import org.joml.Matrix4f;
// 說明：匯入後續會使用到的型別或函式。
import org.joml.Vector3f;

// 說明：匯入後續會使用到的型別或函式。
import java.util.HashMap;
// 說明：匯入後續會使用到的型別或函式。
import java.util.HashSet;
// 說明：匯入後續會使用到的型別或函式。
import java.util.Iterator;
// 說明：匯入後續會使用到的型別或函式。
import java.util.Map;
// 說明：匯入後續會使用到的型別或函式。
import java.util.Set;

// 說明：匯入後續會使用到的型別或函式。
import static org.lwjgl.opengl.GL33C.*;

// 說明：定義主要型別與其結構。
public final class WorldRenderer implements AutoCloseable {
    // 說明：設定或更新變數的值。
    private static final Vector3f SKY_COLOR = new Vector3f(0.56f, 0.74f, 0.95f);
    // 說明：設定或更新變數的值。
    private static final Vector3f SELECTION_COLOR = new Vector3f(0.03f, 0.03f, 0.03f);

    // 說明：下一行程式碼負責執行目前步驟。
    private final TextureAtlas atlas;
    // 說明：下一行程式碼負責執行目前步驟。
    private final ShaderProgram worldShader;
    // 說明：下一行程式碼負責執行目前步驟。
    private final ShaderProgram lineShader;

    // 說明：設定或更新變數的值。
    private final Map<ChunkPos, Mesh> chunkMeshes = new HashMap<>();
    // 說明：設定或更新變數的值。
    private final Set<ChunkPos> visibleChunks = new HashSet<>();

    // 說明：設定或更新變數的值。
    private final Matrix4f projection = new Matrix4f();
    // 說明：設定或更新變數的值。
    private final Matrix4f view = new Matrix4f();
    // 說明：設定或更新變數的值。
    private final Matrix4f model = new Matrix4f();

    // 說明：下一行程式碼負責執行目前步驟。
    private final Mesh selectionMesh;
    // 說明：設定或更新變數的值。
    private int lastSelectionX = Integer.MIN_VALUE;
    // 說明：設定或更新變數的值。
    private int lastSelectionY = Integer.MIN_VALUE;
    // 說明：設定或更新變數的值。
    private int lastSelectionZ = Integer.MIN_VALUE;

    // 說明：定義對外可呼叫的方法。
    public WorldRenderer() {
        // 說明：設定或更新變數的值。
        atlas = new TextureAtlas();
        // 說明：設定或更新變數的值。
        worldShader = new ShaderProgram("/shaders/world.vert", "/shaders/world.frag");
        // 說明：設定或更新變數的值。
        lineShader = new ShaderProgram("/shaders/line.vert", "/shaders/line.frag");
        // 說明：設定或更新變數的值。
        selectionMesh = new Mesh(new float[0], GL_LINES, 3);
    }

    // 說明：定義對外可呼叫的方法。
    public void render(World world, Camera camera, int width, int height, RaycastHit selection) {
        // 說明：呼叫方法執行對應功能。
        glViewport(0, 0, width, height);
        // 說明：呼叫方法執行對應功能。
        glClearColor(SKY_COLOR.x, SKY_COLOR.y, SKY_COLOR.z, 1.0f);
        // 說明：呼叫方法執行對應功能。
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // 說明：下一行程式碼負責執行目前步驟。
        projection.identity()
                // 說明：下一行程式碼負責執行目前步驟。
                .perspective((float) Math.toRadians(GameConfig.FOV_DEGREES), (float) width / Math.max(height, 1),
                        // 說明：下一行程式碼負責執行目前步驟。
                        GameConfig.NEAR_PLANE, GameConfig.FAR_PLANE);
        // 說明：呼叫方法執行對應功能。
        camera.viewMatrix(view);

        // 說明：呼叫方法執行對應功能。
        renderChunks(world, camera);
        // 說明：呼叫方法執行對應功能。
        renderSelectionOutline(selection);
    }

    // 說明：定義類別內部使用的方法。
    private void renderChunks(World world, Camera camera) {
        // 說明：呼叫方法執行對應功能。
        worldShader.use();
        // 說明：呼叫方法執行對應功能。
        worldShader.setMat4("uProjection", projection);
        // 說明：呼叫方法執行對應功能。
        worldShader.setMat4("uView", view);
        // 說明：呼叫方法執行對應功能。
        worldShader.setVec3("uFogColor", SKY_COLOR);
        // 說明：呼叫方法執行對應功能。
        worldShader.setVec3("uCameraPos", camera.position());
        // 說明：呼叫方法執行對應功能。
        worldShader.setFloat("uFogNear", 70.0f);
        // 說明：呼叫方法執行對應功能。
        worldShader.setFloat("uFogFar", 250.0f);
        // 說明：呼叫方法執行對應功能。
        worldShader.setInt("uAtlas", 0);

        // 說明：呼叫方法執行對應功能。
        atlas.bind(0);

        // 說明：宣告並初始化變數。
        int centerChunkX = Math.floorDiv((int) Math.floor(camera.position().x), GameConfig.CHUNK_SIZE);
        // 說明：宣告並初始化變數。
        int centerChunkZ = Math.floorDiv((int) Math.floor(camera.position().z), GameConfig.CHUNK_SIZE);

        // 說明：宣告並初始化變數。
        int viewDistance = GameConfig.RENDER_DISTANCE_CHUNKS;
        // 說明：宣告並初始化變數。
        int maxDistSq = viewDistance * viewDistance;
        // 說明：呼叫方法執行對應功能。
        visibleChunks.clear();

        // 說明：使用迴圈逐一處理每個元素或區間。
        for (int dz = -viewDistance; dz <= viewDistance; dz++) {
            // 說明：使用迴圈逐一處理每個元素或區間。
            for (int dx = -viewDistance; dx <= viewDistance; dx++) {
                // 說明：根據條件決定是否進入此邏輯分支。
                if (dx * dx + dz * dz > maxDistSq) {
                    // 說明：跳過本次迴圈剩餘邏輯，直接進入下一次迭代。
                    continue;
                }

                // 說明：宣告並初始化變數。
                int chunkX = centerChunkX + dx;
                // 說明：宣告並初始化變數。
                int chunkZ = centerChunkZ + dz;
                // 說明：宣告並初始化變數。
                Chunk chunk = world.getChunkIfLoaded(chunkX, chunkZ);
                // 說明：根據條件決定是否進入此邏輯分支。
                if (chunk == null) {
                    // 說明：跳過本次迴圈剩餘邏輯，直接進入下一次迭代。
                    continue;
                }

                // 說明：宣告並初始化變數。
                ChunkPos key = new ChunkPos(chunkX, chunkZ);
                // 說明：呼叫方法執行對應功能。
                visibleChunks.add(key);

                // 說明：宣告並初始化變數。
                Mesh mesh = chunkMeshes.get(key);
                // 說明：根據條件決定是否進入此邏輯分支。
                if (mesh == null || chunk.isMeshDirty()) {
                    // 說明：宣告並初始化變數。
                    float[] vertices = ChunkMesher.build(chunk, world, atlas);
                    // 說明：根據條件決定是否進入此邏輯分支。
                    if (mesh == null) {
                        // 說明：設定或更新變數的值。
                        mesh = new Mesh(vertices, GL_TRIANGLES, 3, 2, 1);
                        // 說明：呼叫方法執行對應功能。
                        chunkMeshes.put(key, mesh);
                    // 說明：下一行程式碼負責執行目前步驟。
                    } else {
                        // 說明：呼叫方法執行對應功能。
                        mesh.update(vertices, ChunkMesher.STRIDE_FLOATS);
                    }
                    // 說明：呼叫方法執行對應功能。
                    chunk.clearMeshDirty();
                }

                // 說明：呼叫方法執行對應功能。
                model.identity().translate(chunk.worldMinX(), 0.0f, chunk.worldMinZ());
                // 說明：呼叫方法執行對應功能。
                worldShader.setMat4("uModel", model);
                // 說明：呼叫方法執行對應功能。
                mesh.draw();
            }
        }

        // 說明：呼叫方法執行對應功能。
        pruneChunkMeshes(visibleChunks);
    }

    // 說明：定義類別內部使用的方法。
    private void pruneChunkMeshes(Set<ChunkPos> visibleChunks) {
        // 說明：設定或更新變數的值。
        Iterator<Map.Entry<ChunkPos, Mesh>> iterator = chunkMeshes.entrySet().iterator();
        // 說明：在條件成立時重複執行此區塊。
        while (iterator.hasNext()) {
            // 說明：設定或更新變數的值。
            Map.Entry<ChunkPos, Mesh> entry = iterator.next();
            // 說明：根據條件決定是否進入此邏輯分支。
            if (visibleChunks.contains(entry.getKey())) {
                // 說明：跳過本次迴圈剩餘邏輯，直接進入下一次迭代。
                continue;
            }
            // 說明：呼叫方法執行對應功能。
            entry.getValue().close();
            // 說明：呼叫方法執行對應功能。
            iterator.remove();
        }
    }

    // 說明：定義類別內部使用的方法。
    private void renderSelectionOutline(RaycastHit hit) {
        // 說明：根據條件決定是否進入此邏輯分支。
        if (hit == null) {
            // 說明：設定或更新變數的值。
            lastSelectionX = Integer.MIN_VALUE;
            // 說明：設定或更新變數的值。
            lastSelectionY = Integer.MIN_VALUE;
            // 說明：設定或更新變數的值。
            lastSelectionZ = Integer.MIN_VALUE;
            // 說明：下一行程式碼負責執行目前步驟。
            return;
        }

        // 說明：根據條件決定是否進入此邏輯分支。
        if (hit.x() != lastSelectionX || hit.y() != lastSelectionY || hit.z() != lastSelectionZ) {
            // 說明：宣告並初始化變數。
            float[] vertices = buildWireCube(hit.x(), hit.y(), hit.z());
            // 說明：呼叫方法執行對應功能。
            selectionMesh.update(vertices, 3);
            // 說明：設定或更新變數的值。
            lastSelectionX = hit.x();
            // 說明：設定或更新變數的值。
            lastSelectionY = hit.y();
            // 說明：設定或更新變數的值。
            lastSelectionZ = hit.z();
        }

        // 說明：呼叫方法執行對應功能。
        lineShader.use();
        // 說明：呼叫方法執行對應功能。
        lineShader.setMat4("uProjection", projection);
        // 說明：呼叫方法執行對應功能。
        lineShader.setMat4("uView", view);
        // 說明：呼叫方法執行對應功能。
        model.identity();
        // 說明：呼叫方法執行對應功能。
        lineShader.setMat4("uModel", model);
        // 說明：呼叫方法執行對應功能。
        lineShader.setVec3("uColor", SELECTION_COLOR);

        // 說明：呼叫方法執行對應功能。
        glLineWidth(2.2f);
        // 說明：呼叫方法執行對應功能。
        selectionMesh.draw();
        // 說明：呼叫方法執行對應功能。
        glLineWidth(1.0f);
    }

    // 說明：定義類別內部使用的方法。
    private float[] buildWireCube(int x, int y, int z) {
        // 說明：宣告並初始化變數。
        float minX = x - 0.0015f;
        // 說明：宣告並初始化變數。
        float minY = y - 0.0015f;
        // 說明：宣告並初始化變數。
        float minZ = z - 0.0015f;
        // 說明：宣告並初始化變數。
        float maxX = x + 1.0015f;
        // 說明：宣告並初始化變數。
        float maxY = y + 1.0015f;
        // 說明：宣告並初始化變數。
        float maxZ = z + 1.0015f;

        // 說明：下一行程式碼負責執行目前步驟。
        return new float[]{
                // 說明：下一行程式碼負責執行目前步驟。
                minX, minY, minZ, maxX, minY, minZ,
                // 說明：下一行程式碼負責執行目前步驟。
                maxX, minY, minZ, maxX, minY, maxZ,
                // 說明：下一行程式碼負責執行目前步驟。
                maxX, minY, maxZ, minX, minY, maxZ,
                // 說明：下一行程式碼負責執行目前步驟。
                minX, minY, maxZ, minX, minY, minZ,

                // 說明：下一行程式碼負責執行目前步驟。
                minX, maxY, minZ, maxX, maxY, minZ,
                // 說明：下一行程式碼負責執行目前步驟。
                maxX, maxY, minZ, maxX, maxY, maxZ,
                // 說明：下一行程式碼負責執行目前步驟。
                maxX, maxY, maxZ, minX, maxY, maxZ,
                // 說明：下一行程式碼負責執行目前步驟。
                minX, maxY, maxZ, minX, maxY, minZ,

                // 說明：下一行程式碼負責執行目前步驟。
                minX, minY, minZ, minX, maxY, minZ,
                // 說明：下一行程式碼負責執行目前步驟。
                maxX, minY, minZ, maxX, maxY, minZ,
                // 說明：下一行程式碼負責執行目前步驟。
                maxX, minY, maxZ, maxX, maxY, maxZ,
                // 說明：下一行程式碼負責執行目前步驟。
                minX, minY, maxZ, minX, maxY, maxZ
        };
    }

    // 說明：宣告註解標記，提供編譯器或框架額外資訊。
    @Override
    // 說明：定義對外可呼叫的方法。
    public void close() {
        // 說明：使用迴圈逐一處理每個元素或區間。
        for (Mesh mesh : chunkMeshes.values()) {
            // 說明：呼叫方法執行對應功能。
            mesh.close();
        }
        // 說明：呼叫方法執行對應功能。
        chunkMeshes.clear();

        // 說明：呼叫方法執行對應功能。
        selectionMesh.close();
        // 說明：呼叫方法執行對應功能。
        worldShader.close();
        // 說明：呼叫方法執行對應功能。
        lineShader.close();
        // 說明：呼叫方法執行對應功能。
        atlas.close();
    }
}

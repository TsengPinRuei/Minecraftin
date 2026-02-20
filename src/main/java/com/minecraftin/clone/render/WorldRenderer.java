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

public final class WorldRenderer implements AutoCloseable {
    private static final Vector3f SKY_COLOR = new Vector3f(0.56f, 0.74f, 0.95f);
    private static final Vector3f SELECTION_COLOR = new Vector3f(0.03f, 0.03f, 0.03f);

    private final TextureAtlas atlas;
    private final ShaderProgram worldShader;
    private final ShaderProgram lineShader;

    private final Map<ChunkPos, Mesh> chunkMeshes = new HashMap<>();
    private final Set<ChunkPos> visibleChunks = new HashSet<>();

    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f view = new Matrix4f();
    private final Matrix4f model = new Matrix4f();

    private final Mesh selectionMesh;
    private int lastSelectionX = Integer.MIN_VALUE;
    private int lastSelectionY = Integer.MIN_VALUE;
    private int lastSelectionZ = Integer.MIN_VALUE;

    public WorldRenderer() {
        atlas = new TextureAtlas();
        worldShader = new ShaderProgram("/shaders/world.vert", "/shaders/world.frag");
        lineShader = new ShaderProgram("/shaders/line.vert", "/shaders/line.frag");
        selectionMesh = new Mesh(new float[0], GL_LINES, 3);
    }

    public void render(World world, Camera camera, int width, int height, RaycastHit selection) {
        glViewport(0, 0, width, height);
        glClearColor(SKY_COLOR.x, SKY_COLOR.y, SKY_COLOR.z, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        projection.identity()
                .perspective((float) Math.toRadians(GameConfig.FOV_DEGREES), (float) width / Math.max(height, 1),
                        GameConfig.NEAR_PLANE, GameConfig.FAR_PLANE);
        camera.viewMatrix(view);

        renderChunks(world, camera);
        renderSelectionOutline(selection);
    }

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

        int centerChunkX = Math.floorDiv((int) Math.floor(camera.position().x), GameConfig.CHUNK_SIZE);
        int centerChunkZ = Math.floorDiv((int) Math.floor(camera.position().z), GameConfig.CHUNK_SIZE);

        int viewDistance = GameConfig.RENDER_DISTANCE_CHUNKS;
        int maxDistSq = viewDistance * viewDistance;
        visibleChunks.clear();

        for (int dz = -viewDistance; dz <= viewDistance; dz++) {
            for (int dx = -viewDistance; dx <= viewDistance; dx++) {
                if (dx * dx + dz * dz > maxDistSq) {
                    continue;
                }

                int chunkX = centerChunkX + dx;
                int chunkZ = centerChunkZ + dz;
                Chunk chunk = world.getChunkIfLoaded(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }

                ChunkPos key = new ChunkPos(chunkX, chunkZ);
                visibleChunks.add(key);

                Mesh mesh = chunkMeshes.get(key);
                if (mesh == null || chunk.isMeshDirty()) {
                    float[] vertices = ChunkMesher.build(chunk, world, atlas);
                    if (mesh == null) {
                        mesh = new Mesh(vertices, GL_TRIANGLES, 3, 2, 1);
                        chunkMeshes.put(key, mesh);
                    } else {
                        mesh.update(vertices, ChunkMesher.STRIDE_FLOATS);
                    }
                    chunk.clearMeshDirty();
                }

                model.identity().translate(chunk.worldMinX(), 0.0f, chunk.worldMinZ());
                worldShader.setMat4("uModel", model);
                mesh.draw();
            }
        }

        pruneChunkMeshes(visibleChunks);
    }

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

    private void renderSelectionOutline(RaycastHit hit) {
        if (hit == null) {
            lastSelectionX = Integer.MIN_VALUE;
            lastSelectionY = Integer.MIN_VALUE;
            lastSelectionZ = Integer.MIN_VALUE;
            return;
        }

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

        glLineWidth(2.2f);
        selectionMesh.draw();
        glLineWidth(1.0f);
    }

    private float[] buildWireCube(int x, int y, int z) {
        float minX = x - 0.0015f;
        float minY = y - 0.0015f;
        float minZ = z - 0.0015f;
        float maxX = x + 1.0015f;
        float maxY = y + 1.0015f;
        float maxZ = z + 1.0015f;

        return new float[]{
                minX, minY, minZ, maxX, minY, minZ,
                maxX, minY, minZ, maxX, minY, maxZ,
                maxX, minY, maxZ, minX, minY, maxZ,
                minX, minY, maxZ, minX, minY, minZ,

                minX, maxY, minZ, maxX, maxY, minZ,
                maxX, maxY, minZ, maxX, maxY, maxZ,
                maxX, maxY, maxZ, minX, maxY, maxZ,
                minX, maxY, maxZ, minX, maxY, minZ,

                minX, minY, minZ, minX, maxY, minZ,
                maxX, minY, minZ, maxX, maxY, minZ,
                maxX, minY, maxZ, maxX, maxY, maxZ,
                minX, minY, maxZ, minX, maxY, maxZ
        };
    }

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

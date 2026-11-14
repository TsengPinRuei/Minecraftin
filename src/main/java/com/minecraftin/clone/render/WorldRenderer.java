package com.minecraftin.clone.render;

import com.minecraftin.clone.config.GameConfig;
import com.minecraftin.clone.engine.Camera;
import com.minecraftin.clone.engine.Mesh;
import com.minecraftin.clone.engine.ResourceScope;
import com.minecraftin.clone.engine.ShaderProgram;
import com.minecraftin.clone.engine.TextureAtlas;
import com.minecraftin.clone.util.FloatArrayBuilder;
import com.minecraftin.clone.world.BlockType;
import com.minecraftin.clone.world.BlockBounds;
import com.minecraftin.clone.world.Chunk;
import com.minecraftin.clone.world.ChunkMesher;
import com.minecraftin.clone.world.ChunkPos;
import com.minecraftin.clone.world.ChunkSnapshot;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.lwjgl.opengl.GL33C.*;

// Own world-rendering GPU resources and background mesh jobs.
// Use this renderer on the context-owning thread and close it before destroying the window.
public final class WorldRenderer implements AutoCloseable {

    // Blend sky colors over the day/night cycle.
    private static final Vector3f DAY_SKY_COLOR = new Vector3f(0.56f, 0.74f, 0.95f);
    private static final Vector3f NIGHT_SKY_COLOR = new Vector3f(0.015f, 0.025f, 0.07f);
    private static final Vector3f SUNSET_SKY_COLOR = new Vector3f(0.98f, 0.52f, 0.30f);

    // Keep a minimum sky-light multiplier at night so exposed terrain remains visible.
    private static final float MOONLIGHT_FLOOR = 0.13f;

    // Fade toward the render radius to soften distant chunk boundaries; fog is based on each
    // fragment's distance.
    private static final float FOG_FAR = GameConfig.RENDER_DISTANCE_CHUNKS * GameConfig.CHUNK_SIZE;
    private static final float FOG_NEAR = FOG_FAR * 0.6f;

    // Move square sun and moon billboards around the camera in the X-Y plane.
    private static final float SKY_BODY_DISTANCE = 420.0f;
    private static final float SUN_HALF_SIZE = 36.0f;
    private static final float MOON_HALF_SIZE = 24.0f;
    private static final Vector3f SUN_COLOR = new Vector3f(1.0f, 0.97f, 0.82f);
    private static final Vector3f MOON_COLOR = new Vector3f(0.88f, 0.90f, 0.98f);

    // Generate flat cloud cells from seeded noise and drift the layer toward positive X.
    private static final float CLOUD_Y = 150.0f;
    private static final float CLOUD_CELL_SIZE = 12.0f;
    private static final int CLOUD_RADIUS_CELLS = 30;
    private static final float CLOUD_DRIFT_SPEED = 0.7f;
    private static final Vector3f CLOUD_COLOR = new Vector3f(1.0f, 1.0f, 1.0f);
    private static final float CLOUD_ALPHA = 0.72f;

    private static final Vector3f SELECTION_COLOR = new Vector3f(0.03f, 0.03f, 0.03f);

    // Use thin boxes for selection outlines so thickness does not depend on glLineWidth support.
    private static final float SELECTION_OUTLINE_PADDING = 0.003f;
    private static final float SELECTION_OUTLINE_THICKNESS = 0.0092f;

    // Bound debris count and lifetime to keep block breaking from accumulating particles.
    private static final int BREAK_PARTICLES_PER_BLOCK = 28;
    private static final int MAX_BREAK_PARTICLES = 512;
    private static final float BREAK_PARTICLE_GRAVITY = 5.2f;

    // Cache Face.values() because it returns a new array on each call.
    private static final Face[] FACES = Face.values();

    // Derive a conservative chunk box from every supported render shape, including overhangs.
    static final BlockBounds CHUNK_RENDER_BOUNDS = chunkRenderBounds();

    private static BlockBounds chunkRenderBounds() {
        float minX = 0, minY = 0, minZ = 0, maxX = 1, maxY = 1, maxZ = 1;
        for (BlockType block : BlockType.values()) {
            for (BlockBounds bounds : block.renderBoxes()) {
                minX = Math.min(minX, bounds.minX());
                minY = Math.min(minY, bounds.minY());
                minZ = Math.min(minZ, bounds.minZ());
                maxX = Math.max(maxX, bounds.maxX());
                maxY = Math.max(maxY, bounds.maxY());
                maxZ = Math.max(maxZ, bounds.maxZ());
            }
        }
        return new BlockBounds(minX, minY, minZ, GameConfig.CHUNK_SIZE - 1 + maxX,
                GameConfig.CHUNK_HEIGHT - 1 + maxY, GameConfig.CHUNK_SIZE - 1 + maxZ);
    }

    static boolean chunkVisible(FrustumIntersection frustum, int worldMinX, int worldMinZ) {
        BlockBounds bounds = CHUNK_RENDER_BOUNDS;
        return frustum.testAab(worldMinX + bounds.minX(), bounds.minY(), worldMinZ + bounds.minZ(),
                worldMinX + bounds.maxX(), bounds.maxY(), worldMinZ + bounds.maxZ());
    }

    private final TextureAtlas atlas;

    private final ShaderProgram worldShader;

    private final ShaderProgram lineShader;

    // Cache separate opaque and translucent meshes for each chunk.
    private final Map<ChunkPos, ChunkMeshes> chunkMeshes = new HashMap<>();

    private final Set<ChunkPos> visibleChunks = new HashSet<>();

    private final List<ChunkPos> visibleChunkOrder = new ArrayList<>();

    // Retain transparent faces only for cached chunks, bounded by the existing render radius.
    // Rebuild one sorted GPU batch when the camera, visible geometry, or debris changes.
    private final Mesh translucentMesh;
    private final List<TransparentFace> translucentFaces = new ArrayList<>();
    private final List<TransparentFace> particleTranslucentFaces = new ArrayList<>();
    private final Matrix4f cachedTransparencyProjectionView = new Matrix4f();
    private boolean transparencyDirty = true;

    private final Matrix4f projection = new Matrix4f();

    private final Matrix4f view = new Matrix4f();

    private final Matrix4f model = new Matrix4f();

    private final Mesh selectionMesh;

    // Batch block debris into one dynamic mesh rebuilt after particle updates.
    private final Mesh breakParticleMesh;
    private final List<BreakParticle> breakParticles = new ArrayList<>();
    private final Random particleRandom = new Random();
    private boolean breakParticleMeshDirty;

    // Reuse one mesh for sun and moon, uploading each body's vertices before drawing it.
    private final Mesh skyBodyMesh;

    // Rebuild clouds when the camera crosses a cell in drift-adjusted cloud coordinates.
    private final Mesh cloudMesh;
    private int cachedCloudCellX = Integer.MIN_VALUE;
    private int cachedCloudCellZ = Integer.MIN_VALUE;
    private float cloudDrift;

    // Frustum culling affects drawing and build requests, but does not evict meshes still inside
    // render distance.
    private final FrustumIntersection frustum = new FrustumIntersection();
    private final Matrix4f projViewMatrix = new Matrix4f();

    // Workers build CPU vertex arrays; the context-owning thread processes at most 16 completed
    // results per frame.
    private static final int MAX_MESH_UPLOADS_PER_FRAME = 16;
    private final ExecutorService meshExecutor;
    private final int maxPendingMeshBuilds;
    private final Set<ChunkPos> pendingMeshBuilds = new HashSet<>();
    private final ConcurrentLinkedQueue<MeshBuildResult> completedMeshBuilds = new ConcurrentLinkedQueue<>();
    private final List<MeshBuildCandidate> meshBuildCandidates = new ArrayList<>();

    // Return either CPU mesh data or the original worker failure for main-thread handling.
    private record MeshBuildResult(ChunkPos pos, ChunkMesher.MeshData data, Throwable failure) {
    }

    // Sort build candidates by distance so nearby chunks are submitted first.
    private record MeshBuildCandidate(Chunk chunk, ChunkPos pos, float distanceSq) {
    }

    private final Vector3f skyColor = new Vector3f(DAY_SKY_COLOR);
    private float dayLight = 1.0f;

    // Cache both the target coordinates and block variant so shape changes rebuild the outline.
    private int lastSelectionX = Integer.MIN_VALUE;
    private int lastSelectionY = Integer.MIN_VALUE;
    private int lastSelectionZ = Integer.MIN_VALUE;
    private BlockType lastSelectionType;

    public WorldRenderer() {
        int workers = Math.max(1, Math.min(3, Runtime.getRuntime().availableProcessors() - 2));
        maxPendingMeshBuilds = workers * 2;
        try (ResourceScope resources = new ResourceScope()) {
            atlas = resources.add(new TextureAtlas());
            worldShader = resources.add(new ShaderProgram("/shaders/world.vert", "/shaders/world.frag"));
            lineShader = resources.add(new ShaderProgram("/shaders/line.vert", "/shaders/line.frag"));
            selectionMesh = resources.add(new Mesh(new float[0], GL_TRIANGLES, 3));
            breakParticleMesh = resources.add(new Mesh(new float[0], GL_TRIANGLES, 3, 2, 3));
            translucentMesh = resources.add(new Mesh(new float[0], GL_TRIANGLES, 3, 2, 3));
            skyBodyMesh = resources.add(new Mesh(new float[0], GL_TRIANGLES, 3));
            cloudMesh = resources.add(new Mesh(new float[0], GL_TRIANGLES, 3));
            meshExecutor = Executors.newFixedThreadPool(workers, runnable -> {
                Thread thread = new Thread(runnable, "chunk-mesher");
                thread.setDaemon(true);
                return thread;
            });
            resources.release();
        }
    }

    public TextureAtlas atlas() {
        return atlas;
    }

    // Advance cloud drift and debris lifetimes using simulation time.
    public void update(float deltaSeconds) {
        if (deltaSeconds <= 0.0f) {
            return;
        }

        // Use accumulated simulation time for cloud drift so the day/night wrap does not reset
        // cloud positions.
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

    // Freeze each particle's sky and block light from the brightest nearby samples when it is
    // spawned.
    public void spawnBlockBreakEffect(World world, BlockType block, int x, int y, int z) {
        if (block == BlockType.AIR) {
            return;
        }

        float skyLight = neighborMaxSkyLight(world, x, y, z) / (float) 15;
        float blockLight = neighborMaxBlockLight(world, x, y, z) / (float) 15;

        int excess = breakParticles.size() + BREAK_PARTICLES_PER_BLOCK - MAX_BREAK_PARTICLES;
        if (excess > 0) {
            breakParticles.subList(0, excess).clear();
        }

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

            Face face = FACES[particleRandom.nextInt(FACES.length)];
            int tile = block.tileForFace(face);
            float tileU0 = TextureAtlas.u0(tile);
            float tileV0 = TextureAtlas.v0(tile);
            float tileU1 = TextureAtlas.u1(tile);
            float tileV1 = TextureAtlas.v1(tile);
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
                    skyLight, blockLight, block.isTranslucent()));
        }

        breakParticleMeshDirty = true;
    }

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

    public void render(World world, Camera camera, int width, int height, RaycastHit selection) {
        updateDayCycle(world.timeOfDay());

        glViewport(0, 0, width, height);

        glClearColor(skyColor.x, skyColor.y, skyColor.z, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        projection.identity()
                .perspective(
                        (float) Math.toRadians(GameConfig.FOV_DEGREES),
                        (float) width / Math.max(height, 1),
                        GameConfig.NEAR_PLANE,
                        GameConfig.FAR_PLANE);

        camera.viewMatrix(view);
        projViewMatrix.set(projection).mul(view);
        frustum.set(projViewMatrix);

        // Draw sky bodies first without depth writes so terrain drawn afterward can cover them.
        renderSkyBodies(camera, world.timeOfDay());
        renderChunks(world, camera);
        renderBreakParticles(camera);
        // All legal world geometry and its short-lived debris remain below the cloud plane.
        // From below the plane clouds are behind the world; from above they are in front.
        if (cloudsBehindWorld(camera.position().y)) renderClouds(world, camera);
        renderTranslucent(camera);
        if (!cloudsBehindWorld(camera.position().y)) renderClouds(world, camera);
        renderSelectionOutline(selection);
    }

    private void updateDayCycle(float timeOfDay) {
        // Time zero is sunrise, one quarter is noon, and one half is sunset.
        float elevation = (float) Math.sin(timeOfDay * Math.PI * 2.0);

        // Blend daylight near the horizon to create a short dawn/dusk transition.
        float daylight = clamp((elevation + 0.15f) / 0.30f, 0.0f, 1.0f);
        dayLight = MOONLIGHT_FLOOR + (1.0f - MOONLIGHT_FLOOR) * daylight;

        skyColor.set(NIGHT_SKY_COLOR).lerp(DAY_SKY_COLOR, daylight);

        float sunsetStrength = clamp(1.0f - Math.abs(elevation) / 0.15f, 0.0f, 1.0f);
        skyColor.lerp(SUNSET_SKY_COLOR, sunsetStrength * 0.45f);
    }

    // Draw sky bodies without depth writes so later terrain can occlude them.
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

        lineShader.use();
        lineShader.setMat4("uProjection", projection);
        lineShader.setMat4("uView", view);
        model.identity();
        lineShader.setMat4("uModel", model);

        glDepthMask(false);

        if (sunVisible && moonVisible) {
            // Use separate draws when both bodies are visible because their uniform colors differ.
            float[] vertices = out.toArray();
            float[] sunVertices = new float[18];
            float[] moonVertices = new float[18];
            System.arraycopy(vertices, 0, sunVertices, 0, 18);
            System.arraycopy(vertices, 18, moonVertices, 0, 18);

            // Draw each body immediately after uploading it; the next upload replaces this shared
            // mesh.
            skyBodyMesh.update(sunVertices);
            lineShader.setVec3("uColor", SUN_COLOR);
            lineShader.setFloat("uAlpha", 1.0f);
            skyBodyMesh.draw();

            skyBodyMesh.update(moonVertices);
            lineShader.setVec3("uColor", MOON_COLOR);
            skyBodyMesh.draw();
        } else {
            skyBodyMesh.update(out.toArray());
            lineShader.setVec3("uColor", sunVisible ? SUN_COLOR : MOON_COLOR);
            lineShader.setFloat("uAlpha", 1.0f);
            skyBodyMesh.draw();
        }

        glDepthMask(true);
    }

    private void addSkyBodyQuad(FloatArrayBuilder out, Vector3f cameraPosition, float dirX, float dirY,
            float halfSize) {
        float centerX = cameraPosition.x + dirX * SKY_BODY_DISTANCE;
        float centerY = cameraPosition.y + dirY * SKY_BODY_DISTANCE;
        float centerZ = cameraPosition.z;

        // Use the orbit tangent and Z axis to form a billboard perpendicular to the sun direction.
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

    private void renderClouds(World world, Camera camera) {
        // Build clouds in drift-adjusted coordinates and apply the drift through the model matrix.
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

                // The noise threshold controls cloud coverage while preserving repeatable shapes
                // for this seed.
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

        cloudMesh.update(out.toArray());
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    // Build vertex data on workers and upload/draw it on the context-owning thread.
    // New chunks wait for their first mesh; dirty chunks continue displaying their last uploaded
    // mesh.
    private void renderChunks(World world, Camera camera) {
        Vector3f cameraPosition = camera.position();

        drainCompletedMeshBuilds(world, cameraPosition);

        worldShader.use();
        worldShader.setMat4("uProjection", projection);
        worldShader.setMat4("uView", view);
        worldShader.setVec3("uFogColor", skyColor);
        worldShader.setVec3("uCameraPos", cameraPosition);
        worldShader.setFloat("uFogNear", FOG_NEAR);
        worldShader.setFloat("uFogFar", FOG_FAR);
        worldShader.setFloat("uDayLight", dayLight);
        worldShader.setInt("uAtlas", 0);

        worldShader.setInt("uOpaquePass", 1);
        atlas.bind(0);

        int centerChunkX = Math.floorDiv((int) Math.floor(cameraPosition.x), GameConfig.CHUNK_SIZE);
        int centerChunkZ = Math.floorDiv((int) Math.floor(cameraPosition.z), GameConfig.CHUNK_SIZE);

        int viewDistance = GameConfig.RENDER_DISTANCE_CHUNKS;
        int maxDistSq = viewDistance * viewDistance;

        visibleChunks.clear();
        visibleChunkOrder.clear();
        meshBuildCandidates.clear();

        for (int dz = -viewDistance; dz <= viewDistance; dz++) {
            for (int dx = -viewDistance; dx <= viewDistance; dx++) {
                // Use a circular chunk radius to match loading and completed-result distance
                // checks.
                if (dx * dx + dz * dz > maxDistSq) {
                    continue;
                }

                int chunkX = centerChunkX + dx;
                int chunkZ = centerChunkZ + dz;

                Chunk chunk = world.getChunkIfLoaded(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }

                ChunkPos key = chunk.pos();
                // Keep in-range chunks in the retention set even when the frustum skips their
                // drawing and build requests.
                visibleChunks.add(key);

                int worldMinX = chunkX * GameConfig.CHUNK_SIZE;
                int worldMinZ = chunkZ * GameConfig.CHUNK_SIZE;
                if (!chunkVisible(frustum, worldMinX, worldMinZ)) {
                    continue;
                }

                float distanceSq = chunkDistanceSq(key, cameraPosition.x, cameraPosition.z);
                visibleChunkOrder.add(key);

                ChunkMeshes meshes = chunkMeshes.get(key);

                // Queue missing or dirty meshes only if the chunk has no pending build.
                if ((meshes == null || chunk.isMeshDirty()) && !pendingMeshBuilds.contains(key)) {
                    meshBuildCandidates.add(new MeshBuildCandidate(chunk, key, distanceSq));
                }

                // A chunk with no uploaded mesh remains hidden until a worker result is accepted.
                if (meshes == null) {
                    continue;
                }

                model.identity().translate(chunk.worldMinX(), 0.0f, chunk.worldMinZ());
                worldShader.setMat4("uModel", model);
                meshes.opaque.draw();
            }
        }

        // Submit nearer chunks first.
        submitMeshBuilds(world);

        // Release meshes outside the retained render radius; off-screen meshes inside the radius
        // remain cached.
        // World keeps the corresponding block data.
        pruneChunkMeshes(visibleChunks);
    }

    // Capture data and clear meshDirty before submitting work on the game thread.
    // Later changes set meshDirty again, causing the stale result to be discarded and rebuilt.
    private void submitMeshBuilds(World world) {
        if (meshBuildCandidates.isEmpty()) {
            return;
        }

        meshBuildCandidates.sort((a, b) -> Float.compare(a.distanceSq(), b.distanceSq()));

        for (MeshBuildCandidate candidate : meshBuildCandidates) {
            Chunk chunk = candidate.chunk();
            ChunkPos key = candidate.pos();

            // Keep completed results counted as pending until the main thread consumes them.
            // This limit bounds retained snapshots and CPU mesh results as well as queued jobs.
            if (pendingMeshBuilds.size() >= maxPendingMeshBuilds) {
                break;
            }
            ChunkSnapshot snapshot = ChunkSnapshot.capture(chunk, world);
            chunk.clearMeshDirty();
            pendingMeshBuilds.add(key);
            meshExecutor.execute(() -> {
                try {
                    completedMeshBuilds.add(new MeshBuildResult(key, ChunkMesher.build(snapshot), null));
                } catch (Throwable failure) {
                    if (!meshExecutor.isShutdown()) {
                        completedMeshBuilds.add(new MeshBuildResult(key, null, failure));
                    }
                }
            });
        }

        meshBuildCandidates.clear();
    }

    // Consume completed results and upload accepted data only on the thread holding the OpenGL
    // context.
    private void drainCompletedMeshBuilds(World world, Vector3f cameraPosition) {
        int centerX = Math.floorDiv((int) Math.floor(cameraPosition.x), GameConfig.CHUNK_SIZE);
        int centerZ = Math.floorDiv((int) Math.floor(cameraPosition.z), GameConfig.CHUNK_SIZE);
        for (int processed = 0; processed < MAX_MESH_UPLOADS_PER_FRAME; processed++) {
            MeshBuildResult result = completedMeshBuilds.poll();
            if (result == null) {
                return;
            }
            pendingMeshBuilds.remove(result.pos());
            if (result.failure() != null) {
                throw new IllegalStateException("Failed to build chunk mesh " + result.pos(), result.failure());
            }
            Chunk chunk = world.getChunkIfLoaded(result.pos().x(), result.pos().z());
            long dx = (long) result.pos().x() - centerX;
            long dz = (long) result.pos().z() - centerZ;
            // Discard results for changed, missing, or out-of-range chunks.
            // A dirty flag or missing cached mesh triggers a future rebuild when the chunk is
            // needed.
            if (chunk == null || chunk.isMeshDirty()
                    || dx * dx + dz * dz > GameConfig.RENDER_DISTANCE_CHUNKS * GameConfig.RENDER_DISTANCE_CHUNKS) {
                continue;
            }
            transparencyDirty = true;
            ChunkMeshes meshes = chunkMeshes.get(result.pos());
            if (meshes == null) {
                try (ResourceScope resources = new ResourceScope()) {
                    Mesh opaque = resources.add(new Mesh(result.data().opaqueVertices(), GL_TRIANGLES, 3, 2, 3));
                    chunkMeshes.put(result.pos(), new ChunkMeshes(opaque,
                            transparentFaces(result.data().translucentVertices(), chunk.worldMinX(), chunk.worldMinZ())));
                    resources.release();
                }
            } else {
                meshes.opaque.update(result.data().opaqueVertices());
                meshes.translucent = transparentFaces(result.data().translucentVertices(),
                        chunk.worldMinX(), chunk.worldMinZ());
            }
        }
    }

    // Prioritize chunk builds by horizontal distance to their centers.
    private float chunkDistanceSq(ChunkPos pos, float cameraX, float cameraZ) {
        float chunkCenterX = pos.x() * GameConfig.CHUNK_SIZE + GameConfig.CHUNK_SIZE * 0.5f;
        float chunkCenterZ = pos.z() * GameConfig.CHUNK_SIZE + GameConfig.CHUNK_SIZE * 0.5f;
        float dx = chunkCenterX - cameraX;
        float dz = chunkCenterZ - cameraZ;
        return dx * dx + dz * dz;
    }

    // Evict meshes outside the in-range retention set, including chunks no longer loaded.
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

    private void renderSelectionOutline(RaycastHit hit) {
        if (hit == null) {
            lastSelectionX = Integer.MIN_VALUE;
            lastSelectionY = Integer.MIN_VALUE;
            lastSelectionZ = Integer.MIN_VALUE;
            return;
        }

        // Rebuild when coordinates or block variant change, including door state changes at the
        // same position.
        if (hit.x() != lastSelectionX || hit.y() != lastSelectionY || hit.z() != lastSelectionZ || hit.block() != lastSelectionType) {
            float[] vertices = buildSelectionOutline(hit);
            selectionMesh.update(vertices);

            lastSelectionX = hit.x();
            lastSelectionY = hit.y();
            lastSelectionZ = hit.z();
            lastSelectionType = hit.block();
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

    // Reuse the world shader so debris shares atlas textures, face shading, and fog.
    private void renderBreakParticles(Camera camera) {
        if (breakParticleMeshDirty) {
            updateBreakParticleMesh();
        }

        if (breakParticles.isEmpty()) {
            return;
        }

        useWorldShader(camera);

        worldShader.setInt("uOpaquePass", 1);
        model.identity();
        worldShader.setMat4("uModel", model);
        breakParticleMesh.draw();
    }

    private void useWorldShader(Camera camera) {
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
    }

    private void updateBreakParticleMesh() {
        FloatArrayBuilder opaque = new FloatArrayBuilder(breakParticles.size() * 36 * ChunkMesher.STRIDE_FLOATS);
        FloatArrayBuilder translucent = new FloatArrayBuilder(1024);
        for (BreakParticle particle : breakParticles) {
            addParticleCube(particle.translucent ? translucent : opaque, particle);
        }
        breakParticleMesh.update(opaque.toArray());
        particleTranslucentFaces.clear();
        particleTranslucentFaces.addAll(transparentFaces(translucent.toArray(), 0, 0));
        breakParticleMeshDirty = false;
        transparencyDirty = true;
    }

    static boolean cloudsBehindWorld(float cameraY) {
        return cameraY <= CLOUD_Y;
    }

    private void renderTranslucent(Camera camera) {
        if (transparencyDirty || !projViewMatrix.equals(cachedTransparencyProjectionView)) {
            translucentFaces.clear();
            for (ChunkPos pos : visibleChunkOrder) {
                ChunkMeshes meshes = chunkMeshes.get(pos);
                if (meshes != null) translucentFaces.addAll(meshes.translucent);
            }
            translucentFaces.addAll(particleTranslucentFaces);
            for (TransparentFace face : translucentFaces) face.updateDepth(view);
            translucentFaces.sort((a, b) -> Float.compare(b.depth, a.depth));
            FloatArrayBuilder vertices = new FloatArrayBuilder(translucentFaces.size() * 48);
            for (TransparentFace face : translucentFaces) face.appendTo(vertices);
            translucentMesh.update(vertices.toArray());
            cachedTransparencyProjectionView.set(projViewMatrix);
            transparencyDirty = false;
        }
        useWorldShader(camera);
        worldShader.setInt("uOpaquePass", 0);
        model.identity();
        worldShader.setMat4("uModel", model);
        glDepthMask(false);
        translucentMesh.draw();
        glDepthMask(true);
    }

    private static List<TransparentFace> transparentFaces(float[] vertices, int offsetX, int offsetZ) {
        // Each mesher/particle face is a pair of triangles (six vertices, stride eight).
        for (int i = 0; i < vertices.length; i += ChunkMesher.STRIDE_FLOATS) {
            vertices[i] += offsetX;
            vertices[i + 2] += offsetZ;
        }
        List<TransparentFace> faces = new ArrayList<>(vertices.length / 48);
        for (int offset = 0; offset < vertices.length; offset += 48) {
            faces.add(new TransparentFace(vertices, offset));
        }
        return faces;
    }

    private static final class TransparentFace {
        private final float[] vertices;
        private final int offset;
        private final float centerX;
        private final float centerY;
        private final float centerZ;
        private float depth;

        private TransparentFace(float[] vertices, int offset) {
            this.vertices = vertices;
            this.offset = offset;
            // Vertices zero and two are opposite corners in both mesh emitters.
            centerX = (vertices[offset] + vertices[offset + 16]) * 0.5f;
            centerY = (vertices[offset + 1] + vertices[offset + 17]) * 0.5f;
            centerZ = (vertices[offset + 2] + vertices[offset + 18]) * 0.5f;
        }

        private void updateDepth(Matrix4f view) {
            depth = -(view.m02() * centerX + view.m12() * centerY + view.m22() * centerZ + view.m32());
        }

        private void appendTo(FloatArrayBuilder out) {
            for (int i = offset; i < offset + 48; i += 8) {
                out.add(vertices[i], vertices[i + 1], vertices[i + 2], vertices[i + 3], vertices[i + 4],
                        vertices[i + 5], vertices[i + 6], vertices[i + 7]);
            }
        }
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

    private float[] buildSelectionOutline(RaycastHit hit) {
        FloatArrayBuilder out = new FloatArrayBuilder(1600 * hit.block().renderBoxes().length);
        for (BlockBounds bounds : hit.block().renderBoxes()) {
            float minX = hit.x() + bounds.minX() - SELECTION_OUTLINE_PADDING;
            float minY = hit.y() + bounds.minY() - SELECTION_OUTLINE_PADDING;
            float minZ = hit.z() + bounds.minZ() - SELECTION_OUTLINE_PADDING;
            float maxX = hit.x() + bounds.maxX() + SELECTION_OUTLINE_PADDING;
            float maxY = hit.y() + bounds.maxY() + SELECTION_OUTLINE_PADDING;
            float maxZ = hit.z() + bounds.maxZ() + SELECTION_OUTLINE_PADDING;
            float half = SELECTION_OUTLINE_THICKNESS * 0.5f;

            // Edges parallel to X.
            addEdgeBox(out, minX - half, minY - half, minZ - half, maxX + half, minY + half, minZ + half);
            addEdgeBox(out, minX - half, minY - half, maxZ - half, maxX + half, minY + half, maxZ + half);
            addEdgeBox(out, minX - half, maxY - half, minZ - half, maxX + half, maxY + half, minZ + half);
            addEdgeBox(out, minX - half, maxY - half, maxZ - half, maxX + half, maxY + half, maxZ + half);

            // Edges parallel to Y.
            addEdgeBox(out, minX - half, minY - half, minZ - half, minX + half, maxY + half, minZ + half);
            addEdgeBox(out, maxX - half, minY - half, minZ - half, maxX + half, maxY + half, minZ + half);
            addEdgeBox(out, minX - half, minY - half, maxZ - half, minX + half, maxY + half, maxZ + half);
            addEdgeBox(out, maxX - half, minY - half, maxZ - half, maxX + half, maxY + half, maxZ + half);

            // Edges parallel to Z.
            addEdgeBox(out, minX - half, minY - half, minZ - half, minX + half, minY + half, maxZ + half);
            addEdgeBox(out, maxX - half, minY - half, minZ - half, maxX + half, minY + half, maxZ + half);
            addEdgeBox(out, minX - half, maxY - half, minZ - half, minX + half, maxY + half, maxZ + half);
            addEdgeBox(out, maxX - half, maxY - half, minZ - half, maxX + half, maxY + half, maxZ + half);

        }
        return out.toArray();
    }

    // Build the outline from thin boxes instead of relying on driver-specific line width.
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

    // Interrupt CPU mesh workers, then release GPU resources while the context is still current.
    @Override
    public void close() {
        meshExecutor.shutdownNow();
        pendingMeshBuilds.clear();
        completedMeshBuilds.clear();
        try (ResourceScope resources = new ResourceScope()) {
            resources.add(atlas);
            resources.add(worldShader);
            resources.add(lineShader);
            resources.add(selectionMesh);
            resources.add(breakParticleMesh);
            resources.add(translucentMesh);
            resources.add(skyBodyMesh);
            resources.add(cloudMesh);
            for (ChunkMeshes meshes : chunkMeshes.values()) {
                resources.add(meshes);
            }
            chunkMeshes.clear();
        }
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
        private final boolean translucent;
        private float life;

        private BreakParticle(float x, float y, float z, float velocityX, float velocityY, float velocityZ,
                float size, float lifetime, float u0, float v0, float u1, float v1,
                float skyLight, float blockLight, boolean translucent) {
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
            this.translucent = translucent;
        }
    }

    private static final class ChunkMeshes implements AutoCloseable {
        private final Mesh opaque;
        private List<TransparentFace> translucent;

        private ChunkMeshes(Mesh opaque, List<TransparentFace> translucent) {
            this.opaque = opaque;
            this.translucent = translucent;
        }

        @Override
        public void close() {
            opaque.close();
        }
    }
}

package com.minecraftin.clone.render;

import com.minecraftin.clone.engine.*;
import com.minecraftin.clone.util.FloatArrayBuilder;
import com.minecraftin.clone.world.*;
import org.lwjgl.system.MemoryUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL33C.*;

// Exercise the actual renderer passes against alpha-over reference pixels, with deterministic
// geometry installed into its caches so worker timing and terrain do not affect the result.
final class TransparencyRegressionFixture {
    private static final int SIZE = 128;
    private static final float GLASS_ALPHA = 128f / 255;
    private static final float CLOUD_ALPHA = .72f;

    private TransparencyRegressionFixture() { }

    static void run() throws Exception {
        StringBuilder report = new StringBuilder();
        try (WorldRenderer renderer = new WorldRenderer(); World world = new World(Path.of("unused-fixture.dat"), 123)) {
            Chunk chunk = new Chunk(0, 0);
            chunk.clearMeshDirty();
            @SuppressWarnings("unchecked") Map<ChunkPos, Chunk> chunks = (Map<ChunkPos, Chunk>) get(world, "chunks");
            chunks.put(chunk.pos(), chunk);
            set(world, "timeOfDay", .25f);
            renderer.atlas().bind(0);
            fillTile(AtlasTiles.GLASS, 40, 120, 240, 128);
            fillTile(AtlasTiles.STONE, 200, 80, 40, 255);
            fillTile(AtlasTiles.WATER, 220, 60, 80, 128);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LEQUAL);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glDisable(GL_CULL_FACE);
            Camera camera = new Camera();
            camera.setPosition(8, 100, 10);
            camera.setRotation(-90, 0);
            prepareCloud(renderer, camera, false);
            for (boolean transparentParticle : new boolean[] {false, true}) {
                for (boolean front : new boolean[] {false, true}) {
                    setPane(renderer, world, false, false);
                    setParticle(renderer, false, transparentParticle, front);
                    int[] background = render(renderer, world, camera);
                    setPane(renderer, world, true, false);
                    int[] glass = render(renderer, world, camera);
                    setPane(renderer, world, false, false);
                    setParticle(renderer, true, transparentParticle, front);
                    int[] particle = render(renderer, world, camera);
                    setPane(renderer, world, true, false);
                    int[] combined = render(renderer, world, camera);
                    float particleAlpha = transparentParticle ? 1 - (1 - GLASS_ALPHA) * (1 - GLASS_ALPHA) : 1;
                    float[] expected = front ? over(particle, glass, background, particleAlpha)
                            : over(glass, particle, background, GLASS_ALPHA);
                    check("particle " + (transparentParticle ? "glass" : "opaque")
                            + (front ? " in front" : " behind"), combined, expected, report);
                }
            }
            setParticle(renderer, false, false, false);
            for (float y : new float[] {125, 175, 150}) {
                camera.setPosition(8, y, 8);
                camera.setRotation(-90, y < 150 ? 89.9f : -89.9f);
                setPane(renderer, world, false, true);
                prepareCloud(renderer, camera, false);
                int[] background = render(renderer, world, camera);
                setPane(renderer, world, true, true);
                int[] glass = render(renderer, world, camera);
                setPane(renderer, world, false, true);
                prepareCloud(renderer, camera, true);
                int[] cloud = render(renderer, world, camera);
                setPane(renderer, world, true, true);
                int[] combined = render(renderer, world, camera);
                float[] expected = y < 150 ? over(glass, cloud, background, GLASS_ALPHA)
                        : over(cloud, glass, background, CLOUD_ALPHA);
                if (y == 150) expected = new float[] {glass[0], glass[1], glass[2]};
                check("cloud plane camera y=" + y, combined, expected, report);
            }
            checkFenceOverhang(renderer, world, report);
            measureTransparencyBatch(renderer, world, report);
            int error = glGetError();
            if (error != GL_NO_ERROR) throw new AssertionError("Transparency fixture GL error " + error);
        }
        Path output = Path.of("build/reports/graphics-regression/fixed");
        Files.createDirectories(output);
        Files.writeString(output.resolve("transparency-results.txt"), report);
        System.out.print(report);
    }

    private static void checkFenceOverhang(WorldRenderer renderer, World world, StringBuilder report) throws Exception {
        setParticle(renderer, false, false, false);
        Chunk chunk = world.getChunkIfLoaded(0, 0);
        chunk.set(8, 127, 15, BlockType.OAK_FENCE);
        new LightEngine(world).onChunkLoaded(chunk);
        ChunkMesher.MeshData data = ChunkMesher.build(ChunkSnapshot.capture(chunk, world));
        for (float x : new float[] {8.49f, 8.50f, 8.51f}) {
            Camera camera = new Camera();
            camera.setPosition(x, 128.1f, 16.1f);
            camera.setRotation(-90, 45);
            prepareCloud(renderer, camera, false);
            setPane(renderer, world, false, false);
            renderer.render(world, camera, SIZE, SIZE, null);
            int[] sky = imagePixels();
            @SuppressWarnings("unchecked") Map<ChunkPos, ?> meshes = (Map<ChunkPos, ?>) get(renderer, "chunkMeshes");
            ((Mesh) get(meshes.get(chunk.pos()), "opaque")).update(data.opaqueVertices());
            renderer.render(world, camera, SIZE, SIZE, null);
            int[] fence = imagePixels();
            int changed = 0;
            for (int i = 0; i < sky.length; i++) if (sky[i] != fence[i]) changed++;
            if (changed == 0) throw new AssertionError("Visible fence overhang disappeared at camera x=" + x);
            report.append("Fence at top build layer, camera x=").append(x)
                    .append(" visible colored pixels=").append(changed).append('\n');
        }
    }

    private static int[] imagePixels() {
        ByteBuffer pixels = MemoryUtil.memAlloc(SIZE * SIZE * 4);
        try {
            glReadPixels(0, 0, SIZE, SIZE, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            int[] colors = new int[SIZE * SIZE];
            for (int i = 0; i < colors.length; i++) colors[i] = pixels.getInt(i * 4);
            return colors;
        } finally { MemoryUtil.memFree(pixels); }
    }

    private static void measureTransparencyBatch(WorldRenderer renderer, World world, StringBuilder report) throws Exception {
        @SuppressWarnings("unchecked") Map<ChunkPos, AutoCloseable> meshes = (Map<ChunkPos, AutoCloseable>) get(renderer, "chunkMeshes");
        @SuppressWarnings("unchecked") Map<ChunkPos, Chunk> chunks = (Map<ChunkPos, Chunk>) get(world, "chunks");
        for (AutoCloseable mesh : meshes.values()) mesh.close();
        meshes.clear();
        chunks.clear();
        Method faces = WorldRenderer.class.getDeclaredMethod("transparentFaces", float[].class, int.class, int.class);
        faces.setAccessible(true);
        Class<?> type = Class.forName(WorldRenderer.class.getName() + "$ChunkMeshes");
        Constructor<?> constructor = type.getDeclaredConstructor(Mesh.class, List.class);
        constructor.setAccessible(true);
        float u = (TextureAtlas.u0(AtlasTiles.WATER) + TextureAtlas.u1(AtlasTiles.WATER)) * .5f;
        float v = (TextureAtlas.v0(AtlasTiles.WATER) + TextureAtlas.v1(AtlasTiles.WATER)) * .5f;
        for (int z = -2; z <= 2; z++) {
            for (int x = -2; x <= 2; x++) {
                Chunk chunk = new Chunk(x, z);
                chunk.clearMeshDirty();
                chunks.put(chunk.pos(), chunk);
                FloatArrayBuilder vertices = new FloatArrayBuilder(16 * 16 * 48);
                for (int dz = 0; dz < 16; dz++) {
                    for (int dx = 0; dx < 16; dx++) {
                        vertices.add(dx,100,dz,u,v,1,1,0);
                        vertices.add(dx+1,100,dz,u,v,1,1,0);
                        vertices.add(dx+1,100,dz+1,u,v,1,1,0);
                        vertices.add(dx+1,100,dz+1,u,v,1,1,0);
                        vertices.add(dx,100,dz+1,u,v,1,1,0);
                        vertices.add(dx,100,dz,u,v,1,1,0);
                    }
                }
                meshes.put(chunk.pos(), (AutoCloseable) constructor.newInstance(
                        new Mesh(new float[0], GL_TRIANGLES, 3, 2, 3),
                        faces.invoke(null, vertices.toArray(), x * 16, z * 16)));
            }
        }
        Camera camera = new Camera();
        camera.setPosition(8, 120, 60);
        camera.setRotation(-90, -25);
        prepareCloud(renderer, camera, false);
        set(renderer, "transparencyDirty", true);
        double[] movingMillis = new double[60];
        int faceCount = 0;
        for (int frame = 0; frame < 80; frame++) {
            camera.setPosition(8 + frame * .01f, 120, 60);
            glFinish();
            long start = System.nanoTime();
            renderer.render(world, camera, SIZE, SIZE, null);
            glFinish();
            if (frame >= 20) movingMillis[frame - 20] = (System.nanoTime() - start) / 1_000_000.0;
            faceCount = Math.max(faceCount, ((List<?>) get(renderer, "translucentFaces")).size());
        }
        java.util.Arrays.sort(movingMillis);
        if (faceCount < 1_000) throw new AssertionError("Transparency measurement did not exercise enough visible faces");
        report.append("Moving camera over 25 synthetic water chunks: visible faces=").append(faceCount)
                .append(", upload bytes=").append(faceCount * 48 * Float.BYTES)
                .append(", 60 measured frames including glFinish median ms=").append(movingMillis[30])
                .append(", p95 ms=").append(movingMillis[57]).append('\n');
        glFinish();
        long start = System.nanoTime();
        for (int frame = 0; frame < 60; frame++) renderer.render(world, camera, SIZE, SIZE, null);
        glFinish();
        report.append("Stationary camera 60-frame mean ms=")
                .append((System.nanoTime() - start) / 60_000_000.0)
                .append(" (sort/upload cache reused; synthetic fixture, not gameplay FPS)\n");
    }

    private static int[] render(WorldRenderer renderer, World world, Camera camera) {
        renderer.render(world, camera, SIZE, SIZE, null);
        ByteBuffer pixel = MemoryUtil.memAlloc(4);
        try {
            glReadPixels(SIZE / 2, SIZE / 2, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixel);
            return new int[] {pixel.get(0) & 255, pixel.get(1) & 255, pixel.get(2) & 255};
        } finally { MemoryUtil.memFree(pixel); }
    }

    private static float[] over(int[] near, int[] far, int[] background, float alpha) {
        return new float[] {near[0] + (far[0] - background[0]) * (1 - alpha),
                near[1] + (far[1] - background[1]) * (1 - alpha),
                near[2] + (far[2] - background[2]) * (1 - alpha)};
    }

    private static void check(String label, int[] actual, float[] expected, StringBuilder report) {
        float maxError = 0;
        for (int i = 0; i < 3; i++) maxError = Math.max(maxError, Math.abs(actual[i] - expected[i]));
        report.append(label).append(" actual=").append(java.util.Arrays.toString(actual))
                .append(" expected=").append(java.util.Arrays.toString(expected))
                .append(" max channel error=").append(maxError).append('\n');
        if (maxError > 3) throw new AssertionError(label + " alpha-over mismatch: " + maxError);
    }

    private static void setPane(WorldRenderer renderer, World world, boolean enabled, boolean horizontal) throws Exception {
        @SuppressWarnings("unchecked") Map<ChunkPos, AutoCloseable> meshes = (Map<ChunkPos, AutoCloseable>) get(renderer, "chunkMeshes");
        AutoCloseable old = meshes.remove(new ChunkPos(0, 0));
        if (old != null) old.close();
        Method faces = WorldRenderer.class.getDeclaredMethod("transparentFaces", float[].class, int.class, int.class);
        faces.setAccessible(true);
        Object geometry = faces.invoke(null, enabled ? pane(horizontal) : new float[0], 0, 0);
        Class<?> type = Class.forName(WorldRenderer.class.getName() + "$ChunkMeshes");
        Constructor<?> constructor = type.getDeclaredConstructor(Mesh.class, List.class);
        constructor.setAccessible(true);
        meshes.put(new ChunkPos(0, 0), (AutoCloseable) constructor.newInstance(new Mesh(new float[0], GL_TRIANGLES, 3, 2, 3), geometry));
        set(renderer, "transparencyDirty", true);
        world.getChunkIfLoaded(0, 0).clearMeshDirty();
    }

    private static void setParticle(WorldRenderer renderer, boolean enabled, boolean translucent, boolean front) throws Exception {
        @SuppressWarnings("unchecked") List<Object> particles = (List<Object>) get(renderer, "breakParticles");
        particles.clear();
        if (enabled) {
            Class<?> type = Class.forName(WorldRenderer.class.getName() + "$BreakParticle");
            Constructor<?> constructor = type.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            int tile = translucent ? AtlasTiles.WATER : AtlasTiles.STONE;
            float u = (TextureAtlas.u0(tile) + TextureAtlas.u1(tile)) * .5f;
            float v = (TextureAtlas.v0(tile) + TextureAtlas.v1(tile)) * .5f;
            particles.add(constructor.newInstance(8f, 100f, front ? 9f : 7f, 0f, 0f, 0f,
                    .6f, 1f, u, v, u, v, 1f, 0f, translucent));
        }
        set(renderer, "breakParticleMeshDirty", true);
    }

    private static void prepareCloud(WorldRenderer renderer, Camera camera, boolean enabled) throws Exception {
        set(renderer, "cachedCloudCellX", (int) Math.floor(camera.position().x / 12));
        set(renderer, "cachedCloudCellZ", (int) Math.floor(camera.position().z / 12));
        Mesh mesh = (Mesh) get(renderer, "cloudMesh");
        mesh.update(enabled ? new float[] {-100,150,-100, 100,150,-100, 100,150,100,
                100,150,100, -100,150,100, -100,150,-100} : new float[0]);
    }

    private static float[] pane(boolean horizontal) {
        FloatArrayBuilder vertices = new FloatArrayBuilder(48);
        float u = (TextureAtlas.u0(AtlasTiles.GLASS) + TextureAtlas.u1(AtlasTiles.GLASS)) * .5f;
        float v = (TextureAtlas.v0(AtlasTiles.GLASS) + TextureAtlas.v1(AtlasTiles.GLASS)) * .5f;
        float[][] points = horizontal ? new float[][] {{0,127,0},{16,127,0},{16,127,16},{16,127,16},{0,127,16},{0,127,0}}
                : new float[][] {{6,98,8},{10,98,8},{10,102,8},{10,102,8},{6,102,8},{6,98,8}};
        for (float[] point : points) vertices.add(point[0], point[1], point[2], u, v, 1, 1, 0);
        return vertices.toArray();
    }

    private static void fillTile(int tile, int r, int g, int b, int a) {
        ByteBuffer pixels = MemoryUtil.memAlloc(16 * 16 * 4);
        try {
            for (int i = 0; i < 16 * 16; i++) pixels.put((byte) r).put((byte) g).put((byte) b).put((byte) a);
            pixels.flip();
            glTexSubImage2D(GL_TEXTURE_2D, 0, tile % 16 * 16, tile / 16 * 16, 16, 16, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        } finally { MemoryUtil.memFree(pixels); }
    }

    private static Object get(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}

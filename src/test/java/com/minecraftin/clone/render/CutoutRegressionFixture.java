package com.minecraftin.clone.render;

import com.minecraftin.clone.engine.Mesh;
import com.minecraftin.clone.engine.ResourceScope;
import com.minecraftin.clone.engine.ShaderProgram;
import com.minecraftin.clone.engine.TextureAtlas;
import com.minecraftin.clone.engine.Window;
import com.minecraftin.clone.util.FloatArrayBuilder;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.opengl.GL33C.*;

// Controlled production-atlas/shader pixel checks. Run separately from headless JUnit tests.
public final class CutoutRegressionFixture {
    private static final int SIZE = 128;

    private CutoutRegressionFixture() { }

    public static void main(String[] args) throws Exception {
        boolean baseline = args.length > 0 && args[0].equals("baseline");
        try (Window window = new Window()) {
            window.create();
            run(!baseline);
        }
    }

    static void run(boolean requireCorrect) throws Exception {
        Path output = Path.of("build/reports/graphics-regression", requireCorrect ? "fixed" : "baseline");
        Files.createDirectories(output);
        StringBuilder report = new StringBuilder("OpenGL: " + glGetString(GL_VERSION)
                + "\nVendor: " + glGetString(GL_VENDOR) + "\nRenderer: " + glGetString(GL_RENDERER) + "\n");
        int allDifferences = 0;
        int framebuffer = glGenFramebuffers();
        int color = glGenTextures();
        int depth = glGenRenderbuffers();
        try (ResourceScope resources = new ResourceScope()) {
            resources.add(() -> glDeleteFramebuffers(framebuffer));
            resources.add(() -> glDeleteTextures(color));
            resources.add(() -> glDeleteRenderbuffers(depth));
            glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
            glBindTexture(GL_TEXTURE_2D, color);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, SIZE, SIZE, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, color, 0);
            glBindRenderbuffer(GL_RENDERBUFFER, depth);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, SIZE, SIZE);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depth);
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                throw new AssertionError("Incomplete pixel fixture framebuffer");
            }
            TextureAtlas atlas = resources.add(new TextureAtlas());
            ShaderProgram shader = resources.add(new ShaderProgram("/shaders/world.vert", "/shaders/world.frag"));
            shader.use();
            Matrix4f identity = new Matrix4f();
            shader.setMat4("uProjection", identity);
            shader.setMat4("uView", identity);
            shader.setMat4("uModel", identity);
            shader.setVec3("uFogColor", new Vector3f());
            shader.setVec3("uCameraPos", new Vector3f());
            shader.setFloat("uFogNear", 100);
            shader.setFloat("uFogFar", 200);
            shader.setFloat("uDayLight", 1);
            shader.setInt("uAtlas", 0);
            shader.setInt("uOpaquePass", 1); // Absent in the original shader; safely ignored there.
            atlas.bind(0);
            glViewport(0, 0, SIZE, SIZE);
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS);
            glDepthMask(true);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glDisable(GL_CULL_FACE);
            for (int tile : new int[] {AtlasTiles.LADDER, AtlasTiles.TORCH}) {
                for (int wall : new int[] {AtlasTiles.WHITE, AtlasTiles.STONE}) {
                    try (Mesh foreground = new Mesh(quad(tile, -.5f), GL_TRIANGLES, 3, 2, 3);
                         Mesh background = new Mesh(quad(wall, .5f), GL_TRIANGLES, 3, 2, 3)) {
                        double baseCoverage = 0;
                        for (int lod = 0; lod <= 4; lod++) {
                            // Force the real generated mip level to isolate alpha/depth behavior.
                            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, lod);
                            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, lod);
                            int[] frontFirst = draw(foreground, background);
                            int[] backFirst = draw(background, foreground);
                            int[] wallOnly = draw(background, background);
                            int changed = 0, foregroundPixels = 0;
                            for (int i = 0; i < frontFirst.length; i++) {
                                if (frontFirst[i] != backFirst[i]) changed++;
                                if (frontFirst[i] != wallOnly[i]) foregroundPixels++;
                            }
                            double coverage = foregroundPixels / (double) frontFirst.length;
                            if (lod == 0) baseCoverage = coverage;
                            if (requireCorrect && (coverage < .1 || coverage > .5
                                    || Math.abs(coverage - baseCoverage) > .065)) {
                                throw new AssertionError("Cutout disappeared or inflated at tile=" + tile
                                        + ", LOD=" + lod + ", foreground coverage=" + coverage);
                            }
                            allDifferences += changed;
                            String name = "tile-" + tile + "-wall-" + wall + "-lod-" + lod;
                            report.append(name).append(" order-dependent pixels=").append(changed)
                                    .append(", foreground coverage=").append(coverage).append('\n');
                            writeImage(output.resolve(name + "-front-first.png"), frontFirst);
                            writeImage(output.resolve(name + "-back-first.png"), backFirst);
                        }
                    }
                }
            }
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 4);
            int error = glGetError();
            if (error != GL_NO_ERROR) throw new AssertionError("Cutout fixture GL error " + error);
        } finally {
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }
        Files.writeString(output.resolve("cutout-results.txt"), report);
        System.out.print(report);
        if (requireCorrect && allDifferences != 0) throw new AssertionError("Cutout depth depends on submission order");
        if (!requireCorrect && allDifferences == 0) throw new AssertionError("Original cutout issue was not reproduced");
    }

    private static int[] draw(Mesh first, Mesh second) {
        glClearColor(.1f, .2f, .9f, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        first.draw();
        second.draw();
        ByteBuffer pixels = MemoryUtil.memAlloc(SIZE * SIZE * 4);
        try {
            glReadPixels(0, 0, SIZE, SIZE, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            int[] colors = new int[SIZE * SIZE];
            for (int i = 0; i < colors.length; i++) {
                colors[i] = 0xff000000 | (pixels.get(i * 4) & 255) << 16
                        | (pixels.get(i * 4 + 1) & 255) << 8 | (pixels.get(i * 4 + 2) & 255);
            }
            return colors;
        } finally {
            MemoryUtil.memFree(pixels);
        }
    }

    private static float[] quad(int tile, float z) {
        float u0 = TextureAtlas.u0(tile), u1 = TextureAtlas.u1(tile);
        float v0 = TextureAtlas.v0(tile), v1 = TextureAtlas.v1(tile);
        FloatArrayBuilder out = new FloatArrayBuilder(48);
        out.add(-1, -1, z, u0, v0, 1, 1, 0);
        out.add(1, -1, z, u1, v0, 1, 1, 0);
        out.add(1, 1, z, u1, v1, 1, 1, 0);
        out.add(1, 1, z, u1, v1, 1, 1, 0);
        out.add(-1, 1, z, u0, v1, 1, 1, 0);
        out.add(-1, -1, z, u0, v0, 1, 1, 0);
        return out.toArray();
    }

    private static void writeImage(Path file, int[] pixels) throws Exception {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) image.setRGB(x, SIZE - 1 - y, pixels[y * SIZE + x]);
        }
        ImageIO.write(image, "png", file.toFile());
    }
}

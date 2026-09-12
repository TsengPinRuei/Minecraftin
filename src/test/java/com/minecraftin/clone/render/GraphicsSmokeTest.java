package com.minecraftin.clone.render;

import com.minecraftin.clone.engine.*;
import com.minecraftin.clone.world.*;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;

// 由 graphicsSmokeTest 啟動，需桌面與 OpenGL context；不混入無視窗的 JUnit test 工作。
public final class GraphicsSmokeTest {
    private GraphicsSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        Path output = Path.of("build", "reports", "graphics-smoke");
        Files.createDirectories(output);
        Path temporary = Files.createTempDirectory("minecraftin-graphics-");
        try (ResourceScope resources = new ResourceScope()) {
            Window window = resources.add(new Window());
            window.create();
            new InputState().attach(window.handle());
            glfwSwapInterval(1);
            glfwSetWindowSize(window.handle(), 800, 600);
            window.pollEvents();

            World world = new World(temporary.resolve("world.dat"), 123);
            world.initialize();
            world.ensureChunksAround(0, 0, 1);
            for (int z = -1; z <= 1; z++) {
                for (int x = -1; x <= 1; x++) {
                    Chunk chunk = world.getChunkIfLoaded(x, z);
                    if (chunk != null) {
                        Arrays.fill(chunk.rawBlocks(), (short) 0);
                        new LightEngine(world).onChunkLoaded(chunk);
                    }
                }
            }
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) world.setBlock(x, 99, z, BlockType.GRASS);
            }
            BlockType[] hotbar = {BlockType.GLASS, BlockType.OAK_SLAB, BlockType.OAK_STAIRS_NORTH,
                    BlockType.OAK_FENCE, BlockType.OAK_DOOR_NORTH_BOTTOM, BlockType.TORCH,
                    BlockType.GLOWSTONE, BlockType.CHEST, BlockType.LADDER_NORTH};
            for (int i = 0; i < hotbar.length; i++) world.setBlock(i + 3, 100, 8, hotbar[i]);
            world.setBlock(7, 101, 8, BlockType.OAK_DOOR_NORTH_TOP);
            WorldRenderer renderer = resources.add(new WorldRenderer());
            HudRenderer hud = resources.add(new HudRenderer(renderer.atlas()));
            Camera camera = new Camera();
            camera.setPosition(8, 104, 18);
            camera.setRotation(-90, -24);
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LEQUAL);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            BlockType[] palette = BlockType.creativePalette();
            for (int frame = 0; frame < 150; frame++) {
                window.pollEvents();
                if (frame == 50) {
                    renderer.spawnBlockBreakEffect(world, BlockType.GLASS, 3, 100, 8);
                    world.setBlock(3, 100, 8, BlockType.AIR);
                    world.setBlock(8, 100, 8, BlockType.SEA_LANTERN);
                }
                world.update(1.0f / 60);
                renderer.update(1.0f / 60);
                RaycastHit hit = world.raycast(camera.position(), camera.forward(new Vector3f()), 20);
                renderer.render(world, camera, window.width(), window.height(), hit);
                boolean inventory = frame >= 120;
                hud.render(hotbar, 1, true, inventory, palette, 0,
                        HudRenderer.creativeTotalPages(palette.length), false,
                        new String[]{"OpenGL smoke test", "Frames: " + frame},
                        window.width(), window.height());
                int error = glGetError();
                if (error != GL_NO_ERROR) throw new AssertionError("OpenGL error: " + error);
                if (frame == 119 || frame == 149) {
                    capture(output.resolve(inventory ? "inventory.png" : "world.png"), window.width(), window.height());
                }
                window.swapBuffers();
            }
            // 確認背景工作確實產生並上傳網格，而非只驗證空畫面。
            var meshes = WorldRenderer.class.getDeclaredField("chunkMeshes");
            meshes.setAccessible(true);
            int uploaded = ((java.util.Map<?, ?>) meshes.get(renderer)).size();
            if (uploaded == 0) throw new AssertionError("No chunk meshes uploaded");
            System.out.println("Graphics smoke passed: " + glGetString(GL_VERSION)
                    + ", uploaded chunks=" + uploaded);
        } finally {
            Files.deleteIfExists(temporary.resolve("world.dat"));
            Files.deleteIfExists(temporary);
        }
    }

    private static void capture(Path file, int width, int height) throws Exception {
        ByteBuffer pixels = MemoryUtil.memAlloc(width * height * 4);
        try {
            glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = (y * width + x) * 4;
                    int color = 0xFF000000 | (pixels.get(index) & 255) << 16
                            | (pixels.get(index + 1) & 255) << 8 | (pixels.get(index + 2) & 255);
                    image.setRGB(x, height - y - 1, color);
                }
            }
            ImageIO.write(image, "png", file.toFile());
        } finally {
            MemoryUtil.memFree(pixels);
        }
    }
}

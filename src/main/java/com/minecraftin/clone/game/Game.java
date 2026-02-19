package com.minecraftin.clone.game;

import com.minecraftin.clone.config.GameConfig;
import com.minecraftin.clone.engine.Camera;
import com.minecraftin.clone.engine.InputState;
import com.minecraftin.clone.engine.Window;
import com.minecraftin.clone.gameplay.Player;
import com.minecraftin.clone.render.HudRenderer;
import com.minecraftin.clone.render.WorldRenderer;
import com.minecraftin.clone.world.BlockType;
import com.minecraftin.clone.world.RaycastHit;
import com.minecraftin.clone.world.World;
import org.joml.Vector3f;

import java.nio.file.Paths;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;

public final class Game {
    private static final String MODE_LABEL = GameConfig.CREATIVE_MODE_ONLY ? "CREATIVE" : "SURVIVAL";

    private final BlockType[] hotbar = {
            BlockType.RED_BLOCK,
            BlockType.ORANGE_BLOCK,
            BlockType.YELLOW_BLOCK,
            BlockType.GREEN_BLOCK,
            BlockType.BLUE_BLOCK,
            BlockType.PURPLE_BLOCK,
            BlockType.DIRT,
            BlockType.STONE,
            BlockType.GLASS
    };

    private final Window window = new Window();
    private final InputState input = new InputState();
    private final Camera camera = new Camera();
    private final Player player = new Player();
    private final World world = new World(Paths.get(GameConfig.WORLD_FILE), GameConfig.DEFAULT_WORLD_SEED);

    private WorldRenderer worldRenderer;
    private HudRenderer hudRenderer;

    private boolean cursorCaptured = false;
    private int hotbarIndex;

    private float breakCooldown;
    private float placeCooldown;
    private float autosaveTimer;

    private RaycastHit targetedBlock;

    private double fpsTimer;
    private int fpsFrames;
    private boolean worldInitialized;

    public void run() {
        try {
            window.create();
            input.attach(window.handle());

            initGraphicsState();

            world.initialize();
            worldInitialized = true;
            worldRenderer = new WorldRenderer();
            hudRenderer = new HudRenderer();

            Vector3f spawn = world.defaultSpawn(new Vector3f());
            player.setPosition(spawn.x, spawn.y, spawn.z);
            camera.setRotation(-90.0f, -15.0f);
            updateCameraFromPlayer();

            window.captureCursor(false);
            input.resetMouseTracking();

            loop();
        } finally {
            if (worldInitialized) {
                safeSaveWorld();
            }
            if (hudRenderer != null) {
                hudRenderer.close();
            }
            if (worldRenderer != null) {
                worldRenderer.close();
            }
            window.close();
        }
    }

    private void initGraphicsState() {
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glDisable(GL_CULL_FACE);
        glEnable(GL_MULTISAMPLE);
    }

    private void loop() {
        double lastTime = glfwGetTime();

        while (!window.shouldClose()) {
            double now = glfwGetTime();
            float delta = (float) Math.min(0.05, now - lastTime);
            lastTime = now;

            window.pollEvents();
            handleInputState();

            int playerChunkX = Math.floorDiv((int) Math.floor(player.position().x), GameConfig.CHUNK_SIZE);
            int playerChunkZ = Math.floorDiv((int) Math.floor(player.position().z), GameConfig.CHUNK_SIZE);
            world.ensureChunksAround(playerChunkX, playerChunkZ, GameConfig.RENDER_DISTANCE_CHUNKS);

            if (cursorCaptured) {
                applyLookFromMouse();
                player.update(input, camera, world, delta);
            }

            updateCameraFromPlayer();
            updateTargetBlock();
            if (cursorCaptured) {
                updateBlockInteraction(delta);
            }

            worldRenderer.render(world, camera, window.width(), window.height(), targetedBlock);
            hudRenderer.render(hotbar, hotbarIndex);

            updateDebugTitle(now);

            window.swapBuffers();
            input.endFrame();

            autosaveTimer += delta;
            if (autosaveTimer >= 20.0f) {
                safeSaveWorld();
                autosaveTimer = 0.0f;
            }
        }
    }

    private void handleInputState() {
        if (input.wasKeyPressed(GLFW_KEY_ESCAPE) && cursorCaptured) {
            cursorCaptured = false;
            window.captureCursor(false);
            input.resetMouseTracking();
        }

        if (!cursorCaptured && input.wasMousePressed(GLFW_MOUSE_BUTTON_LEFT)) {
            cursorCaptured = true;
            window.captureCursor(true);
            input.resetMouseTracking();
        }

        if (input.wasKeyPressed(GLFW_KEY_Q)) {
            window.requestClose();
        }

        for (int i = 0; i < hotbar.length && i < 9; i++) {
            int key = GLFW_KEY_1 + i;
            if (input.wasKeyPressed(key)) {
                hotbarIndex = i;
            }
        }

        double scroll = input.consumeScrollDeltaY();
        if (scroll != 0.0) {
            int direction = scroll > 0.0 ? -1 : 1;
            hotbarIndex = Math.floorMod(hotbarIndex + direction, hotbar.length);
        }
    }

    private void applyLookFromMouse() {
        float yawDelta = (float) input.mouseDeltaX() * GameConfig.MOUSE_SENSITIVITY;
        float pitchDelta = (float) -input.mouseDeltaY() * GameConfig.MOUSE_SENSITIVITY;
        camera.rotate(yawDelta, pitchDelta);
    }

    private void updateCameraFromPlayer() {
        camera.setPosition(
                player.position().x,
                player.position().y + GameConfig.PLAYER_EYE_HEIGHT,
                player.position().z
        );
    }

    private void updateTargetBlock() {
        Vector3f eye = player.eyePosition(new Vector3f());
        Vector3f direction = camera.forward(new Vector3f());
        targetedBlock = world.raycast(eye, direction, GameConfig.BLOCK_REACH);
    }

    private void updateBlockInteraction(float deltaSeconds) {
        breakCooldown -= deltaSeconds;
        placeCooldown -= deltaSeconds;

        if (targetedBlock != null && input.wasMousePressed(GLFW_MOUSE_BUTTON_LEFT) && breakCooldown <= 0.0f) {
            if (targetedBlock.block() != BlockType.BEDROCK) {
                world.setBlock(targetedBlock.x(), targetedBlock.y(), targetedBlock.z(), BlockType.AIR);
            }
            breakCooldown = GameConfig.BREAK_COOLDOWN_SECONDS;
        }

        if (targetedBlock != null && input.wasMousePressed(GLFW_MOUSE_BUTTON_RIGHT) && placeCooldown <= 0.0f) {
            int px = targetedBlock.x() + targetedBlock.normalX();
            int py = targetedBlock.y() + targetedBlock.normalY();
            int pz = targetedBlock.z() + targetedBlock.normalZ();

            BlockType current = world.getBlock(px, py, pz);
            if ((current == BlockType.AIR || current == BlockType.WATER) && !player.intersectsBlock(px, py, pz)) {
                world.setBlock(px, py, pz, hotbar[hotbarIndex]);
            }
            placeCooldown = GameConfig.PLACE_COOLDOWN_SECONDS;
        }
    }

    private void updateDebugTitle(double now) {
        fpsFrames++;
        if (fpsTimer == 0.0) {
            fpsTimer = now;
        }

        double elapsed = now - fpsTimer;
        if (elapsed >= 1.0) {
            int fps = (int) Math.round(fpsFrames / elapsed);
            BlockType selected = hotbar[hotbarIndex];
            String title = GameConfig.WINDOW_TITLE
                    + " | " + MODE_LABEL
                    + " | Flight: " + (player.isFlying() ? "ON" : "OFF")
                    + " | FPS: " + fps
                    + " | Chunks: " + world.chunkCount()
                    + " | Block: " + selected.name();
            glfwSetWindowTitle(window.handle(), title);

            fpsFrames = 0;
            fpsTimer = now;
        }
    }

    private void safeSaveWorld() {
        try {
            world.save();
        } catch (Exception e) {
            System.err.println("World save failed: " + e.getMessage());
        }
    }
}

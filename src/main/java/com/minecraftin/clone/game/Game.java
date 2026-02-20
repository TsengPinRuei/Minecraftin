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
    private static final float WALK_BOB_VERTICAL_BASE = 0.020f;
    private static final float WALK_BOB_VERTICAL_SCALE = 0.024f;
    private static final float WALK_BOB_HORIZONTAL_FACTOR = 0.60f;
    private static final float WALK_BOB_RESET_SPEED = 8.0f;

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
    private final Vector3f tmpCameraRight = new Vector3f();
    private final Vector3f tmpRayOrigin = new Vector3f();
    private final Vector3f tmpRayDirection = new Vector3f();

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
    private float walkBobPhase;
    private float walkBobVertical;
    private float walkBobHorizontal;

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
            updateCameraFromPlayer(0.0f);

            window.captureCursor(false);
            input.resetMouseTracking();

            loop();
        } finally {
            if (worldInitialized && world.hasModifiedChunks()) {
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
            // Load one extra ring so rendered chunks always have neighbor data at the edges.
            world.ensureChunksAround(playerChunkX, playerChunkZ, GameConfig.RENDER_DISTANCE_CHUNKS + 1);

            if (cursorCaptured) {
                applyLookFromMouse();
                player.update(input, camera, world, delta);
            }

            updateCameraFromPlayer(delta);
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
                if (world.hasModifiedChunks()) {
                    safeSaveWorld();
                }
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

    private void updateCameraFromPlayer(float deltaSeconds) {
        updateWalkBob(deltaSeconds);

        Vector3f right = camera.right(tmpCameraRight);
        camera.setPosition(
                player.position().x + right.x * walkBobHorizontal,
                player.position().y + GameConfig.PLAYER_EYE_HEIGHT + walkBobVertical,
                player.position().z + right.z * walkBobHorizontal
        );
    }

    private void updateWalkBob(float deltaSeconds) {
        if (deltaSeconds <= 0.0f) {
            walkBobVertical = 0.0f;
            walkBobHorizontal = 0.0f;
            return;
        }

        if (!cursorCaptured || player.isFlying() || !player.isOnGround()) {
            walkBobVertical = approach(walkBobVertical, 0.0f, WALK_BOB_RESET_SPEED * deltaSeconds);
            walkBobHorizontal = approach(walkBobHorizontal, 0.0f, WALK_BOB_RESET_SPEED * deltaSeconds);
            return;
        }

        float speedRatio = Math.min(player.horizontalSpeed() / Math.max(GameConfig.WALK_SPEED, 0.001f), 1.6f);
        if (speedRatio < 0.06f) {
            walkBobVertical = approach(walkBobVertical, 0.0f, WALK_BOB_RESET_SPEED * deltaSeconds);
            walkBobHorizontal = approach(walkBobHorizontal, 0.0f, WALK_BOB_RESET_SPEED * deltaSeconds);
            return;
        }

        walkBobPhase += deltaSeconds * (8.0f + 4.0f * speedRatio);
        if (walkBobPhase > (float) (Math.PI * 2.0)) {
            walkBobPhase -= (float) (Math.PI * 2.0);
        }

        float amplitude = WALK_BOB_VERTICAL_BASE + WALK_BOB_VERTICAL_SCALE * speedRatio;
        walkBobVertical = (float) Math.sin(walkBobPhase * 2.0f) * amplitude;
        walkBobHorizontal = (float) Math.cos(walkBobPhase) * amplitude * WALK_BOB_HORIZONTAL_FACTOR;
    }

    private void updateTargetBlock() {
        tmpRayOrigin.set(camera.position());
        camera.forward(tmpRayDirection);
        targetedBlock = world.raycast(tmpRayOrigin, tmpRayDirection, GameConfig.BLOCK_REACH);
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
                    + " | Block: " + selected.displayName();
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

    private float approach(float current, float target, float delta) {
        if (current < target) {
            return Math.min(current + delta, target);
        }
        return Math.max(current - delta, target);
    }
}

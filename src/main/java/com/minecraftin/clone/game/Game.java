package com.minecraftin.clone.game;

import com.minecraftin.clone.config.GameConfig;
import com.minecraftin.clone.engine.Camera;
import com.minecraftin.clone.engine.InputState;
import com.minecraftin.clone.engine.Window;
import com.minecraftin.clone.engine.ResourceScope;
import com.minecraftin.clone.gameplay.Player;
import com.minecraftin.clone.render.HudRenderer;
import com.minecraftin.clone.render.WorldRenderer;
import com.minecraftin.clone.world.BlockType;
import com.minecraftin.clone.world.RaycastHit;
import com.minecraftin.clone.world.World;
import com.minecraftin.clone.world.WorldBounds;
import org.joml.Vector3f;

import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;

// Coordinate window setup, world initialization, input, simulation, rendering, and shutdown.
public final class Game {
    private static final String MODE_LABEL = GameConfig.CREATIVE_MODE_ONLY ? "CREATIVE" : "SURVIVAL";

    // Walking bob changes the camera and targeting ray origin, but not the player's collision
    // position.
    private static final float WALK_BOB_VERTICAL_BASE = 0.020f;
    private static final float WALK_BOB_VERTICAL_SCALE = 0.024f;
    private static final float WALK_BOB_HORIZONTAL_FACTOR = 0.60f;
    private static final float WALK_BOB_RESET_SPEED = 8.0f;

    // Hotbar order matches number keys 1 through 9; inventory selection can replace these defaults.
    private final BlockType[] hotbar = {
            BlockType.GRASS,
            BlockType.DIRT,
            BlockType.STONE,
            BlockType.COBBLESTONE,
            BlockType.PLANKS,
            BlockType.LOG,
            BlockType.GLASS,
            BlockType.TORCH,
            BlockType.CRAFTING_TABLE
    };

    private final BlockType[] creativeBlocks = BlockType.creativePalette();
    private final int creativeTotalPages = HudRenderer.creativeTotalPages(creativeBlocks.length);

    private final Window window = new Window();
    private final InputState input = new InputState();
    private final Camera camera = new Camera();
    private final Player player = new Player();
    private final World world;

    // Reuse vectors across frames to avoid temporary allocations.
    private final Vector3f tmpCameraRight = new Vector3f();
    private final Vector3f tmpRayOrigin = new Vector3f();
    private final Vector3f tmpRayDirection = new Vector3f();

    private WorldRenderer worldRenderer;
    private HudRenderer hudRenderer;

    private boolean cursorCaptured = false;

    // Ignore world interaction until both mouse buttons are released after capturing the cursor.
    private boolean suppressInteractionUntilRelease;

    private int hotbarIndex;

    // Show the selected block name for this many simulation seconds.
    private static final float HOTBAR_LABEL_SECONDS = 2.0f;
    private float hotbarLabelTimer;

    // Remember cursor capture state so closing the inventory can restore first-person control.
    private boolean creativeInventoryOpen;
    private boolean recaptureCursorAfterInventory;
    private int creativeInventoryPage;

    // The chest is currently an empty UI; it has no stored items or slot interactions.
    private boolean chestOpen;
    private boolean recaptureCursorAfterChest;

    private float breakCooldown;
    private float placeCooldown;
    private float autosaveTimer;
    private final SaveController saves = new SaveController();
    private boolean recaptureAfterSavePrompt;
    private long sampledHeapUsed;
    private long sampledHeapMax = Runtime.getRuntime().maxMemory();
    private long savedFileBytes;

    private int loadedCenterX = Integer.MIN_VALUE;
    private int loadedCenterZ = Integer.MIN_VALUE;

    private RaycastHit targetedBlock;

    private double fpsTimer;
    private int fpsFrames;
    private int lastFps;

    private boolean debugOverlayVisible;

    // Save on exit only after world initialization succeeds; record respawn only after player
    // placement succeeds.
    private boolean worldInitialized;
    private boolean playerSpawnInitialized;

    private float walkBobPhase;
    private float walkBobVertical;
    private float walkBobHorizontal;

    public Game() {
        this(new World(Paths.get(GameConfig.WORLD_FILE), GameConfig.DEFAULT_WORLD_SEED));
    }

    Game(World world) {
        this.world = java.util.Objects.requireNonNull(world);
    }

    public void run() {
        try (ResourceScope resources = new ResourceScope()) {
            // Register the window before create() so partial initialization can be cleaned up.
            // Register GPU resources later so they close before the window destroys its context.
            resources.add(window);
            Throwable primaryFailure = null;
            try {
                window.create();
                input.attach(window.handle());

                initGraphicsState();

                resources.add(world);
                world.initialize();
                worldInitialized = true;

                // The HUD borrows the world renderer's atlas, so it must close before the renderer.
                worldRenderer = resources.add(new WorldRenderer());
                hudRenderer = resources.add(new HudRenderer(worldRenderer.atlas()));

                // Use the saved exit position when present; otherwise compute a new spawn point.
                Vector3f spawn = new Vector3f();
                if (!world.tryGetSavedRespawnPosition(spawn)) {
                    world.defaultSpawn(spawn);
                }

                player.restorePosition(world, spawn.x, spawn.y, spawn.z);
                playerSpawnInitialized = true;
                camera.setRotation(-90.0f, -15.0f);
                updateCameraFromPlayer(0.0f);

                window.captureCursor(false);
                input.resetMouseTracking();

                sampleResources();
                loop();
            } catch (RuntimeException | Error failure) {
                primaryFailure = failure;
                throw failure;
            } finally {
                saveOnUnexpectedExit(primaryFailure);
            }
        }
    }

    private void saveOnUnexpectedExit(Throwable primaryFailure) {
        // Normal exits have already saved or explicitly discarded. During another failure,
        // preserve the original error and attach any final-save error as suppressed evidence.
        if (!worldInitialized || saves.exitReady()) return;
        try {
            if (playerSpawnInitialized) {
                world.setRespawnPosition(player.position().x, player.position().y, player.position().z);
            }
            if (!safeSaveWorld(false)) {
                throw new IllegalStateException("Final world save failed", saves.failure());
            }
        } catch (RuntimeException | Error saveFailure) {
            if (primaryFailure != null) primaryFailure.addSuppressed(saveFailure);
            else throw saveFailure;
        }
    }

    private void initGraphicsState() {
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Render both sides of water, glass, and thin block geometry.
        glDisable(GL_CULL_FACE);
    }

    private void loop() {
        double lastTime = glfwGetTime();

        while (!saves.exitReady()) {
            double now = glfwGetTime();

            // Cap the simulation step to limit collision movement after a stalled frame.
            // Time beyond the cap is dropped, so simulation timers may lag wall-clock time.
            float delta = (float) Math.min(0.05, now - lastTime);
            lastTime = now;

            window.pollEvents();
            if (window.shouldClose()) {
                glfwSetWindowShouldClose(window.handle(), false);
                if (!saves.exitPromptOpen()) requestExit();
            }
            if (saves.exitReady()) break;
            handleInputState();
            if (saves.exitReady()) break;

            // Load one extra ring for neighbor sampling and interactions near the render boundary.
            int playerChunkX = Math.floorDiv((int) Math.floor(player.position().x), GameConfig.CHUNK_SIZE);
            int playerChunkZ = Math.floorDiv((int) Math.floor(player.position().z), GameConfig.CHUNK_SIZE);
            if (playerChunkX != loadedCenterX || playerChunkZ != loadedCenterZ) {
                world.ensureChunksAround(playerChunkX, playerChunkZ, GameConfig.RENDER_DISTANCE_CHUNKS + 1);
                loadedCenterX = playerChunkX;
                loadedCenterZ = playerChunkZ;
            }

            if (cursorCaptured) {
                applyLookFromMouse();
                player.update(input, camera, world, delta);
            }

            updateCameraFromPlayer(delta);
            updateTargetBlock();
            if (cursorCaptured) {
                updateBlockInteraction(delta);
            }

            // World time and water keep advancing while the cursor is released or a panel is open.
            if (!saves.exitPromptOpen()) {
                world.update(delta);
                worldRenderer.update(delta);
            }

            hotbarLabelTimer = Math.max(0.0f, hotbarLabelTimer - delta);

            worldRenderer.render(world, camera, window.width(), window.height(), targetedBlock);
            hudRenderer.render(hotbar, hotbarIndex, hotbarLabelTimer > 0.0f, creativeInventoryOpen, creativeBlocks,
                    creativeInventoryPage, creativeTotalPages, chestOpen, buildOverlayLines(), window.width(), window.height());

            updateDebugTitle(now);

            window.swapBuffers();
            input.endFrame();

            // Autosave is triggered by modified blocks or respawn state; a successful save writes
            // all loaded chunks.
            // A failed autosave remains visible until a later save succeeds.
            autosaveTimer += delta;
            if (autosaveTimer >= 20.0f && !saves.exitPromptOpen()) {
                if (world.hasPendingSave()) {
                    safeSaveWorld(false);
                }
                autosaveTimer = 0.0f;
            }
        }
    }

    private void handleInputState() {
        if (saves.exitPromptOpen()) {
            handleSavePrompt();
            return;
        }
        if (saves.hasFailure() && input.wasKeyPressed(GLFW_KEY_R)) {
            safeSaveWorld(false);
        }
        if (input.wasFocusLost() || !cursorCaptured) {
            player.resetControlSequence();
        }
        // Allow the debug overlay to toggle even while a panel is open.
        if (input.wasKeyPressed(GLFW_KEY_F3)) {
            debugOverlayVisible = !debugOverlayVisible;
        }

        if (chestOpen) {
            // Consume panel controls here so they cannot also trigger inventory or world actions.
            if (input.wasKeyPressed(GLFW_KEY_E) || input.wasKeyPressed(GLFW_KEY_ESCAPE)) {
                closeChest(true);
            }
            return;
        }

        if (input.wasKeyPressed(GLFW_KEY_E)) {
            if (creativeInventoryOpen) {
                closeCreativeInventory(true);
            } else {
                openCreativeInventory();
            }
        }

        if (creativeInventoryOpen) {
            if (input.wasKeyPressed(GLFW_KEY_ESCAPE)) {
                closeCreativeInventory(true);
            }
            handleHotbarKeySelection();
            // Route clicks to inventory selection while the panel is open.
            handleCreativeInventorySelection();
            return;
        }

        if (input.wasKeyPressed(GLFW_KEY_ESCAPE) && cursorCaptured) {
            setCursorCaptured(false);
        }

        // The click that captures the cursor must not also break the targeted block.
        if (!cursorCaptured && input.wasMousePressed(GLFW_MOUSE_BUTTON_LEFT)) {
            setCursorCaptured(true);
        }

        handleHotbarKeySelection();

        double scroll = input.consumeScrollDeltaY();
        if (scroll != 0.0) {
            int direction = scroll > 0.0 ? -1 : 1;
            hotbarIndex = Math.floorMod(hotbarIndex + direction, hotbar.length);
            hotbarLabelTimer = HOTBAR_LABEL_SECONDS;
        }
    }

    private void setCursorCaptured(boolean captured) {
        player.resetControlSequence();
        cursorCaptured = captured;
        if (captured) {
            suppressInteractionUntilRelease = true;
        }
        window.captureCursor(captured);
        input.resetMouseTracking();
    }

    private void handleHotbarKeySelection() {
        for (int i = 0; i < hotbar.length && i < 9; i++) {
            int key = GLFW_KEY_1 + i;
            if (input.wasKeyPressed(key)) {
                hotbarIndex = i;
                hotbarLabelTimer = HOTBAR_LABEL_SECONDS;
            }
        }
    }

    private void handleCreativeInventorySelection() {
        // Consume scrolling once here; the world hotbar handler is skipped while inventory is open.
        double scroll = input.consumeScrollDeltaY();
        if (scroll != 0.0) {
            int direction = scroll > 0.0 ? -1 : 1;
            setCreativeInventoryPage(creativeInventoryPage + direction);
        }

        boolean leftClick = input.wasMousePressed(GLFW_MOUSE_BUTTON_LEFT);
        boolean rightClick = input.wasMousePressed(GLFW_MOUSE_BUTTON_RIGHT);
        if (!leftClick && !rightClick) {
            return;
        }

        int blockIndex = HudRenderer.creativeSlotAt(
                input.mouseX(),
                input.mouseY(),
                window.windowWidth(),
                window.windowHeight(),
                creativeInventoryPage,
                creativeBlocks.length);

        if (blockIndex < 0 || blockIndex >= creativeBlocks.length) {
            return;
        }

        hotbar[hotbarIndex] = creativeBlocks[blockIndex];
        hotbarLabelTimer = HOTBAR_LABEL_SECONDS;

        // Right-click selects and closes the inventory; left-click keeps it open.
        if (rightClick) {
            closeCreativeInventory(true);
        }
    }

    private void openCreativeInventory() {
        // Restore the prior cursor capture state when this panel closes.
        recaptureCursorAfterInventory = cursorCaptured;
        creativeInventoryOpen = true;
        setCursorCaptured(false);
        focusCreativePageOnSelectedBlock();
    }

    private void closeCreativeInventory(boolean recaptureIfNeeded) {
        if (!creativeInventoryOpen) {
            return;
        }

        creativeInventoryOpen = false;
        if (recaptureIfNeeded && recaptureCursorAfterInventory) {
            setCursorCaptured(true);
        }
        recaptureCursorAfterInventory = false;
    }

    private void openChest() {
        // Keep the chest and creative inventory mutually exclusive so they do not compete for
        // cursor state.
        closeCreativeInventory(false);
        recaptureCursorAfterChest = cursorCaptured;
        chestOpen = true;
        setCursorCaptured(false);
    }

    private void closeChest(boolean recaptureIfNeeded) {
        if (!chestOpen) {
            return;
        }

        chestOpen = false;
        if (recaptureIfNeeded && recaptureCursorAfterChest) {
            setCursorCaptured(true);
        }
        recaptureCursorAfterChest = false;
    }

    private void focusCreativePageOnSelectedBlock() {
        // Open the inventory on the page containing the currently selected hotbar block.
        BlockType selected = hotbar[hotbarIndex];
        for (int i = 0; i < creativeBlocks.length; i++) {
            if (creativeBlocks[i] == selected) {
                creativeInventoryPage = i / HudRenderer.CREATIVE_SLOTS_PER_PAGE;
                return;
            }
        }
        setCreativeInventoryPage(creativeInventoryPage);
    }

    private void setCreativeInventoryPage(int page) {
        creativeInventoryPage = Math.max(0, Math.min(page, creativeTotalPages - 1));
    }

    private void applyLookFromMouse() {
        float yawDelta = (float) input.mouseDeltaX() * GameConfig.MOUSE_SENSITIVITY;
        float pitchDelta = (float) -input.mouseDeltaY() * GameConfig.MOUSE_SENSITIVITY;
        camera.rotate(yawDelta, pitchDelta);
    }

    private void updateCameraFromPlayer(float deltaSeconds) {
        updateWalkBob(deltaSeconds);

        // Apply bobbing along the camera's right vector without moving the collision box.
        // The step offset smooths camera height after the body steps upward.
        Vector3f right = camera.right(tmpCameraRight);
        camera.setPosition(
                player.position().x + right.x * walkBobHorizontal,
                player.position().y + player.eyeHeight() + walkBobVertical + player.stepSmoothingOffset(),
                player.position().z + right.z * walkBobHorizontal);
    }

    private void updateWalkBob(float deltaSeconds) {
        if (deltaSeconds <= 0.0f) {
            walkBobVertical = 0.0f;
            walkBobHorizontal = 0.0f;
            return;
        }

        // Ease bobbing back to zero when walking stops or control mode changes.
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

        // Wait for both buttons to be released so recapturing the cursor cannot edit the world.
        if (suppressInteractionUntilRelease) {
            if (input.isMouseDown(GLFW_MOUSE_BUTTON_LEFT) || input.isMouseDown(GLFW_MOUSE_BUTTON_RIGHT)) {
                return;
            }
            suppressInteractionUntilRelease = false;
            // Discard this whole frame, including a press and release delivered together.
            return;
        }

        // A fresh press acts immediately; cooldowns only limit held-button repetition.
        if (input.wasMousePressed(GLFW_MOUSE_BUTTON_LEFT)) {
            breakCooldown = 0.0f;
        }
        if (input.wasMousePressed(GLFW_MOUSE_BUTTON_RIGHT)) {
            placeCooldown = 0.0f;
        }

        // Held left-click repeats breaking, except for bedrock.
        if (targetedBlock != null
                && (input.wasMousePressed(GLFW_MOUSE_BUTTON_LEFT) || input.isMouseDown(GLFW_MOUSE_BUTTON_LEFT))
                && breakCooldown <= 0.0f) {
            if (targetedBlock.block() != BlockType.BEDROCK) {
                breakTargetedBlock();
                updateTargetBlock();
            }
            breakCooldown = GameConfig.BREAK_COOLDOWN_SECONDS;
        }

        // Right-click interaction takes priority over block placement.
        if (targetedBlock != null
                && (input.wasMousePressed(GLFW_MOUSE_BUTTON_RIGHT) || input.isMouseDown(GLFW_MOUSE_BUTTON_RIGHT))
                && placeCooldown <= 0.0f) {
            if (!interactWithTargetedBlock()) {
                int px = targetedBlock.x() + targetedBlock.normalX();
                int py = targetedBlock.y() + targetedBlock.normalY();
                int pz = targetedBlock.z() + targetedBlock.normalZ();

                placeSelectedBlock(px, py, pz);
            }

            placeCooldown = GameConfig.PLACE_COOLDOWN_SECONDS;
        }
    }

    private boolean interactWithTargetedBlock() {
        BlockType block = targetedBlock.block();

        if (block.isDoorBlock()) {
            toggleDoorBlock();
            return true;
        }

        if (block.isTrapdoorBlock()) {
            toggleTrapdoorBlock();
            return true;
        }

        if (block == BlockType.CHEST) {
            openChest();
            return true;
        }

        return false;
    }

    private void toggleDoorBlock() {
        // Validate both halves before toggling a door so malformed pairs cannot modify unrelated
        // blocks.
        int x = targetedBlock.x();
        int y = targetedBlock.y();
        int z = targetedBlock.z();
        BlockType hitBlock = targetedBlock.block();
        int bottomY = hitBlock.isDoorTop() ? y - 1 : y;
        BlockType bottom = world.getBlock(x, bottomY, z);
        BlockType top = world.getBlock(x, bottomY + 1, z);

        if (!bottom.isDoorBlock() || bottom.isDoorTop() || top != bottom.matchingDoorHalf()) {
            return;
        }

        BlockType newBottom = bottom.toggledDoorVariant();
        BlockType newTop = newBottom.doorTopVariant();
        if (player.intersectsBlock(x, bottomY, z, newBottom)
                || player.intersectsBlock(x, bottomY + 1, z, newTop)) {
            return;
        }

        world.setBlock(x, bottomY, z, newBottom);
        world.setBlock(x, bottomY + 1, z, newTop);
    }

    private void toggleTrapdoorBlock() {
        // Reject a trapdoor toggle if its new collision shape would overlap the player.
        int x = targetedBlock.x();
        int y = targetedBlock.y();
        int z = targetedBlock.z();
        BlockType next = targetedBlock.block().toggledTrapdoorVariant();

        if (player.intersectsBlock(x, y, z, next)) {
            return;
        }

        world.setBlock(x, y, z, next);
    }

    // Remove the other door half only if it matches the targeted half's orientation and state.
    private void breakTargetedBlock() {
        BlockType block = targetedBlock.block();
        int x = targetedBlock.x();
        int y = targetedBlock.y();
        int z = targetedBlock.z();

        worldRenderer.spawnBlockBreakEffect(world, block, x, y, z);
        world.setBlock(x, y, z, BlockType.AIR);

        if (!block.isDoorBlock()) {
            return;
        }

        int otherY = block.isDoorTop() ? y - 1 : y + 1;
        BlockType other = world.getBlock(x, otherY, z);
        if (other == block.matchingDoorHalf()) {
            worldRenderer.spawnBlockBreakEffect(world, other, x, otherY, z);
            world.setBlock(x, otherY, z, BlockType.AIR);
        }
    }

    // Convert palette entries to placed variants before checking their actual collision shapes.
    private void placeSelectedBlock(int x, int y, int z) {
        BlockType selected = hotbar[hotbarIndex];
        int facing = selected == BlockType.LADDER ? ladderFacingFromTargetNormal() : facingFromCamera();
        BlockType placed = selected.placedVariantForFacing(facing);

        if (selected.placesAsDoor()) {
            placeDoorBlock(x, y, z, placed);
            return;
        }

        if (!canPlaceAt(x, y, z, placed)) {
            return;
        }

        world.setBlock(x, y, z, placed);
    }

    // Check space for both door halves before placing either one.
    // If the upper write returns false, clear the new lower half to AIR; exceptions do not trigger
    // this cleanup.
    private void placeDoorBlock(int x, int y, int z, BlockType bottom) {
        BlockType top = bottom.doorTopVariant();
        int topY = y + 1;

        if (!canPlaceAt(x, y, z, bottom) || !canPlaceAt(x, topY, z, top)) {
            return;
        }

        if (world.setBlock(x, y, z, bottom) && !world.setBlock(x, topY, z, top)) {
            world.setBlock(x, y, z, BlockType.AIR);
        }
    }

    // Placement can replace air or water, but the new collision shape must not overlap the player.
    private boolean canPlaceAt(int x, int y, int z, BlockType type) {
        if (!WorldBounds.containsCell(x, y, z)) {
            return false;
        }

        BlockType current = world.getBlock(x, y, z);
        return (current == BlockType.AIR || current.isWaterBlock()) && !player.intersectsBlock(x, y, z, type);
    }

    // Facing indices are shared with BlockType: 0 = north, 1 = east, 2 = south, 3 = west.
    private int facingFromCamera() {
        camera.forward(tmpRayDirection);
        float absX = Math.abs(tmpRayDirection.x);
        float absZ = Math.abs(tmpRayDirection.z);

        if (absX > absZ) {
            return tmpRayDirection.x >= 0.0f ? 1 : 3;
        }
        return tmpRayDirection.z >= 0.0f ? 2 : 0;
    }

    // Attach ladders to the selected side face; top/bottom hits fall back to camera facing.
    private int ladderFacingFromTargetNormal() {
        if (targetedBlock.normalZ() > 0) {
            return 0;
        }
        if (targetedBlock.normalX() < 0) {
            return 1;
        }
        if (targetedBlock.normalZ() < 0) {
            return 2;
        }
        if (targetedBlock.normalX() > 0) {
            return 3;
        }
        return facingFromCamera();
    }

    // Return null when debug output is hidden so the HUD skips it.
    private String[] buildDebugLines() {
        if (!debugOverlayVisible) {
            return null;
        }

        Vector3f position = player.position();
        int blockX = (int) Math.floor(position.x);
        int blockY = (int) Math.floor(position.y);
        int blockZ = (int) Math.floor(position.z);
        int chunkX = Math.floorDiv(blockX, GameConfig.CHUNK_SIZE);
        int chunkZ = Math.floorDiv(blockZ, GameConfig.CHUNK_SIZE);

        // World time zero represents 06:00.
        float dayHours = (6.0f + world.timeOfDay() * 24.0f) % 24.0f;
        int hour = (int) dayHours;
        int minute = (int) ((dayHours - hour) * 60.0f);

        String facing = switch (facingFromCamera()) {
            case 0 -> "north";
            case 1 -> "east";
            case 2 -> "south";
            default -> "west";
        };

        return new String[] {
                "Minecraftin " + lastFps + " fps",
                String.format(Locale.ROOT, "XYZ: %.2f / %.2f / %.2f", position.x, position.y, position.z),
                "Block: " + blockX + " " + blockY + " " + blockZ,
                "Chunk: " + chunkX + " " + chunkZ + " loaded: " + world.chunkCount(),
                "Facing: " + facing,
                "Light: " + world.skyLightAt(blockX, blockY, blockZ) + " sky, "
                        + world.blockLightAt(blockX, blockY, blockZ) + " block",
                String.format(Locale.ROOT, "Time: %02d:%02d", hour, minute),
        };
    }

    private void updateDebugTitle(double now) {
        fpsFrames++;

        if (fpsTimer == 0.0) {
            fpsTimer = now;
        }

        double elapsed = now - fpsTimer;

        // Throttle title updates to about once per second.
        if (elapsed >= 1.0) {
            sampleResources();
            int fps = (int) Math.round(fpsFrames / elapsed);
            lastFps = fps;
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

    private void requestExit() {
        world.setRespawnPosition(player.position().x, player.position().y, player.position().z);
        if (!safeSaveWorld(true)) {
            recaptureAfterSavePrompt = cursorCaptured;
            closeCreativeInventory(false);
            closeChest(false);
            setCursorCaptured(false);
        }
    }

    private void handleSavePrompt() {
        if (input.wasKeyPressed(GLFW_KEY_ESCAPE)) {
            saves.cancel();
            if (!saves.exitPromptOpen()) setCursorCaptured(recaptureAfterSavePrompt);
        } else if (saves.exitState() == SaveController.ExitState.CONFIRM_DISCARD) {
            if (input.wasKeyPressed(GLFW_KEY_Y)) saves.confirmDiscard();
        } else if (input.wasKeyPressed(GLFW_KEY_R)) {
            safeSaveWorld(true);
        } else if (input.wasKeyPressed(GLFW_KEY_D)) {
            saves.requestDiscard();
        }
    }

    private boolean safeSaveWorld(boolean exiting) {
        boolean saved = saves.attempt(world::save, exiting);
        if (!saved) {
            System.err.println("World save failed: " + saves.failure().getMessage());
            saves.failure().printStackTrace(System.err);
        }
        sampleResources();
        return saved;
    }

    private void sampleResources() {
        Runtime runtime = Runtime.getRuntime();
        sampledHeapUsed = runtime.totalMemory() - runtime.freeMemory();
        sampledHeapMax = runtime.maxMemory();
        try {
            var path = Paths.get(GameConfig.WORLD_FILE);
            savedFileBytes = Files.exists(path) ? Files.size(path) : 0;
        } catch (java.io.IOException ignored) {
            // A size read is telemetry only; it must not change a successful save result.
        }
    }

    private String[] buildOverlayLines() {
        List<String> lines = new ArrayList<>();
        if (saves.hasFailure()) {
            lines.add("SAVE FAILED - CHANGES ARE NOT SAVED");
            if (saves.exitState() == SaveController.ExitState.CONFIRM_DISCARD) {
                lines.add("DISCARD UNSAVED CHANGES AND EXIT?");
                lines.add("Y: CONFIRM DISCARD   ESC: BACK");
            } else if (saves.exitPromptOpen()) {
                lines.add("R: RETRY SAVE AND EXIT   ESC: RETURN TO GAME");
                lines.add("D: REVIEW DISCARD AND EXIT");
            } else {
                lines.add("R: RETRY SAVE. CHECK DISK SPACE AND PERMISSIONS.");
            }
        }
        if (ResourceBudget.exceeded(world.chunkCount(), sampledHeapUsed, sampledHeapMax, saves.lastSaveMillis())) {
            lines.add("RESOURCE BUDGET REACHED - STOP EXPLORING");
            lines.add("SAVE AND EXIT; BACK UP BEFORE STARTING A NEW WORLD");
        }
        String[] debug = buildDebugLines();
        if (debug != null) {
            java.util.Collections.addAll(lines, debug);
            lines.add(String.format(Locale.ROOT, "Heap: %d / %d MiB (warning at 75%%)",
                    sampledHeapUsed / 1048576, sampledHeapMax / 1048576));
            lines.add(String.format(Locale.ROOT, "Chunk arrays: %d MiB; budget: %d chunks",
                    ResourceBudget.chunkPayloadBytes(world.chunkCount()) / 1048576, ResourceBudget.CHUNK_WARNING));
            lines.add(String.format(Locale.ROOT, "Save: %.1f MiB / %.0f ms (budget: 1000 ms)",
                    savedFileBytes / 1048576.0, saves.lastSaveMillis()));
        }
        return lines.isEmpty() ? null : lines.toArray(String[]::new);
    }

    private float approach(float current, float target, float delta) {
        if (current < target) {
            return Math.min(current + delta, target);
        }
        return Math.max(current - delta, target);
    }
}

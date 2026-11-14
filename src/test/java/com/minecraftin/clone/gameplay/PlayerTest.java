package com.minecraftin.clone.gameplay;

import com.minecraftin.clone.engine.Camera;
import com.minecraftin.clone.engine.InputState;
import com.minecraftin.clone.world.BlockType;
import com.minecraftin.clone.world.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import java.nio.file.Path;
import static org.lwjgl.glfw.GLFW.*;
import static org.junit.jupiter.api.Assertions.*;

final class PlayerTest {
    @TempDir Path tempDir;

    @Test
    void lookingNearlyStraightDownPreservesDiagonalMovement() throws Exception {
        World world = new World(tempDir.resolve("world.dat"), 123);
        Player player = new Player();
        player.setPosition(8, 124, 8);
        Camera camera = new Camera();
        camera.setRotation(-90, -89.9f);
        InputState input = input(GLFW_KEY_W, GLFW_KEY_D);
        for (int i = 0; i < 10; i++) {
            player.update(input, camera, world, 0.05f);
            input.endFrame();
        }
        assertEquals(player.position().x - 8, 8 - player.position().z, 0.002f);
    }

    @Test
    void fallingAgainstSlabDoesNotStepUpInMidair() throws Exception {
        World world = new World(tempDir.resolve("world.dat"), 123);
        world.setBlock(9, 120, 8, BlockType.OAK_SLAB);
        Player player = new Player();
        player.setPosition(8.69f, 120.2f, 8.5f);
        Camera camera = new Camera();
        InputState input = input(GLFW_KEY_D);
        float initialY = player.position().y;
        player.update(input, camera, world, 0.05f);
        assertTrue(player.position().y < initialY);
        assertTrue(player.position().x < 8.71f);
    }

    @Test
    void supportedPlayerCanStillWalkOntoSlab() throws Exception {
        World world = new World(tempDir.resolve("world.dat"), 123);
        world.setBlock(8, 119, 8, BlockType.STONE);
        world.setBlock(9, 119, 8, BlockType.STONE);
        world.setBlock(9, 120, 8, BlockType.OAK_SLAB);
        Player player = new Player();
        player.setPosition(8.65f, 120.01f, 8.5f);
        InputState input = input(GLFW_KEY_D);
        Camera camera = new Camera();
        for (int i = 0; i < 8; i++) {
            player.update(input, camera, world, 0.05f);
            input.endFrame();
        }
        assertTrue(player.position().x > 9);
        assertTrue(player.position().y >= 120.5f);
    }

    @Test
    void restoresSavedCrouchUnderCeilingWithAndWithoutShift() throws Exception {
        Path save = tempDir.resolve("crouch.dat");
        float savedY;
        try (World world = new World(save, 123)) {
            world.initialize();
            world.setBlock(8, 120, 8, BlockType.OAK_SLAB);
            Player original = new Player();
            original.setPosition(8.5f, 120.51f, 8.5f);
            original.update(input(GLFW_KEY_LEFT_SHIFT), new Camera(), world, 0.016f);
            assertFalse(original.intersectsBlock(8, 122, 8, BlockType.STONE));
            world.setBlock(8, 122, 8, BlockType.STONE);
            original.update(new InputState(), new Camera(), world, 0.016f);
            savedY = original.position().y;
            world.setRespawnPosition(8.5f, savedY, 8.5f);
            world.save();
        }
        try (World loaded = new World(save, 123)) {
            loaded.initialize();
            var spawn = new org.joml.Vector3f();
            assertTrue(loaded.tryGetSavedRespawnPosition(spawn));
            for (InputState keys : new InputState[] {new InputState(), input(GLFW_KEY_LEFT_SHIFT)}) {
                Player restored = new Player();
                restored.restorePosition(loaded, spawn.x, spawn.y, spawn.z);
                assertEquals(1.25f, restored.eyeHeight());
                restored.update(keys, new Camera(), loaded, 0.016f);
                assertEquals(8.5f, restored.position().x);
                assertEquals(8.5f, restored.position().z);
                assertEquals(savedY, restored.position().y, 0.02f);
                assertFalse(restored.isFlying());
            }
            Player restored = new Player();
            restored.restorePosition(loaded, spawn.x, spawn.y, spawn.z);
            loaded.setBlock(8, 122, 8, BlockType.AIR);
            for (int i = 0; i < 12; i++) {
                restored.update(new InputState(), new Camera(), loaded, 0.016f);
            }
            assertEquals(1.62f, restored.eyeHeight(), 0.001f);
        }
    }

    @Test
    void controlResetEndsOldSpaceGestureButPreservesFreshDoubleTap() throws Exception {
        World world = new World(tempDir.resolve("gesture.dat"), 123);
        Player player = new Player();
        player.setPosition(8, 124, 8);
        player.update(input(GLFW_KEY_SPACE), new Camera(), world, 0.016f);
        assertFalse(player.isFlying());
        player.resetControlSequence();
        player.update(input(GLFW_KEY_SPACE), new Camera(), world, 0.016f);
        assertFalse(player.isFlying());
        player.update(input(GLFW_KEY_SPACE), new Camera(), world, 0.016f);
        assertTrue(player.isFlying());
    }

    @Test
    void restoreStillRecoversTrulyBlockedStandingPosition() {
        World world = new World(tempDir.resolve("blocked.dat"), 123);
        world.setBlock(8, 120, 8, BlockType.STONE);
        Player player = new Player();
        player.restorePosition(world, 8.5f, 120, 8.5f);
        assertTrue(player.position().y >= 121);
        assertEquals(1.62f, player.eyeHeight());
    }

    @Test
    void rejectsExtremeFinitePositionsAndMutablePositionCorruptionPromptly() {
        Player player = new Player();
        assertThrows(IllegalArgumentException.class, () -> player.setPosition(Float.MAX_VALUE, 120, 8));
        assertThrows(IllegalArgumentException.class, () -> player.setPosition(8, 1.0e20f, 8));
        player.position().set(Float.MAX_VALUE, 1.0e20f, 0);
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(1), () ->
                assertThrows(IllegalArgumentException.class, () -> player.update(new InputState(), new Camera(),
                        new World(tempDir.resolve("extreme.dat"), 123), 0.016f)));
    }

    @Test
    void movementCannotCrossAnyHorizontalWorldEdge() throws Exception {
        float min = com.minecraftin.clone.world.WorldBounds.MIN_PLAYER_XZ;
        float max = com.minecraftin.clone.world.WorldBounds.MAX_PLAYER_XZ;
        float[][] positions = {{min, 0}, {max, 0}, {0, min}, {0, max}};
        int[] keys = {GLFW_KEY_A, GLFW_KEY_D, GLFW_KEY_W, GLFW_KEY_S};
        World world = new World(tempDir.resolve("edges.dat"), 123);
        for (int edge = 0; edge < positions.length; edge++) {
            Player player = new Player();
            player.setPosition(positions[edge][0], 124, positions[edge][1]);
            InputState input = input(keys[edge]);
            for (int i = 0; i < 20; i++) {
                player.update(input, new Camera(), world, 0.05f);
                input.endFrame();
            }
            assertTrue(com.minecraftin.clone.world.WorldBounds.containsPlayerPosition(
                    player.position().x, player.position().y, player.position().z));
            assertEquals(positions[edge][0], player.position().x, 0.001f);
            assertEquals(positions[edge][1], player.position().z, 0.001f);
        }
    }

    // Inject key presses through the callback so movement tests do not need a native window.
    private static InputState input(int... keys) throws Exception {
        InputState input = new InputState();
        var field = InputState.class.getDeclaredField("keyCallback");
        field.setAccessible(true);
        var callback = (GLFWKeyCallbackI) field.get(input);
        for (int key : keys) {
            callback.invoke(0, key, 0, GLFW_PRESS, 0);
        }
        return input;
    }
}

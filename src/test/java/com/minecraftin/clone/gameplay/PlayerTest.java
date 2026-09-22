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

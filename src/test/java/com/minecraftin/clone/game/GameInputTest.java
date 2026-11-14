package com.minecraftin.clone.game;

import com.minecraftin.clone.engine.InputState;
import com.minecraftin.clone.gameplay.Player;
import com.minecraftin.clone.world.BlockType;
import com.minecraftin.clone.world.RaycastHit;
import com.minecraftin.clone.world.World;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWWindowFocusCallbackI;
import java.util.Locale;
import static org.lwjgl.glfw.GLFW.*;
import static org.junit.jupiter.api.Assertions.*;

final class GameInputTest {
    @Test
    void sameFrameRightPressReleasePlacesOnceAndHeldPressHonorsCooldown() throws Exception {
        Game game = fixture();
        InputState input = field(game, "input", InputState.class);
        World world = field(game, "world", World.class);
        click(input, GLFW_MOUSE_BUTTON_RIGHT, true);
        interact(game, 0.016f);
        assertEquals(BlockType.GRASS, world.getBlock(8, 121, 8));
        world.setBlock(8, 121, 8, BlockType.AIR);
        input.endFrame();
        interact(game, 0.016f);
        assertEquals(BlockType.AIR, world.getBlock(8, 121, 8));
        click(input, GLFW_MOUSE_BUTTON_RIGHT, false);
        interact(game, 0.016f);
        assertEquals(BlockType.GRASS, world.getBlock(8, 121, 8));
        input.endFrame();
        world.setBlock(8, 121, 8, BlockType.AIR);
        interact(game, 0.016f);
        assertEquals(BlockType.AIR, world.getBlock(8, 121, 8));
        interact(game, 0.21f);
        assertEquals(BlockType.GRASS, world.getBlock(8, 121, 8));
    }

    @Test
    void sameFrameLeftPressReleaseReachesBreakGate() throws Exception {
        Game game = fixture();
        set(game, "targetedBlock", new RaycastHit(8, 120, 8, 0, 1, 0, 1, BlockType.BEDROCK));
        click(field(game, "input", InputState.class), GLFW_MOUSE_BUTTON_LEFT, true);
        interact(game, 0.016f);
        assertEquals(0.25f, field(game, "breakCooldown", Float.class));
    }

    @Test
    void captureOrPanelClosingClickIsDiscardedEvenAfterSameFrameRelease() throws Exception {
        for (boolean released : new boolean[] {true, false}) {
            Game game = fixture();
            InputState input = field(game, "input", InputState.class);
            World world = field(game, "world", World.class);
            set(game, "suppressInteractionUntilRelease", true);
            click(input, GLFW_MOUSE_BUTTON_RIGHT, released);
            interact(game, 0.016f);
            assertEquals(BlockType.AIR, world.getBlock(8, 121, 8));
            input.endFrame();
            callback(input).invoke(0, GLFW_MOUSE_BUTTON_RIGHT, GLFW_RELEASE, 0);
            interact(game, 0.016f);
            input.endFrame();
            click(input, GLFW_MOUSE_BUTTON_RIGHT, true);
            interact(game, 0.016f);
            assertEquals(BlockType.GRASS, world.getBlock(8, 121, 8));
        }
    }

    @Test
    void focusLossClearsGameplayGesture() throws Exception {
        Game game = fixture();
        set(game, "cursorCaptured", true);
        Player player = field(game, "player", Player.class);
        set(player, "timeSinceLastSpaceTap", 0.01f);
        InputState input = field(game, "input", InputState.class);
        field(input, "focusCallback", GLFWWindowFocusCallbackI.class).invoke(0, false);
        invoke(game, "handleInputState");
        assertEquals(Float.POSITIVE_INFINITY, field(player, "timeSinceLastSpaceTap", Float.class));
    }

    @Test
    void debugNumbersStayAsciiInEveryFormatLocale() throws Exception {
        Locale original = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Game game = fixture();
            set(game, "debugOverlayVisible", true);
            String[] expected = null;
            for (String tag : new String[] {"en-US", "zh-TW", "ar-EG"}) {
                Locale.setDefault(Locale.Category.FORMAT, Locale.forLanguageTag(tag));
                String[] lines = (String[]) invoke(game, "buildDebugLines");
                if (expected == null) expected = lines;
                assertArrayEquals(expected, lines);
                for (String line : lines) assertTrue(line.chars().allMatch(c -> c >= 32 && c < 127));
            }
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, original);
        }
    }

    @Test
    void placingPastWorldEdgeIsRejectedWithoutEndingGame() throws Exception {
        Game game = fixture();
        int min = com.minecraftin.clone.world.WorldBounds.MIN_BLOCK;
        int max = com.minecraftin.clone.world.WorldBounds.MAX_BLOCK;
        var method = Game.class.getDeclaredMethod("canPlaceAt", int.class, int.class, int.class, BlockType.class);
        method.setAccessible(true);
        for (int[] position : new int[][] {{min - 1, 8}, {max + 1, 8}, {8, min - 1}, {8, max + 1}}) {
            assertEquals(false, method.invoke(game, position[0], 120, position[1], BlockType.STONE));
        }
    }

    private static Game fixture() throws Exception {
        Game game = new Game();
        field(game, "player", Player.class).setPosition(12, 124, 12);
        World world = field(game, "world", World.class);
        world.setBlock(8, 120, 8, BlockType.STONE);
        world.setBlock(8, 121, 8, BlockType.AIR);
        set(game, "targetedBlock", new RaycastHit(8, 120, 8, 0, 1, 0, 1, BlockType.STONE));
        return game;
    }

    private static void click(InputState input, int button, boolean release) throws Exception {
        callback(input).invoke(0, button, GLFW_PRESS, 0);
        if (release) callback(input).invoke(0, button, GLFW_RELEASE, 0);
    }

    private static GLFWMouseButtonCallbackI callback(InputState input) throws Exception {
        return field(input, "mouseCallback", GLFWMouseButtonCallbackI.class);
    }

    private static void interact(Game game, float delta) throws Exception {
        var method = Game.class.getDeclaredMethod("updateBlockInteraction", float.class);
        method.setAccessible(true);
        method.invoke(game, delta);
    }

    private static Object invoke(Game game, String name) throws Exception {
        var method = Game.class.getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(game);
    }

    private static <T> T field(Object object, String name, Class<T> type) throws Exception {
        var field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(object));
    }

    private static void set(Object object, String name, Object value) throws Exception {
        var field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }
}

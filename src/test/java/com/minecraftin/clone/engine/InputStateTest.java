package com.minecraftin.clone.engine;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.glfw.GLFWWindowFocusCallbackI;
import static org.lwjgl.glfw.GLFW.*;
import static org.junit.jupiter.api.Assertions.*;

final class InputStateTest {
    @Test
    void unconsumedScrollDoesNotLeakIntoNextFrame() throws Exception {
        InputState input = new InputState();
        callback(input, "scrollCallback", GLFWScrollCallbackI.class).invoke(0, 0, 3);
        input.endFrame();
        assertEquals(0, input.consumeScrollDeltaY());
    }

    @Test
    void losingFocusClearsHeldAndInstantaneousInput() throws Exception {
        InputState input = new InputState();
        callback(input, "keyCallback", GLFWKeyCallbackI.class).invoke(0, GLFW_KEY_W, 0, GLFW_PRESS, 0);
        callback(input, "mouseCallback", GLFWMouseButtonCallbackI.class).invoke(0, 0, GLFW_PRESS, 0);
        callback(input, "focusCallback", GLFWWindowFocusCallbackI.class).invoke(0, false);
        assertFalse(input.isKeyDown(GLFW_KEY_W));
        assertFalse(input.wasKeyPressed(GLFW_KEY_W));
        assertFalse(input.isMouseDown(0));
        assertFalse(input.wasMousePressed(0));
    }

    private static <T> T callback(InputState input, String name, Class<T> type) throws Exception {
        var field = InputState.class.getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(input));
    }
}

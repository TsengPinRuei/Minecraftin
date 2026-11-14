package com.minecraftin.clone.engine;

import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.glfw.GLFWWindowFocusCallbackI;

import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;

// Keep held input separate from press events that last for one frame.
public final class InputState {
    private final boolean[] keys = new boolean[GLFW_KEY_LAST + 1];

    private final boolean[] keysPressed = new boolean[GLFW_KEY_LAST + 1];

    private final boolean[] mousePressed = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];

    private final boolean[] mouseDown = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];

    // Treat the next cursor event as a new baseline to avoid a large camera jump.
    private boolean firstMouse = true;

    private double lastMouseX;
    private double lastMouseY;

    private double mouseDeltaX;
    private double mouseDeltaY;

    private double scrollDeltaY;

    private boolean focusLost;

    private final GLFWKeyCallbackI keyCallback = (window, key, scancode, action, mods) -> {
        if (key < 0 || key >= keys.length) {
            return;
        }

        if (action == GLFW_PRESS) {
            keys[key] = true;
            keysPressed[key] = true;
        } else if (action == GLFW_RELEASE) {
            keys[key] = false;
        }
    };

    private final GLFWMouseButtonCallbackI mouseCallback = (window, button, action, mods) -> {
        if (button < 0 || button >= mousePressed.length) {
            return;
        }

        if (action == GLFW_PRESS) {
            mousePressed[button] = true;
            mouseDown[button] = true;
        } else if (action == GLFW_RELEASE) {
            mouseDown[button] = false;
        }
    };

    private final GLFWCursorPosCallbackI cursorCallback = (window, xpos, ypos) -> {
        // Establish a baseline before accumulating movement.
        if (firstMouse) {
            lastMouseX = xpos;
            lastMouseY = ypos;
            firstMouse = false;
        }

        mouseDeltaX += xpos - lastMouseX;
        mouseDeltaY += ypos - lastMouseY;

        lastMouseX = xpos;
        lastMouseY = ypos;
    };

    private final GLFWScrollCallbackI scrollCallback = (window, xOffset, yOffset) -> scrollDeltaY += yOffset;

    private final GLFWWindowFocusCallbackI focusCallback = (window, focused) -> {
        if (!focused) {
            Arrays.fill(keys, false);
            Arrays.fill(mouseDown, false);
            endFrame();
            focusLost = true;
        }
        // Reset on focus changes so the next cursor position cannot cause a camera jump.
        resetMouseTracking();
    };

    public void attach(long windowHandle) {
        glfwSetKeyCallback(windowHandle, keyCallback);
        glfwSetCursorPosCallback(windowHandle, cursorCallback);
        glfwSetMouseButtonCallback(windowHandle, mouseCallback);
        glfwSetScrollCallback(windowHandle, scrollCallback);
        glfwSetWindowFocusCallback(windowHandle, focusCallback);
    }

    public boolean wasFocusLost() {
        return focusLost;
    }

    public boolean isKeyDown(int keyCode) {
        return keyCode >= 0 && keyCode < keys.length && keys[keyCode];
    }

    public boolean wasKeyPressed(int keyCode) {
        return keyCode >= 0 && keyCode < keysPressed.length && keysPressed[keyCode];
    }

    public boolean wasMousePressed(int button) {
        return button >= 0 && button < mousePressed.length && mousePressed[button];
    }

    public boolean isMouseDown(int button) {
        return button >= 0 && button < mouseDown.length && mouseDown[button];
    }

    public double mouseDeltaX() {
        return mouseDeltaX;
    }

    public double mouseDeltaY() {
        return mouseDeltaY;
    }

    // GLFW window coordinates, not framebuffer pixels; use window dimensions for HUD hit tests.
    public double mouseX() {
        return lastMouseX;
    }

    // GLFW window coordinates, not framebuffer pixels; use window dimensions for HUD hit tests.
    public double mouseY() {
        return lastMouseY;
    }

    // Consume accumulated vertical scrolling once; subsequent reads return zero until another
    // event.
    public double consumeScrollDeltaY() {
        double value = scrollDeltaY;
        scrollDeltaY = 0.0;
        return value;
    }

    // Clear transient input only after all consumers have processed the frame.
    public void endFrame() {
        focusLost = false;
        Arrays.fill(keysPressed, false);
        Arrays.fill(mousePressed, false);
        scrollDeltaY = 0.0;
        mouseDeltaX = 0.0;
        mouseDeltaY = 0.0;
    }

    // Reset after cursor capture changes so movement is not measured from an old cursor position.
    public void resetMouseTracking() {
        firstMouse = true;
        mouseDeltaX = 0.0;
        mouseDeltaY = 0.0;
    }
}

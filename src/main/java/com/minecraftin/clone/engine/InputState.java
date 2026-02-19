package com.minecraftin.clone.engine;

import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;

import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;

public final class InputState {
    private final boolean[] keys = new boolean[GLFW_KEY_LAST + 1];
    private final boolean[] keysPressed = new boolean[GLFW_KEY_LAST + 1];
    private final boolean[] mousePressed = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];

    private boolean firstMouse = true;
    private double lastMouseX;
    private double lastMouseY;
    private double mouseDeltaX;
    private double mouseDeltaY;
    private double scrollDeltaY;

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
        }
    };

    private final GLFWCursorPosCallbackI cursorCallback = (window, xpos, ypos) -> {
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

    public void attach(long windowHandle) {
        glfwSetKeyCallback(windowHandle, keyCallback);
        glfwSetCursorPosCallback(windowHandle, cursorCallback);
        glfwSetMouseButtonCallback(windowHandle, mouseCallback);
        glfwSetScrollCallback(windowHandle, scrollCallback);
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

    public double mouseDeltaX() {
        return mouseDeltaX;
    }

    public double mouseDeltaY() {
        return mouseDeltaY;
    }

    public double consumeScrollDeltaY() {
        double value = scrollDeltaY;
        scrollDeltaY = 0.0;
        return value;
    }

    public void endFrame() {
        Arrays.fill(keysPressed, false);
        Arrays.fill(mousePressed, false);
        mouseDeltaX = 0.0;
        mouseDeltaY = 0.0;
    }

    public void resetMouseTracking() {
        firstMouse = true;
        mouseDeltaX = 0.0;
        mouseDeltaY = 0.0;
    }
}

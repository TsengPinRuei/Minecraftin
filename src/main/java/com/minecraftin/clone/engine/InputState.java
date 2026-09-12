package com.minecraftin.clone.engine;

import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.glfw.GLFWWindowFocusCallbackI;

import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;

// 將 GLFW callback 收到的事件整理成「持續按住」與「本幀剛發生」兩種輸入狀態。
public final class InputState {
    private final boolean[] keys = new boolean[GLFW_KEY_LAST + 1];

    private final boolean[] keysPressed = new boolean[GLFW_KEY_LAST + 1];

    private final boolean[] mousePressed = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];

    private final boolean[] mouseDown = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];

    // 用來判斷滑鼠是否第一次移動，避免第一次就產生很大的位移量。
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
            mouseDown[button] = true;
        } else if (action == GLFW_RELEASE) {
            mouseDown[button] = false;
        }
    };

    private final GLFWCursorPosCallbackI cursorCallback = (window, xpos, ypos) -> {
        // 第一次接收到滑鼠位置時，只初始化座標，不計算位移。
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
        }
        // 重新取得焦點時的游標座標可能不同，不能把它當成一次視角轉動。
        resetMouseTracking();
    };

    public void attach(long windowHandle) {
        glfwSetKeyCallback(windowHandle, keyCallback);
        glfwSetCursorPosCallback(windowHandle, cursorCallback);
        glfwSetMouseButtonCallback(windowHandle, mouseCallback);
        glfwSetScrollCallback(windowHandle, scrollCallback);
        glfwSetWindowFocusCallback(windowHandle, focusCallback);
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

    // 取得目前滑鼠 X 座標，單位與 GLFW 視窗座標一致。
    public double mouseX() {
        return lastMouseX;
    }

    // 取得目前滑鼠 Y 座標，單位與 GLFW 視窗座標一致。
    public double mouseY() {
        return lastMouseY;
    }

    // 取得目前累積的滾輪垂直位移，並在取出後清空。
    public double consumeScrollDeltaY() {
        double value = scrollDeltaY;
        scrollDeltaY = 0.0;
        return value;
    }

    // 每一幀結束時呼叫，清除只需要記錄一幀的輸入資料；呼叫太早會讓 Game 讀不到瞬時按鍵。
    public void endFrame() {
        Arrays.fill(keysPressed, false);
        Arrays.fill(mousePressed, false);
        scrollDeltaY = 0.0;
        mouseDeltaX = 0.0;
        mouseDeltaY = 0.0;
    }

    // 重新初始化滑鼠追蹤狀態；切換游標鎖定時必須呼叫，避免第一次 delta 使用舊座標造成鏡頭暴衝。
    public void resetMouseTracking() {
        firstMouse = true;
        mouseDeltaX = 0.0;
        mouseDeltaY = 0.0;
    }
}

package com.minecraftin.clone.engine;

import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;

import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;

public final class InputState {
    // 記錄每個鍵盤按鍵目前是否正在被按住。
    private final boolean[] keys = new boolean[GLFW_KEY_LAST + 1];

    // 記錄每個按鍵在「這一幀是否剛被按下」。
    private final boolean[] keysPressed = new boolean[GLFW_KEY_LAST + 1];

    // 記錄滑鼠按鍵在「這一幀是否剛被按下」。
    private final boolean[] mousePressed = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];

    // 用來判斷滑鼠是否第一次移動，避免第一次就產生很大的位移量。
    private boolean firstMouse = true;

    // 記錄上一個滑鼠座標。
    private double lastMouseX;
    private double lastMouseY;

    // 記錄這一幀滑鼠總共移動了多少距離。
    private double mouseDeltaX;
    private double mouseDeltaY;

    // 記錄滑鼠滾輪在這段時間內的垂直滾動量。
    private double scrollDeltaY;

    // 鍵盤事件發生時會呼叫這段邏輯。
    private final GLFWKeyCallbackI keyCallback = (window, key, scancode, action, mods) -> {
        // 如果按鍵代碼超出陣列範圍，就直接忽略。
        if (key < 0 || key >= keys.length) {
            return;
        }

        // 按鍵被按下時，記錄為持續按住，並標記為本幀剛按下。
        if (action == GLFW_PRESS) {
            keys[key] = true;
            keysPressed[key] = true;
            // 按鍵放開時，只更新為沒有按住。
        } else if (action == GLFW_RELEASE) {
            keys[key] = false;
        }
    };

    // 滑鼠按鍵事件發生時會呼叫這段邏輯。
    private final GLFWMouseButtonCallbackI mouseCallback = (window, button, action, mods) -> {
        // 如果按鍵編號超出範圍，就直接忽略。
        if (button < 0 || button >= mousePressed.length) {
            return;
        }

        // 只在按下當下記錄一次。
        if (action == GLFW_PRESS) {
            mousePressed[button] = true;
        }
    };

    // 滑鼠移動時會呼叫這段邏輯。
    private final GLFWCursorPosCallbackI cursorCallback = (window, xpos, ypos) -> {
        // 第一次接收到滑鼠位置時，只初始化座標，不計算位移。
        if (firstMouse) {
            lastMouseX = xpos;
            lastMouseY = ypos;
            firstMouse = false;
        }

        // 累加滑鼠本幀的移動距離。
        mouseDeltaX += xpos - lastMouseX;
        mouseDeltaY += ypos - lastMouseY;

        // 更新最新座標，供下一次移動時計算差值。
        lastMouseX = xpos;
        lastMouseY = ypos;
    };

    // 滾輪滾動時，把垂直方向的變化量累加起來。
    private final GLFWScrollCallbackI scrollCallback = (window, xOffset, yOffset) -> scrollDeltaY += yOffset;

    // 把這個輸入狀態物件綁定到指定視窗上。
    public void attach(long windowHandle) {
        glfwSetKeyCallback(windowHandle, keyCallback);
        glfwSetCursorPosCallback(windowHandle, cursorCallback);
        glfwSetMouseButtonCallback(windowHandle, mouseCallback);
        glfwSetScrollCallback(windowHandle, scrollCallback);
    }

    // 檢查某個按鍵目前是否正在被按住。
    public boolean isKeyDown(int keyCode) {
        return keyCode >= 0 && keyCode < keys.length && keys[keyCode];
    }

    // 檢查某個按鍵是否在這一幀剛被按下。
    public boolean wasKeyPressed(int keyCode) {
        return keyCode >= 0 && keyCode < keysPressed.length && keysPressed[keyCode];
    }

    // 檢查某個滑鼠按鍵是否在這一幀剛被按下。
    public boolean wasMousePressed(int button) {
        return button >= 0 && button < mousePressed.length && mousePressed[button];
    }

    // 取得這一幀滑鼠在 X 軸上的移動量。
    public double mouseDeltaX() {
        return mouseDeltaX;
    }

    // 取得這一幀滑鼠在 Y 軸上的移動量。
    public double mouseDeltaY() {
        return mouseDeltaY;
    }

    // 取得目前累積的滾輪垂直位移，並在取出後清空。
    public double consumeScrollDeltaY() {
        double value = scrollDeltaY;
        scrollDeltaY = 0.0;
        return value;
    }

    // 每一幀結束時呼叫，清除只需要記錄一幀的輸入資料。
    public void endFrame() {
        Arrays.fill(keysPressed, false);
        Arrays.fill(mousePressed, false);
        mouseDeltaX = 0.0;
        mouseDeltaY = 0.0;
    }

    // 重新初始化滑鼠追蹤狀態。
    public void resetMouseTracking() {
        firstMouse = true;
        mouseDeltaX = 0.0;
        mouseDeltaY = 0.0;
    }
}
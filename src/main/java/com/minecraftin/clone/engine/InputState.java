// 宣告此檔案所屬的套件。
package com.minecraftin.clone.engine;

// 匯入後續會使用到的型別或函式。
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
// 匯入後續會使用到的型別或函式。
import org.lwjgl.glfw.GLFWKeyCallbackI;
// 匯入後續會使用到的型別或函式。
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
// 匯入後續會使用到的型別或函式。
import org.lwjgl.glfw.GLFWScrollCallbackI;

// 匯入後續會使用到的型別或函式。
import java.util.Arrays;

// 匯入後續會使用到的型別或函式。
import static org.lwjgl.glfw.GLFW.*;

// 定義主要型別與其結構。
public final class InputState {
    // 設定或更新變數的值。
    private final boolean[] keys = new boolean[GLFW_KEY_LAST + 1];
    // 設定或更新變數的值。
    private final boolean[] keysPressed = new boolean[GLFW_KEY_LAST + 1];
    // 設定或更新變數的值。
    private final boolean[] mousePressed = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];

    // 設定或更新變數的值。
    private boolean firstMouse = true;
    // 下一行程式碼負責執行目前步驟。
    private double lastMouseX;
    // 下一行程式碼負責執行目前步驟。
    private double lastMouseY;
    // 下一行程式碼負責執行目前步驟。
    private double mouseDeltaX;
    // 下一行程式碼負責執行目前步驟。
    private double mouseDeltaY;
    // 下一行程式碼負責執行目前步驟。
    private double scrollDeltaY;

    // 定義類別內部使用的方法。
    private final GLFWKeyCallbackI keyCallback = (window, key, scancode, action, mods) -> {
        // 根據條件決定是否進入此邏輯分支。
        if (key < 0 || key >= keys.length) {
            // 下一行程式碼負責執行目前步驟。
            return;
        }
        // 根據條件決定是否進入此邏輯分支。
        if (action == GLFW_PRESS) {
            // 設定或更新變數的值。
            keys[key] = true;
            // 設定或更新變數的值。
            keysPressed[key] = true;
        // 下一行程式碼負責執行目前步驟。
        } else if (action == GLFW_RELEASE) {
            // 設定或更新變數的值。
            keys[key] = false;
        }
    };

    // 定義類別內部使用的方法。
    private final GLFWMouseButtonCallbackI mouseCallback = (window, button, action, mods) -> {
        // 根據條件決定是否進入此邏輯分支。
        if (button < 0 || button >= mousePressed.length) {
            // 下一行程式碼負責執行目前步驟。
            return;
        }
        // 根據條件決定是否進入此邏輯分支。
        if (action == GLFW_PRESS) {
            // 設定或更新變數的值。
            mousePressed[button] = true;
        }
    };

    // 定義類別內部使用的方法。
    private final GLFWCursorPosCallbackI cursorCallback = (window, xpos, ypos) -> {
        // 根據條件決定是否進入此邏輯分支。
        if (firstMouse) {
            // 設定或更新變數的值。
            lastMouseX = xpos;
            // 設定或更新變數的值。
            lastMouseY = ypos;
            // 設定或更新變數的值。
            firstMouse = false;
        }
        // 設定或更新變數的值。
        mouseDeltaX += xpos - lastMouseX;
        // 設定或更新變數的值。
        mouseDeltaY += ypos - lastMouseY;
        // 設定或更新變數的值。
        lastMouseX = xpos;
        // 設定或更新變數的值。
        lastMouseY = ypos;
    };

    // 設定或更新變數的值。
    private final GLFWScrollCallbackI scrollCallback = (window, xOffset, yOffset) -> scrollDeltaY += yOffset;

    // 定義對外可呼叫的方法。
    public void attach(long windowHandle) {
        // 呼叫方法執行對應功能。
        glfwSetKeyCallback(windowHandle, keyCallback);
        // 呼叫方法執行對應功能。
        glfwSetCursorPosCallback(windowHandle, cursorCallback);
        // 呼叫方法執行對應功能。
        glfwSetMouseButtonCallback(windowHandle, mouseCallback);
        // 呼叫方法執行對應功能。
        glfwSetScrollCallback(windowHandle, scrollCallback);
    }

    // 定義對外可呼叫的方法。
    public boolean isKeyDown(int keyCode) {
        // 設定或更新變數的值。
        return keyCode >= 0 && keyCode < keys.length && keys[keyCode];
    }

    // 定義對外可呼叫的方法。
    public boolean wasKeyPressed(int keyCode) {
        // 設定或更新變數的值。
        return keyCode >= 0 && keyCode < keysPressed.length && keysPressed[keyCode];
    }

    // 定義對外可呼叫的方法。
    public boolean wasMousePressed(int button) {
        // 設定或更新變數的值。
        return button >= 0 && button < mousePressed.length && mousePressed[button];
    }

    // 定義對外可呼叫的方法。
    public double mouseDeltaX() {
        // 下一行程式碼負責執行目前步驟。
        return mouseDeltaX;
    }

    // 定義對外可呼叫的方法。
    public double mouseDeltaY() {
        // 下一行程式碼負責執行目前步驟。
        return mouseDeltaY;
    }

    // 定義對外可呼叫的方法。
    public double consumeScrollDeltaY() {
        // 宣告並初始化變數。
        double value = scrollDeltaY;
        // 設定或更新變數的值。
        scrollDeltaY = 0.0;
        // 下一行程式碼負責執行目前步驟。
        return value;
    }

    // 定義對外可呼叫的方法。
    public void endFrame() {
        // 呼叫方法執行對應功能。
        Arrays.fill(keysPressed, false);
        // 呼叫方法執行對應功能。
        Arrays.fill(mousePressed, false);
        // 設定或更新變數的值。
        mouseDeltaX = 0.0;
        // 設定或更新變數的值。
        mouseDeltaY = 0.0;
    }

    // 定義對外可呼叫的方法。
    public void resetMouseTracking() {
        // 設定或更新變數的值。
        firstMouse = true;
        // 設定或更新變數的值。
        mouseDeltaX = 0.0;
        // 設定或更新變數的值。
        mouseDeltaY = 0.0;
    }
}

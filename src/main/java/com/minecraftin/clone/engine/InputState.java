// 說明：宣告此檔案所屬的套件。
package com.minecraftin.clone.engine;

// 說明：匯入後續會使用到的型別或函式。
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
// 說明：匯入後續會使用到的型別或函式。
import org.lwjgl.glfw.GLFWKeyCallbackI;
// 說明：匯入後續會使用到的型別或函式。
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
// 說明：匯入後續會使用到的型別或函式。
import org.lwjgl.glfw.GLFWScrollCallbackI;

// 說明：匯入後續會使用到的型別或函式。
import java.util.Arrays;

// 說明：匯入後續會使用到的型別或函式。
import static org.lwjgl.glfw.GLFW.*;

// 說明：定義主要型別與其結構。
public final class InputState {
    // 說明：設定或更新變數的值。
    private final boolean[] keys = new boolean[GLFW_KEY_LAST + 1];
    // 說明：設定或更新變數的值。
    private final boolean[] keysPressed = new boolean[GLFW_KEY_LAST + 1];
    // 說明：設定或更新變數的值。
    private final boolean[] mousePressed = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];

    // 說明：設定或更新變數的值。
    private boolean firstMouse = true;
    // 說明：下一行程式碼負責執行目前步驟。
    private double lastMouseX;
    // 說明：下一行程式碼負責執行目前步驟。
    private double lastMouseY;
    // 說明：下一行程式碼負責執行目前步驟。
    private double mouseDeltaX;
    // 說明：下一行程式碼負責執行目前步驟。
    private double mouseDeltaY;
    // 說明：下一行程式碼負責執行目前步驟。
    private double scrollDeltaY;

    // 說明：定義類別內部使用的方法。
    private final GLFWKeyCallbackI keyCallback = (window, key, scancode, action, mods) -> {
        // 說明：根據條件決定是否進入此邏輯分支。
        if (key < 0 || key >= keys.length) {
            // 說明：下一行程式碼負責執行目前步驟。
            return;
        }
        // 說明：根據條件決定是否進入此邏輯分支。
        if (action == GLFW_PRESS) {
            // 說明：設定或更新變數的值。
            keys[key] = true;
            // 說明：設定或更新變數的值。
            keysPressed[key] = true;
        // 說明：下一行程式碼負責執行目前步驟。
        } else if (action == GLFW_RELEASE) {
            // 說明：設定或更新變數的值。
            keys[key] = false;
        }
    };

    // 說明：定義類別內部使用的方法。
    private final GLFWMouseButtonCallbackI mouseCallback = (window, button, action, mods) -> {
        // 說明：根據條件決定是否進入此邏輯分支。
        if (button < 0 || button >= mousePressed.length) {
            // 說明：下一行程式碼負責執行目前步驟。
            return;
        }
        // 說明：根據條件決定是否進入此邏輯分支。
        if (action == GLFW_PRESS) {
            // 說明：設定或更新變數的值。
            mousePressed[button] = true;
        }
    };

    // 說明：定義類別內部使用的方法。
    private final GLFWCursorPosCallbackI cursorCallback = (window, xpos, ypos) -> {
        // 說明：根據條件決定是否進入此邏輯分支。
        if (firstMouse) {
            // 說明：設定或更新變數的值。
            lastMouseX = xpos;
            // 說明：設定或更新變數的值。
            lastMouseY = ypos;
            // 說明：設定或更新變數的值。
            firstMouse = false;
        }
        // 說明：設定或更新變數的值。
        mouseDeltaX += xpos - lastMouseX;
        // 說明：設定或更新變數的值。
        mouseDeltaY += ypos - lastMouseY;
        // 說明：設定或更新變數的值。
        lastMouseX = xpos;
        // 說明：設定或更新變數的值。
        lastMouseY = ypos;
    };

    // 說明：設定或更新變數的值。
    private final GLFWScrollCallbackI scrollCallback = (window, xOffset, yOffset) -> scrollDeltaY += yOffset;

    // 說明：定義對外可呼叫的方法。
    public void attach(long windowHandle) {
        // 說明：呼叫方法執行對應功能。
        glfwSetKeyCallback(windowHandle, keyCallback);
        // 說明：呼叫方法執行對應功能。
        glfwSetCursorPosCallback(windowHandle, cursorCallback);
        // 說明：呼叫方法執行對應功能。
        glfwSetMouseButtonCallback(windowHandle, mouseCallback);
        // 說明：呼叫方法執行對應功能。
        glfwSetScrollCallback(windowHandle, scrollCallback);
    }

    // 說明：定義對外可呼叫的方法。
    public boolean isKeyDown(int keyCode) {
        // 說明：設定或更新變數的值。
        return keyCode >= 0 && keyCode < keys.length && keys[keyCode];
    }

    // 說明：定義對外可呼叫的方法。
    public boolean wasKeyPressed(int keyCode) {
        // 說明：設定或更新變數的值。
        return keyCode >= 0 && keyCode < keysPressed.length && keysPressed[keyCode];
    }

    // 說明：定義對外可呼叫的方法。
    public boolean wasMousePressed(int button) {
        // 說明：設定或更新變數的值。
        return button >= 0 && button < mousePressed.length && mousePressed[button];
    }

    // 說明：定義對外可呼叫的方法。
    public double mouseDeltaX() {
        // 說明：下一行程式碼負責執行目前步驟。
        return mouseDeltaX;
    }

    // 說明：定義對外可呼叫的方法。
    public double mouseDeltaY() {
        // 說明：下一行程式碼負責執行目前步驟。
        return mouseDeltaY;
    }

    // 說明：定義對外可呼叫的方法。
    public double consumeScrollDeltaY() {
        // 說明：宣告並初始化變數。
        double value = scrollDeltaY;
        // 說明：設定或更新變數的值。
        scrollDeltaY = 0.0;
        // 說明：下一行程式碼負責執行目前步驟。
        return value;
    }

    // 說明：定義對外可呼叫的方法。
    public void endFrame() {
        // 說明：呼叫方法執行對應功能。
        Arrays.fill(keysPressed, false);
        // 說明：呼叫方法執行對應功能。
        Arrays.fill(mousePressed, false);
        // 說明：設定或更新變數的值。
        mouseDeltaX = 0.0;
        // 說明：設定或更新變數的值。
        mouseDeltaY = 0.0;
    }

    // 說明：定義對外可呼叫的方法。
    public void resetMouseTracking() {
        // 說明：設定或更新變數的值。
        firstMouse = true;
        // 說明：設定或更新變數的值。
        mouseDeltaX = 0.0;
        // 說明：設定或更新變數的值。
        mouseDeltaY = 0.0;
    }
}

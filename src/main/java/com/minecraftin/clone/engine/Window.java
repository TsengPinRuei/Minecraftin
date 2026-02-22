// 宣告此檔案所屬的套件。
package com.minecraftin.clone.engine;

// 匯入後續會使用到的型別或函式。
import com.minecraftin.clone.config.GameConfig;
// 匯入後續會使用到的型別或函式。
import org.lwjgl.glfw.GLFWErrorCallback;
// 匯入後續會使用到的型別或函式。
import org.lwjgl.glfw.GLFWVidMode;
// 匯入後續會使用到的型別或函式。
import org.lwjgl.opengl.GL;
// 匯入後續會使用到的型別或函式。
import org.lwjgl.system.MemoryStack;

// 匯入後續會使用到的型別或函式。
import java.nio.IntBuffer;

// 匯入後續會使用到的型別或函式。
import static org.lwjgl.glfw.GLFW.*;
// 匯入後續會使用到的型別或函式。
import static org.lwjgl.system.MemoryUtil.NULL;

// 定義主要型別與其結構。
public final class Window implements AutoCloseable {
    // 下一行程式碼負責執行目前步驟。
    private long handle;
    // 下一行程式碼負責執行目前步驟。
    private int width;
    // 下一行程式碼負責執行目前步驟。
    private int height;
    // 下一行程式碼負責執行目前步驟。
    private boolean glfwInitialized;

    // 定義對外可呼叫的方法。
    public void create() {
        // 呼叫方法執行對應功能。
        GLFWErrorCallback.createPrint(System.err).set();

        // 根據條件決定是否進入此邏輯分支。
        if (!glfwInit()) {
            // 呼叫方法執行對應功能。
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        // 設定或更新變數的值。
        glfwInitialized = true;

        // 呼叫方法執行對應功能。
        glfwDefaultWindowHints();
        // 呼叫方法執行對應功能。
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        // 呼叫方法執行對應功能。
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        // 呼叫方法執行對應功能。
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        // 呼叫方法執行對應功能。
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        // 呼叫方法執行對應功能。
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        // 呼叫方法執行對應功能。
        glfwWindowHint(GLFW_DECORATED, GLFW_TRUE);

        // 宣告並初始化變數。
        long monitor = glfwGetPrimaryMonitor();
        // 宣告並初始化變數。
        GLFWVidMode videoMode = monitor != NULL ? glfwGetVideoMode(monitor) : null;

        // 根據條件決定是否進入此邏輯分支。
        if (videoMode != null) {
            // 宣告並初始化變數。
            int availableWidth = Math.max(1, videoMode.width() - 80);
            // 宣告並初始化變數。
            int availableHeight = Math.max(1, videoMode.height() - 120);

            // 設定或更新變數的值。
            width = Math.min(GameConfig.WINDOW_WIDTH, availableWidth);
            // 設定或更新變數的值。
            height = Math.min(GameConfig.WINDOW_HEIGHT, availableHeight);

            // 宣告並初始化變數。
            int minWidth = Math.min(960, videoMode.width());
            // 宣告並初始化變數。
            int minHeight = Math.min(640, videoMode.height());
            // 設定或更新變數的值。
            width = Math.max(width, minWidth);
            // 設定或更新變數的值。
            height = Math.max(height, minHeight);
        // 下一行程式碼負責執行目前步驟。
        } else {
            // 設定或更新變數的值。
            width = GameConfig.WINDOW_WIDTH;
            // 設定或更新變數的值。
            height = GameConfig.WINDOW_HEIGHT;
        }

        // 設定或更新變數的值。
        handle = glfwCreateWindow(width, height, GameConfig.WINDOW_TITLE, NULL, NULL);
        // 根據條件決定是否進入此邏輯分支。
        if (handle == NULL) {
            // 呼叫方法執行對應功能。
            throw new IllegalStateException("Failed to create window");
        }

        // 根據條件決定是否進入此邏輯分支。
        if (videoMode != null) {
            // 宣告並初始化變數。
            int xpos = (videoMode.width() - width) / 2;
            // 宣告並初始化變數。
            int ypos = (videoMode.height() - height) / 2;
            // 呼叫方法執行對應功能。
            glfwSetWindowPos(handle, xpos, ypos);
        }

        // 呼叫方法執行對應功能。
        glfwMakeContextCurrent(handle);
        // 呼叫方法執行對應功能。
        glfwSwapInterval(0);
        // 呼叫方法執行對應功能。
        GL.createCapabilities();

        // 呼叫方法執行對應功能。
        refreshFramebufferSize();

        // 下一行程式碼負責執行目前步驟。
        glfwSetFramebufferSizeCallback(handle, (w, newWidth, newHeight) -> {
            // 設定或更新變數的值。
            width = Math.max(newWidth, 1);
            // 設定或更新變數的值。
            height = Math.max(newHeight, 1);
        // 下一行程式碼負責執行目前步驟。
        });
    }

    // 定義對外可呼叫的方法。
    public long handle() {
        // 下一行程式碼負責執行目前步驟。
        return handle;
    }

    // 定義對外可呼叫的方法。
    public int width() {
        // 下一行程式碼負責執行目前步驟。
        return width;
    }

    // 定義對外可呼叫的方法。
    public int height() {
        // 下一行程式碼負責執行目前步驟。
        return height;
    }

    // 定義對外可呼叫的方法。
    public boolean shouldClose() {
        // 呼叫方法執行對應功能。
        return glfwWindowShouldClose(handle);
    }

    // 定義對外可呼叫的方法。
    public void requestClose() {
        // 呼叫方法執行對應功能。
        glfwSetWindowShouldClose(handle, true);
    }

    // 定義對外可呼叫的方法。
    public void captureCursor(boolean capture) {
        // 呼叫方法執行對應功能。
        glfwSetInputMode(handle, GLFW_CURSOR, capture ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
    }

    // 定義對外可呼叫的方法。
    public void pollEvents() {
        // 呼叫方法執行對應功能。
        glfwPollEvents();
        // 呼叫方法執行對應功能。
        refreshFramebufferSize();
    }

    // 定義對外可呼叫的方法。
    public void swapBuffers() {
        // 呼叫方法執行對應功能。
        glfwSwapBuffers(handle);
    }

    // 宣告註解標記，提供編譯器或框架額外資訊。
    @Override
    // 定義對外可呼叫的方法。
    public void close() {
        // 根據條件決定是否進入此邏輯分支。
        if (handle != NULL) {
            // 呼叫方法執行對應功能。
            glfwDestroyWindow(handle);
            // 設定或更新變數的值。
            handle = NULL;
        }
        // 根據條件決定是否進入此邏輯分支。
        if (glfwInitialized) {
            // 呼叫方法執行對應功能。
            glfwTerminate();
            // 設定或更新變數的值。
            glfwInitialized = false;
        }

        // 宣告並初始化變數。
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        // 根據條件決定是否進入此邏輯分支。
        if (callback != null) {
            // 呼叫方法執行對應功能。
            callback.free();
        }
    }

    // 定義類別內部使用的方法。
    private void refreshFramebufferSize() {
        // 根據條件決定是否進入此邏輯分支。
        if (handle == NULL) {
            // 下一行程式碼負責執行目前步驟。
            return;
        }
        // 下一行程式碼負責執行目前步驟。
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // 宣告並初始化變數。
            IntBuffer framebufferWidth = stack.mallocInt(1);
            // 宣告並初始化變數。
            IntBuffer framebufferHeight = stack.mallocInt(1);
            // 呼叫方法執行對應功能。
            glfwGetFramebufferSize(handle, framebufferWidth, framebufferHeight);
            // 設定或更新變數的值。
            width = Math.max(framebufferWidth.get(0), 1);
            // 設定或更新變數的值。
            height = Math.max(framebufferHeight.get(0), 1);
        }
    }
}

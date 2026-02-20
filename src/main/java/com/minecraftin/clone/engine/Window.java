// 說明：宣告此檔案所屬的套件。
package com.minecraftin.clone.engine;

// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.config.GameConfig;
// 說明：匯入後續會使用到的型別或函式。
import org.lwjgl.glfw.GLFWErrorCallback;
// 說明：匯入後續會使用到的型別或函式。
import org.lwjgl.glfw.GLFWVidMode;
// 說明：匯入後續會使用到的型別或函式。
import org.lwjgl.opengl.GL;
// 說明：匯入後續會使用到的型別或函式。
import org.lwjgl.system.MemoryStack;

// 說明：匯入後續會使用到的型別或函式。
import java.nio.IntBuffer;

// 說明：匯入後續會使用到的型別或函式。
import static org.lwjgl.glfw.GLFW.*;
// 說明：匯入後續會使用到的型別或函式。
import static org.lwjgl.system.MemoryUtil.NULL;

// 說明：定義主要型別與其結構。
public final class Window implements AutoCloseable {
    // 說明：下一行程式碼負責執行目前步驟。
    private long handle;
    // 說明：下一行程式碼負責執行目前步驟。
    private int width;
    // 說明：下一行程式碼負責執行目前步驟。
    private int height;
    // 說明：下一行程式碼負責執行目前步驟。
    private boolean glfwInitialized;

    // 說明：定義對外可呼叫的方法。
    public void create() {
        // 說明：呼叫方法執行對應功能。
        GLFWErrorCallback.createPrint(System.err).set();

        // 說明：根據條件決定是否進入此邏輯分支。
        if (!glfwInit()) {
            // 說明：呼叫方法執行對應功能。
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        // 說明：設定或更新變數的值。
        glfwInitialized = true;

        // 說明：呼叫方法執行對應功能。
        glfwDefaultWindowHints();
        // 說明：呼叫方法執行對應功能。
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        // 說明：呼叫方法執行對應功能。
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        // 說明：呼叫方法執行對應功能。
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        // 說明：呼叫方法執行對應功能。
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        // 說明：呼叫方法執行對應功能。
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        // 說明：呼叫方法執行對應功能。
        glfwWindowHint(GLFW_DECORATED, GLFW_TRUE);

        // 說明：宣告並初始化變數。
        long monitor = glfwGetPrimaryMonitor();
        // 說明：宣告並初始化變數。
        GLFWVidMode videoMode = monitor != NULL ? glfwGetVideoMode(monitor) : null;

        // 說明：根據條件決定是否進入此邏輯分支。
        if (videoMode != null) {
            // 說明：宣告並初始化變數。
            int availableWidth = Math.max(1, videoMode.width() - 80);
            // 說明：宣告並初始化變數。
            int availableHeight = Math.max(1, videoMode.height() - 120);

            // 說明：設定或更新變數的值。
            width = Math.min(GameConfig.WINDOW_WIDTH, availableWidth);
            // 說明：設定或更新變數的值。
            height = Math.min(GameConfig.WINDOW_HEIGHT, availableHeight);

            // 說明：宣告並初始化變數。
            int minWidth = Math.min(960, videoMode.width());
            // 說明：宣告並初始化變數。
            int minHeight = Math.min(640, videoMode.height());
            // 說明：設定或更新變數的值。
            width = Math.max(width, minWidth);
            // 說明：設定或更新變數的值。
            height = Math.max(height, minHeight);
        // 說明：下一行程式碼負責執行目前步驟。
        } else {
            // 說明：設定或更新變數的值。
            width = GameConfig.WINDOW_WIDTH;
            // 說明：設定或更新變數的值。
            height = GameConfig.WINDOW_HEIGHT;
        }

        // 說明：設定或更新變數的值。
        handle = glfwCreateWindow(width, height, GameConfig.WINDOW_TITLE, NULL, NULL);
        // 說明：根據條件決定是否進入此邏輯分支。
        if (handle == NULL) {
            // 說明：呼叫方法執行對應功能。
            throw new IllegalStateException("Failed to create window");
        }

        // 說明：根據條件決定是否進入此邏輯分支。
        if (videoMode != null) {
            // 說明：宣告並初始化變數。
            int xpos = (videoMode.width() - width) / 2;
            // 說明：宣告並初始化變數。
            int ypos = (videoMode.height() - height) / 2;
            // 說明：呼叫方法執行對應功能。
            glfwSetWindowPos(handle, xpos, ypos);
        }

        // 說明：呼叫方法執行對應功能。
        glfwMakeContextCurrent(handle);
        // 說明：呼叫方法執行對應功能。
        glfwSwapInterval(0);
        // 說明：呼叫方法執行對應功能。
        GL.createCapabilities();

        // 說明：呼叫方法執行對應功能。
        refreshFramebufferSize();

        // 說明：下一行程式碼負責執行目前步驟。
        glfwSetFramebufferSizeCallback(handle, (w, newWidth, newHeight) -> {
            // 說明：設定或更新變數的值。
            width = Math.max(newWidth, 1);
            // 說明：設定或更新變數的值。
            height = Math.max(newHeight, 1);
        // 說明：下一行程式碼負責執行目前步驟。
        });
    }

    // 說明：定義對外可呼叫的方法。
    public long handle() {
        // 說明：下一行程式碼負責執行目前步驟。
        return handle;
    }

    // 說明：定義對外可呼叫的方法。
    public int width() {
        // 說明：下一行程式碼負責執行目前步驟。
        return width;
    }

    // 說明：定義對外可呼叫的方法。
    public int height() {
        // 說明：下一行程式碼負責執行目前步驟。
        return height;
    }

    // 說明：定義對外可呼叫的方法。
    public boolean shouldClose() {
        // 說明：呼叫方法執行對應功能。
        return glfwWindowShouldClose(handle);
    }

    // 說明：定義對外可呼叫的方法。
    public void requestClose() {
        // 說明：呼叫方法執行對應功能。
        glfwSetWindowShouldClose(handle, true);
    }

    // 說明：定義對外可呼叫的方法。
    public void captureCursor(boolean capture) {
        // 說明：呼叫方法執行對應功能。
        glfwSetInputMode(handle, GLFW_CURSOR, capture ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
    }

    // 說明：定義對外可呼叫的方法。
    public void pollEvents() {
        // 說明：呼叫方法執行對應功能。
        glfwPollEvents();
        // 說明：呼叫方法執行對應功能。
        refreshFramebufferSize();
    }

    // 說明：定義對外可呼叫的方法。
    public void swapBuffers() {
        // 說明：呼叫方法執行對應功能。
        glfwSwapBuffers(handle);
    }

    // 說明：宣告註解標記，提供編譯器或框架額外資訊。
    @Override
    // 說明：定義對外可呼叫的方法。
    public void close() {
        // 說明：根據條件決定是否進入此邏輯分支。
        if (handle != NULL) {
            // 說明：呼叫方法執行對應功能。
            glfwDestroyWindow(handle);
            // 說明：設定或更新變數的值。
            handle = NULL;
        }
        // 說明：根據條件決定是否進入此邏輯分支。
        if (glfwInitialized) {
            // 說明：呼叫方法執行對應功能。
            glfwTerminate();
            // 說明：設定或更新變數的值。
            glfwInitialized = false;
        }

        // 說明：宣告並初始化變數。
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        // 說明：根據條件決定是否進入此邏輯分支。
        if (callback != null) {
            // 說明：呼叫方法執行對應功能。
            callback.free();
        }
    }

    // 說明：定義類別內部使用的方法。
    private void refreshFramebufferSize() {
        // 說明：根據條件決定是否進入此邏輯分支。
        if (handle == NULL) {
            // 說明：下一行程式碼負責執行目前步驟。
            return;
        }
        // 說明：下一行程式碼負責執行目前步驟。
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // 說明：宣告並初始化變數。
            IntBuffer framebufferWidth = stack.mallocInt(1);
            // 說明：宣告並初始化變數。
            IntBuffer framebufferHeight = stack.mallocInt(1);
            // 說明：呼叫方法執行對應功能。
            glfwGetFramebufferSize(handle, framebufferWidth, framebufferHeight);
            // 說明：設定或更新變數的值。
            width = Math.max(framebufferWidth.get(0), 1);
            // 說明：設定或更新變數的值。
            height = Math.max(framebufferHeight.get(0), 1);
        }
    }
}

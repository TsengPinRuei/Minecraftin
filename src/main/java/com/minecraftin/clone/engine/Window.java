package com.minecraftin.clone.engine;

import com.minecraftin.clone.config.GameConfig;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class Window implements AutoCloseable {
    // GLFW 建立出來的視窗控制代號。
    private long handle;

    // 目前 framebuffer 的寬度。
    private int width;

    // 目前 framebuffer 的高度。
    private int height;

    // 記錄 GLFW 是否已成功初始化，方便關閉時正確釋放資源。
    private boolean glfwInitialized;

    // 建立視窗並初始化 OpenGL 環境。
    public void create() {
        // 設定 GLFW 發生錯誤時，要把錯誤訊息印到標準錯誤輸出。
        GLFWErrorCallback.createPrint(System.err).set();

        // 初始化 GLFW，失敗就直接停止。
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        glfwInitialized = true;

        // 設定建立視窗前的各種參數。
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_DECORATED, GLFW_TRUE);

        // 取得主要螢幕與其顯示模式，用來決定視窗大小與位置。
        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode videoMode = monitor != NULL ? glfwGetVideoMode(monitor) : null;

        if (videoMode != null) {
            // 預留一些邊界，避免視窗太貼近螢幕邊緣。
            int availableWidth = Math.max(1, videoMode.width() - 80);
            int availableHeight = Math.max(1, videoMode.height() - 120);

            // 視窗大小以設定值為主，但不能超過螢幕可用範圍。
            width = Math.min(GameConfig.WINDOW_WIDTH, availableWidth);
            height = Math.min(GameConfig.WINDOW_HEIGHT, availableHeight);

            // 也限制視窗不要小於一個基本可用大小。
            int minWidth = Math.min(960, videoMode.width());
            int minHeight = Math.min(640, videoMode.height());
            width = Math.max(width, minWidth);
            height = Math.max(height, minHeight);
        } else {
            // 如果拿不到螢幕資訊，就直接使用預設設定值。
            width = GameConfig.WINDOW_WIDTH;
            height = GameConfig.WINDOW_HEIGHT;
        }

        // 建立 GLFW 視窗。
        handle = glfwCreateWindow(width, height, GameConfig.WINDOW_TITLE, NULL, NULL);
        if (handle == NULL) {
            throw new IllegalStateException("Failed to create window");
        }

        // 若有螢幕資訊，就把視窗置中顯示。
        if (videoMode != null) {
            int xpos = (videoMode.width() - width) / 2;
            int ypos = (videoMode.height() - height) / 2;
            glfwSetWindowPos(handle, xpos, ypos);
        }

        // 把這個視窗設為目前 OpenGL 要使用的 context。
        glfwMakeContextCurrent(handle);

        // 關閉垂直同步，讓畫面交換不受螢幕更新率限制。
        glfwSwapInterval(0);

        // 建立 OpenGL 功能表，之後才能呼叫 OpenGL API。
        GL.createCapabilities();

        // 先同步一次 framebuffer 的實際大小。
        refreshFramebufferSize();

        // 當 framebuffer 大小改變時，同步更新寬高。
        glfwSetFramebufferSizeCallback(handle, (w, newWidth, newHeight) -> {
            width = Math.max(newWidth, 1);
            height = Math.max(newHeight, 1);
        });
    }

    // 取得視窗控制代號。
    public long handle() {
        return handle;
    }

    // 取得目前寬度。
    public int width() {
        return width;
    }

    // 取得目前高度。
    public int height() {
        return height;
    }

    // 檢查視窗是否已被要求關閉。
    public boolean shouldClose() {
        return glfwWindowShouldClose(handle);
    }

    // 主動要求視窗關閉。
    public void requestClose() {
        glfwSetWindowShouldClose(handle, true);
    }

    // 設定是否要鎖定滑鼠游標。
    // true 代表隱藏並鎖定游標，常用在第一人稱視角。
    public void captureCursor(boolean capture) {
        glfwSetInputMode(handle, GLFW_CURSOR, capture ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
    }

    // 處理視窗事件，並同步更新 framebuffer 大小。
    public void pollEvents() {
        glfwPollEvents();
        refreshFramebufferSize();
    }

    // 交換前後畫面緩衝，將本幀畫面顯示到螢幕上。
    public void swapBuffers() {
        glfwSwapBuffers(handle);
    }

    @Override
    // 釋放視窗、GLFW 與錯誤回呼資源。
    public void close() {
        if (handle != NULL) {
            glfwDestroyWindow(handle);
            handle = NULL;
        }

        if (glfwInitialized) {
            glfwTerminate();
            glfwInitialized = false;
        }

        // 取回目前的錯誤回呼並釋放它。
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
        }
    }

    // 重新取得 framebuffer 的實際大小。
    // 這比單純使用視窗大小更準確，特別是在高 DPI 顯示器上。
    private void refreshFramebufferSize() {
        if (handle == NULL) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer framebufferWidth = stack.mallocInt(1);
            IntBuffer framebufferHeight = stack.mallocInt(1);

            glfwGetFramebufferSize(handle, framebufferWidth, framebufferHeight);

            width = Math.max(framebufferWidth.get(0), 1);
            height = Math.max(framebufferHeight.get(0), 1);
        }
    }
}
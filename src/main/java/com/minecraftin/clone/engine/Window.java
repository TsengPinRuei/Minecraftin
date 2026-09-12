package com.minecraftin.clone.engine;

import com.minecraftin.clone.config.GameConfig;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

// 封裝 GLFW 視窗與 OpenGL context 建立流程，並持續追蹤 framebuffer 實際尺寸。
public final class Window implements AutoCloseable {
    private long handle;

    private int width;

    private int height;

    // 目前 GLFW 視窗邏輯寬度；滑鼠座標使用這個座標系。
    private int windowWidth;

    // 目前 GLFW 視窗邏輯高度；滑鼠座標使用這個座標系。
    private int windowHeight;

    private boolean glfwInitialized;

    public void create() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        glfwInitialized = true;

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_DECORATED, GLFW_TRUE);

        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode videoMode = monitor != NULL ? glfwGetVideoMode(monitor) : null;

        if (videoMode != null) {
            // 預留一些邊界，避免視窗太貼近螢幕邊緣。
            int availableWidth = Math.max(1, videoMode.width() - 80);
            int availableHeight = Math.max(1, videoMode.height() - 120);

            width = Math.min(GameConfig.WINDOW_WIDTH, availableWidth);
            height = Math.min(GameConfig.WINDOW_HEIGHT, availableHeight);

            int minWidth = Math.min(960, videoMode.width());
            int minHeight = Math.min(640, videoMode.height());
            width = Math.max(width, minWidth);
            height = Math.max(height, minHeight);
        } else {
            width = GameConfig.WINDOW_WIDTH;
            height = GameConfig.WINDOW_HEIGHT;
        }

        windowWidth = width;
        windowHeight = height;

        handle = glfwCreateWindow(width, height, GameConfig.WINDOW_TITLE, NULL, NULL);
        if (handle == NULL) {
            throw new IllegalStateException("Failed to create window");
        }

        if (videoMode != null) {
            int xpos = (videoMode.width() - width) / 2;
            int ypos = (videoMode.height() - height) / 2;
            glfwSetWindowPos(handle, xpos, ypos);
        }

        glfwMakeContextCurrent(handle);

        // 關閉垂直同步，讓畫面交換不受螢幕更新率限制；目前 FPS 顯示會反映未鎖幀的渲染速度。
        glfwSwapInterval(0);

        GL.createCapabilities();

        refreshFramebufferSize();
        refreshWindowSize();

        glfwSetFramebufferSizeCallback(handle, (w, newWidth, newHeight) -> {
            width = Math.max(newWidth, 1);
            height = Math.max(newHeight, 1);
        });

        glfwSetWindowSizeCallback(handle, (w, newWidth, newHeight) -> {
            windowWidth = Math.max(newWidth, 1);
            windowHeight = Math.max(newHeight, 1);
        });
    }

    public long handle() {
        return handle;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int windowWidth() {
        return windowWidth;
    }

    public int windowHeight() {
        return windowHeight;
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(handle);
    }

    // 設定是否要鎖定滑鼠游標。
    // true 代表隱藏並鎖定游標，常用在第一人稱視角。
    public void captureCursor(boolean capture) {
        glfwSetInputMode(handle, GLFW_CURSOR, capture ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
    }

    public void pollEvents() {
        glfwPollEvents();
    }

    public void swapBuffers() {
        glfwSwapBuffers(handle);
    }

    @Override
    public void close() {
        if (handle != NULL) {
            Callbacks.glfwFreeCallbacks(handle);
            glfwDestroyWindow(handle);
            GL.setCapabilities(null);
            handle = NULL;
        }

        if (glfwInitialized) {
            glfwTerminate();
            glfwInitialized = false;
        }

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

    // 重新取得 GLFW 視窗的邏輯大小。
    // 這組尺寸與滑鼠座標同一個座標系，HUD hit test 不能直接拿高 DPI framebuffer 尺寸替代。
    private void refreshWindowSize() {
        if (handle == NULL) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer currentWidth = stack.mallocInt(1);
            IntBuffer currentHeight = stack.mallocInt(1);

            glfwGetWindowSize(handle, currentWidth, currentHeight);

            windowWidth = Math.max(currentWidth.get(0), 1);
            windowHeight = Math.max(currentHeight.get(0), 1);
        }
    }
}

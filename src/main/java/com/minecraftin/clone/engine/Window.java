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
    private long handle;
    private int width;
    private int height;
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
        glfwSwapInterval(0);
        GL.createCapabilities();

        refreshFramebufferSize();

        glfwSetFramebufferSizeCallback(handle, (w, newWidth, newHeight) -> {
            width = Math.max(newWidth, 1);
            height = Math.max(newHeight, 1);
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

    public boolean shouldClose() {
        return glfwWindowShouldClose(handle);
    }

    public void requestClose() {
        glfwSetWindowShouldClose(handle, true);
    }

    public void captureCursor(boolean capture) {
        glfwSetInputMode(handle, GLFW_CURSOR, capture ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
    }

    public void pollEvents() {
        glfwPollEvents();
        refreshFramebufferSize();
    }

    public void swapBuffers() {
        glfwSwapBuffers(handle);
    }

    @Override
    public void close() {
        if (handle != NULL) {
            glfwDestroyWindow(handle);
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

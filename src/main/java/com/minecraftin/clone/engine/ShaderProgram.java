package com.minecraftin.clone.engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL33C.*;

public final class ShaderProgram implements AutoCloseable {
    private final int id;

    public ShaderProgram(String vertexResource, String fragmentResource) {
        int vertexShader = compile(GL_VERTEX_SHADER, loadResource(vertexResource));
        int fragmentShader = compile(GL_FRAGMENT_SHADER, loadResource(fragmentResource));

        id = glCreateProgram();
        glAttachShader(id, vertexShader);
        glAttachShader(id, fragmentShader);
        glLinkProgram(id);

        if (glGetProgrami(id, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(id);
            throw new IllegalStateException("Program link failed: " + log);
        }

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }

    private static int compile(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            throw new IllegalStateException("Shader compile failed: " + log);
        }
        return shader;
    }

    private static String loadResource(String resourcePath) {
        try (InputStream inputStream = ShaderProgram.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Missing shader resource: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read shader: " + resourcePath, e);
        }
    }

    public void use() {
        glUseProgram(id);
    }

    public void setInt(String name, int value) {
        int location = glGetUniformLocation(id, name);
        if (location >= 0) {
            glUniform1i(location, value);
        }
    }

    public void setFloat(String name, float value) {
        int location = glGetUniformLocation(id, name);
        if (location >= 0) {
            glUniform1f(location, value);
        }
    }

    public void setVec3(String name, Vector3f vec) {
        int location = glGetUniformLocation(id, name);
        if (location >= 0) {
            glUniform3f(location, vec.x, vec.y, vec.z);
        }
    }

    public void setMat4(String name, Matrix4f matrix) {
        int location = glGetUniformLocation(id, name);
        if (location < 0) {
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            matrix.get(buffer);
            glUniformMatrix4fv(location, false, buffer);
        }
    }

    @Override
    public void close() {
        glDeleteProgram(id);
    }
}

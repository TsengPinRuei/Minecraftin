package com.minecraftin.clone.engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33C.*;

// Load and link GLSL programs; all OpenGL operations require a current context.
public final class ShaderProgram implements AutoCloseable {
    private final int id;

    // Cache uniform locations, including missing ones, to avoid repeated OpenGL lookups.
    private final Map<String, Integer> uniformLocations = new HashMap<>();

    public ShaderProgram(String vertexResource, String fragmentResource) {
        String vertexSource = loadResource(vertexResource);
        String fragmentSource = loadResource(fragmentResource);
        try (ResourceScope shaders = new ResourceScope()) {
            int vertexShader = compile(GL_VERTEX_SHADER, vertexSource);
            shaders.add(() -> glDeleteShader(vertexShader));
            int fragmentShader = compile(GL_FRAGMENT_SHADER, fragmentSource);
            shaders.add(() -> glDeleteShader(fragmentShader));
            int program = glCreateProgram();
            try {
                glAttachShader(program, vertexShader);
                glAttachShader(program, fragmentShader);
                glLinkProgram(program);
                if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
                    throw new IllegalStateException("Program link failed: " + glGetProgramInfoLog(program));
                }
                id = program;
            } catch (RuntimeException | Error e) {
                glDeleteProgram(program);
                throw e;
            }
        }
    }

    private static int compile(int type, String source) {
        int shader = glCreateShader(type);
        try {
            glShaderSource(shader, source);
            glCompileShader(shader);
            if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
                throw new IllegalStateException("Shader compile failed: " + glGetShaderInfoLog(shader));
            }
            return shader;
        } catch (RuntimeException | Error e) {
            glDeleteShader(shader);
            throw e;
        }
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
        int location = uniformLocation(name);

        if (location >= 0) {
            glUniform1i(location, value);
        }
    }

    public void setFloat(String name, float value) {
        int location = uniformLocation(name);

        if (location >= 0) {
            glUniform1f(location, value);
        }
    }

    public void setVec3(String name, Vector3f vec) {
        int location = uniformLocation(name);

        if (location >= 0) {
            glUniform3f(location, vec.x, vec.y, vec.z);
        }
    }

    public void setMat4(String name, Matrix4f matrix) {
        int location = uniformLocation(name);

        if (location < 0) {
            return;
        }

        // Keep the temporary matrix buffer scoped to this upload.
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);

            matrix.get(buffer);
            glUniformMatrix4fv(location, false, buffer);
        }
    }

    // Cache -1 for uniforms that are absent or optimized out; setters skip those locations.
    private int uniformLocation(String name) {
        return uniformLocations.computeIfAbsent(name, key -> glGetUniformLocation(id, key));
    }

    @Override
    public void close() {
        glDeleteProgram(id);
    }
}

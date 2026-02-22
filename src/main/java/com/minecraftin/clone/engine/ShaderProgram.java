// 宣告此檔案所屬的套件。
package com.minecraftin.clone.engine;

// 匯入後續會使用到的型別或函式。
import org.joml.Matrix4f;
// 匯入後續會使用到的型別或函式。
import org.joml.Vector3f;
// 匯入後續會使用到的型別或函式。
import org.lwjgl.system.MemoryStack;

// 匯入後續會使用到的型別或函式。
import java.io.IOException;
// 匯入後續會使用到的型別或函式。
import java.io.InputStream;
// 匯入後續會使用到的型別或函式。
import java.nio.FloatBuffer;
// 匯入後續會使用到的型別或函式。
import java.nio.charset.StandardCharsets;
// 匯入後續會使用到的型別或函式。
import java.util.HashMap;
// 匯入後續會使用到的型別或函式。
import java.util.Map;

// 匯入後續會使用到的型別或函式。
import static org.lwjgl.opengl.GL33C.*;

// 定義主要型別與其結構。
public final class ShaderProgram implements AutoCloseable {
    // 下一行程式碼負責執行目前步驟。
    private final int id;
    // 設定或更新變數的值。
    private final Map<String, Integer> uniformLocations = new HashMap<>();

    // 定義對外可呼叫的方法。
    public ShaderProgram(String vertexResource, String fragmentResource) {
        // 宣告並初始化變數。
        int vertexShader = compile(GL_VERTEX_SHADER, loadResource(vertexResource));
        // 宣告並初始化變數。
        int fragmentShader = compile(GL_FRAGMENT_SHADER, loadResource(fragmentResource));
        // 宣告並初始化變數。
        int programId = glCreateProgram();
        // 下一行程式碼負責執行目前步驟。
        try {
            // 呼叫方法執行對應功能。
            glAttachShader(programId, vertexShader);
            // 呼叫方法執行對應功能。
            glAttachShader(programId, fragmentShader);
            // 呼叫方法執行對應功能。
            glLinkProgram(programId);

            // 根據條件決定是否進入此邏輯分支。
            if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
                // 宣告並初始化變數。
                String log = glGetProgramInfoLog(programId);
                // 呼叫方法執行對應功能。
                throw new IllegalStateException("Program link failed: " + log);
            }
        // 下一行程式碼負責執行目前步驟。
        } catch (RuntimeException e) {
            // 呼叫方法執行對應功能。
            glDeleteProgram(programId);
            // 下一行程式碼負責執行目前步驟。
            throw e;
        // 下一行程式碼負責執行目前步驟。
        } finally {
            // 呼叫方法執行對應功能。
            glDeleteShader(vertexShader);
            // 呼叫方法執行對應功能。
            glDeleteShader(fragmentShader);
        }
        // 設定或更新變數的值。
        id = programId;
    }

    // 定義類別內部使用的方法。
    private static int compile(int type, String source) {
        // 宣告並初始化變數。
        int shader = glCreateShader(type);
        // 呼叫方法執行對應功能。
        glShaderSource(shader, source);
        // 呼叫方法執行對應功能。
        glCompileShader(shader);

        // 根據條件決定是否進入此邏輯分支。
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            // 宣告並初始化變數。
            String log = glGetShaderInfoLog(shader);
            // 呼叫方法執行對應功能。
            glDeleteShader(shader);
            // 呼叫方法執行對應功能。
            throw new IllegalStateException("Shader compile failed: " + log);
        }
        // 下一行程式碼負責執行目前步驟。
        return shader;
    }

    // 定義類別內部使用的方法。
    private static String loadResource(String resourcePath) {
        // 下一行程式碼負責執行目前步驟。
        try (InputStream inputStream = ShaderProgram.class.getResourceAsStream(resourcePath)) {
            // 根據條件決定是否進入此邏輯分支。
            if (inputStream == null) {
                // 呼叫方法執行對應功能。
                throw new IllegalArgumentException("Missing shader resource: " + resourcePath);
            }
            // 呼叫方法執行對應功能。
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        // 下一行程式碼負責執行目前步驟。
        } catch (IOException e) {
            // 呼叫方法執行對應功能。
            throw new IllegalStateException("Failed to read shader: " + resourcePath, e);
        }
    }

    // 定義對外可呼叫的方法。
    public void use() {
        // 呼叫方法執行對應功能。
        glUseProgram(id);
    }

    // 定義對外可呼叫的方法。
    public void setInt(String name, int value) {
        // 宣告並初始化變數。
        int location = uniformLocation(name);
        // 根據條件決定是否進入此邏輯分支。
        if (location >= 0) {
            // 呼叫方法執行對應功能。
            glUniform1i(location, value);
        }
    }

    // 定義對外可呼叫的方法。
    public void setFloat(String name, float value) {
        // 宣告並初始化變數。
        int location = uniformLocation(name);
        // 根據條件決定是否進入此邏輯分支。
        if (location >= 0) {
            // 呼叫方法執行對應功能。
            glUniform1f(location, value);
        }
    }

    // 定義對外可呼叫的方法。
    public void setVec3(String name, Vector3f vec) {
        // 宣告並初始化變數。
        int location = uniformLocation(name);
        // 根據條件決定是否進入此邏輯分支。
        if (location >= 0) {
            // 呼叫方法執行對應功能。
            glUniform3f(location, vec.x, vec.y, vec.z);
        }
    }

    // 定義對外可呼叫的方法。
    public void setMat4(String name, Matrix4f matrix) {
        // 宣告並初始化變數。
        int location = uniformLocation(name);
        // 根據條件決定是否進入此邏輯分支。
        if (location < 0) {
            // 下一行程式碼負責執行目前步驟。
            return;
        }
        // 下一行程式碼負責執行目前步驟。
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // 宣告並初始化變數。
            FloatBuffer buffer = stack.mallocFloat(16);
            // 呼叫方法執行對應功能。
            matrix.get(buffer);
            // 呼叫方法執行對應功能。
            glUniformMatrix4fv(location, false, buffer);
        }
    }

    // 定義類別內部使用的方法。
    private int uniformLocation(String name) {
        // 呼叫方法執行對應功能。
        return uniformLocations.computeIfAbsent(name, key -> glGetUniformLocation(id, key));
    }

    // 宣告註解標記，提供編譯器或框架額外資訊。
    @Override
    // 定義對外可呼叫的方法。
    public void close() {
        // 呼叫方法執行對應功能。
        glDeleteProgram(id);
    }
}

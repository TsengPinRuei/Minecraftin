// 說明：宣告此檔案所屬的套件。
package com.minecraftin.clone.engine;

// 說明：匯入後續會使用到的型別或函式。
import org.joml.Matrix4f;
// 說明：匯入後續會使用到的型別或函式。
import org.joml.Vector3f;
// 說明：匯入後續會使用到的型別或函式。
import org.lwjgl.system.MemoryStack;

// 說明：匯入後續會使用到的型別或函式。
import java.io.IOException;
// 說明：匯入後續會使用到的型別或函式。
import java.io.InputStream;
// 說明：匯入後續會使用到的型別或函式。
import java.nio.FloatBuffer;
// 說明：匯入後續會使用到的型別或函式。
import java.nio.charset.StandardCharsets;
// 說明：匯入後續會使用到的型別或函式。
import java.util.HashMap;
// 說明：匯入後續會使用到的型別或函式。
import java.util.Map;

// 說明：匯入後續會使用到的型別或函式。
import static org.lwjgl.opengl.GL33C.*;

// 說明：定義主要型別與其結構。
public final class ShaderProgram implements AutoCloseable {
    // 說明：下一行程式碼負責執行目前步驟。
    private final int id;
    // 說明：設定或更新變數的值。
    private final Map<String, Integer> uniformLocations = new HashMap<>();

    // 說明：定義對外可呼叫的方法。
    public ShaderProgram(String vertexResource, String fragmentResource) {
        // 說明：宣告並初始化變數。
        int vertexShader = compile(GL_VERTEX_SHADER, loadResource(vertexResource));
        // 說明：宣告並初始化變數。
        int fragmentShader = compile(GL_FRAGMENT_SHADER, loadResource(fragmentResource));
        // 說明：宣告並初始化變數。
        int programId = glCreateProgram();
        // 說明：下一行程式碼負責執行目前步驟。
        try {
            // 說明：呼叫方法執行對應功能。
            glAttachShader(programId, vertexShader);
            // 說明：呼叫方法執行對應功能。
            glAttachShader(programId, fragmentShader);
            // 說明：呼叫方法執行對應功能。
            glLinkProgram(programId);

            // 說明：根據條件決定是否進入此邏輯分支。
            if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
                // 說明：宣告並初始化變數。
                String log = glGetProgramInfoLog(programId);
                // 說明：呼叫方法執行對應功能。
                throw new IllegalStateException("Program link failed: " + log);
            }
        // 說明：下一行程式碼負責執行目前步驟。
        } catch (RuntimeException e) {
            // 說明：呼叫方法執行對應功能。
            glDeleteProgram(programId);
            // 說明：下一行程式碼負責執行目前步驟。
            throw e;
        // 說明：下一行程式碼負責執行目前步驟。
        } finally {
            // 說明：呼叫方法執行對應功能。
            glDeleteShader(vertexShader);
            // 說明：呼叫方法執行對應功能。
            glDeleteShader(fragmentShader);
        }
        // 說明：設定或更新變數的值。
        id = programId;
    }

    // 說明：定義類別內部使用的方法。
    private static int compile(int type, String source) {
        // 說明：宣告並初始化變數。
        int shader = glCreateShader(type);
        // 說明：呼叫方法執行對應功能。
        glShaderSource(shader, source);
        // 說明：呼叫方法執行對應功能。
        glCompileShader(shader);

        // 說明：根據條件決定是否進入此邏輯分支。
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            // 說明：宣告並初始化變數。
            String log = glGetShaderInfoLog(shader);
            // 說明：呼叫方法執行對應功能。
            glDeleteShader(shader);
            // 說明：呼叫方法執行對應功能。
            throw new IllegalStateException("Shader compile failed: " + log);
        }
        // 說明：下一行程式碼負責執行目前步驟。
        return shader;
    }

    // 說明：定義類別內部使用的方法。
    private static String loadResource(String resourcePath) {
        // 說明：下一行程式碼負責執行目前步驟。
        try (InputStream inputStream = ShaderProgram.class.getResourceAsStream(resourcePath)) {
            // 說明：根據條件決定是否進入此邏輯分支。
            if (inputStream == null) {
                // 說明：呼叫方法執行對應功能。
                throw new IllegalArgumentException("Missing shader resource: " + resourcePath);
            }
            // 說明：呼叫方法執行對應功能。
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        // 說明：下一行程式碼負責執行目前步驟。
        } catch (IOException e) {
            // 說明：呼叫方法執行對應功能。
            throw new IllegalStateException("Failed to read shader: " + resourcePath, e);
        }
    }

    // 說明：定義對外可呼叫的方法。
    public void use() {
        // 說明：呼叫方法執行對應功能。
        glUseProgram(id);
    }

    // 說明：定義對外可呼叫的方法。
    public void setInt(String name, int value) {
        // 說明：宣告並初始化變數。
        int location = uniformLocation(name);
        // 說明：根據條件決定是否進入此邏輯分支。
        if (location >= 0) {
            // 說明：呼叫方法執行對應功能。
            glUniform1i(location, value);
        }
    }

    // 說明：定義對外可呼叫的方法。
    public void setFloat(String name, float value) {
        // 說明：宣告並初始化變數。
        int location = uniformLocation(name);
        // 說明：根據條件決定是否進入此邏輯分支。
        if (location >= 0) {
            // 說明：呼叫方法執行對應功能。
            glUniform1f(location, value);
        }
    }

    // 說明：定義對外可呼叫的方法。
    public void setVec3(String name, Vector3f vec) {
        // 說明：宣告並初始化變數。
        int location = uniformLocation(name);
        // 說明：根據條件決定是否進入此邏輯分支。
        if (location >= 0) {
            // 說明：呼叫方法執行對應功能。
            glUniform3f(location, vec.x, vec.y, vec.z);
        }
    }

    // 說明：定義對外可呼叫的方法。
    public void setMat4(String name, Matrix4f matrix) {
        // 說明：宣告並初始化變數。
        int location = uniformLocation(name);
        // 說明：根據條件決定是否進入此邏輯分支。
        if (location < 0) {
            // 說明：下一行程式碼負責執行目前步驟。
            return;
        }
        // 說明：下一行程式碼負責執行目前步驟。
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // 說明：宣告並初始化變數。
            FloatBuffer buffer = stack.mallocFloat(16);
            // 說明：呼叫方法執行對應功能。
            matrix.get(buffer);
            // 說明：呼叫方法執行對應功能。
            glUniformMatrix4fv(location, false, buffer);
        }
    }

    // 說明：定義類別內部使用的方法。
    private int uniformLocation(String name) {
        // 說明：呼叫方法執行對應功能。
        return uniformLocations.computeIfAbsent(name, key -> glGetUniformLocation(id, key));
    }

    // 說明：宣告註解標記，提供編譯器或框架額外資訊。
    @Override
    // 說明：定義對外可呼叫的方法。
    public void close() {
        // 說明：呼叫方法執行對應功能。
        glDeleteProgram(id);
    }
}

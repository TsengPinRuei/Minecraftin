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

// 封裝 GLSL shader program 的載入、編譯、連結與 uniform 設定。
// 建立與釋放都必須在有效的 OpenGL context 中進行。
public final class ShaderProgram implements AutoCloseable {
    // OpenGL shader program 的 ID，用來代表已建立完成的 shader 程式。
    private final int id;

    // 快取 uniform 變數的位置，避免每次設定值時都重新查詢。
    private final Map<String, Integer> uniformLocations = new HashMap<>();

    // 建立 shader program，並載入頂點著色器與片段著色器。
    public ShaderProgram(String vertexResource, String fragmentResource) {
        // 先編譯頂點著色器與片段著色器。
        int vertexShader = compile(GL_VERTEX_SHADER, loadResource(vertexResource));
        int fragmentShader = compile(GL_FRAGMENT_SHADER, loadResource(fragmentResource));

        // 建立 OpenGL program。
        int programId = glCreateProgram();

        try {
            // 把兩個 shader 掛到同一個 program 上。
            glAttachShader(programId, vertexShader);
            glAttachShader(programId, fragmentShader);

            // 將 shader 連結成可執行的 program。
            glLinkProgram(programId);

            // 如果連結失敗，就取出錯誤訊息並拋出例外。
            if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
                String log = glGetProgramInfoLog(programId);
                throw new IllegalStateException("Program link failed: " + log);
            }
        } catch (RuntimeException e) {
            // 如果中途失敗，記得刪除已建立的 program，避免資源洩漏。
            glDeleteProgram(programId);
            throw e;
        } finally {
            // 無論成功或失敗，shader 物件本身都可以刪除。
            // 因為成功連結後，program 內部已經保留需要的內容。
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);
        }

        id = programId;
    }

    // 編譯單一 shader，type 可能是頂點著色器或片段著色器。
    private static int compile(int type, String source) {
        int shader = glCreateShader(type);

        // 把 GLSL 原始碼交給 OpenGL。
        glShaderSource(shader, source);

        // 開始編譯 shader。
        glCompileShader(shader);

        // 如果編譯失敗，就取得錯誤訊息並丟出例外。
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException("Shader compile failed: " + log);
        }

        return shader;
    }

    // 從 resources 讀取 shader 檔案內容。
    private static String loadResource(String resourcePath) {
        try (InputStream inputStream = ShaderProgram.class.getResourceAsStream(resourcePath)) {
            // 找不到資源檔時直接拋出錯誤。
            if (inputStream == null) {
                throw new IllegalArgumentException("Missing shader resource: " + resourcePath);
            }

            // 讀取整個檔案，並用 UTF-8 轉成字串。
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // 讀取失敗時包成執行期例外。
            throw new IllegalStateException("Failed to read shader: " + resourcePath, e);
        }
    }

    // 啟用這個 shader program，之後的繪圖就會使用它。
    public void use() {
        glUseProgram(id);
    }

    // 設定 int 型別的 uniform 變數。
    public void setInt(String name, int value) {
        int location = uniformLocation(name);

        // 如果找得到這個 uniform，才進行設定。
        if (location >= 0) {
            glUniform1i(location, value);
        }
    }

    // 設定 float 型別的 uniform 變數。
    public void setFloat(String name, float value) {
        int location = uniformLocation(name);

        if (location >= 0) {
            glUniform1f(location, value);
        }
    }

    // 設定 vec3 型別的 uniform 變數。
    public void setVec3(String name, Vector3f vec) {
        int location = uniformLocation(name);

        if (location >= 0) {
            glUniform3f(location, vec.x, vec.y, vec.z);
        }
    }

    // 設定 mat4 型別的 uniform 變數。
    public void setMat4(String name, Matrix4f matrix) {
        int location = uniformLocation(name);

        // 如果 shader 中沒有這個 uniform，就直接略過。
        if (location < 0) {
            return;
        }

        // 使用 MemoryStack 暫時配置一塊記憶體來存放 4x4 矩陣資料。
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);

            // 把矩陣內容寫入 buffer，再傳給 OpenGL。
            matrix.get(buffer);
            glUniformMatrix4fv(location, false, buffer);
        }
    }

    // 取得 uniform 位置，並把查詢結果快取起來；OpenGL 會用 -1 表示 shader 最佳化後不存在的 uniform。
    private int uniformLocation(String name) {
        return uniformLocations.computeIfAbsent(name, key -> glGetUniformLocation(id, key));
    }

    // 釋放 shader program 佔用的 OpenGL 資源。
    @Override
    public void close() {
        glDeleteProgram(id);
    }
}

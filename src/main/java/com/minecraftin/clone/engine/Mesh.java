// 宣告此檔案所屬的套件。
package com.minecraftin.clone.engine;

// 匯入後續會使用到的型別或函式。
import static org.lwjgl.opengl.GL33C.*;

// 定義主要型別與其結構。
public final class Mesh implements AutoCloseable {
    // 下一行程式碼負責執行目前步驟。
    private final int vao;
    // 下一行程式碼負責執行目前步驟。
    private final int vbo;
    // 下一行程式碼負責執行目前步驟。
    private final int mode;
    // 下一行程式碼負責執行目前步驟。
    private int vertexCount;

    // 定義對外可呼叫的方法。
    public Mesh(float[] vertices, int mode, int... attributeSizes) {
        // 根據條件決定是否進入此邏輯分支。
        if (attributeSizes.length == 0) {
            // 呼叫方法執行對應功能。
            throw new IllegalArgumentException("attributeSizes must not be empty");
        }

        // 設定或更新變數的值。
        this.mode = mode;
        // 設定或更新變數的值。
        vao = glGenVertexArrays();
        // 設定或更新變數的值。
        vbo = glGenBuffers();

        // 呼叫方法執行對應功能。
        glBindVertexArray(vao);
        // 呼叫方法執行對應功能。
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        // 呼叫方法執行對應功能。
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);

        // 宣告並初始化變數。
        int strideFloats = 0;
        // 使用迴圈逐一處理每個元素或區間。
        for (int size : attributeSizes) {
            // 根據條件決定是否進入此邏輯分支。
            if (size <= 0) {
                // 呼叫方法執行對應功能。
                throw new IllegalArgumentException("attributeSizes must be positive");
            }
            // 設定或更新變數的值。
            strideFloats += size;
        }
        // 呼叫方法執行對應功能。
        validateVertexLayout(vertices, strideFloats);

        // 宣告並初始化變數。
        int offsetFloats = 0;
        // 使用迴圈逐一處理每個元素或區間。
        for (int i = 0; i < attributeSizes.length; i++) {
            // 宣告並初始化變數。
            int size = attributeSizes[i];
            // 呼叫方法執行對應功能。
            glEnableVertexAttribArray(i);
            // 呼叫方法執行對應功能。
            glVertexAttribPointer(i, size, GL_FLOAT, false, strideFloats * Float.BYTES, (long) offsetFloats * Float.BYTES);
            // 設定或更新變數的值。
            offsetFloats += size;
        }

        // 呼叫方法執行對應功能。
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        // 呼叫方法執行對應功能。
        glBindVertexArray(0);

        // 設定或更新變數的值。
        vertexCount = vertices.length / strideFloats;
    }

    // 定義對外可呼叫的方法。
    public void update(float[] vertices, int strideFloats) {
        // 呼叫方法執行對應功能。
        validateVertexLayout(vertices, strideFloats);
        // 呼叫方法執行對應功能。
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        // 呼叫方法執行對應功能。
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);
        // 呼叫方法執行對應功能。
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        // 設定或更新變數的值。
        vertexCount = vertices.length / strideFloats;
    }

    // 定義對外可呼叫的方法。
    public void draw() {
        // 根據條件決定是否進入此邏輯分支。
        if (vertexCount <= 0) {
            // 下一行程式碼負責執行目前步驟。
            return;
        }
        // 呼叫方法執行對應功能。
        glBindVertexArray(vao);
        // 呼叫方法執行對應功能。
        glDrawArrays(mode, 0, vertexCount);
        // 呼叫方法執行對應功能。
        glBindVertexArray(0);
    }

    // 宣告註解標記，提供編譯器或框架額外資訊。
    @Override
    // 定義對外可呼叫的方法。
    public void close() {
        // 呼叫方法執行對應功能。
        glDeleteBuffers(vbo);
        // 呼叫方法執行對應功能。
        glDeleteVertexArrays(vao);
    }

    // 定義類別內部使用的方法。
    private static void validateVertexLayout(float[] vertices, int strideFloats) {
        // 根據條件決定是否進入此邏輯分支。
        if (strideFloats <= 0) {
            // 呼叫方法執行對應功能。
            throw new IllegalArgumentException("strideFloats must be positive");
        }
        // 根據條件決定是否進入此邏輯分支。
        if (vertices.length % strideFloats != 0) {
            // 呼叫方法執行對應功能。
            throw new IllegalArgumentException("vertices length must be divisible by stride");
        }
    }
}

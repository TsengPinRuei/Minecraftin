package com.minecraftin.clone.engine;

import static org.lwjgl.opengl.GL33C.*;

public final class Mesh implements AutoCloseable {
    // 記錄這個模型的頂點設定方式。
    private final int vao;

    // 存放實際的頂點資料。
    private final int vbo;

    // 繪圖模式，例如三角形、線段等。
    private final int mode;

    // 目前總共有多少個頂點可以被拿來繪製。
    private int vertexCount;

    // 建立 Mesh，並把頂點資料與屬性格式一起設定到 OpenGL。
    public Mesh(float[] vertices, int mode, int... attributeSizes) {
        // 至少要提供一種頂點屬性格式，否則無法知道資料怎麼切分。
        if (attributeSizes.length == 0) {
            throw new IllegalArgumentException("attributeSizes must not be empty");
        }

        this.mode = mode;
        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        // 綁定 VAO 與 VBO，準備設定頂點資料。
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        // 把頂點資料送進顯示卡，並標記為之後可能會更新。
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);

        // 計算一個頂點總共包含幾個 float。
        int strideFloats = 0;
        for (int size : attributeSizes) {
            // 每個屬性的大小都必須大於 0。
            if (size <= 0) {
                throw new IllegalArgumentException("attributeSizes must be positive");
            }
            strideFloats += size;
        }

        // 檢查頂點資料長度是否符合頂點格式。
        validateVertexLayout(vertices, strideFloats);

        // 依序設定每個頂點屬性的位置與大小。
        int offsetFloats = 0;
        for (int i = 0; i < attributeSizes.length; i++) {
            int size = attributeSizes[i];

            // 啟用第 i 個頂點屬性。
            glEnableVertexAttribArray(i);

            // 告訴 OpenGL 第 i 個屬性要從哪裡開始讀、一次讀幾個 float。
            glVertexAttribPointer(
                    i,
                    size,
                    GL_FLOAT,
                    false,
                    strideFloats * Float.BYTES,
                    (long) offsetFloats * Float.BYTES);

            offsetFloats += size;
        }

        // 設定完成後解除綁定。
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        // 根據總資料量與每個頂點大小，算出頂點數量。
        vertexCount = vertices.length / strideFloats;
    }

    // 更新 VBO 內的頂點資料。
    public void update(float[] vertices, int strideFloats) {
        // 先確認新資料長度仍然符合原本的頂點格式。
        validateVertexLayout(vertices, strideFloats);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        // 重新計算更新後的頂點數量。
        vertexCount = vertices.length / strideFloats;
    }

    // 使用目前的 VAO 與 VBO 資料進行繪製。
    public void draw() {
        // 沒有頂點時就不需要畫。
        if (vertexCount <= 0) {
            return;
        }

        glBindVertexArray(vao);
        glDrawArrays(mode, 0, vertexCount);
        glBindVertexArray(0);
    }

    @Override
    // 釋放 OpenGL 資源，避免記憶體或顯示卡資源洩漏。
    public void close() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }

    // 檢查頂點資料是否能正確依照 stride 切成一筆一筆頂點。
    private static void validateVertexLayout(float[] vertices, int strideFloats) {
        // stride 必須大於 0，否則代表頂點格式不合法。
        if (strideFloats <= 0) {
            throw new IllegalArgumentException("strideFloats must be positive");
        }

        // 頂點資料總長度必須剛好能被 stride 整除。
        if (vertices.length % strideFloats != 0) {
            throw new IllegalArgumentException("vertices length must be divisible by stride");
        }
    }
}
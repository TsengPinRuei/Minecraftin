package com.minecraftin.clone.engine;

import static org.lwjgl.opengl.GL33C.*;

public final class Mesh implements AutoCloseable {
    private final int vao;
    private final int vbo;
    private final int mode;
    private int vertexCount;

    public Mesh(float[] vertices, int mode, int... attributeSizes) {
        if (attributeSizes.length == 0) {
            throw new IllegalArgumentException("attributeSizes must not be empty");
        }

        this.mode = mode;
        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);

        int strideFloats = 0;
        for (int size : attributeSizes) {
            strideFloats += size;
        }

        int offsetFloats = 0;
        for (int i = 0; i < attributeSizes.length; i++) {
            int size = attributeSizes[i];
            glEnableVertexAttribArray(i);
            glVertexAttribPointer(i, size, GL_FLOAT, false, strideFloats * Float.BYTES, (long) offsetFloats * Float.BYTES);
            offsetFloats += size;
        }

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        vertexCount = vertices.length / strideFloats;
    }

    public void update(float[] vertices, int strideFloats) {
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        vertexCount = vertices.length / strideFloats;
    }

    public void draw() {
        if (vertexCount <= 0) {
            return;
        }
        glBindVertexArray(vao);
        glDrawArrays(mode, 0, vertexCount);
        glBindVertexArray(0);
    }

    @Override
    public void close() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}

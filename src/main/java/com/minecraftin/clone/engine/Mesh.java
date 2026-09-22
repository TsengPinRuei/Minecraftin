package com.minecraftin.clone.engine;

import static org.lwjgl.opengl.GL33C.*;

// Attribute sizes define interleaved float data in shader location order.
// Construct, update, draw, and close only while the owning OpenGL context is current.
public final class Mesh implements AutoCloseable {
    private final int vao;
    private final int vbo;
    private final int mode;
    private final int strideFloats;
    private int vertexCount;

    public Mesh(float[] vertices, int mode, int... attributeSizes) {
        if (attributeSizes.length == 0) {
            throw new IllegalArgumentException("attributeSizes must not be empty");
        }
        int stride = 0;
        for (int size : attributeSizes) {
            if (size < 1 || size > 4) {
                throw new IllegalArgumentException("attributeSizes must be between 1 and 4");
            }
            stride = Math.addExact(stride, size);
        }
        validateVertexLayout(vertices, stride);
        this.strideFloats = stride;
        this.mode = mode;

        try (ResourceScope resources = new ResourceScope()) {
            vao = glGenVertexArrays();
            resources.add(() -> glDeleteVertexArrays(vao));
            vbo = glGenBuffers();
            resources.add(() -> glDeleteBuffers(vbo));
            try {
                glBindVertexArray(vao);
                glBindBuffer(GL_ARRAY_BUFFER, vbo);
                glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);
                int offset = 0;
                for (int i = 0; i < attributeSizes.length; i++) {
                    glEnableVertexAttribArray(i);
                    glVertexAttribPointer(i, attributeSizes[i], GL_FLOAT, false,
                            strideFloats * Float.BYTES, (long) offset * Float.BYTES);
                    offset += attributeSizes[i];
                }
            } finally {
                glBindBuffer(GL_ARRAY_BUFFER, 0);
                glBindVertexArray(0);
            }
            vertexCount = vertices.length / strideFloats;
            resources.release();
        }
    }

    // Keep the constructor's vertex stride; changing it would invalidate the existing attribute
    // layout.
    public void update(float[] vertices) {
        validateVertexLayout(vertices, strideFloats);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        try {
            glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);
            vertexCount = vertices.length / strideFloats;
        } finally {
            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }
    }

    public void draw() {
        if (vertexCount == 0) {
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

    private static void validateVertexLayout(float[] vertices, int stride) {
        if (vertices.length % stride != 0) {
            throw new IllegalArgumentException("vertices length must be divisible by stride");
        }
    }
}

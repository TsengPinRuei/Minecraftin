package com.minecraftin.clone.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class MeshTest {
    @Test
    void invalidLayoutIsRejectedBeforeRequiringAnOpenGlContext() {
        assertThrows(IllegalArgumentException.class, () -> new Mesh(new float[3], 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Mesh(new float[5], 0, 5));
        assertThrows(IllegalArgumentException.class, () -> new Mesh(new float[4], 0, 3));
        assertThrows(IllegalArgumentException.class, () -> new Mesh(new float[0], 0));
    }
}

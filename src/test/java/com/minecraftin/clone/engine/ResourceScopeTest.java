package com.minecraftin.clone.engine;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class ResourceScopeTest {
    @Test
    void failureStillClosesEveryResourceInReverseOrder() {
        List<Integer> closed = new ArrayList<>();
        RuntimeException failure = new RuntimeException("close failed");
        ResourceScope resources = new ResourceScope();
        resources.add(() -> closed.add(1));
        resources.add(() -> { closed.add(2); throw failure; });
        resources.add(() -> closed.add(3));
        assertSame(failure, assertThrows(RuntimeException.class, resources::close));
        assertEquals(List.of(3, 2, 1), closed);
        assertDoesNotThrow(resources::close);
    }

    @Test
    void initializationFailureRemainsPrimaryWhenCleanupAlsoFails() {
        RuntimeException init = new RuntimeException("init");
        RuntimeException cleanup = new RuntimeException("cleanup");
        RuntimeException result = assertThrows(RuntimeException.class, () -> {
            try (ResourceScope resources = new ResourceScope()) {
                resources.add(() -> { throw cleanup; });
                throw init;
            }
        });
        assertSame(init, result);
        assertArrayEquals(new Throwable[]{cleanup}, result.getSuppressed());
    }

    @Test
    void releasedResourcesRemainOwnedByCaller() {
        List<Integer> closed = new ArrayList<>();
        try (ResourceScope resources = new ResourceScope()) {
            resources.add(() -> closed.add(1));
            resources.release();
        }
        assertTrue(closed.isEmpty());
    }
}

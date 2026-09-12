package com.minecraftin.clone.util;

import org.junit.jupiter.api.Test;
import java.util.NoSuchElementException;
import static org.junit.jupiter.api.Assertions.*;

final class LongQueueTest {
    @Test
    void wrappingAndGrowingPreserveOrder() {
        LongQueue queue = new LongQueue(16);
        for (int i = 0; i < 16; i++) queue.add(i);
        for (int i = 0; i < 10; i++) assertEquals(i, queue.poll());
        for (int i = 16; i < 100; i++) queue.add(i);
        for (int i = 10; i < 100; i++) assertEquals(i, queue.poll());
        assertTrue(queue.isEmpty());
        assertThrows(NoSuchElementException.class, queue::poll);
        queue.add(101);
        assertEquals(101, queue.poll());
    }
}

package com.minecraftin.clone.game;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ResourceBudgetTest {
    @Test
    void prototypeBudgetAccountsForPayloadAndWarnsOnAnyResource() {
        assertEquals(131072, ResourceBudget.CHUNK_PAYLOAD_BYTES);
        assertEquals(268435456, ResourceBudget.chunkPayloadBytes(2048));
        assertFalse(ResourceBudget.exceeded(529, 500, 2000, 100));
        assertTrue(ResourceBudget.exceeded(2048, 500, 2000, 100));
        assertTrue(ResourceBudget.exceeded(529, 1500, 2000, 100));
        assertTrue(ResourceBudget.exceeded(529, 500, 2000, 1000));
    }
}

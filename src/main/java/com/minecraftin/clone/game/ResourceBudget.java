package com.minecraftin.clone.game;

import com.minecraftin.clone.config.GameConfig;

// Advisory prototype budget: retained block/light data grows with exploration, even after saving.
final class ResourceBudget {
    static final int CHUNK_WARNING = 2048;
    static final double HEAP_WARNING_FRACTION = 0.75;
    static final double SAVE_WARNING_MILLIS = 1000.0;
    static final long CHUNK_PAYLOAD_BYTES = (long) GameConfig.CHUNK_SIZE * GameConfig.CHUNK_HEIGHT
            * GameConfig.CHUNK_SIZE * (Short.BYTES + 2L * Byte.BYTES);

    static long chunkPayloadBytes(int chunks) {
        return Math.max(0L, chunks) * CHUNK_PAYLOAD_BYTES;
    }

    static boolean exceeded(int chunks, long usedHeap, long maxHeap, double saveMillis) {
        return chunks >= CHUNK_WARNING || usedHeap >= maxHeap * HEAP_WARNING_FRACTION
                || saveMillis >= SAVE_WARNING_MILLIS;
    }

    private ResourceBudget() { }
}

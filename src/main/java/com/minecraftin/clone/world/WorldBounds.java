package com.minecraftin.clone.world;

import com.minecraftin.clone.config.GameConfig;

// One shared range for float physics, chunk arithmetic, lighting, water, and persisted data.
// Within +/-8192 a float's spacing is at most 1/1024, below the collision epsilon (0.001).
public final class WorldBounds {
    public static final int MIN_BLOCK = -8192;
    public static final int MAX_BLOCK = 8191;
    public static final float MAX_POSITION = MAX_BLOCK + 1.0f;
    public static final float MIN_Y = -8192.0f;
    public static final float MAX_Y = 8192.0f;
    public static final float MIN_PLAYER_XZ = MIN_BLOCK + GameConfig.PLAYER_WIDTH * 0.5f;
    public static final float MAX_PLAYER_XZ = MAX_POSITION - GameConfig.PLAYER_WIDTH * 0.5f;
    public static final float MAX_PLAYER_Y = MAX_Y - GameConfig.PLAYER_HEIGHT;
    public static final int MIN_CHUNK = Math.floorDiv(MIN_BLOCK, GameConfig.CHUNK_SIZE);
    public static final int MAX_CHUNK = Math.floorDiv(MAX_BLOCK, GameConfig.CHUNK_SIZE);
    public static final float MAX_RAY_DISTANCE = 3.0f * (MAX_POSITION - MIN_BLOCK);

    private WorldBounds() {
    }

    public static boolean containsBlock(int x, int z) {
        return x >= MIN_BLOCK && x <= MAX_BLOCK && z >= MIN_BLOCK && z <= MAX_BLOCK;
    }

    public static boolean containsCell(int x, int y, int z) {
        return containsBlock(x, z) && y >= 0 && y < GameConfig.CHUNK_HEIGHT;
    }

    public static boolean containsChunk(int x, int z) {
        return x >= MIN_CHUNK && x <= MAX_CHUNK && z >= MIN_CHUNK && z <= MAX_CHUNK;
    }

    public static boolean containsPosition(float x, float y, float z) {
        return Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z)
                && x >= MIN_BLOCK && x < MAX_POSITION && z >= MIN_BLOCK && z < MAX_POSITION
                && y >= MIN_Y && y <= MAX_Y;
    }

    public static boolean containsPlayerPosition(float x, float y, float z) {
        return containsPosition(x, y, z) && x >= MIN_PLAYER_XZ && x <= MAX_PLAYER_XZ
                && z >= MIN_PLAYER_XZ && z <= MAX_PLAYER_XZ && y <= MAX_PLAYER_Y;
    }

    public static void requirePlayerPosition(float x, float y, float z) {
        if (!containsPlayerPosition(x, y, z)) {
            throw new IllegalArgumentException("Player outside supported world bounds: " + x + ", " + y + ", " + z);
        }
    }

    public static void requirePosition(float x, float y, float z) {
        if (!containsPosition(x, y, z)) {
            throw new IllegalArgumentException("Position outside supported world bounds: " + x + ", " + y + ", " + z);
        }
    }

    public static void requireBlock(int x, int z) {
        if (!containsBlock(x, z)) {
            throw new IllegalArgumentException("Block outside supported world bounds: " + x + ", " + z);
        }
    }

    public static void requireChunk(int x, int z) {
        if (!containsChunk(x, z)) {
            throw new IllegalArgumentException("Chunk outside supported world bounds: " + x + ", " + z);
        }
    }
}

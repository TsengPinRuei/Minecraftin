package com.minecraftin.clone.world;

import com.minecraftin.clone.config.GameConfig;
import com.minecraftin.clone.util.LongQueue;

// Maintain sky and block light as separate levels from 0 to 15.
// Spread additions breadth-first; remove dependent light before spreading surviving sources back
// into the cleared area.
public final class LightEngine {

    public static final int MAX_LIGHT = 15;

    private static final int CHANNEL_SKY = 0;
    private static final int CHANNEL_BLOCK = 1;

    // Use parallel direction arrays in the propagation loop to avoid nested array lookups.
    private static final int[] DIRECTION_X = { 0, 0, -1, 1, 0, 0 };
    private static final int[] DIRECTION_Y = { 1, -1, 0, 0, 0, 0 };
    private static final int[] DIRECTION_Z = { 0, 0, 0, 0, -1, 1 };
    private static final int DIRECTION_COUNT = DIRECTION_X.length;
    private static final int DIRECTION_DOWN = 1;

    private final World world;

    // Reuse propagation queues between updates.
    private final LongQueue spreadQueue = new LongQueue(4096);
    private final LongQueue removalQueue = new LongQueue(1024);
    private final LongQueue neighborSeedQueue = new LongQueue(1024);
    private final LongQueue blockSeedQueue = new LongQueue(1024);
    private final LongQueue retainedSources = new LongQueue(64);

    public LightEngine(World world) {
        this.world = world;
    }

    // Initialize direct sky light and block emission, then exchange light with already loaded
    // neighbors.
    public void onChunkLoaded(Chunk chunk) {
        int minX = chunk.worldMinX();
        int minZ = chunk.worldMinZ();

        // Seed direct light from the top; opaque full cubes stop it, while water and leaves reduce
        // it.
        for (int z = 0; z < GameConfig.CHUNK_SIZE; z++) {
            for (int x = 0; x < GameConfig.CHUNK_SIZE; x++) {
                int level = MAX_LIGHT;
                for (int y = GameConfig.CHUNK_HEIGHT - 1; y >= 0; y--) {
                    if (level > 0) {
                        level = Math.max(0, propagatedLevel(CHANNEL_SKY, level, chunk.get(x, y, z).lightOpacity(), true));
                    }
                    chunk.setSkyLight(x, y, z, level);
                }
            }
        }

        // Initialize block emission and queue sky-light cells above level 1; lower levels cannot
        // brighten neighbors.
        for (int y = 0; y < GameConfig.CHUNK_HEIGHT; y++) {
            for (int z = 0; z < GameConfig.CHUNK_SIZE; z++) {
                for (int x = 0; x < GameConfig.CHUNK_SIZE; x++) {
                    int sky = chunk.skyLight(x, y, z);
                    if (sky > 1) {
                        spreadQueue.add(pack(minX + x, y, minZ + z));
                    }

                    int emission = chunk.get(x, y, z).lightEmission();
                    chunk.setBlockLight(x, y, z, emission);
                }
            }
        }
        spread(CHANNEL_SKY);

        for (int y = 0; y < GameConfig.CHUNK_HEIGHT; y++) {
            for (int z = 0; z < GameConfig.CHUNK_SIZE; z++) {
                for (int x = 0; x < GameConfig.CHUNK_SIZE; x++) {
                    if (chunk.blockLight(x, y, z) > 1) {
                        spreadQueue.add(pack(minX + x, y, minZ + z));
                    }
                }
            }
        }
        spread(CHANNEL_BLOCK);

        seedFromNeighborBorders(chunk);
    }

    // World calls this after replacing the block so propagation sees the new material.
    public void onBlockChanged(int x, int y, int z, BlockType oldBlock, BlockType newBlock) {
        boolean opacityChanged = oldBlock.lightOpacity() != newBlock.lightOpacity();
        if (opacityChanged || oldBlock.lightEmission() != newBlock.lightEmission()) {
            removeLight(CHANNEL_BLOCK, x, y, z);
            int emission = newBlock.lightEmission();
            if (emission > 0) {
                setLight(CHANNEL_BLOCK, x, y, z, emission);
                spreadQueue.add(pack(x, y, z));
            }
            enqueueNeighbors(x, y, z);
            spread(CHANNEL_BLOCK);
        }

        // Changes such as water strength or door direction need no sky-light rebuild when opacity
        // is unchanged.
        if (opacityChanged) {
            removeLight(CHANNEL_SKY, x, y, z);
            if (y == GameConfig.CHUNK_HEIGHT - 1) {
                setLight(CHANNEL_SKY, x, y, z, Math.max(0, MAX_LIGHT - newBlock.lightOpacity()));
                spreadQueue.add(pack(x, y, z));
            }
            enqueueNeighbors(x, y, z);
            spread(CHANNEL_SKY);
        }
    }

    // Treat missing chunks as full sky light for rendering, without generating them.
    public int skyLightAt(int x, int y, int z) {
        if (y >= GameConfig.CHUNK_HEIGHT) {
            return MAX_LIGHT;
        }
        if (y < 0) {
            return 0;
        }
        int chunkX = World.chunkCoord(x);
        int chunkZ = World.chunkCoord(z);
        Chunk chunk = world.getChunkIfLoaded(chunkX, chunkZ);
        if (chunk == null) {
            return MAX_LIGHT;
        }
        return chunk.skyLight(World.localCoord(x, chunkX), y, World.localCoord(z, chunkZ));
    }

    // Treat missing chunks as having no block light.
    public int blockLightAt(int x, int y, int z) {
        if (y < 0 || y >= GameConfig.CHUNK_HEIGHT) {
            return 0;
        }
        int chunkX = World.chunkCoord(x);
        int chunkZ = World.chunkCoord(z);
        Chunk chunk = world.getChunkIfLoaded(chunkX, chunkZ);
        if (chunk == null) {
            return 0;
        }
        return chunk.blockLight(World.localCoord(x, chunkX), y, World.localCoord(z, chunkZ));
    }

    // Seed from loaded neighbor borders so existing light can enter the new chunk.
    private void seedFromNeighborBorders(Chunk chunk) {
        int minX = chunk.worldMinX();
        int minZ = chunk.worldMinZ();
        int maxX = minX + GameConfig.CHUNK_SIZE - 1;
        int maxZ = minZ + GameConfig.CHUNK_SIZE - 1;

        // Copy seed coordinates for both channels; spread reads the current level when each
        // coordinate is processed.
        neighborSeedQueue.clear();
        blockSeedQueue.clear();
        for (int y = 0; y < GameConfig.CHUNK_HEIGHT; y++) {
            for (int x = minX; x <= maxX; x++) {
                enqueueIfLoaded(neighborSeedQueue, x, y, minZ - 1);
                enqueueIfLoaded(neighborSeedQueue, x, y, maxZ + 1);
            }
            for (int z = minZ; z <= maxZ; z++) {
                enqueueIfLoaded(neighborSeedQueue, minX - 1, y, z);
                enqueueIfLoaded(neighborSeedQueue, maxX + 1, y, z);
            }
        }

        while (!neighborSeedQueue.isEmpty()) {
            long value = neighborSeedQueue.poll();
            spreadQueue.add(value);
            blockSeedQueue.add(value);
        }
        spread(CHANNEL_SKY);

        while (!blockSeedQueue.isEmpty()) {
            spreadQueue.add(blockSeedQueue.poll());
        }
        spread(CHANNEL_BLOCK);
    }

    private void enqueueIfLoaded(LongQueue out, int x, int y, int z) {
        int chunkX = World.chunkCoord(x);
        int chunkZ = World.chunkCoord(z);
        Chunk chunk = world.getChunkIfLoaded(chunkX, chunkZ);
        if (chunk == null) {
            return;
        }
        int localX = World.localCoord(x, chunkX);
        int localZ = World.localCoord(z, chunkZ);
        if (chunk.skyLight(localX, y, localZ) > 1 || chunk.blockLight(localX, y, localZ) > 1) {
            out.add(pack(x, y, z));
        }
    }

    private void enqueueNeighbors(int x, int y, int z) {
        for (int i = 0; i < DIRECTION_COUNT; i++) {
            spreadQueue.add(pack(x + DIRECTION_X[i], y + DIRECTION_Y[i], z + DIRECTION_Z[i]));
        }
    }

    // Read levels when dequeuing so repeated coordinates use the latest propagated value.
    private void spread(int channel) {
        while (!spreadQueue.isEmpty()) {
            long packed = spreadQueue.poll();
            int x = unpackX(packed);
            int y = unpackY(packed);
            int z = unpackZ(packed);

            // Levels 0 and 1 cannot brighten a neighbor because every applicable outgoing step
            // loses at least one level.
            int level = getLight(channel, x, y, z);
            if (level <= 1) {
                continue;
            }

            for (int i = 0; i < DIRECTION_COUNT; i++) {
                int nx = x + DIRECTION_X[i];
                int ny = y + DIRECTION_Y[i];
                int nz = z + DIRECTION_Z[i];

                if (ny < 0 || ny >= GameConfig.CHUNK_HEIGHT) {
                    continue;
                }

                int chunkX = World.chunkCoord(nx);
                int chunkZ = World.chunkCoord(nz);
                Chunk chunk = world.getChunkIfLoaded(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }

                int localX = World.localCoord(nx, chunkX);
                int localZ = World.localCoord(nz, chunkZ);
                int opacity = chunk.get(localX, ny, localZ).lightOpacity();

                int target = propagatedLevel(channel, level, opacity, i == DIRECTION_DOWN);

                if (target <= 0) {
                    continue;
                }

                int current = channel == CHANNEL_SKY
                        ? chunk.skyLight(localX, ny, localZ)
                        : chunk.blockLight(localX, ny, localZ);
                if (target <= current) {
                    continue;
                }

                writeLight(channel, chunk, localX, ny, localZ, target);
                spreadQueue.add(pack(nx, ny, nz));
            }
        }
    }

    // Use the same attenuation for initial columns and later updates.
    // Only full-strength sky light moving downward can avoid the minimum one-level loss.
    private static int propagatedLevel(int channel, int level, int opacity, boolean downward) {
        if (channel == CHANNEL_SKY && downward && level == MAX_LIGHT) {
            return level - opacity;
        }
        return level - Math.max(1, opacity);
    }

    // Clear light that may depend on the removed cell, then spread surviving boundary sources back
    // through the cleared region.
    private void removeLight(int channel, int x, int y, int z) {
        int old = getLight(channel, x, y, z);
        setLight(channel, x, y, z, 0);
        if (old <= 0) {
            return;
        }

        removalQueue.clear();
        retainedSources.clear();
        removalQueue.add(packWithLevel(x, y, z, old));

        while (!removalQueue.isEmpty()) {
            long packed = removalQueue.poll();
            int cx = unpackX(packed);
            int cy = unpackY(packed);
            int cz = unpackZ(packed);
            int level = unpackLevel(packed);

            for (int i = 0; i < DIRECTION_COUNT; i++) {
                int nx = cx + DIRECTION_X[i];
                int ny = cy + DIRECTION_Y[i];
                int nz = cz + DIRECTION_Z[i];

                int neighborLevel = getLight(channel, nx, ny, nz);
                if (neighborLevel <= 0) {
                    continue;
                }

                // Full-strength sky light travels downward without fading through cells with zero
                // light opacity.
                // Its removal must also clear equal-level sky light directly below.
                boolean dependsOnRemoved = neighborLevel < level
                        || (channel == CHANNEL_SKY && i == DIRECTION_DOWN
                                && level == MAX_LIGHT && neighborLevel == MAX_LIGHT);

                if (dependsOnRemoved) {
                    if (channel == CHANNEL_BLOCK && world.peekBlock(nx, ny, nz).lightEmission() > 0) {
                        retainedSources.add(pack(nx, ny, nz));
                    }
                    setLight(channel, nx, ny, nz, 0);
                    removalQueue.add(packWithLevel(nx, ny, nz, neighborLevel));
                } else {
                    // Queue surviving boundary light for re-propagation after removal.
                    spreadQueue.add(pack(nx, ny, nz));
                }
            }
        }
        // Restore weaker emitters cleared by the removal pass before propagating their own light
        // again.
        while (!retainedSources.isEmpty()) {
            long source = retainedSources.poll();
            int sx = unpackX(source);
            int sy = unpackY(source);
            int sz = unpackZ(source);
            setLight(CHANNEL_BLOCK, sx, sy, sz, world.peekBlock(sx, sy, sz).lightEmission());
            spreadQueue.add(source);
        }
    }

    private int getLight(int channel, int x, int y, int z) {
        return channel == CHANNEL_SKY ? skyLightAtInternal(x, y, z) : blockLightAt(x, y, z);
    }

    // Internal propagation treats unloaded space as dark so missing chunks cannot become sky-light
    // sources.
    private int skyLightAtInternal(int x, int y, int z) {
        if (y < 0 || y >= GameConfig.CHUNK_HEIGHT) {
            return 0;
        }
        int chunkX = World.chunkCoord(x);
        int chunkZ = World.chunkCoord(z);
        Chunk chunk = world.getChunkIfLoaded(chunkX, chunkZ);
        if (chunk == null) {
            return 0;
        }
        return chunk.skyLight(World.localCoord(x, chunkX), y, World.localCoord(z, chunkZ));
    }

    private void setLight(int channel, int x, int y, int z, int value) {
        if (y < 0 || y >= GameConfig.CHUNK_HEIGHT) {
            return;
        }
        int chunkX = World.chunkCoord(x);
        int chunkZ = World.chunkCoord(z);
        Chunk chunk = world.getChunkIfLoaded(chunkX, chunkZ);
        if (chunk == null) {
            return;
        }
        writeLight(channel, chunk, World.localCoord(x, chunkX), y, World.localCoord(z, chunkZ), value);
    }

    // Invalidate affected meshes when light changes, including neighbor meshes that sample the
    // boundary.
    private void writeLight(int channel, Chunk chunk, int localX, int y, int localZ, int value) {
        boolean changed = channel == CHANNEL_SKY
                ? chunk.setSkyLight(localX, y, localZ, value)
                : chunk.setBlockLight(localX, y, localZ, value);
        if (!changed) {
            return;
        }

        chunk.markMeshDirty();

        world.markNeighborMeshesDirty(chunk, localX, localZ);
    }

    // Pack signed 24-bit X/Z, unsigned eight-bit Y, and a four-bit light level.
    // X/Z must fit this range; CHUNK_HEIGHT must stay below 256 so out-of-range Y neighbors remain
    // invalid.
    private static long pack(int x, int y, int z) {
        return packWithLevel(x, y, z, 0);
    }

    private static long packWithLevel(int x, int y, int z, int level) {
        return ((long) (x & 0xFFFFFF) << 36)
                | ((long) (z & 0xFFFFFF) << 12)
                | ((long) (y & 0xFF) << 4)
                | (level & 0xF);
    }

    private static int unpackX(long packed) {
        return (int) (packed << 4 >> 40);
    }

    private static int unpackZ(long packed) {
        return (int) (packed << 28 >> 40);
    }

    private static int unpackY(long packed) {
        return (int) ((packed >> 4) & 0xFF);
    }

    private static int unpackLevel(long packed) {
        return (int) (packed & 0xF);
    }
}

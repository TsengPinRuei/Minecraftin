package com.minecraftin.clone.world;

import com.minecraftin.clone.config.GameConfig;

// Capture immutable block and light data on the game thread so workers never read mutable World or
// Chunk state.
// The one-cell X/Z border supplies face, corner-light, and ambient-occlusion samples.
public final class ChunkSnapshot {
    private static final int WIDTH = GameConfig.CHUNK_SIZE + 2;
    private final short[] blocks = new short[WIDTH * WIDTH * GameConfig.CHUNK_HEIGHT];
    private final byte[] sky = new byte[blocks.length];
    private final byte[] light = new byte[blocks.length];

    private ChunkSnapshot() {
    }

    public static ChunkSnapshot capture(Chunk center, World world) {
        ChunkSnapshot snapshot = new ChunkSnapshot();
        for (int z = -1; z <= GameConfig.CHUNK_SIZE; z++) {
            for (int x = -1; x <= GameConfig.CHUNK_SIZE; x++) {
                int wx = center.worldMinX() + x;
                int wz = center.worldMinZ() + z;
                int cx = World.chunkCoord(wx);
                int cz = World.chunkCoord(wz);
                Chunk chunk = world.getChunkIfLoaded(cx, cz);
                int lx = World.localCoord(wx, cx);
                int lz = World.localCoord(wz, cz);
                for (int y = 0; y < GameConfig.CHUNK_HEIGHT; y++) {
                    int index = index(x, y, z);
                    // Missing neighbors use AIR, zero block light, and full sky light without
                    // generating terrain.
                    // Loading those neighbors later invalidates the affected meshes.
                    if (chunk == null) {
                        snapshot.sky[index] = LightEngine.MAX_LIGHT;
                    } else {
                        snapshot.blocks[index] = (short) chunk.get(lx, y, lz).id();
                        snapshot.sky[index] = (byte) chunk.skyLight(lx, y, lz);
                        snapshot.light[index] = (byte) chunk.blockLight(lx, y, lz);
                    }
                }
            }
        }
        return snapshot;
    }

    // Sampler X/Z coordinates are local to the center chunk and must stay in [-1, CHUNK_SIZE].
    // Only Y is handled outside the world height; X/Z bounds are not checked here.
    public BlockType get(int x, int y, int z) {
        if (y < 0) {
            return BlockType.BEDROCK;
        }
        if (y >= GameConfig.CHUNK_HEIGHT) {
            return BlockType.AIR;
        }
        return BlockType.byId(blocks[index(x, y, z)]);
    }

    public int sky(int x, int y, int z) {
        if (y >= GameConfig.CHUNK_HEIGHT) {
            return LightEngine.MAX_LIGHT;
        }
        return y < 0 ? 0 : sky[index(x, y, z)];
    }

    public int block(int x, int y, int z) {
        return y < 0 || y >= GameConfig.CHUNK_HEIGHT ? 0 : light[index(x, y, z)];
    }

    public boolean occludes(int x, int y, int z) {
        BlockType block = get(x, y, z);
        return block.isFullCube() && block.isOpaque();
    }

    private static int index(int x, int y, int z) {
        return (y * WIDTH + z + 1) * WIDTH + x + 1;
    }
}

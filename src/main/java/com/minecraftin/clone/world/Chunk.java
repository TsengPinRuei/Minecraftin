package com.minecraftin.clone.world;

import com.minecraftin.clone.config.GameConfig;
import java.util.Arrays;

// Store one chunk's blocks, lighting, and dirty flags; World coordinates changes across chunks.
public final class Chunk {

    private final int chunkX;

    private final int chunkZ;

    // Reuse a stable value key when looking up this chunk in renderer maps.
    private final ChunkPos pos;

    // Store blocks with X varying fastest, then Z, then Y; the save format uses this exact order.
    private final short[] blocks;

    // Sky and block light use the block-array index and levels 0-15; they are rebuilt instead of
    // saved.
    private final byte[] skyLight;
    private final byte[] blockLight;

    // Rebuild geometry after block or sampled neighbor-light changes.
    private boolean meshDirty = true;

    // Track unsaved block changes, including water simulation changes; successful saves clear this
    // flag.
    private boolean modified;

    public Chunk(int chunkX, int chunkZ) {
        WorldBounds.requireChunk(chunkX, chunkZ);
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.pos = new ChunkPos(chunkX, chunkZ);
        this.blocks = new short[GameConfig.CHUNK_SIZE * GameConfig.CHUNK_HEIGHT * GameConfig.CHUNK_SIZE];
        Arrays.fill(this.blocks, (short) BlockType.AIR.id());
        this.skyLight = new byte[this.blocks.length];
        this.blockLight = new byte[this.blocks.length];
    }

    public int chunkX() {
        return chunkX;
    }

    public int chunkZ() {
        return chunkZ;
    }

    public ChunkPos pos() {
        return pos;
    }

    public int worldMinX() {
        return chunkX * GameConfig.CHUNK_SIZE;
    }

    public int worldMinZ() {
        return chunkZ * GameConfig.CHUNK_SIZE;
    }

    // Expose live storage for persistence and controlled bulk setup.
    // Direct writes bypass dirty flags, light updates, and water scheduling; runtime edits should
    // use World.setBlock().
    public short[] rawBlocks() {
        return blocks;
    }

    // Return AIR for local coordinates outside this chunk.
    public BlockType get(int localX, int y, int localZ) {
        if (!inBounds(localX, y, localZ)) {
            return BlockType.AIR;
        }
        return BlockType.byId(blocks[index(localX, y, localZ)]);
    }

    // Ignore out-of-bounds or unchanged writes; changed writes mark this chunk dirty and modified.
    public void set(int localX, int y, int localZ, BlockType type) {
        if (!inBounds(localX, y, localZ)) {
            return;
        }

        int index = index(localX, y, localZ);
        short old = blocks[index];
        short now = (short) type.id();

        if (old == now) {
            return;
        }

        blocks[index] = now;

        meshDirty = true;
        modified = true;
    }

    // Write an ID without updating lighting or dirty flags; bulk callers must restore the required
    // state afterward.
    public void setRaw(int localX, int y, int localZ, short blockId) {
        if (!inBounds(localX, y, localZ)) {
            return;
        }
        blocks[index(localX, y, localZ)] = blockId;
    }

    public int skyLight(int localX, int y, int localZ) {
        if (!inBounds(localX, y, localZ)) {
            return 0;
        }
        return skyLight[index(localX, y, localZ)];
    }

    // Return whether sky light changed; LightEngine is responsible for mesh invalidation.
    public boolean setSkyLight(int localX, int y, int localZ, int value) {
        if (!inBounds(localX, y, localZ)) {
            return false;
        }
        int index = index(localX, y, localZ);
        if (skyLight[index] == (byte) value) {
            return false;
        }
        skyLight[index] = (byte) value;
        return true;
    }

    public int blockLight(int localX, int y, int localZ) {
        if (!inBounds(localX, y, localZ)) {
            return 0;
        }
        return blockLight[index(localX, y, localZ)];
    }

    // Return whether block light changed; LightEngine is responsible for mesh invalidation.
    public boolean setBlockLight(int localX, int y, int localZ, int value) {
        if (!inBounds(localX, y, localZ)) {
            return false;
        }
        int index = index(localX, y, localZ);
        if (blockLight[index] == (byte) value) {
            return false;
        }
        blockLight[index] = (byte) value;
        return true;
    }

    public boolean isMeshDirty() {
        return meshDirty;
    }

    public void markMeshDirty() {
        meshDirty = true;
    }

    public void clearMeshDirty() {
        meshDirty = false;
    }

    public boolean isModified() {
        return modified;
    }

    public void clearModified() {
        modified = false;
    }

    private int index(int localX, int y, int localZ) {
        return (y * GameConfig.CHUNK_SIZE + localZ) * GameConfig.CHUNK_SIZE + localX;
    }

    private boolean inBounds(int localX, int y, int localZ) {
        return localX >= 0
                && localX < GameConfig.CHUNK_SIZE
                && y >= 0
                && y < GameConfig.CHUNK_HEIGHT
                && localZ >= 0
                && localZ < GameConfig.CHUNK_SIZE;
    }
}

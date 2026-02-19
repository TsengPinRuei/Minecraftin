package com.minecraftin.clone.world;

import com.minecraftin.clone.config.GameConfig;

import java.util.Arrays;

public final class Chunk {
    private final int chunkX;
    private final int chunkZ;
    private final short[] blocks;

    private boolean meshDirty = true;
    private boolean modified;

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.blocks = new short[GameConfig.CHUNK_SIZE * GameConfig.CHUNK_HEIGHT * GameConfig.CHUNK_SIZE];
        Arrays.fill(this.blocks, (short) BlockType.AIR.id());
    }

    public int chunkX() {
        return chunkX;
    }

    public int chunkZ() {
        return chunkZ;
    }

    public int worldMinX() {
        return chunkX * GameConfig.CHUNK_SIZE;
    }

    public int worldMinZ() {
        return chunkZ * GameConfig.CHUNK_SIZE;
    }

    public short[] rawBlocks() {
        return blocks;
    }

    public BlockType get(int localX, int y, int localZ) {
        if (!inBounds(localX, y, localZ)) {
            return BlockType.AIR;
        }
        return BlockType.byId(blocks[index(localX, y, localZ)]);
    }

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

    public void setRaw(int localX, int y, int localZ, short blockId) {
        if (!inBounds(localX, y, localZ)) {
            return;
        }
        blocks[index(localX, y, localZ)] = blockId;
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

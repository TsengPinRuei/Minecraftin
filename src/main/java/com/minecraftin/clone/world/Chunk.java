package com.minecraftin.clone.world;

import com.minecraftin.clone.config.GameConfig;
import java.util.Arrays;

// Chunk 代表世界中的一小塊區域，負責儲存該區域內所有方塊資料。
// 它只知道本地座標與 dirty 狀態；跨 Chunk 的生成、存檔與 mesh 管理由 World/WorldRenderer 協調。
public final class Chunk {

    // 這個 Chunk 在世界中的 X 座標位置。
    private final int chunkX;

    // 這個 Chunk 在世界中的 Z 座標位置。
    private final int chunkZ;

    // 用一維陣列儲存 Chunk 內所有方塊 id，排序為 y -> z -> x，需與存檔讀寫保持一致。
    private final short[] blocks;

    // 表示這個 Chunk 的模型是否需要重新生成；方塊改變或鄰近 Chunk 邊界改變時都要標記。
    private boolean meshDirty = true;

    // 表示這個 Chunk 是否曾被玩家或水流等遊戲行為修改過，用來判斷是否需要存檔。
    private boolean modified;

    // 建立一個新的 Chunk，並將所有方塊初始化為 AIR。
    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.blocks = new short[GameConfig.CHUNK_SIZE * GameConfig.CHUNK_HEIGHT * GameConfig.CHUNK_SIZE];
        Arrays.fill(this.blocks, (short) BlockType.AIR.id());
    }

    // 回傳 Chunk 的 X 座標。
    public int chunkX() {
        return chunkX;
    }

    // 回傳 Chunk 的 Z 座標。
    public int chunkZ() {
        return chunkZ;
    }

    // 回傳這個 Chunk 在世界中的最小 X 座標。
    public int worldMinX() {
        return chunkX * GameConfig.CHUNK_SIZE;
    }

    // 回傳這個 Chunk 在世界中的最小 Z 座標。
    public int worldMinZ() {
        return chunkZ * GameConfig.CHUNK_SIZE;
    }

    // 直接回傳內部的方塊資料陣列，僅供存檔讀寫使用；呼叫端必須自行維護 dirty 狀態。
    public short[] rawBlocks() {
        return blocks;
    }

    // 取得指定區域座標的方塊種類。
    // 如果座標超出範圍，回傳 AIR。
    public BlockType get(int localX, int y, int localZ) {
        if (!inBounds(localX, y, localZ)) {
            return BlockType.AIR;
        }
        return BlockType.byId(blocks[index(localX, y, localZ)]);
    }

    // 設定指定區域座標的方塊種類。
    // 如果座標超出範圍，或新舊方塊相同，就不做任何事。
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

        // 方塊內容改變後，代表模型需要重建，且資料已被修改。
        meshDirty = true;
        modified = true;
    }

    // 直接用方塊 id 設定資料，不額外處理 modified 或 meshDirty。
    // 通常用在地形生成或載入原始資料時，完成後由呼叫端統一設定 dirty/modified。
    public void setRaw(int localX, int y, int localZ, short blockId) {
        if (!inBounds(localX, y, localZ)) {
            return;
        }
        blocks[index(localX, y, localZ)] = blockId;
    }

    // 回傳目前是否需要重新生成模型。
    public boolean isMeshDirty() {
        return meshDirty;
    }

    // 手動標記這個 Chunk 的模型需要重建。
    public void markMeshDirty() {
        meshDirty = true;
    }

    // 清除模型需重建的標記。
    public void clearMeshDirty() {
        meshDirty = false;
    }

    // 回傳這個 Chunk 是否被修改過。
    public boolean isModified() {
        return modified;
    }

    // 清除已修改標記。
    public void clearModified() {
        modified = false;
    }

    // 將三維座標轉成一維陣列索引。
    private int index(int localX, int y, int localZ) {
        return (y * GameConfig.CHUNK_SIZE + localZ) * GameConfig.CHUNK_SIZE + localX;
    }

    // 檢查座標是否落在 Chunk 的合法範圍內。
    private boolean inBounds(int localX, int y, int localZ) {
        return localX >= 0
                && localX < GameConfig.CHUNK_SIZE
                && y >= 0
                && y < GameConfig.CHUNK_HEIGHT
                && localZ >= 0
                && localZ < GameConfig.CHUNK_SIZE;
    }
}

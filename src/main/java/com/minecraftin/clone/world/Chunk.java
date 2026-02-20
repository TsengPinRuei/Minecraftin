// 說明：宣告此檔案所屬的套件。
package com.minecraftin.clone.world;

// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.config.GameConfig;

// 說明：匯入後續會使用到的型別或函式。
import java.util.Arrays;

// 說明：定義主要型別與其結構。
public final class Chunk {
    // 說明：下一行程式碼負責執行目前步驟。
    private final int chunkX;
    // 說明：下一行程式碼負責執行目前步驟。
    private final int chunkZ;
    // 說明：下一行程式碼負責執行目前步驟。
    private final short[] blocks;

    // 說明：設定或更新變數的值。
    private boolean meshDirty = true;
    // 說明：下一行程式碼負責執行目前步驟。
    private boolean modified;

    // 說明：定義對外可呼叫的方法。
    public Chunk(int chunkX, int chunkZ) {
        // 說明：設定或更新變數的值。
        this.chunkX = chunkX;
        // 說明：設定或更新變數的值。
        this.chunkZ = chunkZ;
        // 說明：設定或更新變數的值。
        this.blocks = new short[GameConfig.CHUNK_SIZE * GameConfig.CHUNK_HEIGHT * GameConfig.CHUNK_SIZE];
        // 說明：呼叫方法執行對應功能。
        Arrays.fill(this.blocks, (short) BlockType.AIR.id());
    }

    // 說明：定義對外可呼叫的方法。
    public int chunkX() {
        // 說明：下一行程式碼負責執行目前步驟。
        return chunkX;
    }

    // 說明：定義對外可呼叫的方法。
    public int chunkZ() {
        // 說明：下一行程式碼負責執行目前步驟。
        return chunkZ;
    }

    // 說明：定義對外可呼叫的方法。
    public int worldMinX() {
        // 說明：下一行程式碼負責執行目前步驟。
        return chunkX * GameConfig.CHUNK_SIZE;
    }

    // 說明：定義對外可呼叫的方法。
    public int worldMinZ() {
        // 說明：下一行程式碼負責執行目前步驟。
        return chunkZ * GameConfig.CHUNK_SIZE;
    }

    // 說明：定義對外可呼叫的方法。
    public short[] rawBlocks() {
        // 說明：下一行程式碼負責執行目前步驟。
        return blocks;
    }

    // 說明：定義對外可呼叫的方法。
    public BlockType get(int localX, int y, int localZ) {
        // 說明：根據條件決定是否進入此邏輯分支。
        if (!inBounds(localX, y, localZ)) {
            // 說明：下一行程式碼負責執行目前步驟。
            return BlockType.AIR;
        }
        // 說明：呼叫方法執行對應功能。
        return BlockType.byId(blocks[index(localX, y, localZ)]);
    }

    // 說明：定義對外可呼叫的方法。
    public void set(int localX, int y, int localZ, BlockType type) {
        // 說明：根據條件決定是否進入此邏輯分支。
        if (!inBounds(localX, y, localZ)) {
            // 說明：下一行程式碼負責執行目前步驟。
            return;
        }
        // 說明：宣告並初始化變數。
        int index = index(localX, y, localZ);
        // 說明：宣告並初始化變數。
        short old = blocks[index];
        // 說明：宣告並初始化變數。
        short now = (short) type.id();
        // 說明：根據條件決定是否進入此邏輯分支。
        if (old == now) {
            // 說明：下一行程式碼負責執行目前步驟。
            return;
        }
        // 說明：設定或更新變數的值。
        blocks[index] = now;
        // 說明：設定或更新變數的值。
        meshDirty = true;
        // 說明：設定或更新變數的值。
        modified = true;
    }

    // 說明：定義對外可呼叫的方法。
    public void setRaw(int localX, int y, int localZ, short blockId) {
        // 說明：根據條件決定是否進入此邏輯分支。
        if (!inBounds(localX, y, localZ)) {
            // 說明：下一行程式碼負責執行目前步驟。
            return;
        }
        // 說明：設定或更新變數的值。
        blocks[index(localX, y, localZ)] = blockId;
    }

    // 說明：定義對外可呼叫的方法。
    public boolean isMeshDirty() {
        // 說明：下一行程式碼負責執行目前步驟。
        return meshDirty;
    }

    // 說明：定義對外可呼叫的方法。
    public void markMeshDirty() {
        // 說明：設定或更新變數的值。
        meshDirty = true;
    }

    // 說明：定義對外可呼叫的方法。
    public void clearMeshDirty() {
        // 說明：設定或更新變數的值。
        meshDirty = false;
    }

    // 說明：定義對外可呼叫的方法。
    public boolean isModified() {
        // 說明：下一行程式碼負責執行目前步驟。
        return modified;
    }

    // 說明：定義對外可呼叫的方法。
    public void clearModified() {
        // 說明：設定或更新變數的值。
        modified = false;
    }

    // 說明：定義類別內部使用的方法。
    private int index(int localX, int y, int localZ) {
        // 說明：呼叫方法執行對應功能。
        return (y * GameConfig.CHUNK_SIZE + localZ) * GameConfig.CHUNK_SIZE + localX;
    }

    // 說明：定義類別內部使用的方法。
    private boolean inBounds(int localX, int y, int localZ) {
        // 說明：下一行程式碼負責執行目前步驟。
        return localX >= 0
                // 說明：下一行程式碼負責執行目前步驟。
                && localX < GameConfig.CHUNK_SIZE
                // 說明：下一行程式碼負責執行目前步驟。
                && y >= 0
                // 說明：下一行程式碼負責執行目前步驟。
                && y < GameConfig.CHUNK_HEIGHT
                // 說明：下一行程式碼負責執行目前步驟。
                && localZ >= 0
                // 說明：下一行程式碼負責執行目前步驟。
                && localZ < GameConfig.CHUNK_SIZE;
    }
}

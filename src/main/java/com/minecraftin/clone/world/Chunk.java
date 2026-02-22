// 宣告此檔案所屬的套件。
package com.minecraftin.clone.world;

// 匯入後續會使用到的型別或函式。
import com.minecraftin.clone.config.GameConfig;

// 匯入後續會使用到的型別或函式。
import java.util.Arrays;

// 定義主要型別與其結構。
public final class Chunk {
    // 下一行程式碼負責執行目前步驟。
    private final int chunkX;
    // 下一行程式碼負責執行目前步驟。
    private final int chunkZ;
    // 下一行程式碼負責執行目前步驟。
    private final short[] blocks;

    // 設定或更新變數的值。
    private boolean meshDirty = true;
    // 下一行程式碼負責執行目前步驟。
    private boolean modified;

    // 定義對外可呼叫的方法。
    public Chunk(int chunkX, int chunkZ) {
        // 設定或更新變數的值。
        this.chunkX = chunkX;
        // 設定或更新變數的值。
        this.chunkZ = chunkZ;
        // 設定或更新變數的值。
        this.blocks = new short[GameConfig.CHUNK_SIZE * GameConfig.CHUNK_HEIGHT * GameConfig.CHUNK_SIZE];
        // 呼叫方法執行對應功能。
        Arrays.fill(this.blocks, (short) BlockType.AIR.id());
    }

    // 定義對外可呼叫的方法。
    public int chunkX() {
        // 下一行程式碼負責執行目前步驟。
        return chunkX;
    }

    // 定義對外可呼叫的方法。
    public int chunkZ() {
        // 下一行程式碼負責執行目前步驟。
        return chunkZ;
    }

    // 定義對外可呼叫的方法。
    public int worldMinX() {
        // 下一行程式碼負責執行目前步驟。
        return chunkX * GameConfig.CHUNK_SIZE;
    }

    // 定義對外可呼叫的方法。
    public int worldMinZ() {
        // 下一行程式碼負責執行目前步驟。
        return chunkZ * GameConfig.CHUNK_SIZE;
    }

    // 定義對外可呼叫的方法。
    public short[] rawBlocks() {
        // 下一行程式碼負責執行目前步驟。
        return blocks;
    }

    // 定義對外可呼叫的方法。
    public BlockType get(int localX, int y, int localZ) {
        // 根據條件決定是否進入此邏輯分支。
        if (!inBounds(localX, y, localZ)) {
            // 下一行程式碼負責執行目前步驟。
            return BlockType.AIR;
        }
        // 呼叫方法執行對應功能。
        return BlockType.byId(blocks[index(localX, y, localZ)]);
    }

    // 定義對外可呼叫的方法。
    public void set(int localX, int y, int localZ, BlockType type) {
        // 根據條件決定是否進入此邏輯分支。
        if (!inBounds(localX, y, localZ)) {
            // 下一行程式碼負責執行目前步驟。
            return;
        }
        // 宣告並初始化變數。
        int index = index(localX, y, localZ);
        // 宣告並初始化變數。
        short old = blocks[index];
        // 宣告並初始化變數。
        short now = (short) type.id();
        // 根據條件決定是否進入此邏輯分支。
        if (old == now) {
            // 下一行程式碼負責執行目前步驟。
            return;
        }
        // 設定或更新變數的值。
        blocks[index] = now;
        // 設定或更新變數的值。
        meshDirty = true;
        // 設定或更新變數的值。
        modified = true;
    }

    // 定義對外可呼叫的方法。
    public void setRaw(int localX, int y, int localZ, short blockId) {
        // 根據條件決定是否進入此邏輯分支。
        if (!inBounds(localX, y, localZ)) {
            // 下一行程式碼負責執行目前步驟。
            return;
        }
        // 設定或更新變數的值。
        blocks[index(localX, y, localZ)] = blockId;
    }

    // 定義對外可呼叫的方法。
    public boolean isMeshDirty() {
        // 下一行程式碼負責執行目前步驟。
        return meshDirty;
    }

    // 定義對外可呼叫的方法。
    public void markMeshDirty() {
        // 設定或更新變數的值。
        meshDirty = true;
    }

    // 定義對外可呼叫的方法。
    public void clearMeshDirty() {
        // 設定或更新變數的值。
        meshDirty = false;
    }

    // 定義對外可呼叫的方法。
    public boolean isModified() {
        // 下一行程式碼負責執行目前步驟。
        return modified;
    }

    // 定義對外可呼叫的方法。
    public void clearModified() {
        // 設定或更新變數的值。
        modified = false;
    }

    // 定義類別內部使用的方法。
    private int index(int localX, int y, int localZ) {
        // 呼叫方法執行對應功能。
        return (y * GameConfig.CHUNK_SIZE + localZ) * GameConfig.CHUNK_SIZE + localX;
    }

    // 定義類別內部使用的方法。
    private boolean inBounds(int localX, int y, int localZ) {
        // 下一行程式碼負責執行目前步驟。
        return localX >= 0
                // 下一行程式碼負責執行目前步驟。
                && localX < GameConfig.CHUNK_SIZE
                // 下一行程式碼負責執行目前步驟。
                && y >= 0
                // 下一行程式碼負責執行目前步驟。
                && y < GameConfig.CHUNK_HEIGHT
                // 下一行程式碼負責執行目前步驟。
                && localZ >= 0
                // 下一行程式碼負責執行目前步驟。
                && localZ < GameConfig.CHUNK_SIZE;
    }
}

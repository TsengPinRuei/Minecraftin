// 說明：宣告此檔案所屬的套件。
package com.minecraftin.clone.world;

// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.config.GameConfig;
// 說明：匯入後續會使用到的型別或函式。
import org.joml.Vector3f;

// 說明：匯入後續會使用到的型別或函式。
import java.io.BufferedInputStream;
// 說明：匯入後續會使用到的型別或函式。
import java.io.BufferedOutputStream;
// 說明：匯入後續會使用到的型別或函式。
import java.io.DataInputStream;
// 說明：匯入後續會使用到的型別或函式。
import java.io.DataOutputStream;
// 說明：匯入後續會使用到的型別或函式。
import java.io.IOException;
// 說明：匯入後續會使用到的型別或函式。
import java.nio.file.Files;
// 說明：匯入後續會使用到的型別或函式。
import java.nio.file.Path;
// 說明：匯入後續會使用到的型別或函式。
import java.util.ArrayDeque;
// 說明：匯入後續會使用到的型別或函式。
import java.util.HashMap;
// 說明：匯入後續會使用到的型別或函式。
import java.util.Map;

// 說明：定義主要型別與其結構。
public final class World {
    // 說明：下一行程式碼負責執行目前步驟。
    private static final int SAVE_MAGIC = 0x4D434C4E; // MCLN
    // 說明：設定或更新變數的值。
    private static final int SAVE_VERSION = 1;
    // 說明：設定或更新變數的值。
    private static final float SPAWN_Y_OFFSET = 1.05f;
    // 說明：限制單次補水量，避免玩家挖開超大空腔時造成卡頓。
    private static final int WATER_FLOOD_MAX_BLOCKS = 32768;

    // 說明：設定或更新變數的值。
    private final Map<ChunkPos, Chunk> chunks = new HashMap<>();
    // 說明：下一行程式碼負責執行目前步驟。
    private final Path worldFile;

    // 說明：下一行程式碼負責執行目前步驟。
    private long seed;
    // 說明：下一行程式碼負責執行目前步驟。
    private TerrainGenerator terrainGenerator;

    // 說明：定義對外可呼叫的方法。
    public World(Path worldFile, long defaultSeed) {
        // 說明：設定或更新變數的值。
        this.worldFile = worldFile;
        // 說明：設定或更新變數的值。
        this.seed = defaultSeed;
        // 說明：設定或更新變數的值。
        this.terrainGenerator = new TerrainGenerator(seed, 62);
    }

    // 說明：定義對外可呼叫的方法。
    public void initialize() {
        // 說明：下一行程式碼負責執行目前步驟。
        try {
            // 說明：宣告並初始化變數。
            Path parent = worldFile.getParent();
            // 說明：根據條件決定是否進入此邏輯分支。
            if (parent != null) {
                // 說明：呼叫方法執行對應功能。
                Files.createDirectories(parent);
            }
        // 說明：下一行程式碼負責執行目前步驟。
        } catch (IOException e) {
            // 說明：呼叫方法執行對應功能。
            throw new IllegalStateException("Failed to create save directory", e);
        }

        // 說明：根據條件決定是否進入此邏輯分支。
        if (!load()) {
            // 說明：呼叫方法執行對應功能。
            chunks.clear();
            // 說明：設定或更新變數的值。
            terrainGenerator = new TerrainGenerator(seed, 62);
        }
    }

    // 說明：定義對外可呼叫的方法。
    public long seed() {
        // 說明：下一行程式碼負責執行目前步驟。
        return seed;
    }

    // 說明：定義對外可呼叫的方法。
    public int seaLevel() {
        // 說明：呼叫方法執行對應功能。
        return terrainGenerator.seaLevel();
    }

    // 說明：定義對外可呼叫的方法。
    public int chunkCount() {
        // 說明：呼叫方法執行對應功能。
        return chunks.size();
    }

    // 說明：定義對外可呼叫的方法。
    public boolean hasModifiedChunks() {
        // 說明：使用迴圈逐一處理每個元素或區間。
        for (Chunk chunk : chunks.values()) {
            // 說明：根據條件決定是否進入此邏輯分支。
            if (chunk.isModified()) {
                // 說明：下一行程式碼負責執行目前步驟。
                return true;
            }
        }
        // 說明：下一行程式碼負責執行目前步驟。
        return false;
    }

    // 說明：定義對外可呼叫的方法。
    public void ensureChunksAround(int centerChunkX, int centerChunkZ, int radius) {
        // 說明：宣告並初始化變數。
        int radiusSq = radius * radius;
        // 說明：使用迴圈逐一處理每個元素或區間。
        for (int dz = -radius; dz <= radius; dz++) {
            // 說明：使用迴圈逐一處理每個元素或區間。
            for (int dx = -radius; dx <= radius; dx++) {
                // 說明：根據條件決定是否進入此邏輯分支。
                if (dx * dx + dz * dz > radiusSq) {
                    // 說明：跳過本次迴圈剩餘邏輯，直接進入下一次迭代。
                    continue;
                }
                // 說明：呼叫方法執行對應功能。
                getOrCreateChunk(centerChunkX + dx, centerChunkZ + dz);
            }
        }
    }

    // 說明：定義對外可呼叫的方法。
    public Chunk getChunkIfLoaded(int chunkX, int chunkZ) {
        // 說明：呼叫方法執行對應功能。
        return chunks.get(new ChunkPos(chunkX, chunkZ));
    }

    // 說明：定義對外可呼叫的方法。
    public Chunk getOrCreateChunk(int chunkX, int chunkZ) {
        // 說明：宣告並初始化變數。
        ChunkPos key = new ChunkPos(chunkX, chunkZ);
        // 說明：宣告並初始化變數。
        Chunk existing = chunks.get(key);
        // 說明：根據條件決定是否進入此邏輯分支。
        if (existing != null) {
            // 說明：下一行程式碼負責執行目前步驟。
            return existing;
        }

        // 說明：宣告並初始化變數。
        Chunk chunk = new Chunk(chunkX, chunkZ);
        // 說明：呼叫方法執行對應功能。
        terrainGenerator.generate(chunk);
        // 說明：呼叫方法執行對應功能。
        chunks.put(key, chunk);
        // 說明：呼叫方法執行對應功能。
        markChunkMeshDirty(chunkX - 1, chunkZ);
        // 說明：呼叫方法執行對應功能。
        markChunkMeshDirty(chunkX + 1, chunkZ);
        // 說明：呼叫方法執行對應功能。
        markChunkMeshDirty(chunkX, chunkZ - 1);
        // 說明：呼叫方法執行對應功能。
        markChunkMeshDirty(chunkX, chunkZ + 1);
        // 說明：下一行程式碼負責執行目前步驟。
        return chunk;
    }

    // 說明：定義對外可呼叫的方法。
    public BlockType getBlock(int worldX, int y, int worldZ) {
        // 說明：根據條件決定是否進入此邏輯分支。
        if (y < 0) {
            // 說明：下一行程式碼負責執行目前步驟。
            return BlockType.BEDROCK;
        }
        // 說明：根據條件決定是否進入此邏輯分支。
        if (y >= GameConfig.CHUNK_HEIGHT) {
            // 說明：下一行程式碼負責執行目前步驟。
            return BlockType.AIR;
        }

        // 說明：宣告並初始化變數。
        int chunkX = Math.floorDiv(worldX, GameConfig.CHUNK_SIZE);
        // 說明：宣告並初始化變數。
        int chunkZ = Math.floorDiv(worldZ, GameConfig.CHUNK_SIZE);

        // 說明：宣告並初始化變數。
        int localX = Math.floorMod(worldX, GameConfig.CHUNK_SIZE);
        // 說明：宣告並初始化變數。
        int localZ = Math.floorMod(worldZ, GameConfig.CHUNK_SIZE);

        // 說明：宣告並初始化變數。
        Chunk chunk = getOrCreateChunk(chunkX, chunkZ);
        // 說明：呼叫方法執行對應功能。
        return chunk.get(localX, y, localZ);
    }

    // 說明：定義對外可呼叫的方法。
    public BlockType peekBlock(int worldX, int y, int worldZ) {
        // 說明：根據條件決定是否進入此邏輯分支。
        if (y < 0) {
            // 說明：下一行程式碼負責執行目前步驟。
            return BlockType.BEDROCK;
        }
        // 說明：根據條件決定是否進入此邏輯分支。
        if (y >= GameConfig.CHUNK_HEIGHT) {
            // 說明：下一行程式碼負責執行目前步驟。
            return BlockType.AIR;
        }

        // 說明：宣告並初始化變數。
        int chunkX = Math.floorDiv(worldX, GameConfig.CHUNK_SIZE);
        // 說明：宣告並初始化變數。
        int chunkZ = Math.floorDiv(worldZ, GameConfig.CHUNK_SIZE);

        // 說明：宣告並初始化變數。
        int localX = Math.floorMod(worldX, GameConfig.CHUNK_SIZE);
        // 說明：宣告並初始化變數。
        int localZ = Math.floorMod(worldZ, GameConfig.CHUNK_SIZE);

        // 說明：宣告並初始化變數。
        Chunk chunk = getChunkIfLoaded(chunkX, chunkZ);
        // 說明：根據條件決定是否進入此邏輯分支。
        if (chunk == null) {
            // 說明：下一行程式碼負責執行目前步驟。
            return BlockType.AIR;
        }
        // 說明：呼叫方法執行對應功能。
        return chunk.get(localX, y, localZ);
    }

    // 說明：定義對外可呼叫的方法。
    public boolean setBlock(int worldX, int y, int worldZ, BlockType type) {
        // 說明：先套用方塊變更，玩家建造/破壞都走同一條更新路徑。
        boolean changed = setBlockInternal(worldX, y, worldZ, type, true);
        // 說明：根據條件決定是否進入此邏輯分支。
        if (!changed) {
            // 說明：下一行程式碼負責執行目前步驟。
            return false;
        }

        // 說明：破壞方塊後若形成水下空腔，從相鄰水體開始局部補水。
        if (type == BlockType.AIR) {
            // 說明：呼叫方法執行對應功能。
            floodWaterIntoAirPocket(worldX, y, worldZ);
        }

        // 說明：下一行程式碼負責執行目前步驟。
        return true;
    }

    // 說明：定義類別內部使用的方法。
    private boolean setBlockInternal(int worldX, int y, int worldZ, BlockType type, boolean createMissingChunk) {
        // 說明：根據條件決定是否進入此邏輯分支。
        if (y < 0 || y >= GameConfig.CHUNK_HEIGHT) {
            // 說明：下一行程式碼負責執行目前步驟。
            return false;
        }

        // 說明：宣告並初始化變數。
        int chunkX = Math.floorDiv(worldX, GameConfig.CHUNK_SIZE);
        // 說明：宣告並初始化變數。
        int chunkZ = Math.floorDiv(worldZ, GameConfig.CHUNK_SIZE);
        // 說明：宣告並初始化變數。
        int localX = Math.floorMod(worldX, GameConfig.CHUNK_SIZE);
        // 說明：宣告並初始化變數。
        int localZ = Math.floorMod(worldZ, GameConfig.CHUNK_SIZE);

        // 說明：補水流程不應為了遠方邊界而新生成區塊，因此可選擇只操作已載入區塊。
        Chunk chunk = createMissingChunk ? getOrCreateChunk(chunkX, chunkZ) : getChunkIfLoaded(chunkX, chunkZ);
        // 說明：根據條件決定是否進入此邏輯分支。
        if (chunk == null) {
            // 說明：下一行程式碼負責執行目前步驟。
            return false;
        }

        // 說明：宣告並初始化變數。
        BlockType existing = chunk.get(localX, y, localZ);
        // 說明：根據條件決定是否進入此邏輯分支。
        if (existing == type) {
            // 說明：下一行程式碼負責執行目前步驟。
            return false;
        }

        // 說明：呼叫方法執行對應功能。
        chunk.set(localX, y, localZ, type);

        // 說明：若修改在 chunk 邊界，鄰居 mesh 也要重建避免接縫顯示錯誤。
        if (localX == 0) {
            // 說明：呼叫方法執行對應功能。
            markChunkMeshDirty(chunkX - 1, chunkZ);
        // 說明：下一行程式碼負責執行目前步驟。
        } else if (localX == GameConfig.CHUNK_SIZE - 1) {
            // 說明：呼叫方法執行對應功能。
            markChunkMeshDirty(chunkX + 1, chunkZ);
        }

        // 說明：若修改在 chunk 邊界，鄰居 mesh 也要重建避免接縫顯示錯誤。
        if (localZ == 0) {
            // 說明：呼叫方法執行對應功能。
            markChunkMeshDirty(chunkX, chunkZ - 1);
        // 說明：下一行程式碼負責執行目前步驟。
        } else if (localZ == GameConfig.CHUNK_SIZE - 1) {
            // 說明：呼叫方法執行對應功能。
            markChunkMeshDirty(chunkX, chunkZ + 1);
        }

        // 說明：下一行程式碼負責執行目前步驟。
        return true;
    }

    // 說明：定義類別內部使用的方法。
    private void floodWaterIntoAirPocket(int worldX, int y, int worldZ) {
        // 說明：只處理海平面以下的空腔，避免讓海水不合理地往上爬。
        if (y < 0 || y >= GameConfig.CHUNK_HEIGHT || y > seaLevel()) {
            // 說明：下一行程式碼負責執行目前步驟。
            return;
        }

        // 說明：若目標已不是空氣，代表後續流程或其他更新已處理過。
        if (peekBlock(worldX, y, worldZ) != BlockType.AIR) {
            // 說明：下一行程式碼負責執行目前步驟。
            return;
        }

        // 說明：只有接觸到已載入的水方塊時才開始補水，避免無條件灌水。
        if (!hasAdjacentLoadedWater(worldX, y, worldZ)) {
            // 說明：下一行程式碼負責執行目前步驟。
            return;
        }

        // 說明：使用 BFS 局部填充空腔，效果接近「水往附近空格流入」。
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        // 說明：先填入起點，後續以它為源頭擴散。
        if (!setBlockInternal(worldX, y, worldZ, BlockType.WATER, false)) {
            // 說明：下一行程式碼負責執行目前步驟。
            return;
        }
        // 說明：呼叫方法執行對應功能。
        queue.addLast(new int[]{worldX, y, worldZ});

        // 說明：宣告並初始化變數。
        int filled = 1;
        // 說明：在條件成立時重複執行此區塊。
        while (!queue.isEmpty() && filled < WATER_FLOOD_MAX_BLOCKS) {
            // 說明：宣告並初始化變數。
            int[] cell = queue.removeFirst();
            // 說明：宣告並初始化變數。
            int cx = cell[0];
            // 說明：宣告並初始化變數。
            int cy = cell[1];
            // 說明：宣告並初始化變數。
            int cz = cell[2];

            // 說明：依序檢查六個方向，把相連空氣補成水。
            filled = tryFloodNeighbor(queue, filled, cx + 1, cy, cz);
            // 說明：根據條件決定是否進入此邏輯分支。
            if (filled >= WATER_FLOOD_MAX_BLOCKS) {
                // 說明：跳出迴圈以結束目前流程。
                break;
            }
            // 說明：呼叫方法執行對應功能。
            filled = tryFloodNeighbor(queue, filled, cx - 1, cy, cz);
            // 說明：根據條件決定是否進入此邏輯分支。
            if (filled >= WATER_FLOOD_MAX_BLOCKS) {
                // 說明：跳出迴圈以結束目前流程。
                break;
            }
            // 說明：呼叫方法執行對應功能。
            filled = tryFloodNeighbor(queue, filled, cx, cy, cz + 1);
            // 說明：根據條件決定是否進入此邏輯分支。
            if (filled >= WATER_FLOOD_MAX_BLOCKS) {
                // 說明：跳出迴圈以結束目前流程。
                break;
            }
            // 說明：呼叫方法執行對應功能。
            filled = tryFloodNeighbor(queue, filled, cx, cy, cz - 1);
            // 說明：根據條件決定是否進入此邏輯分支。
            if (filled >= WATER_FLOOD_MAX_BLOCKS) {
                // 說明：跳出迴圈以結束目前流程。
                break;
            }
            // 說明：呼叫方法執行對應功能。
            filled = tryFloodNeighbor(queue, filled, cx, cy - 1, cz);
            // 說明：根據條件決定是否進入此邏輯分支。
            if (filled >= WATER_FLOOD_MAX_BLOCKS) {
                // 說明：跳出迴圈以結束目前流程。
                break;
            }
            // 說明：呼叫方法執行對應功能。
            filled = tryFloodNeighbor(queue, filled, cx, cy + 1, cz);
        }
    }

    // 說明：定義類別內部使用的方法。
    private int tryFloodNeighbor(ArrayDeque<int[]> queue, int filled, int x, int y, int z) {
        // 說明：水體補充不超過世界高度，也不超過海平面高度。
        if (y < 0 || y >= GameConfig.CHUNK_HEIGHT || y > seaLevel()) {
            // 說明：下一行程式碼負責執行目前步驟。
            return filled;
        }

        // 說明：只有空氣格才會被補成水，其它方塊維持不變。
        if (peekBlock(x, y, z) != BlockType.AIR) {
            // 說明：下一行程式碼負責執行目前步驟。
            return filled;
        }

        // 說明：只修改已載入區塊，避免補水流程導致遠方 chunk 被動生成。
        if (!setBlockInternal(x, y, z, BlockType.WATER, false)) {
            // 說明：下一行程式碼負責執行目前步驟。
            return filled;
        }

        // 說明：呼叫方法執行對應功能。
        queue.addLast(new int[]{x, y, z});
        // 說明：下一行程式碼負責執行目前步驟。
        return filled + 1;
    }

    // 說明：定義類別內部使用的方法。
    private boolean hasAdjacentLoadedWater(int worldX, int y, int worldZ) {
        // 說明：以面接觸作為流動判定，避免斜角接觸時不合理補水。
        return peekBlock(worldX + 1, y, worldZ) == BlockType.WATER
                || peekBlock(worldX - 1, y, worldZ) == BlockType.WATER
                || peekBlock(worldX, y, worldZ + 1) == BlockType.WATER
                || peekBlock(worldX, y, worldZ - 1) == BlockType.WATER
                || peekBlock(worldX, y + 1, worldZ) == BlockType.WATER
                || peekBlock(worldX, y - 1, worldZ) == BlockType.WATER;
    }

    // 說明：定義類別內部使用的方法。
    private void markChunkMeshDirty(int chunkX, int chunkZ) {
        // 說明：宣告並初始化變數。
        Chunk chunk = getChunkIfLoaded(chunkX, chunkZ);
        // 說明：根據條件決定是否進入此邏輯分支。
        if (chunk != null) {
            // 說明：呼叫方法執行對應功能。
            chunk.markMeshDirty();
        }
    }

    // 說明：定義對外可呼叫的方法。
    public int topSolidY(int worldX, int worldZ) {
        // 說明：使用迴圈逐一處理每個元素或區間。
        for (int y = GameConfig.CHUNK_HEIGHT - 1; y >= 1; y--) {
            // 說明：宣告並初始化變數。
            BlockType block = getBlock(worldX, y, worldZ);
            // 說明：根據條件決定是否進入此邏輯分支。
            if (block.isSolid() && block != BlockType.LEAVES && block != BlockType.WATER) {
                // 說明：下一行程式碼負責執行目前步驟。
                return y;
            }
        }
        // 說明：下一行程式碼負責執行目前步驟。
        return 1;
    }

    // 說明：定義對外可呼叫的方法。
    public Vector3f defaultSpawn(Vector3f out) {
        // 說明：宣告並初始化變數。
        int maxRadius = 256;
        // 說明：宣告並初始化變數。
        int step = 8;

        // 說明：使用迴圈逐一處理每個元素或區間。
        for (int radius = 0; radius <= maxRadius; radius += step) {
            // 說明：根據條件決定是否進入此邏輯分支。
            if (trySpawnAt(out, -radius, -radius)) {
                // 說明：下一行程式碼負責執行目前步驟。
                return out;
            }
            // 說明：根據條件決定是否進入此邏輯分支。
            if (trySpawnAt(out, radius, radius)) {
                // 說明：下一行程式碼負責執行目前步驟。
                return out;
            }

            // 說明：使用迴圈逐一處理每個元素或區間。
            for (int x = -radius; x <= radius; x += step) {
                // 說明：根據條件決定是否進入此邏輯分支。
                if (trySpawnAt(out, x, -radius) || trySpawnAt(out, x, radius)) {
                    // 說明：下一行程式碼負責執行目前步驟。
                    return out;
                }
            }
            // 說明：使用迴圈逐一處理每個元素或區間。
            for (int z = -radius + step; z <= radius - step; z += step) {
                // 說明：根據條件決定是否進入此邏輯分支。
                if (trySpawnAt(out, -radius, z) || trySpawnAt(out, radius, z)) {
                    // 說明：下一行程式碼負責執行目前步驟。
                    return out;
                }
            }
        }

        // 說明：宣告並初始化變數。
        int fallbackX = 0;
        // 說明：宣告並初始化變數。
        int fallbackZ = 0;
        // 說明：宣告並初始化變數。
        int fallbackY = topSolidY(fallbackX, fallbackZ);
        // 說明：呼叫方法執行對應功能。
        out.set(fallbackX + 0.5f, fallbackY + SPAWN_Y_OFFSET, fallbackZ + 0.5f);
        // 說明：下一行程式碼負責執行目前步驟。
        return out;
    }

    // 說明：定義類別內部使用的方法。
    private boolean trySpawnAt(Vector3f out, int x, int z) {
        // 說明：宣告並初始化變數。
        int y = topSolidY(x, z);
        // 說明：根據條件決定是否進入此邏輯分支。
        if (y <= seaLevel()) {
            // 說明：下一行程式碼負責執行目前步驟。
            return false;
        }

        // 說明：宣告並初始化變數。
        BlockType floor = getBlock(x, y, z);
        // 說明：根據條件決定是否進入此邏輯分支。
        if (!floor.isSolid() || floor == BlockType.LEAVES || floor == BlockType.WATER) {
            // 說明：下一行程式碼負責執行目前步驟。
            return false;
        }

        // 說明：宣告並初始化變數。
        BlockType feet = getBlock(x, y + 1, z);
        // 說明：宣告並初始化變數。
        BlockType head = getBlock(x, y + 2, z);
        // 說明：根據條件決定是否進入此邏輯分支。
        if (feet != BlockType.AIR || head != BlockType.AIR) {
            // 說明：下一行程式碼負責執行目前步驟。
            return false;
        }

        // 說明：呼叫方法執行對應功能。
        out.set(x + 0.5f, y + SPAWN_Y_OFFSET, z + 0.5f);
        // 說明：下一行程式碼負責執行目前步驟。
        return true;
    }

    // 說明：定義對外可呼叫的方法。
    public RaycastHit raycast(Vector3f origin, Vector3f direction, float maxDistance) {
        // 說明：宣告並初始化變數。
        float dx = direction.x;
        // 說明：宣告並初始化變數。
        float dy = direction.y;
        // 說明：宣告並初始化變數。
        float dz = direction.z;

        // 說明：宣告並初始化變數。
        int x = fastFloor(origin.x);
        // 說明：宣告並初始化變數。
        int y = fastFloor(origin.y);
        // 說明：宣告並初始化變數。
        int z = fastFloor(origin.z);

        // 說明：宣告並初始化變數。
        int stepX = dx > 0 ? 1 : (dx < 0 ? -1 : 0);
        // 說明：宣告並初始化變數。
        int stepY = dy > 0 ? 1 : (dy < 0 ? -1 : 0);
        // 說明：宣告並初始化變數。
        int stepZ = dz > 0 ? 1 : (dz < 0 ? -1 : 0);

        // 說明：宣告並初始化變數。
        float invDx = dx == 0.0f ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dx);
        // 說明：宣告並初始化變數。
        float invDy = dy == 0.0f ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dy);
        // 說明：宣告並初始化變數。
        float invDz = dz == 0.0f ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dz);

        // 說明：宣告並初始化變數。
        float tx = dx == 0.0f ? Float.POSITIVE_INFINITY : (dx > 0 ? (x + 1 - origin.x) * invDx : (origin.x - x) * invDx);
        // 說明：宣告並初始化變數。
        float ty = dy == 0.0f ? Float.POSITIVE_INFINITY : (dy > 0 ? (y + 1 - origin.y) * invDy : (origin.y - y) * invDy);
        // 說明：宣告並初始化變數。
        float tz = dz == 0.0f ? Float.POSITIVE_INFINITY : (dz > 0 ? (z + 1 - origin.z) * invDz : (origin.z - z) * invDz);

        // 說明：宣告並初始化變數。
        float traveled = 0.0f;
        // 說明：宣告並初始化變數。
        int normalX = 0;
        // 說明：宣告並初始化變數。
        int normalY = 0;
        // 說明：宣告並初始化變數。
        int normalZ = 0;

        // 說明：在條件成立時重複執行此區塊。
        while (traveled <= maxDistance) {
            // 說明：宣告並初始化變數。
            BlockType block = getBlock(x, y, z);
            // 說明：根據條件決定是否進入此邏輯分支。
            if (block != BlockType.AIR && block != BlockType.WATER) {
                // 說明：呼叫方法執行對應功能。
                return new RaycastHit(x, y, z, normalX, normalY, normalZ, traveled, block);
            }

            // 說明：根據條件決定是否進入此邏輯分支。
            if (tx < ty) {
                // 說明：根據條件決定是否進入此邏輯分支。
                if (tx < tz) {
                    // 說明：設定或更新變數的值。
                    x += stepX;
                    // 說明：設定或更新變數的值。
                    traveled = tx;
                    // 說明：設定或更新變數的值。
                    tx += invDx;
                    // 說明：設定或更新變數的值。
                    normalX = -stepX;
                    // 說明：設定或更新變數的值。
                    normalY = 0;
                    // 說明：設定或更新變數的值。
                    normalZ = 0;
                // 說明：下一行程式碼負責執行目前步驟。
                } else {
                    // 說明：設定或更新變數的值。
                    z += stepZ;
                    // 說明：設定或更新變數的值。
                    traveled = tz;
                    // 說明：設定或更新變數的值。
                    tz += invDz;
                    // 說明：設定或更新變數的值。
                    normalX = 0;
                    // 說明：設定或更新變數的值。
                    normalY = 0;
                    // 說明：設定或更新變數的值。
                    normalZ = -stepZ;
                }
            // 說明：下一行程式碼負責執行目前步驟。
            } else {
                // 說明：根據條件決定是否進入此邏輯分支。
                if (ty < tz) {
                    // 說明：設定或更新變數的值。
                    y += stepY;
                    // 說明：設定或更新變數的值。
                    traveled = ty;
                    // 說明：設定或更新變數的值。
                    ty += invDy;
                    // 說明：設定或更新變數的值。
                    normalX = 0;
                    // 說明：設定或更新變數的值。
                    normalY = -stepY;
                    // 說明：設定或更新變數的值。
                    normalZ = 0;
                // 說明：下一行程式碼負責執行目前步驟。
                } else {
                    // 說明：設定或更新變數的值。
                    z += stepZ;
                    // 說明：設定或更新變數的值。
                    traveled = tz;
                    // 說明：設定或更新變數的值。
                    tz += invDz;
                    // 說明：設定或更新變數的值。
                    normalX = 0;
                    // 說明：設定或更新變數的值。
                    normalY = 0;
                    // 說明：設定或更新變數的值。
                    normalZ = -stepZ;
                }
            }
        }

        // 說明：下一行程式碼負責執行目前步驟。
        return null;
    }

    // 說明：定義對外可呼叫的方法。
    public void save() {
        // 說明：下一行程式碼負責執行目前步驟。
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(worldFile)))) {
            // 說明：呼叫方法執行對應功能。
            out.writeInt(SAVE_MAGIC);
            // 說明：呼叫方法執行對應功能。
            out.writeInt(SAVE_VERSION);
            // 說明：呼叫方法執行對應功能。
            out.writeLong(seed);
            // 說明：呼叫方法執行對應功能。
            out.writeInt(chunks.size());

            // 說明：使用迴圈逐一處理每個元素或區間。
            for (Chunk chunk : chunks.values()) {
                // 說明：呼叫方法執行對應功能。
                out.writeInt(chunk.chunkX());
                // 說明：呼叫方法執行對應功能。
                out.writeInt(chunk.chunkZ());

                // 說明：宣告並初始化變數。
                short[] data = chunk.rawBlocks();
                // 說明：呼叫方法執行對應功能。
                out.writeInt(data.length);
                // 說明：使用迴圈逐一處理每個元素或區間。
                for (short value : data) {
                    // 說明：呼叫方法執行對應功能。
                    out.writeShort(value);
                }

                // 說明：呼叫方法執行對應功能。
                chunk.clearModified();
            }
        // 說明：下一行程式碼負責執行目前步驟。
        } catch (IOException e) {
            // 說明：呼叫方法執行對應功能。
            throw new IllegalStateException("Failed to save world to " + worldFile, e);
        }
    }

    // 說明：定義類別內部使用的方法。
    private boolean load() {
        // 說明：根據條件決定是否進入此邏輯分支。
        if (!Files.exists(worldFile)) {
            // 說明：下一行程式碼負責執行目前步驟。
            return false;
        }

        // 說明：下一行程式碼負責執行目前步驟。
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(worldFile)))) {
            // 說明：宣告並初始化變數。
            int magic = in.readInt();
            // 說明：宣告並初始化變數。
            int version = in.readInt();

            // 說明：根據條件決定是否進入此邏輯分支。
            if (magic != SAVE_MAGIC || version != SAVE_VERSION) {
                // 說明：下一行程式碼負責執行目前步驟。
                return false;
            }

            // 說明：設定或更新變數的值。
            seed = in.readLong();
            // 說明：設定或更新變數的值。
            terrainGenerator = new TerrainGenerator(seed, 62);

            // 說明：宣告並初始化變數。
            int count = in.readInt();
            // 說明：設定或更新變數的值。
            Map<ChunkPos, Chunk> loadedChunks = new HashMap<>();
            // 說明：使用迴圈逐一處理每個元素或區間。
            for (int i = 0; i < count; i++) {
                // 說明：宣告並初始化變數。
                int chunkX = in.readInt();
                // 說明：宣告並初始化變數。
                int chunkZ = in.readInt();
                // 說明：宣告並初始化變數。
                int length = in.readInt();

                // 說明：宣告並初始化變數。
                Chunk chunk = new Chunk(chunkX, chunkZ);
                // 說明：宣告並初始化變數。
                short[] data = chunk.rawBlocks();

                // 說明：根據條件決定是否進入此邏輯分支。
                if (length != data.length) {
                    // 說明：下一行程式碼負責執行目前步驟。
                    return false;
                }

                // 說明：使用迴圈逐一處理每個元素或區間。
                for (int j = 0; j < length; j++) {
                    // 說明：設定或更新變數的值。
                    data[j] = in.readShort();
                }

                // 說明：呼叫方法執行對應功能。
                chunk.markMeshDirty();
                // 說明：呼叫方法執行對應功能。
                chunk.clearModified();
                // 說明：呼叫方法執行對應功能。
                loadedChunks.put(new ChunkPos(chunkX, chunkZ), chunk);
            }

            // 說明：呼叫方法執行對應功能。
            chunks.clear();
            // 說明：呼叫方法執行對應功能。
            chunks.putAll(loadedChunks);
            // 說明：下一行程式碼負責執行目前步驟。
            return true;
        // 說明：下一行程式碼負責執行目前步驟。
        } catch (IOException e) {
            // 說明：下一行程式碼負責執行目前步驟。
            return false;
        }
    }

    // 說明：定義類別內部使用的方法。
    private int fastFloor(float value) {
        // 說明：宣告並初始化變數。
        int i = (int) value;
        // 說明：下一行程式碼負責執行目前步驟。
        return value < i ? i - 1 : i;
    }
}

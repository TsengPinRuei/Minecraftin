// 宣告此檔案所屬的套件。
package com.minecraftin.clone.world;

// 匯入後續會使用到的型別或函式。
import com.minecraftin.clone.config.GameConfig;
// 匯入後續會使用到的型別或函式。
import org.joml.Vector3f;

// 匯入後續會使用到的型別或函式。
import java.io.BufferedInputStream;
// 匯入後續會使用到的型別或函式。
import java.io.BufferedOutputStream;
// 匯入後續會使用到的型別或函式。
import java.io.DataInputStream;
// 匯入後續會使用到的型別或函式。
import java.io.DataOutputStream;
// 匯入後續會使用到的型別或函式。
import java.io.IOException;
// 匯入後續會使用到的型別或函式。
import java.nio.file.Files;
// 匯入後續會使用到的型別或函式。
import java.nio.file.Path;
// 匯入後續會使用到的型別或函式。
import java.util.ArrayDeque;
// 匯入後續會使用到的型別或函式。
import java.util.HashMap;
// 匯入後續會使用到的型別或函式。
import java.util.Map;

// 定義主要型別與其結構。
public final class World {
    // 下一行程式碼負責執行目前步驟。
    private static final int SAVE_MAGIC = 0x4D434C4E; // MCLN
    // 設定或更新變數的值。
    private static final int SAVE_VERSION = 1;
    // 設定或更新變數的值。
    private static final float SPAWN_Y_OFFSET = 1.05f;
    // 限制單次補水量，避免玩家挖開超大空腔時造成卡頓。
    private static final int WATER_FLOOD_MAX_BLOCKS = 32768;
    // 大片森林出生點搜尋的最大半徑（方塊）。
    private static final int FOREST_SPAWN_SEARCH_MAX_RADIUS = 1536;
    // 大片森林出生點搜尋的粗略步距（方塊）。
    private static final int FOREST_SPAWN_SEARCH_STEP = 64;
    // 在森林中心附近找安全落點的最大半徑（方塊）。
    private static final int FOREST_LOCAL_SPAWN_RADIUS = 56;
    // 在森林中心附近找安全落點的步距（方塊）。
    private static final int FOREST_LOCAL_SPAWN_STEP = 8;

    // 設定或更新變數的值。
    private final Map<ChunkPos, Chunk> chunks = new HashMap<>();
    // 下一行程式碼負責執行目前步驟。
    private final Path worldFile;

    // 下一行程式碼負責執行目前步驟。
    private long seed;
    // 下一行程式碼負責執行目前步驟。
    private TerrainGenerator terrainGenerator;

    // 定義對外可呼叫的方法。
    public World(Path worldFile, long defaultSeed) {
        // 設定或更新變數的值。
        this.worldFile = worldFile;
        // 設定或更新變數的值。
        this.seed = defaultSeed;
        // 設定或更新變數的值。
        this.terrainGenerator = new TerrainGenerator(seed, 62);
    }

    // 定義對外可呼叫的方法。
    public void initialize() {
        // 下一行程式碼負責執行目前步驟。
        try {
            // 宣告並初始化變數。
            Path parent = worldFile.getParent();
            // 根據條件決定是否進入此邏輯分支。
            if (parent != null) {
                // 呼叫方法執行對應功能。
                Files.createDirectories(parent);
            }
        // 下一行程式碼負責執行目前步驟。
        } catch (IOException e) {
            // 呼叫方法執行對應功能。
            throw new IllegalStateException("Failed to create save directory", e);
        }

        // 根據條件決定是否進入此邏輯分支。
        if (!load()) {
            // 呼叫方法執行對應功能。
            chunks.clear();
            // 設定或更新變數的值。
            terrainGenerator = new TerrainGenerator(seed, 62);
        }
    }

    // 定義對外可呼叫的方法。
    public long seed() {
        // 下一行程式碼負責執行目前步驟。
        return seed;
    }

    // 定義對外可呼叫的方法。
    public int seaLevel() {
        // 呼叫方法執行對應功能。
        return terrainGenerator.seaLevel();
    }

    // 定義對外可呼叫的方法。
    public int chunkCount() {
        // 呼叫方法執行對應功能。
        return chunks.size();
    }

    // 定義對外可呼叫的方法。
    public boolean hasModifiedChunks() {
        // 使用迴圈逐一處理每個元素或區間。
        for (Chunk chunk : chunks.values()) {
            // 根據條件決定是否進入此邏輯分支。
            if (chunk.isModified()) {
                // 下一行程式碼負責執行目前步驟。
                return true;
            }
        }
        // 下一行程式碼負責執行目前步驟。
        return false;
    }

    // 定義對外可呼叫的方法。
    public void ensureChunksAround(int centerChunkX, int centerChunkZ, int radius) {
        // 宣告並初始化變數。
        int radiusSq = radius * radius;
        // 使用迴圈逐一處理每個元素或區間。
        for (int dz = -radius; dz <= radius; dz++) {
            // 使用迴圈逐一處理每個元素或區間。
            for (int dx = -radius; dx <= radius; dx++) {
                // 根據條件決定是否進入此邏輯分支。
                if (dx * dx + dz * dz > radiusSq) {
                    // 跳過本次迴圈剩餘邏輯，直接進入下一次迭代。
                    continue;
                }
                // 呼叫方法執行對應功能。
                getOrCreateChunk(centerChunkX + dx, centerChunkZ + dz);
            }
        }
    }

    // 定義對外可呼叫的方法。
    public Chunk getChunkIfLoaded(int chunkX, int chunkZ) {
        // 呼叫方法執行對應功能。
        return chunks.get(new ChunkPos(chunkX, chunkZ));
    }

    // 定義對外可呼叫的方法。
    public Chunk getOrCreateChunk(int chunkX, int chunkZ) {
        // 宣告並初始化變數。
        ChunkPos key = new ChunkPos(chunkX, chunkZ);
        // 宣告並初始化變數。
        Chunk existing = chunks.get(key);
        // 根據條件決定是否進入此邏輯分支。
        if (existing != null) {
            // 下一行程式碼負責執行目前步驟。
            return existing;
        }

        // 宣告並初始化變數。
        Chunk chunk = new Chunk(chunkX, chunkZ);
        // 呼叫方法執行對應功能。
        terrainGenerator.generate(chunk);
        // 呼叫方法執行對應功能。
        chunks.put(key, chunk);
        // 呼叫方法執行對應功能。
        markChunkMeshDirty(chunkX - 1, chunkZ);
        // 呼叫方法執行對應功能。
        markChunkMeshDirty(chunkX + 1, chunkZ);
        // 呼叫方法執行對應功能。
        markChunkMeshDirty(chunkX, chunkZ - 1);
        // 呼叫方法執行對應功能。
        markChunkMeshDirty(chunkX, chunkZ + 1);
        // 下一行程式碼負責執行目前步驟。
        return chunk;
    }

    // 定義對外可呼叫的方法。
    public BlockType getBlock(int worldX, int y, int worldZ) {
        // 根據條件決定是否進入此邏輯分支。
        if (y < 0) {
            // 下一行程式碼負責執行目前步驟。
            return BlockType.BEDROCK;
        }
        // 根據條件決定是否進入此邏輯分支。
        if (y >= GameConfig.CHUNK_HEIGHT) {
            // 下一行程式碼負責執行目前步驟。
            return BlockType.AIR;
        }

        // 宣告並初始化變數。
        int chunkX = Math.floorDiv(worldX, GameConfig.CHUNK_SIZE);
        // 宣告並初始化變數。
        int chunkZ = Math.floorDiv(worldZ, GameConfig.CHUNK_SIZE);

        // 宣告並初始化變數。
        int localX = Math.floorMod(worldX, GameConfig.CHUNK_SIZE);
        // 宣告並初始化變數。
        int localZ = Math.floorMod(worldZ, GameConfig.CHUNK_SIZE);

        // 宣告並初始化變數。
        Chunk chunk = getOrCreateChunk(chunkX, chunkZ);
        // 呼叫方法執行對應功能。
        return chunk.get(localX, y, localZ);
    }

    // 定義對外可呼叫的方法。
    public BlockType peekBlock(int worldX, int y, int worldZ) {
        // 根據條件決定是否進入此邏輯分支。
        if (y < 0) {
            // 下一行程式碼負責執行目前步驟。
            return BlockType.BEDROCK;
        }
        // 根據條件決定是否進入此邏輯分支。
        if (y >= GameConfig.CHUNK_HEIGHT) {
            // 下一行程式碼負責執行目前步驟。
            return BlockType.AIR;
        }

        // 宣告並初始化變數。
        int chunkX = Math.floorDiv(worldX, GameConfig.CHUNK_SIZE);
        // 宣告並初始化變數。
        int chunkZ = Math.floorDiv(worldZ, GameConfig.CHUNK_SIZE);

        // 宣告並初始化變數。
        int localX = Math.floorMod(worldX, GameConfig.CHUNK_SIZE);
        // 宣告並初始化變數。
        int localZ = Math.floorMod(worldZ, GameConfig.CHUNK_SIZE);

        // 宣告並初始化變數。
        Chunk chunk = getChunkIfLoaded(chunkX, chunkZ);
        // 根據條件決定是否進入此邏輯分支。
        if (chunk == null) {
            // 下一行程式碼負責執行目前步驟。
            return BlockType.AIR;
        }
        // 呼叫方法執行對應功能。
        return chunk.get(localX, y, localZ);
    }

    // 定義對外可呼叫的方法。
    public boolean setBlock(int worldX, int y, int worldZ, BlockType type) {
        // 先套用方塊變更，玩家建造/破壞都走同一條更新路徑。
        boolean changed = setBlockInternal(worldX, y, worldZ, type, true);
        // 根據條件決定是否進入此邏輯分支。
        if (!changed) {
            // 下一行程式碼負責執行目前步驟。
            return false;
        }

        // 破壞方塊後若形成水下空腔，從相鄰水體開始局部補水。
        if (type == BlockType.AIR) {
            // 呼叫方法執行對應功能。
            floodWaterIntoAirPocket(worldX, y, worldZ);
        }

        // 下一行程式碼負責執行目前步驟。
        return true;
    }

    // 定義類別內部使用的方法。
    private boolean setBlockInternal(int worldX, int y, int worldZ, BlockType type, boolean createMissingChunk) {
        // 根據條件決定是否進入此邏輯分支。
        if (y < 0 || y >= GameConfig.CHUNK_HEIGHT) {
            // 下一行程式碼負責執行目前步驟。
            return false;
        }

        // 宣告並初始化變數。
        int chunkX = Math.floorDiv(worldX, GameConfig.CHUNK_SIZE);
        // 宣告並初始化變數。
        int chunkZ = Math.floorDiv(worldZ, GameConfig.CHUNK_SIZE);
        // 宣告並初始化變數。
        int localX = Math.floorMod(worldX, GameConfig.CHUNK_SIZE);
        // 宣告並初始化變數。
        int localZ = Math.floorMod(worldZ, GameConfig.CHUNK_SIZE);

        // 補水流程不應為了遠方邊界而新生成區塊，因此可選擇只操作已載入區塊。
        Chunk chunk = createMissingChunk ? getOrCreateChunk(chunkX, chunkZ) : getChunkIfLoaded(chunkX, chunkZ);
        // 根據條件決定是否進入此邏輯分支。
        if (chunk == null) {
            // 下一行程式碼負責執行目前步驟。
            return false;
        }

        // 宣告並初始化變數。
        BlockType existing = chunk.get(localX, y, localZ);
        // 根據條件決定是否進入此邏輯分支。
        if (existing == type) {
            // 下一行程式碼負責執行目前步驟。
            return false;
        }

        // 呼叫方法執行對應功能。
        chunk.set(localX, y, localZ, type);

        // 若修改在 chunk 邊界，鄰居 mesh 也要重建避免接縫顯示錯誤。
        if (localX == 0) {
            // 呼叫方法執行對應功能。
            markChunkMeshDirty(chunkX - 1, chunkZ);
        // 下一行程式碼負責執行目前步驟。
        } else if (localX == GameConfig.CHUNK_SIZE - 1) {
            // 呼叫方法執行對應功能。
            markChunkMeshDirty(chunkX + 1, chunkZ);
        }

        // 若修改在 chunk 邊界，鄰居 mesh 也要重建避免接縫顯示錯誤。
        if (localZ == 0) {
            // 呼叫方法執行對應功能。
            markChunkMeshDirty(chunkX, chunkZ - 1);
        // 下一行程式碼負責執行目前步驟。
        } else if (localZ == GameConfig.CHUNK_SIZE - 1) {
            // 呼叫方法執行對應功能。
            markChunkMeshDirty(chunkX, chunkZ + 1);
        }

        // 下一行程式碼負責執行目前步驟。
        return true;
    }

    // 定義類別內部使用的方法。
    private void floodWaterIntoAirPocket(int worldX, int y, int worldZ) {
        // 只處理海平面以下的空腔，避免讓海水不合理地往上爬。
        if (y < 0 || y >= GameConfig.CHUNK_HEIGHT || y > seaLevel()) {
            // 下一行程式碼負責執行目前步驟。
            return;
        }

        // 若目標已不是空氣，代表後續流程或其他更新已處理過。
        if (peekBlock(worldX, y, worldZ) != BlockType.AIR) {
            // 下一行程式碼負責執行目前步驟。
            return;
        }

        // 只有接觸到已載入的水方塊時才開始補水，避免無條件灌水。
        if (!hasAdjacentLoadedWater(worldX, y, worldZ)) {
            // 下一行程式碼負責執行目前步驟。
            return;
        }

        // 使用 BFS 局部填充空腔，效果接近「水往附近空格流入」。
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        // 先填入起點，後續以它為源頭擴散。
        if (!setBlockInternal(worldX, y, worldZ, BlockType.WATER, false)) {
            // 下一行程式碼負責執行目前步驟。
            return;
        }
        // 呼叫方法執行對應功能。
        queue.addLast(new int[]{worldX, y, worldZ});

        // 宣告並初始化變數。
        int filled = 1;
        // 在條件成立時重複執行此區塊。
        while (!queue.isEmpty() && filled < WATER_FLOOD_MAX_BLOCKS) {
            // 宣告並初始化變數。
            int[] cell = queue.removeFirst();
            // 宣告並初始化變數。
            int cx = cell[0];
            // 宣告並初始化變數。
            int cy = cell[1];
            // 宣告並初始化變數。
            int cz = cell[2];

            // 依序檢查六個方向，把相連空氣補成水。
            filled = tryFloodNeighbor(queue, filled, cx + 1, cy, cz);
            // 根據條件決定是否進入此邏輯分支。
            if (filled >= WATER_FLOOD_MAX_BLOCKS) {
                // 跳出迴圈以結束目前流程。
                break;
            }
            // 呼叫方法執行對應功能。
            filled = tryFloodNeighbor(queue, filled, cx - 1, cy, cz);
            // 根據條件決定是否進入此邏輯分支。
            if (filled >= WATER_FLOOD_MAX_BLOCKS) {
                // 跳出迴圈以結束目前流程。
                break;
            }
            // 呼叫方法執行對應功能。
            filled = tryFloodNeighbor(queue, filled, cx, cy, cz + 1);
            // 根據條件決定是否進入此邏輯分支。
            if (filled >= WATER_FLOOD_MAX_BLOCKS) {
                // 跳出迴圈以結束目前流程。
                break;
            }
            // 呼叫方法執行對應功能。
            filled = tryFloodNeighbor(queue, filled, cx, cy, cz - 1);
            // 根據條件決定是否進入此邏輯分支。
            if (filled >= WATER_FLOOD_MAX_BLOCKS) {
                // 跳出迴圈以結束目前流程。
                break;
            }
            // 呼叫方法執行對應功能。
            filled = tryFloodNeighbor(queue, filled, cx, cy - 1, cz);
            // 根據條件決定是否進入此邏輯分支。
            if (filled >= WATER_FLOOD_MAX_BLOCKS) {
                // 跳出迴圈以結束目前流程。
                break;
            }
            // 呼叫方法執行對應功能。
            filled = tryFloodNeighbor(queue, filled, cx, cy + 1, cz);
        }
    }

    // 定義類別內部使用的方法。
    private int tryFloodNeighbor(ArrayDeque<int[]> queue, int filled, int x, int y, int z) {
        // 水體補充不超過世界高度，也不超過海平面高度。
        if (y < 0 || y >= GameConfig.CHUNK_HEIGHT || y > seaLevel()) {
            // 下一行程式碼負責執行目前步驟。
            return filled;
        }

        // 只有空氣格才會被補成水，其它方塊維持不變。
        if (peekBlock(x, y, z) != BlockType.AIR) {
            // 下一行程式碼負責執行目前步驟。
            return filled;
        }

        // 只修改已載入區塊，避免補水流程導致遠方 chunk 被動生成。
        if (!setBlockInternal(x, y, z, BlockType.WATER, false)) {
            // 下一行程式碼負責執行目前步驟。
            return filled;
        }

        // 呼叫方法執行對應功能。
        queue.addLast(new int[]{x, y, z});
        // 下一行程式碼負責執行目前步驟。
        return filled + 1;
    }

    // 定義類別內部使用的方法。
    private boolean hasAdjacentLoadedWater(int worldX, int y, int worldZ) {
        // 以面接觸作為流動判定，避免斜角接觸時不合理補水。
        return peekBlock(worldX + 1, y, worldZ) == BlockType.WATER
                || peekBlock(worldX - 1, y, worldZ) == BlockType.WATER
                || peekBlock(worldX, y, worldZ + 1) == BlockType.WATER
                || peekBlock(worldX, y, worldZ - 1) == BlockType.WATER
                || peekBlock(worldX, y + 1, worldZ) == BlockType.WATER
                || peekBlock(worldX, y - 1, worldZ) == BlockType.WATER;
    }

    // 定義類別內部使用的方法。
    private void markChunkMeshDirty(int chunkX, int chunkZ) {
        // 宣告並初始化變數。
        Chunk chunk = getChunkIfLoaded(chunkX, chunkZ);
        // 根據條件決定是否進入此邏輯分支。
        if (chunk != null) {
            // 呼叫方法執行對應功能。
            chunk.markMeshDirty();
        }
    }

    // 定義對外可呼叫的方法。
    public int topSolidY(int worldX, int worldZ) {
        // 使用迴圈逐一處理每個元素或區間。
        for (int y = GameConfig.CHUNK_HEIGHT - 1; y >= 1; y--) {
            // 宣告並初始化變數。
            BlockType block = getBlock(worldX, y, worldZ);
            // 根據條件決定是否進入此邏輯分支。
            if (block.isSolid() && block != BlockType.LEAVES && block != BlockType.WATER) {
                // 下一行程式碼負責執行目前步驟。
                return y;
            }
        }
        // 下一行程式碼負責執行目前步驟。
        return 1;
    }

    // 定義對外可呼叫的方法。
    public Vector3f defaultSpawn(Vector3f out) {
        // 優先嘗試把玩家放在大片森林中，而不是靠近原點的小島或海岸。
        if (trySpawnInLargeForest(out)) {
            // 下一行程式碼負責執行目前步驟。
            return out;
        }

        // 宣告並初始化變數。
        int maxRadius = 256;
        // 宣告並初始化變數。
        int step = 8;

        // 使用迴圈逐一處理每個元素或區間。
        for (int radius = 0; radius <= maxRadius; radius += step) {
            // 根據條件決定是否進入此邏輯分支。
            if (trySpawnAt(out, -radius, -radius)) {
                // 下一行程式碼負責執行目前步驟。
                return out;
            }
            // 根據條件決定是否進入此邏輯分支。
            if (trySpawnAt(out, radius, radius)) {
                // 下一行程式碼負責執行目前步驟。
                return out;
            }

            // 使用迴圈逐一處理每個元素或區間。
            for (int x = -radius; x <= radius; x += step) {
                // 根據條件決定是否進入此邏輯分支。
                if (trySpawnAt(out, x, -radius) || trySpawnAt(out, x, radius)) {
                    // 下一行程式碼負責執行目前步驟。
                    return out;
                }
            }
            // 使用迴圈逐一處理每個元素或區間。
            for (int z = -radius + step; z <= radius - step; z += step) {
                // 根據條件決定是否進入此邏輯分支。
                if (trySpawnAt(out, -radius, z) || trySpawnAt(out, radius, z)) {
                    // 下一行程式碼負責執行目前步驟。
                    return out;
                }
            }
        }

        // 宣告並初始化變數。
        int fallbackX = 0;
        // 宣告並初始化變數。
        int fallbackZ = 0;
        // 宣告並初始化變數。
        int fallbackY = topSolidY(fallbackX, fallbackZ);
        // 呼叫方法執行對應功能。
        out.set(fallbackX + 0.5f, fallbackY + SPAWN_Y_OFFSET, fallbackZ + 0.5f);
        // 下一行程式碼負責執行目前步驟。
        return out;
    }

    // 定義類別內部使用的方法。
    private boolean trySpawnInLargeForest(Vector3f out) {
        // 宣告並初始化變數。
        int bestScore = Integer.MIN_VALUE;
        // 宣告並初始化變數。
        int bestX = 0;
        // 宣告並初始化變數。
        int bestZ = 0;
        // 宣告並初始化變數。
        boolean foundForestRegion = false;

        // 使用粗略搜尋先找出「大片森林中心」。
        for (int radius = 0; radius <= FOREST_SPAWN_SEARCH_MAX_RADIUS; radius += FOREST_SPAWN_SEARCH_STEP) {
            // 根據條件決定是否進入此邏輯分支。
            if (considerForestSpawnRegion(-radius, -radius, bestScore)) {
                // 宣告並初始化變數。
                int score = terrainGenerator.forestSpawnRegionScore(-radius, -radius);
                // 設定或更新變數的值。
                bestScore = score;
                // 設定或更新變數的值。
                bestX = -radius;
                // 設定或更新變數的值。
                bestZ = -radius;
                // 設定或更新變數的值。
                foundForestRegion = true;
            }
            // 根據條件決定是否進入此邏輯分支。
            if (considerForestSpawnRegion(radius, radius, bestScore)) {
                // 宣告並初始化變數。
                int score = terrainGenerator.forestSpawnRegionScore(radius, radius);
                // 設定或更新變數的值。
                bestScore = score;
                // 設定或更新變數的值。
                bestX = radius;
                // 設定或更新變數的值。
                bestZ = radius;
                // 設定或更新變數的值。
                foundForestRegion = true;
            }

            // 使用迴圈逐一處理每個元素或區間。
            for (int x = -radius; x <= radius; x += FOREST_SPAWN_SEARCH_STEP) {
                // 根據條件決定是否進入此邏輯分支。
                if (considerForestSpawnRegion(x, -radius, bestScore)) {
                    // 宣告並初始化變數。
                    int score = terrainGenerator.forestSpawnRegionScore(x, -radius);
                    // 根據條件決定是否進入此邏輯分支。
                    if (score > bestScore) {
                        // 設定或更新變數的值。
                        bestScore = score;
                        // 設定或更新變數的值。
                        bestX = x;
                        // 設定或更新變數的值。
                        bestZ = -radius;
                        // 設定或更新變數的值。
                        foundForestRegion = true;
                    }
                }
                // 根據條件決定是否進入此邏輯分支。
                if (considerForestSpawnRegion(x, radius, bestScore)) {
                    // 宣告並初始化變數。
                    int score = terrainGenerator.forestSpawnRegionScore(x, radius);
                    // 根據條件決定是否進入此邏輯分支。
                    if (score > bestScore) {
                        // 設定或更新變數的值。
                        bestScore = score;
                        // 設定或更新變數的值。
                        bestX = x;
                        // 設定或更新變數的值。
                        bestZ = radius;
                        // 設定或更新變數的值。
                        foundForestRegion = true;
                    }
                }
            }
            // 使用迴圈逐一處理每個元素或區間。
            for (int z = -radius + FOREST_SPAWN_SEARCH_STEP; z <= radius - FOREST_SPAWN_SEARCH_STEP; z += FOREST_SPAWN_SEARCH_STEP) {
                // 根據條件決定是否進入此邏輯分支。
                if (considerForestSpawnRegion(-radius, z, bestScore)) {
                    // 宣告並初始化變數。
                    int score = terrainGenerator.forestSpawnRegionScore(-radius, z);
                    // 根據條件決定是否進入此邏輯分支。
                    if (score > bestScore) {
                        // 設定或更新變數的值。
                        bestScore = score;
                        // 設定或更新變數的值。
                        bestX = -radius;
                        // 設定或更新變數的值。
                        bestZ = z;
                        // 設定或更新變數的值。
                        foundForestRegion = true;
                    }
                }
                // 根據條件決定是否進入此邏輯分支。
                if (considerForestSpawnRegion(radius, z, bestScore)) {
                    // 宣告並初始化變數。
                    int score = terrainGenerator.forestSpawnRegionScore(radius, z);
                    // 根據條件決定是否進入此邏輯分支。
                    if (score > bestScore) {
                        // 設定或更新變數的值。
                        bestScore = score;
                        // 設定或更新變數的值。
                        bestX = radius;
                        // 設定或更新變數的值。
                        bestZ = z;
                        // 設定或更新變數的值。
                        foundForestRegion = true;
                    }
                }
            }

            // 若已找到高分內陸森林區，可提早結束降低開局等待時間。
            if (foundForestRegion && bestScore >= 290) {
                // 跳出迴圈以結束目前流程。
                break;
            }
        }

        // 根據條件決定是否進入此邏輯分支。
        if (!foundForestRegion) {
            // 下一行程式碼負責執行目前步驟。
            return false;
        }

        // 在森林中心附近找一個安全且附近看得到樹木的實際出生點。
        return trySpawnNearForestCenter(out, bestX, bestZ);
    }

    // 定義類別內部使用的方法。
    private boolean considerForestSpawnRegion(int x, int z, int currentBestScore) {
        // 宣告並初始化變數。
        int score = terrainGenerator.forestSpawnRegionScore(x, z);
        // 下一行程式碼負責執行目前步驟。
        return score > currentBestScore;
    }

    // 定義類別內部使用的方法。
    private boolean trySpawnNearForestCenter(Vector3f out, int centerX, int centerZ) {
        // 先嘗試中心點本身，若不行再做小範圍螺旋搜尋。
        if (trySpawnAtForestVisible(out, centerX, centerZ)) {
            // 下一行程式碼負責執行目前步驟。
            return true;
        }

        // 使用迴圈逐一處理每個元素或區間。
        for (int radius = FOREST_LOCAL_SPAWN_STEP; radius <= FOREST_LOCAL_SPAWN_RADIUS; radius += FOREST_LOCAL_SPAWN_STEP) {
            // 使用迴圈逐一處理每個元素或區間。
            for (int x = centerX - radius; x <= centerX + radius; x += FOREST_LOCAL_SPAWN_STEP) {
                // 根據條件決定是否進入此邏輯分支。
                if (trySpawnAtForestVisible(out, x, centerZ - radius) || trySpawnAtForestVisible(out, x, centerZ + radius)) {
                    // 下一行程式碼負責執行目前步驟。
                    return true;
                }
            }
            // 使用迴圈逐一處理每個元素或區間。
            for (int z = centerZ - radius + FOREST_LOCAL_SPAWN_STEP; z <= centerZ + radius - FOREST_LOCAL_SPAWN_STEP; z += FOREST_LOCAL_SPAWN_STEP) {
                // 根據條件決定是否進入此邏輯分支。
                if (trySpawnAtForestVisible(out, centerX - radius, z) || trySpawnAtForestVisible(out, centerX + radius, z)) {
                    // 下一行程式碼負責執行目前步驟。
                    return true;
                }
            }
        }

        // 最後退一步，只要是中心附近安全地面也接受，避免找不到出生點。
        for (int radius = 0; radius <= FOREST_LOCAL_SPAWN_RADIUS; radius += FOREST_LOCAL_SPAWN_STEP) {
            // 使用迴圈逐一處理每個元素或區間。
            for (int x = centerX - radius; x <= centerX + radius; x += FOREST_LOCAL_SPAWN_STEP) {
                // 根據條件決定是否進入此邏輯分支。
                if (trySpawnAt(out, x, centerZ - radius) || trySpawnAt(out, x, centerZ + radius)) {
                    // 下一行程式碼負責執行目前步驟。
                    return true;
                }
            }
            // 使用迴圈逐一處理每個元素或區間。
            for (int z = centerZ - radius + FOREST_LOCAL_SPAWN_STEP; z <= centerZ + radius - FOREST_LOCAL_SPAWN_STEP; z += FOREST_LOCAL_SPAWN_STEP) {
                // 根據條件決定是否進入此邏輯分支。
                if (trySpawnAt(out, centerX - radius, z) || trySpawnAt(out, centerX + radius, z)) {
                    // 下一行程式碼負責執行目前步驟。
                    return true;
                }
            }
        }

        // 下一行程式碼負責執行目前步驟。
        return false;
    }

    // 定義類別內部使用的方法。
    private boolean trySpawnAtForestVisible(Vector3f out, int x, int z) {
        // 根據條件決定是否進入此邏輯分支。
        if (!trySpawnAt(out, x, z)) {
            // 下一行程式碼負責執行目前步驟。
            return false;
        }

        // 宣告並初始化變數。
        int y = topSolidY(x, z);
        // 下一行程式碼負責執行目前步驟。
        return hasNearbyForestCover(x, y, z);
    }

    // 定義類別內部使用的方法。
    private boolean hasNearbyForestCover(int x, int y, int z) {
        // 宣告並初始化變數。
        int treeColumns = 0;
        // 使用稀疏取樣確認出生點周圍真的有樹，而不是只有森林生物群系噪聲。
        for (int dz = -18; dz <= 18; dz += 6) {
            // 使用迴圈逐一處理每個元素或區間。
            for (int dx = -18; dx <= 18; dx += 6) {
                // 使用迴圈逐一處理每個元素或區間。
                for (int dy = 1; dy <= 10; dy++) {
                    // 宣告並初始化變數。
                    BlockType block = getBlock(x + dx, y + dy, z + dz);
                    // 根據條件決定是否進入此邏輯分支。
                    if (block == BlockType.LOG || block == BlockType.LEAVES) {
                        // 設定或更新變數的值。
                        treeColumns++;
                        // 跳過本次迴圈剩餘邏輯，直接進入下一次迭代。
                        break;
                    }
                }
            }
        }
        // 至少要有一定數量的樹木取樣命中，才視為森林中出生。
        return treeColumns >= 6;
    }

    // 定義類別內部使用的方法。
    private boolean trySpawnAt(Vector3f out, int x, int z) {
        // 宣告並初始化變數。
        int y = topSolidY(x, z);
        // 根據條件決定是否進入此邏輯分支。
        if (y <= seaLevel()) {
            // 下一行程式碼負責執行目前步驟。
            return false;
        }

        // 宣告並初始化變數。
        BlockType floor = getBlock(x, y, z);
        // 根據條件決定是否進入此邏輯分支。
        if (!floor.isSolid() || floor == BlockType.LEAVES || floor == BlockType.WATER) {
            // 下一行程式碼負責執行目前步驟。
            return false;
        }

        // 宣告並初始化變數。
        BlockType feet = getBlock(x, y + 1, z);
        // 宣告並初始化變數。
        BlockType head = getBlock(x, y + 2, z);
        // 根據條件決定是否進入此邏輯分支。
        if (feet != BlockType.AIR || head != BlockType.AIR) {
            // 下一行程式碼負責執行目前步驟。
            return false;
        }

        // 呼叫方法執行對應功能。
        out.set(x + 0.5f, y + SPAWN_Y_OFFSET, z + 0.5f);
        // 下一行程式碼負責執行目前步驟。
        return true;
    }

    // 定義對外可呼叫的方法。
    public RaycastHit raycast(Vector3f origin, Vector3f direction, float maxDistance) {
        // 宣告並初始化變數。
        float dx = direction.x;
        // 宣告並初始化變數。
        float dy = direction.y;
        // 宣告並初始化變數。
        float dz = direction.z;

        // 宣告並初始化變數。
        int x = fastFloor(origin.x);
        // 宣告並初始化變數。
        int y = fastFloor(origin.y);
        // 宣告並初始化變數。
        int z = fastFloor(origin.z);

        // 宣告並初始化變數。
        int stepX = dx > 0 ? 1 : (dx < 0 ? -1 : 0);
        // 宣告並初始化變數。
        int stepY = dy > 0 ? 1 : (dy < 0 ? -1 : 0);
        // 宣告並初始化變數。
        int stepZ = dz > 0 ? 1 : (dz < 0 ? -1 : 0);

        // 宣告並初始化變數。
        float invDx = dx == 0.0f ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dx);
        // 宣告並初始化變數。
        float invDy = dy == 0.0f ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dy);
        // 宣告並初始化變數。
        float invDz = dz == 0.0f ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dz);

        // 宣告並初始化變數。
        float tx = dx == 0.0f ? Float.POSITIVE_INFINITY : (dx > 0 ? (x + 1 - origin.x) * invDx : (origin.x - x) * invDx);
        // 宣告並初始化變數。
        float ty = dy == 0.0f ? Float.POSITIVE_INFINITY : (dy > 0 ? (y + 1 - origin.y) * invDy : (origin.y - y) * invDy);
        // 宣告並初始化變數。
        float tz = dz == 0.0f ? Float.POSITIVE_INFINITY : (dz > 0 ? (z + 1 - origin.z) * invDz : (origin.z - z) * invDz);

        // 宣告並初始化變數。
        float traveled = 0.0f;
        // 宣告並初始化變數。
        int normalX = 0;
        // 宣告並初始化變數。
        int normalY = 0;
        // 宣告並初始化變數。
        int normalZ = 0;

        // 在條件成立時重複執行此區塊。
        while (traveled <= maxDistance) {
            // 宣告並初始化變數。
            BlockType block = getBlock(x, y, z);
            // 根據條件決定是否進入此邏輯分支。
            if (block != BlockType.AIR && block != BlockType.WATER) {
                // 呼叫方法執行對應功能。
                return new RaycastHit(x, y, z, normalX, normalY, normalZ, traveled, block);
            }

            // 根據條件決定是否進入此邏輯分支。
            if (tx < ty) {
                // 根據條件決定是否進入此邏輯分支。
                if (tx < tz) {
                    // 設定或更新變數的值。
                    x += stepX;
                    // 設定或更新變數的值。
                    traveled = tx;
                    // 設定或更新變數的值。
                    tx += invDx;
                    // 設定或更新變數的值。
                    normalX = -stepX;
                    // 設定或更新變數的值。
                    normalY = 0;
                    // 設定或更新變數的值。
                    normalZ = 0;
                // 下一行程式碼負責執行目前步驟。
                } else {
                    // 設定或更新變數的值。
                    z += stepZ;
                    // 設定或更新變數的值。
                    traveled = tz;
                    // 設定或更新變數的值。
                    tz += invDz;
                    // 設定或更新變數的值。
                    normalX = 0;
                    // 設定或更新變數的值。
                    normalY = 0;
                    // 設定或更新變數的值。
                    normalZ = -stepZ;
                }
            // 下一行程式碼負責執行目前步驟。
            } else {
                // 根據條件決定是否進入此邏輯分支。
                if (ty < tz) {
                    // 設定或更新變數的值。
                    y += stepY;
                    // 設定或更新變數的值。
                    traveled = ty;
                    // 設定或更新變數的值。
                    ty += invDy;
                    // 設定或更新變數的值。
                    normalX = 0;
                    // 設定或更新變數的值。
                    normalY = -stepY;
                    // 設定或更新變數的值。
                    normalZ = 0;
                // 下一行程式碼負責執行目前步驟。
                } else {
                    // 設定或更新變數的值。
                    z += stepZ;
                    // 設定或更新變數的值。
                    traveled = tz;
                    // 設定或更新變數的值。
                    tz += invDz;
                    // 設定或更新變數的值。
                    normalX = 0;
                    // 設定或更新變數的值。
                    normalY = 0;
                    // 設定或更新變數的值。
                    normalZ = -stepZ;
                }
            }
        }

        // 下一行程式碼負責執行目前步驟。
        return null;
    }

    // 定義對外可呼叫的方法。
    public void save() {
        // 下一行程式碼負責執行目前步驟。
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(worldFile)))) {
            // 呼叫方法執行對應功能。
            out.writeInt(SAVE_MAGIC);
            // 呼叫方法執行對應功能。
            out.writeInt(SAVE_VERSION);
            // 呼叫方法執行對應功能。
            out.writeLong(seed);
            // 呼叫方法執行對應功能。
            out.writeInt(chunks.size());

            // 使用迴圈逐一處理每個元素或區間。
            for (Chunk chunk : chunks.values()) {
                // 呼叫方法執行對應功能。
                out.writeInt(chunk.chunkX());
                // 呼叫方法執行對應功能。
                out.writeInt(chunk.chunkZ());

                // 宣告並初始化變數。
                short[] data = chunk.rawBlocks();
                // 呼叫方法執行對應功能。
                out.writeInt(data.length);
                // 使用迴圈逐一處理每個元素或區間。
                for (short value : data) {
                    // 呼叫方法執行對應功能。
                    out.writeShort(value);
                }

                // 呼叫方法執行對應功能。
                chunk.clearModified();
            }
        // 下一行程式碼負責執行目前步驟。
        } catch (IOException e) {
            // 呼叫方法執行對應功能。
            throw new IllegalStateException("Failed to save world to " + worldFile, e);
        }
    }

    // 定義類別內部使用的方法。
    private boolean load() {
        // 根據條件決定是否進入此邏輯分支。
        if (!Files.exists(worldFile)) {
            // 下一行程式碼負責執行目前步驟。
            return false;
        }

        // 下一行程式碼負責執行目前步驟。
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(worldFile)))) {
            // 宣告並初始化變數。
            int magic = in.readInt();
            // 宣告並初始化變數。
            int version = in.readInt();

            // 根據條件決定是否進入此邏輯分支。
            if (magic != SAVE_MAGIC || version != SAVE_VERSION) {
                // 下一行程式碼負責執行目前步驟。
                return false;
            }

            // 設定或更新變數的值。
            seed = in.readLong();
            // 設定或更新變數的值。
            terrainGenerator = new TerrainGenerator(seed, 62);

            // 宣告並初始化變數。
            int count = in.readInt();
            // 設定或更新變數的值。
            Map<ChunkPos, Chunk> loadedChunks = new HashMap<>();
            // 使用迴圈逐一處理每個元素或區間。
            for (int i = 0; i < count; i++) {
                // 宣告並初始化變數。
                int chunkX = in.readInt();
                // 宣告並初始化變數。
                int chunkZ = in.readInt();
                // 宣告並初始化變數。
                int length = in.readInt();

                // 宣告並初始化變數。
                Chunk chunk = new Chunk(chunkX, chunkZ);
                // 宣告並初始化變數。
                short[] data = chunk.rawBlocks();

                // 根據條件決定是否進入此邏輯分支。
                if (length != data.length) {
                    // 下一行程式碼負責執行目前步驟。
                    return false;
                }

                // 使用迴圈逐一處理每個元素或區間。
                for (int j = 0; j < length; j++) {
                    // 設定或更新變數的值。
                    data[j] = in.readShort();
                }

                // 呼叫方法執行對應功能。
                chunk.markMeshDirty();
                // 呼叫方法執行對應功能。
                chunk.clearModified();
                // 呼叫方法執行對應功能。
                loadedChunks.put(new ChunkPos(chunkX, chunkZ), chunk);
            }

            // 呼叫方法執行對應功能。
            chunks.clear();
            // 呼叫方法執行對應功能。
            chunks.putAll(loadedChunks);
            // 下一行程式碼負責執行目前步驟。
            return true;
        // 下一行程式碼負責執行目前步驟。
        } catch (IOException e) {
            // 下一行程式碼負責執行目前步驟。
            return false;
        }
    }

    // 定義類別內部使用的方法。
    private int fastFloor(float value) {
        // 宣告並初始化變數。
        int i = (int) value;
        // 下一行程式碼負責執行目前步驟。
        return value < i ? i - 1 : i;
    }
}

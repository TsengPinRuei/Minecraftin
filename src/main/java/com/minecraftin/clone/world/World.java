package com.minecraftin.clone.world;

import com.minecraftin.clone.config.GameConfig;
import org.joml.Vector3f;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

// 負責管理整個世界的區塊、方塊資料、出生點、射線檢測，以及存檔與讀檔。
// 這個類別是「世界狀態」的邊界：會生成 Chunk、標記 mesh dirty、追蹤需要持久化的變更。
public final class World {

    // 存檔檔頭，用來確認這是不是本遊戲建立的存檔；值為 "MCLN" 的整數形式。
    private static final int SAVE_MAGIC = 0x4D434C4E;

    // 目前存檔格式版本；load() 仍接受版本 1，讓沒有重生點欄位的舊存檔可讀取。
    private static final int SAVE_VERSION = 2;

    // 玩家出生時，腳底上方的偏移量，避免卡進地面。
    private static final float SPAWN_Y_OFFSET = 1.05f;

    // 一次補水最多處理多少格，避免大型空腔造成明顯卡頓。
    private static final int WATER_FLOOD_MAX_BLOCKS = 32768;

    // 玩家主動放置水時，最多保留多少待擴散格，避免連續放水造成過大的更新佇列。
    private static final int PLACED_WATER_FLOW_MAX_PENDING_CELLS = 4096;

    // 放置水落地後最多橫向流動幾格，接近 Minecraft 水流的有限距離感。
    private static final int PLACED_WATER_FLOW_HORIZONTAL_DISTANCE = 7;

    // 放置水每次擴散的時間間隔；讓玩家能看見水流逐步蔓延，而不是瞬間完成。
    private static final float PLACED_WATER_FLOW_STEP_SECONDS = 0.1f;

    // 每個水流更新步驟最多處理幾格，數值越小動畫越明顯。
    private static final int PLACED_WATER_FLOW_CELLS_PER_STEP = 5;

    // 搜尋大片森林出生區域時，最遠搜尋半徑。
    private static final int FOREST_SPAWN_SEARCH_MAX_RADIUS = 1536;

    // 搜尋大片森林時，每次移動的距離。
    private static final int FOREST_SPAWN_SEARCH_STEP = 64;

    // 找到森林中心後，在附近細部搜尋安全出生點的最大半徑。
    private static final int FOREST_LOCAL_SPAWN_RADIUS = 56;

    // 在森林中心附近搜尋出生點時，每次移動的距離。
    private static final int FOREST_LOCAL_SPAWN_STEP = 8;

    // 已載入的所有 Chunk，key 是 Chunk 座標。
    private final Map<ChunkPos, Chunk> chunks = new HashMap<>();

    // 玩家放置水後的待擴散佇列，由 update() 分批處理以形成水流動畫。
    private final ArrayDeque<WaterFlowCell> placedWaterFlowQueue = new ArrayDeque<>();

    // 世界存檔路徑。
    private final Path worldFile;

    // 世界種子。
    private long seed;

    // 地形產生器。
    private TerrainGenerator terrainGenerator;

    // 儲存的重生點座標。
    private final Vector3f savedRespawnPosition = new Vector3f();

    // 是否已有儲存的重生點。
    private boolean hasSavedRespawnPosition;

    // 重生點資料是否有變更，之後需要存檔。
    private boolean respawnPositionDirty;

    // 水流動畫累積時間。
    private float placedWaterFlowTimer;

    // 建立世界物件，並先設定預設種子與地形產生器。
    public World(Path worldFile, long defaultSeed) {
        this.worldFile = worldFile;
        this.seed = defaultSeed;
        this.terrainGenerator = new TerrainGenerator(seed, 62);
    }

    // 初始化世界。
    // 若有舊存檔就嘗試讀取，否則建立新世界狀態；讀檔失敗會回到乾淨的新世界而不是中止遊戲。
    public void initialize() {
        try {
            Path parent = worldFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create save directory", e);
        }

        if (!load()) {
            chunks.clear();
            terrainGenerator = new TerrainGenerator(seed, 62);
            clearSavedRespawnPosition();
        }
    }

    // 回傳世界種子。
    public long seed() {
        return seed;
    }

    // 回傳海平面高度。
    public int seaLevel() {
        return terrainGenerator.seaLevel();
    }

    // 更新世界中的非玩家即時狀態。目前主要用來推進放置水的逐步流動動畫。
    public void update(float deltaSeconds) {
        updatePlacedWaterFlow(deltaSeconds);
    }

    // 回傳目前已載入的 Chunk 數量。
    public int chunkCount() {
        return chunks.size();
    }

    // 檢查是否有任何 Chunk 被修改過。
    public boolean hasModifiedChunks() {
        for (Chunk chunk : chunks.values()) {
            if (chunk.isModified()) {
                return true;
            }
        }
        return false;
    }

    // 檢查目前是否有資料需要存檔。
    public boolean hasPendingSave() {
        return respawnPositionDirty || hasModifiedChunks();
    }

    // 設定玩家重生點。
    // 若座標沒有改變，就不更新。
    public void setRespawnPosition(float x, float y, float z) {
        if (hasSavedRespawnPosition
                && Float.compare(savedRespawnPosition.x, x) == 0
                && Float.compare(savedRespawnPosition.y, y) == 0
                && Float.compare(savedRespawnPosition.z, z) == 0) {
            return;
        }

        savedRespawnPosition.set(x, y, z);
        hasSavedRespawnPosition = true;
        respawnPositionDirty = true;
    }

    // 嘗試取得已儲存的重生點。
    // 有資料時會寫入 out，並回傳 true。
    public boolean tryGetSavedRespawnPosition(Vector3f out) {
        if (!hasSavedRespawnPosition) {
            return false;
        }
        out.set(savedRespawnPosition);
        return true;
    }

    // 確保指定 Chunk 周圍一定半徑內的 Chunk 都已建立。
    public void ensureChunksAround(int centerChunkX, int centerChunkZ, int radius) {
        int radiusSq = radius * radius;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dz * dz > radiusSq) {
                    continue;
                }
                getOrCreateChunk(centerChunkX + dx, centerChunkZ + dz);
            }
        }
    }

    // 取得已載入的 Chunk。
    // 若尚未載入，回傳 null。
    public Chunk getChunkIfLoaded(int chunkX, int chunkZ) {
        return chunks.get(new ChunkPos(chunkX, chunkZ));
    }

    // 取得指定座標的 Chunk。
    // 若不存在，會先建立並生成地形；這是會改變世界快取內容的讀取操作。
    public Chunk getOrCreateChunk(int chunkX, int chunkZ) {
        ChunkPos key = new ChunkPos(chunkX, chunkZ);
        Chunk existing = chunks.get(key);
        if (existing != null) {
            return existing;
        }

        Chunk chunk = new Chunk(chunkX, chunkZ);
        terrainGenerator.generate(chunk);
        chunks.put(key, chunk);

        // 新增 Chunk 後，周圍 Chunk 的邊界顯示可能受影響，因此一併標記為需要重建 mesh。
        markChunkMeshDirty(chunkX - 1, chunkZ);
        markChunkMeshDirty(chunkX + 1, chunkZ);
        markChunkMeshDirty(chunkX, chunkZ - 1);
        markChunkMeshDirty(chunkX, chunkZ + 1);

        return chunk;
    }

    // 取得世界座標上的方塊。
    // 若超出高度範圍，回傳床岩或空氣；合法高度內會自動生成缺少的 Chunk。
    public BlockType getBlock(int worldX, int y, int worldZ) {
        if (y < 0) {
            return BlockType.BEDROCK;
        }
        if (y >= GameConfig.CHUNK_HEIGHT) {
            return BlockType.AIR;
        }

        int chunkX = Math.floorDiv(worldX, GameConfig.CHUNK_SIZE);
        int chunkZ = Math.floorDiv(worldZ, GameConfig.CHUNK_SIZE);

        int localX = Math.floorMod(worldX, GameConfig.CHUNK_SIZE);
        int localZ = Math.floorMod(worldZ, GameConfig.CHUNK_SIZE);

        Chunk chunk = getOrCreateChunk(chunkX, chunkZ);
        return chunk.get(localX, y, localZ);
    }

    // 讀取世界座標上的方塊，但不主動建立新的 Chunk。
    // 常用於渲染鄰面、水流擴散等「只看已載入狀態」的流程，避免查詢本身造成地形生成。
    public BlockType peekBlock(int worldX, int y, int worldZ) {
        if (y < 0) {
            return BlockType.BEDROCK;
        }
        if (y >= GameConfig.CHUNK_HEIGHT) {
            return BlockType.AIR;
        }

        int chunkX = Math.floorDiv(worldX, GameConfig.CHUNK_SIZE);
        int chunkZ = Math.floorDiv(worldZ, GameConfig.CHUNK_SIZE);

        int localX = Math.floorMod(worldX, GameConfig.CHUNK_SIZE);
        int localZ = Math.floorMod(worldZ, GameConfig.CHUNK_SIZE);

        Chunk chunk = getChunkIfLoaded(chunkX, chunkZ);
        if (chunk == null) {
            return BlockType.AIR;
        }
        return chunk.get(localX, y, localZ);
    }

    // 設定世界座標上的方塊。
    // 若成功破壞方塊形成空腔，會處理補水；若放置水，會啟動有限水流擴散。
    public boolean setBlock(int worldX, int y, int worldZ, BlockType type) {
        boolean changed = setBlockInternal(worldX, y, worldZ, type, true);
        if (!changed) {
            return false;
        }

        // 挖掉方塊後，若附近有水，嘗試讓水流入空腔。
        if (type == BlockType.AIR) {
            floodWaterIntoAirPocket(worldX, y, worldZ);
        }

        if (type == BlockType.WATER) {
            queuePlacedWaterFlow(worldX, y, worldZ);
        }

        return true;
    }

    // 實際執行方塊更新的內部方法。
    // createMissingChunk 為 true 時，缺少 Chunk 會自動建立。
    private boolean setBlockInternal(int worldX, int y, int worldZ, BlockType type, boolean createMissingChunk) {
        if (y < 0 || y >= GameConfig.CHUNK_HEIGHT) {
            return false;
        }

        int chunkX = Math.floorDiv(worldX, GameConfig.CHUNK_SIZE);
        int chunkZ = Math.floorDiv(worldZ, GameConfig.CHUNK_SIZE);
        int localX = Math.floorMod(worldX, GameConfig.CHUNK_SIZE);
        int localZ = Math.floorMod(worldZ, GameConfig.CHUNK_SIZE);

        Chunk chunk = createMissingChunk ? getOrCreateChunk(chunkX, chunkZ) : getChunkIfLoaded(chunkX, chunkZ);
        if (chunk == null) {
            return false;
        }

        BlockType existing = chunk.get(localX, y, localZ);
        if (existing == type) {
            return false;
        }

        chunk.set(localX, y, localZ, type);

        // 如果修改位置在 Chunk 邊界，鄰近 Chunk 的 mesh 也要重建。
        if (localX == 0) {
            markChunkMeshDirty(chunkX - 1, chunkZ);
        } else if (localX == GameConfig.CHUNK_SIZE - 1) {
            markChunkMeshDirty(chunkX + 1, chunkZ);
        }

        if (localZ == 0) {
            markChunkMeshDirty(chunkX, chunkZ - 1);
        } else if (localZ == GameConfig.CHUNK_SIZE - 1) {
            markChunkMeshDirty(chunkX, chunkZ + 1);
        }

        return true;
    }

    // 當玩家挖出海平面以下的空腔時，嘗試讓附近的水填進來。
    // 只在已載入 Chunk 內擴散，避免一次挖方塊就生成大片未知地形。
    private void floodWaterIntoAirPocket(int worldX, int y, int worldZ) {
        // 只處理海平面以下的情況。
        if (y < 0 || y >= GameConfig.CHUNK_HEIGHT || y > seaLevel()) {
            return;
        }

        // 目標不是空氣就不用補水。
        if (peekBlock(worldX, y, worldZ) != BlockType.AIR) {
            return;
        }

        // 只有相鄰位置本來就有水時，才開始補水。
        if (!hasAdjacentLoadedWater(worldX, y, worldZ)) {
            return;
        }

        if (!setBlockInternal(worldX, y, worldZ, BlockType.WATER, false)) {
            return;
        }

        // 用 BFS 方式向外擴散補水，並用 WATER_FLOOD_MAX_BLOCKS 防止大型空腔造成長時間卡頓。
        int[] queue = new int[WATER_FLOOD_MAX_BLOCKS * 3];
        int head = 0;
        int tail = enqueueFloodCell(queue, 0, worldX, y, worldZ);

        int filled = 1;
        while (head < tail && filled < WATER_FLOOD_MAX_BLOCKS) {
            int cx = queue[head++];
            int cy = queue[head++];
            int cz = queue[head++];

            int previousTail = tail;
            tail = tryFloodNeighbor(queue, tail, cx + 1, cy, cz);
            filled += tail == previousTail ? 0 : 1;
            if (filled >= WATER_FLOOD_MAX_BLOCKS) {
                break;
            }

            previousTail = tail;
            tail = tryFloodNeighbor(queue, tail, cx - 1, cy, cz);
            filled += tail == previousTail ? 0 : 1;
            if (filled >= WATER_FLOOD_MAX_BLOCKS) {
                break;
            }

            previousTail = tail;
            tail = tryFloodNeighbor(queue, tail, cx, cy, cz + 1);
            filled += tail == previousTail ? 0 : 1;
            if (filled >= WATER_FLOOD_MAX_BLOCKS) {
                break;
            }

            previousTail = tail;
            tail = tryFloodNeighbor(queue, tail, cx, cy, cz - 1);
            filled += tail == previousTail ? 0 : 1;
            if (filled >= WATER_FLOOD_MAX_BLOCKS) {
                break;
            }

            previousTail = tail;
            tail = tryFloodNeighbor(queue, tail, cx, cy - 1, cz);
            filled += tail == previousTail ? 0 : 1;
            if (filled >= WATER_FLOOD_MAX_BLOCKS) {
                break;
            }

            previousTail = tail;
            tail = tryFloodNeighbor(queue, tail, cx, cy + 1, cz);
            filled += tail == previousTail ? 0 : 1;
        }
    }

    // 嘗試把鄰近的一格空氣補成水。
    // 成功時加入佇列，讓它後續也能繼續擴散。
    private int tryFloodNeighbor(int[] queue, int tail, int x, int y, int z) {
        if (y < 0 || y >= GameConfig.CHUNK_HEIGHT || y > seaLevel()) {
            return tail;
        }

        if (peekBlock(x, y, z) != BlockType.AIR) {
            return tail;
        }

        if (!setBlockInternal(x, y, z, BlockType.WATER, false)) {
            return tail;
        }

        return enqueueFloodCell(queue, tail, x, y, z);
    }

    // 將一個 BFS 格子寫入 primitive queue，避免大型補水時建立大量短生命週期 int[]。
    private int enqueueFloodCell(int[] queue, int tail, int x, int y, int z) {
        queue[tail++] = x;
        queue[tail++] = y;
        queue[tail++] = z;
        return tail;
    }

    // 玩家放置水後先排入佇列，後續由 updatePlacedWaterFlow 分批擴散。
    private void queuePlacedWaterFlow(int worldX, int y, int worldZ) {
        if (y < 0 || y >= GameConfig.CHUNK_HEIGHT || peekBlock(worldX, y, worldZ) != BlockType.WATER) {
            return;
        }

        if (placedWaterFlowQueue.size() < PLACED_WATER_FLOW_MAX_PENDING_CELLS) {
            placedWaterFlowQueue.addLast(new WaterFlowCell(worldX, y, worldZ, 0));
        }
    }

    // 分批處理放置水擴散，形成可見的流動過程。
    private void updatePlacedWaterFlow(float deltaSeconds) {
        if (placedWaterFlowQueue.isEmpty()) {
            placedWaterFlowTimer = 0.0f;
            return;
        }

        placedWaterFlowTimer += Math.max(0.0f, deltaSeconds);

        int steps = 0;
        while (placedWaterFlowTimer >= PLACED_WATER_FLOW_STEP_SECONDS && steps < 4) {
            placedWaterFlowTimer -= PLACED_WATER_FLOW_STEP_SECONDS;
            steps++;

            for (int i = 0; i < PLACED_WATER_FLOW_CELLS_PER_STEP && !placedWaterFlowQueue.isEmpty(); i++) {
                WaterFlowCell cell = placedWaterFlowQueue.removeFirst();
                spreadPlacedWaterCell(cell);
            }
        }
    }

    // 優先往下流；下方被擋住時才向四周擴散。
    private void spreadPlacedWaterCell(WaterFlowCell cell) {
        int cx = cell.x();
        int cy = cell.y();
        int cz = cell.z();
        int horizontalDistance = cell.horizontalDistance();

        if (peekBlock(cx, cy, cz) != BlockType.WATER) {
            return;
        }

        if (trySpreadPlacedWaterNeighbor(cx, cy - 1, cz, horizontalDistance)) {
            return;
        }

        if (horizontalDistance >= PLACED_WATER_FLOW_HORIZONTAL_DISTANCE) {
            return;
        }

        int nextDistance = horizontalDistance + 1;
        trySpreadPlacedWaterNeighbor(cx + 1, cy, cz, nextDistance);
        trySpreadPlacedWaterNeighbor(cx - 1, cy, cz, nextDistance);
        trySpreadPlacedWaterNeighbor(cx, cy, cz + 1, nextDistance);
        trySpreadPlacedWaterNeighbor(cx, cy, cz - 1, nextDistance);
    }

    // 嘗試讓放置水流入鄰近空氣；只更新已載入 Chunk，避免水流查詢生成新地形。
    private boolean trySpreadPlacedWaterNeighbor(int x, int y, int z, int horizontalDistance) {
        if (y < 0 || y >= GameConfig.CHUNK_HEIGHT) {
            return false;
        }

        if (placedWaterFlowQueue.size() >= PLACED_WATER_FLOW_MAX_PENDING_CELLS) {
            return false;
        }

        if (peekBlock(x, y, z) != BlockType.AIR) {
            return false;
        }

        if (!setBlockInternal(x, y, z, BlockType.WATER, false)) {
            return false;
        }

        placedWaterFlowQueue.addLast(new WaterFlowCell(x, y, z, horizontalDistance));
        return true;
    }

    private record WaterFlowCell(int x, int y, int z, int horizontalDistance) {
    }

    // 檢查目標位置六個方向是否有已載入的水方塊。
    private boolean hasAdjacentLoadedWater(int worldX, int y, int worldZ) {
        return peekBlock(worldX + 1, y, worldZ) == BlockType.WATER
                || peekBlock(worldX - 1, y, worldZ) == BlockType.WATER
                || peekBlock(worldX, y, worldZ + 1) == BlockType.WATER
                || peekBlock(worldX, y, worldZ - 1) == BlockType.WATER
                || peekBlock(worldX, y + 1, worldZ) == BlockType.WATER
                || peekBlock(worldX, y - 1, worldZ) == BlockType.WATER;
    }

    // 將指定 Chunk 標記為需要重建 mesh。
    private void markChunkMeshDirty(int chunkX, int chunkZ) {
        Chunk chunk = getChunkIfLoaded(chunkX, chunkZ);
        if (chunk != null) {
            chunk.markMeshDirty();
        }
    }

    // 從上往下找出某個座標最上方的實心地面高度。
    public int topSolidY(int worldX, int worldZ) {
        int chunkX = Math.floorDiv(worldX, GameConfig.CHUNK_SIZE);
        int chunkZ = Math.floorDiv(worldZ, GameConfig.CHUNK_SIZE);
        int localX = Math.floorMod(worldX, GameConfig.CHUNK_SIZE);
        int localZ = Math.floorMod(worldZ, GameConfig.CHUNK_SIZE);
        Chunk chunk = getOrCreateChunk(chunkX, chunkZ);

        for (int y = GameConfig.CHUNK_HEIGHT - 1; y >= 1; y--) {
            BlockType block = chunk.get(localX, y, localZ);
            if (block.isSolid() && block != BlockType.LEAVES && block != BlockType.WATER) {
                return y;
            }
        }
        return 1;
    }

    // 計算預設出生點。
    // 優先找大片森林，找不到再退回一般安全地面；這會生成搜尋路徑上的 Chunk。
    public Vector3f defaultSpawn(Vector3f out) {
        if (trySpawnInLargeForest(out)) {
            return out;
        }

        int maxRadius = 256;
        int step = 8;

        for (int radius = 0; radius <= maxRadius; radius += step) {
            if (trySpawnAt(out, -radius, -radius)) {
                return out;
            }
            if (trySpawnAt(out, radius, radius)) {
                return out;
            }

            for (int x = -radius; x <= radius; x += step) {
                if (trySpawnAt(out, x, -radius) || trySpawnAt(out, x, radius)) {
                    return out;
                }
            }

            for (int z = -radius + step; z <= radius - step; z += step) {
                if (trySpawnAt(out, -radius, z) || trySpawnAt(out, radius, z)) {
                    return out;
                }
            }
        }

        int fallbackX = 0;
        int fallbackZ = 0;
        if (trySpawnAt(out, fallbackX, fallbackZ) || tryFindNearestSafeSpawn(out, fallbackX, fallbackZ, 64, 4)) {
            return out;
        }

        int fallbackY = topSolidY(fallbackX, fallbackZ);
        out.set(fallbackX + 0.5f, fallbackY + SPAWN_Y_OFFSET, fallbackZ + 0.5f);
        return out;
    }

    // 先用較大範圍搜尋，找出適合出生的大片森林區域；只評分地表取樣，不立即要求每格都可站立。
    private boolean trySpawnInLargeForest(Vector3f out) {
        int[] bestScore = new int[] { Integer.MIN_VALUE };
        int[] bestX = new int[] { 0 };
        int[] bestZ = new int[] { 0 };
        boolean[] foundForestRegion = new boolean[] { false };

        for (int radius = 0; radius <= FOREST_SPAWN_SEARCH_MAX_RADIUS; radius += FOREST_SPAWN_SEARCH_STEP) {
            evaluateForestSpawnCandidate(-radius, -radius, bestScore, bestX, bestZ, foundForestRegion);
            evaluateForestSpawnCandidate(radius, radius, bestScore, bestX, bestZ, foundForestRegion);

            for (int x = -radius; x <= radius; x += FOREST_SPAWN_SEARCH_STEP) {
                evaluateForestSpawnCandidate(x, -radius, bestScore, bestX, bestZ, foundForestRegion);
                evaluateForestSpawnCandidate(x, radius, bestScore, bestX, bestZ, foundForestRegion);
            }

            for (int z = -radius + FOREST_SPAWN_SEARCH_STEP; z <= radius
                    - FOREST_SPAWN_SEARCH_STEP; z += FOREST_SPAWN_SEARCH_STEP) {
                evaluateForestSpawnCandidate(-radius, z, bestScore, bestX, bestZ, foundForestRegion);
                evaluateForestSpawnCandidate(radius, z, bestScore, bestX, bestZ, foundForestRegion);
            }

            // 已經找到非常理想的森林區域時，就提早停止搜尋。
            if (foundForestRegion[0] && bestScore[0] >= 290) {
                break;
            }
        }

        if (!foundForestRegion[0]) {
            return false;
        }

        return trySpawnNearForestCenter(out, bestX[0], bestZ[0]);
    }

    // 評估某個位置是否適合作為森林出生區候選點；用陣列包裝是為了在 helper 中更新目前最佳結果。
    private void evaluateForestSpawnCandidate(
            int x, int z, int[] bestScore, int[] bestX, int[] bestZ, boolean[] foundForestRegion) {
        int score = terrainGenerator.forestSpawnRegionScore(x, z);
        if (score > bestScore[0]) {
            bestScore[0] = score;
            bestX[0] = x;
            bestZ[0] = z;
            foundForestRegion[0] = true;
        }
    }

    // 以指定中心為起點，往外找最近的安全出生點。
    private boolean tryFindNearestSafeSpawn(Vector3f out, int centerX, int centerZ, int maxRadius, int step) {
        for (int radius = step; radius <= maxRadius; radius += step) {
            for (int x = centerX - radius; x <= centerX + radius; x += step) {
                if (trySpawnAt(out, x, centerZ - radius) || trySpawnAt(out, x, centerZ + radius)) {
                    return true;
                }
            }

            for (int z = centerZ - radius + step; z <= centerZ + radius - step; z += step) {
                if (trySpawnAt(out, centerX - radius, z) || trySpawnAt(out, centerX + radius, z)) {
                    return true;
                }
            }
        }
        return false;
    }

    // 在森林中心附近找一個看得到森林且可安全站立的位置。
    private boolean trySpawnNearForestCenter(Vector3f out, int centerX, int centerZ) {
        if (trySpawnAtForestVisible(out, centerX, centerZ)) {
            return true;
        }

        for (int radius = FOREST_LOCAL_SPAWN_STEP; radius <= FOREST_LOCAL_SPAWN_RADIUS; radius += FOREST_LOCAL_SPAWN_STEP) {
            for (int x = centerX - radius; x <= centerX + radius; x += FOREST_LOCAL_SPAWN_STEP) {
                if (trySpawnAtForestVisible(out, x, centerZ - radius)
                        || trySpawnAtForestVisible(out, x, centerZ + radius)) {
                    return true;
                }
            }

            for (int z = centerZ - radius + FOREST_LOCAL_SPAWN_STEP; z <= centerZ + radius
                    - FOREST_LOCAL_SPAWN_STEP; z += FOREST_LOCAL_SPAWN_STEP) {
                if (trySpawnAtForestVisible(out, centerX - radius, z)
                        || trySpawnAtForestVisible(out, centerX + radius, z)) {
                    return true;
                }
            }
        }

        // 若真的找不到「看得到森林」的位置，退一步接受一般安全地面。
        for (int radius = 0; radius <= FOREST_LOCAL_SPAWN_RADIUS; radius += FOREST_LOCAL_SPAWN_STEP) {
            for (int x = centerX - radius; x <= centerX + radius; x += FOREST_LOCAL_SPAWN_STEP) {
                if (trySpawnAt(out, x, centerZ - radius) || trySpawnAt(out, x, centerZ + radius)) {
                    return true;
                }
            }

            for (int z = centerZ - radius + FOREST_LOCAL_SPAWN_STEP; z <= centerZ + radius
                    - FOREST_LOCAL_SPAWN_STEP; z += FOREST_LOCAL_SPAWN_STEP) {
                if (trySpawnAt(out, centerX - radius, z) || trySpawnAt(out, centerX + radius, z)) {
                    return true;
                }
            }
        }

        return false;
    }

    // 檢查某位置是否能安全出生，且附近確實看得到樹。
    private boolean trySpawnAtForestVisible(Vector3f out, int x, int z) {
        if (!trySpawnAt(out, x, z)) {
            return false;
        }

        int y = topSolidY(x, z);
        return hasNearbyForestCover(x, y, z);
    }

    // 檢查出生點周圍是否有足夠多的樹木。
    private boolean hasNearbyForestCover(int x, int y, int z) {
        int treeColumns = 0;

        for (int dz = -18; dz <= 18; dz += 6) {
            for (int dx = -18; dx <= 18; dx += 6) {
                for (int dy = 1; dy <= 10; dy++) {
                    BlockType block = getBlock(x + dx, y + dy, z + dz);
                    if (block == BlockType.LOG || block == BlockType.LEAVES) {
                        treeColumns++;
                        break;
                    }
                }
            }
        }

        return treeColumns >= 6;
    }

    // 檢查某個座標是否適合作為玩家出生點。
    private boolean trySpawnAt(Vector3f out, int x, int z) {
        int y = topSolidY(x, z);
        if (y <= seaLevel()) {
            return false;
        }

        BlockType floor = getBlock(x, y, z);
        if (!floor.isSolid() || floor == BlockType.LEAVES || floor == BlockType.WATER) {
            return false;
        }

        BlockType feet = getBlock(x, y + 1, z);
        BlockType head = getBlock(x, y + 2, z);
        if (feet != BlockType.AIR || head != BlockType.AIR) {
            return false;
        }

        out.set(x + 0.5f, y + SPAWN_Y_OFFSET, z + 0.5f);
        return true;
    }

    // 從 origin 沿著 direction 發射射線，找出第一個碰到的方塊。
    // 使用 3D DDA 逐格前進，可直接得到命中面 normal，供放置方塊判斷相鄰位置。
    public RaycastHit raycast(Vector3f origin, Vector3f direction, float maxDistance) {
        float dx = direction.x;
        float dy = direction.y;
        float dz = direction.z;

        int x = fastFloor(origin.x);
        int y = fastFloor(origin.y);
        int z = fastFloor(origin.z);

        int stepX = dx > 0 ? 1 : (dx < 0 ? -1 : 0);
        int stepY = dy > 0 ? 1 : (dy < 0 ? -1 : 0);
        int stepZ = dz > 0 ? 1 : (dz < 0 ? -1 : 0);

        float invDx = dx == 0.0f ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dx);
        float invDy = dy == 0.0f ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dy);
        float invDz = dz == 0.0f ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dz);

        // 計算射線距離下一個格線交點還有多遠。
        float tx = dx == 0.0f ? Float.POSITIVE_INFINITY
                : (dx > 0 ? (x + 1 - origin.x) * invDx : (origin.x - x) * invDx);
        float ty = dy == 0.0f ? Float.POSITIVE_INFINITY
                : (dy > 0 ? (y + 1 - origin.y) * invDy : (origin.y - y) * invDy);
        float tz = dz == 0.0f ? Float.POSITIVE_INFINITY
                : (dz > 0 ? (z + 1 - origin.z) * invDz : (origin.z - z) * invDz);

        float traveled = 0.0f;
        int normalX = 0;
        int normalY = 0;
        int normalZ = 0;

        // 使用類似 3D DDA 的方式，沿著方塊格子一步一步前進。
        while (traveled <= maxDistance) {
            BlockType block = getBlock(x, y, z);
            if (block != BlockType.AIR && block != BlockType.WATER) {
                return new RaycastHit(x, y, z, normalX, normalY, normalZ, traveled, block);
            }

            if (tx < ty) {
                if (tx < tz) {
                    x += stepX;
                    traveled = tx;
                    tx += invDx;
                    normalX = -stepX;
                    normalY = 0;
                    normalZ = 0;
                } else {
                    z += stepZ;
                    traveled = tz;
                    tz += invDz;
                    normalX = 0;
                    normalY = 0;
                    normalZ = -stepZ;
                }
            } else {
                if (ty < tz) {
                    y += stepY;
                    traveled = ty;
                    ty += invDy;
                    normalX = 0;
                    normalY = -stepY;
                    normalZ = 0;
                } else {
                    z += stepZ;
                    traveled = tz;
                    tz += invDz;
                    normalX = 0;
                    normalY = 0;
                    normalZ = -stepZ;
                }
            }
        }

        return null;
    }

    // 將目前世界資料寫入存檔。
    // 格式順序為 magic/version/seed/respawn/chunkCount/chunk資料；BlockType.id 必須保持穩定。
    public void save() {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(worldFile)))) {
            out.writeInt(SAVE_MAGIC);
            out.writeInt(SAVE_VERSION);
            out.writeLong(seed);

            out.writeBoolean(hasSavedRespawnPosition);
            if (hasSavedRespawnPosition) {
                out.writeFloat(savedRespawnPosition.x);
                out.writeFloat(savedRespawnPosition.y);
                out.writeFloat(savedRespawnPosition.z);
            }

            out.writeInt(chunks.size());

            for (Chunk chunk : chunks.values()) {
                out.writeInt(chunk.chunkX());
                out.writeInt(chunk.chunkZ());

                short[] data = chunk.rawBlocks();
                out.writeInt(data.length);
                for (short value : data) {
                    out.writeShort(value);
                }

                // 存完後，這個 Chunk 就不再視為未儲存修改。
                chunk.clearModified();
            }

            respawnPositionDirty = false;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save world to " + worldFile, e);
        }
    }

    // 從存檔讀取世界資料。
    // 讀取成功回傳 true，失敗回傳 false；呼叫端會用 false 建立新世界，避免半讀取狀態留下來。
    private boolean load() {
        if (!Files.exists(worldFile)) {
            return false;
        }

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(worldFile)))) {
            int magic = in.readInt();
            int version = in.readInt();

            if (magic != SAVE_MAGIC || (version != 1 && version != SAVE_VERSION)) {
                return false;
            }

            seed = in.readLong();
            terrainGenerator = new TerrainGenerator(seed, 62);

            // 版本 2 之後才有重生點資料。
            if (version >= 2) {
                hasSavedRespawnPosition = in.readBoolean();
                if (hasSavedRespawnPosition) {
                    savedRespawnPosition.set(in.readFloat(), in.readFloat(), in.readFloat());
                } else {
                    savedRespawnPosition.zero();
                }
            } else {
                clearSavedRespawnPosition();
            }

            respawnPositionDirty = false;

            int count = in.readInt();
            Map<ChunkPos, Chunk> loadedChunks = new HashMap<>();

            for (int i = 0; i < count; i++) {
                int chunkX = in.readInt();
                int chunkZ = in.readInt();
                int length = in.readInt();

                Chunk chunk = new Chunk(chunkX, chunkZ);
                short[] data = chunk.rawBlocks();

                if (length != data.length) {
                    return false;
                }

                for (int j = 0; j < length; j++) {
                    data[j] = in.readShort();
                }

                // 讀入後需要重建 mesh，但不算新的修改，否則每次啟動後都會立刻要求重存所有 Chunk。
                chunk.markMeshDirty();
                chunk.clearModified();
                loadedChunks.put(new ChunkPos(chunkX, chunkZ), chunk);
            }

            chunks.clear();
            chunks.putAll(loadedChunks);
            respawnPositionDirty = false;
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // 清除已儲存的重生點資料。
    private void clearSavedRespawnPosition() {
        savedRespawnPosition.zero();
        hasSavedRespawnPosition = false;
        respawnPositionDirty = false;
    }

    // 比 Math.floor 更快地把 float 轉成向下取整的整數。
    private int fastFloor(float value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }
}

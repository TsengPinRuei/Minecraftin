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
import java.util.concurrent.ConcurrentHashMap;

// 負責管理整個世界的區塊、方塊資料、出生點、射線檢測，以及存檔與讀檔。
// 這個類別是「世界狀態」的邊界：會生成 Chunk、標記 mesh dirty、追蹤需要持久化的變更。
public final class World {

    // 存檔檔頭，用來確認這是不是本遊戲建立的存檔；值為 "MCLN" 的整數形式。
    private static final int SAVE_MAGIC = 0x4D434C4E;

    // 目前存檔格式版本；load() 仍接受版本 1 與 2，舊存檔缺少的欄位會用預設值。
    // 版本 3 新增世界時間。
    private static final int SAVE_VERSION = 3;

    // 新世界的初始時間：清晨剛過日出。
    private static final float DEFAULT_TIME_OF_DAY = 0.03f;

    // 玩家出生時，腳底上方的偏移量，避免卡進地面。
    private static final float SPAWN_Y_OFFSET = 1.05f;

    // 水流模擬的 tick 間隔；Minecraft 的水每 5 game tick（0.25 秒）更新一次。
    private static final float WATER_TICK_SECONDS = 0.25f;

    // 每個 tick 最多處理多少格水，避免大規模改動造成單幀卡頓；剩餘格子留到下一個 tick。
    private static final int WATER_MAX_CELLS_PER_TICK = 4096;

    // 排程佇列的上限，防止極端情況下無限堆積。
    private static final int WATER_MAX_SCHEDULED_CELLS = 65536;

    // 搜尋大片森林出生區域時，最遠搜尋半徑。
    private static final int FOREST_SPAWN_SEARCH_MAX_RADIUS = 1536;

    // 搜尋大片森林時，每次移動的距離。
    private static final int FOREST_SPAWN_SEARCH_STEP = 64;

    // 找到森林中心後，在附近細部搜尋安全出生點的最大半徑。
    private static final int FOREST_LOCAL_SPAWN_RADIUS = 56;

    // 在森林中心附近搜尋出生點時，每次移動的距離。
    private static final int FOREST_LOCAL_SPAWN_STEP = 8;

    // 已載入的所有 Chunk，key 是 Chunk 座標。
    // 非同步 meshing 的工作執行緒會同時讀取這張表，因此必須使用 ConcurrentHashMap。
    private final Map<ChunkPos, Chunk> chunks = new ConcurrentHashMap<>();

    // 維護天空光與方塊光的引擎；區塊載入與方塊變更時負責增量更新。
    private final LightEngine lightEngine = new LightEngine(this);

    // 世界時間，0 到 1 為一天；0 是日出、0.25 是正午、0.5 是日落。
    private float timeOfDay = DEFAULT_TIME_OF_DAY;

    // 待更新的水格排程（去重、保留插入順序）；方塊變更會把自己與鄰居排進來，由固定 tick 處理。
    private final java.util.LinkedHashSet<Long> scheduledWaterCells = new java.util.LinkedHashSet<>();

    // 水流 tick 每次最多處理固定數量的格子；重複使用批次陣列，避免每個 tick 都配置 long[]。
    private final long[] waterTickBatch = new long[WATER_MAX_CELLS_PER_TICK];

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

    // 水流 tick 累積時間。
    private float waterTickTimer;

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
            timeOfDay = DEFAULT_TIME_OF_DAY;
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

    // 更新世界中的非玩家即時狀態：世界時間與水流模擬。
    public void update(float deltaSeconds) {
        if (deltaSeconds > 0.0f) {
            timeOfDay = (timeOfDay + deltaSeconds / GameConfig.DAY_LENGTH_SECONDS) % 1.0f;
        }
        updateWaterSimulation(deltaSeconds);
    }

    // 回傳目前世界時間（0 到 1）。
    public float timeOfDay() {
        return timeOfDay;
    }

    // 取得世界座標的天空光強度（0 到 15）。
    public int skyLightAt(int x, int y, int z) {
        return lightEngine.skyLightAt(x, y, z);
    }

    // 取得世界座標的方塊光強度（0 到 15）。
    public int blockLightAt(int x, int y, int z) {
        return lightEngine.blockLightAt(x, y, z);
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

        // 計算新 Chunk 的初始光照，並與已載入鄰居互相傳播。
        lightEngine.onChunkLoaded(chunk);

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

        int chunkX = chunkCoord(worldX);
        int chunkZ = chunkCoord(worldZ);
        int localX = localCoord(worldX, chunkX);
        int localZ = localCoord(worldZ, chunkZ);

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

        int chunkX = chunkCoord(worldX);
        int chunkZ = chunkCoord(worldZ);
        int localX = localCoord(worldX, chunkX);
        int localZ = localCoord(worldZ, chunkZ);

        Chunk chunk = getChunkIfLoaded(chunkX, chunkZ);
        if (chunk == null) {
            return BlockType.AIR;
        }
        return chunk.get(localX, y, localZ);
    }

    // 設定世界座標上的方塊。
    // 水流行為不需要特例：setBlockInternal 會把鄰近水格排進水流模擬，
    // 破壞方塊時水自然流入、擋住水源時失去支撐的流動水會自動退去。
    public boolean setBlock(int worldX, int y, int worldZ, BlockType type) {
        return setBlockInternal(worldX, y, worldZ, type, true);
    }

    // 實際執行方塊更新的內部方法。
    // createMissingChunk 為 true 時，缺少 Chunk 會自動建立。
    private boolean setBlockInternal(int worldX, int y, int worldZ, BlockType type, boolean createMissingChunk) {
        if (y < 0 || y >= GameConfig.CHUNK_HEIGHT) {
            return false;
        }

        int chunkX = chunkCoord(worldX);
        int chunkZ = chunkCoord(worldZ);
        int localX = localCoord(worldX, chunkX);
        int localZ = localCoord(worldZ, chunkZ);

        Chunk chunk = createMissingChunk ? getOrCreateChunk(chunkX, chunkZ) : getChunkIfLoaded(chunkX, chunkZ);
        if (chunk == null) {
            return false;
        }

        BlockType existing = chunk.get(localX, y, localZ);
        if (existing == type) {
            return false;
        }

        chunk.set(localX, y, localZ, type);

        // 增量更新光照；引擎會把光照變化波及的 Chunk 標記為需要重建 mesh。
        lightEngine.onBlockChanged(worldX, y, worldZ, type);

        // 任何方塊變更都喚醒自己與六個鄰格的水流更新；水流模擬本身的寫入也靠這裡形成連鎖。
        scheduleWaterNeighborhood(worldX, y, worldZ);

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

    // ====== 水流模擬 ======
    // 模型與 Minecraft 相同：WATER 是水源（強度 8），WATER_FLOW_7..1 是流動水。
    // 每個 tick 重算排程格子的強度（失去支撐就退去），再向外擴散（向下優先、水平遞減）。

    // 依累積時間推進水流 tick；一次最多補跑 4 個 tick，避免卡頓後爆量。
    private void updateWaterSimulation(float deltaSeconds) {
        if (scheduledWaterCells.isEmpty()) {
            waterTickTimer = 0.0f;
            return;
        }

        waterTickTimer += Math.max(0.0f, deltaSeconds);

        int ticks = 0;
        while (waterTickTimer >= WATER_TICK_SECONDS && ticks < 4) {
            waterTickTimer -= WATER_TICK_SECONDS;
            ticks++;
            runWaterTick();
        }
    }

    // 處理一個水流 tick：取出目前排程的格子逐一更新。
    // 更新過程排入的新格子留到下一個 tick，水才會一格一格地往外流。
    private void runWaterTick() {
        int count = Math.min(scheduledWaterCells.size(), WATER_MAX_CELLS_PER_TICK);
        if (count == 0) {
            return;
        }

        var iterator = scheduledWaterCells.iterator();
        for (int i = 0; i < count; i++) {
            waterTickBatch[i] = iterator.next();
            iterator.remove();
        }

        for (int i = 0; i < count; i++) {
            long packed = waterTickBatch[i];
            updateWaterCell(unpackWaterX(packed), unpackWaterY(packed), unpackWaterZ(packed));
        }
    }

    // 更新單一水格：流動水先依鄰居重算強度，然後嘗試向外流。
    private void updateWaterCell(int x, int y, int z) {
        BlockType block = peekBlock(x, y, z);
        if (!block.isWaterBlock()) {
            return;
        }

        int strength = block.waterStrength();

        // 流動水沒有自己的水量，強度完全由支撐決定；支撐消失就降級直到退成空氣。
        if (strength < 8) {
            int desired = computeWaterStrength(x, y, z);
            if (desired != strength) {
                BlockType next = desired <= 0 ? BlockType.AIR
                        : desired >= 8 ? BlockType.WATER : BlockType.flowingWaterOfStrength(desired);
                setBlockInternal(x, y, z, next, false);
                if (desired <= 0) {
                    return;
                }
                strength = desired;
            }
        }

        spreadWater(x, y, z, strength);
    }

    // 依鄰居計算一格流動水應有的強度。
    private int computeWaterStrength(int x, int y, int z) {
        // 上方有水：垂直落水保持高強度。
        if (y + 1 < GameConfig.CHUNK_HEIGHT && peekBlock(x, y + 1, z).isWaterBlock()) {
            return 7;
        }

        BlockType east = peekBlock(x + 1, y, z);
        BlockType west = peekBlock(x - 1, y, z);
        BlockType south = peekBlock(x, y, z + 1);
        BlockType north = peekBlock(x, y, z - 1);

        int bestHorizontal = Math.max(
                Math.max(east.waterStrength(), west.waterStrength()),
                Math.max(south.waterStrength(), north.waterStrength()));
        int sourceNeighbors = 0;
        sourceNeighbors += east == BlockType.WATER ? 1 : 0;
        sourceNeighbors += west == BlockType.WATER ? 1 : 0;
        sourceNeighbors += south == BlockType.WATER ? 1 : 0;
        sourceNeighbors += north == BlockType.WATER ? 1 : 0;

        // 無限水源規則：兩個以上水源相鄰且下方有支撐，這一格升級為新的水源。
        if (sourceNeighbors >= 2) {
            BlockType below = peekBlock(x, y - 1, z);
            if (below.isSolid() || below == BlockType.WATER) {
                return 8;
            }
        }

        return Math.min(7, bestHorizontal - 1);
    }

    // 把水向外推：能往下流就往下；瀑布中段不水平攤開，只有水源或落在地面/水面上的水才向四周擴散。
    private void spreadWater(int x, int y, int z, int strength) {
        BlockType below = y > 0 ? peekBlock(x, y - 1, z) : BlockType.BEDROCK;

        if (canWaterFlowInto(below, 7)) {
            setBlockInternal(x, y - 1, z, BlockType.flowingWaterOfStrength(7), false);
            return;
        }

        // 只有「落定」的水會水平擴散：水源、踩在實心方塊上，或浮在水源面上（瀑布落入水池的那一層）。
        boolean landed = strength == 8 || below.isSolid() || below == BlockType.WATER;
        if (!landed || strength <= 1) {
            return;
        }

        int spreadStrength = Math.min(7, strength - 1);
        trySpreadWaterTo(x + 1, y, z, spreadStrength);
        trySpreadWaterTo(x - 1, y, z, spreadStrength);
        trySpreadWaterTo(x, y, z + 1, spreadStrength);
        trySpreadWaterTo(x, y, z - 1, spreadStrength);
    }

    private void trySpreadWaterTo(int x, int y, int z, int strength) {
        if (canWaterFlowInto(peekBlock(x, y, z), strength)) {
            setBlockInternal(x, y, z, BlockType.flowingWaterOfStrength(strength), false);
        }
    }

    // 水能否流入：空氣可以，較弱的流動水會被蓋掉；不會吞掉火把、門這類非完整方塊。
    private boolean canWaterFlowInto(BlockType target, int incomingStrength) {
        if (target == BlockType.AIR) {
            return true;
        }
        return target.isFlowingWater() && target.waterStrength() < incomingStrength;
    }

    // 把一格與它的六個鄰格排入水流更新；setBlockInternal 在每次方塊變更後呼叫。
    private void scheduleWaterNeighborhood(int x, int y, int z) {
        scheduleWaterCell(x, y, z);
        scheduleWaterCell(x + 1, y, z);
        scheduleWaterCell(x - 1, y, z);
        scheduleWaterCell(x, y + 1, z);
        scheduleWaterCell(x, y - 1, z);
        scheduleWaterCell(x, y, z + 1);
        scheduleWaterCell(x, y, z - 1);
    }

    private void scheduleWaterCell(int x, int y, int z) {
        if (y < 0 || y >= GameConfig.CHUNK_HEIGHT || scheduledWaterCells.size() >= WATER_MAX_SCHEDULED_CELLS) {
            return;
        }
        scheduledWaterCells.add(packWaterCell(x, y, z));
    }

    // 將座標壓進一個 long：x 與 z 各 26 bits（二補數含符號）、y 12 bits。
    // 這只給短期水流排程使用；unpack 依賴位移做符號還原，不能拿來存放任意大座標。
    private static long packWaterCell(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    private static int unpackWaterX(long packed) {
        return (int) (packed >> 38);
    }

    private static int unpackWaterZ(long packed) {
        return (int) (packed << 26 >> 38);
    }

    private static int unpackWaterY(long packed) {
        return (int) (packed & 0xFFF);
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
        int chunkX = chunkCoord(worldX);
        int chunkZ = chunkCoord(worldZ);
        int localX = localCoord(worldX, chunkX);
        int localZ = localCoord(worldZ, chunkZ);
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
        int y = topSolidY(x, z);
        if (!isSafeSpawnAt(x, y, z)) {
            return false;
        }

        out.set(x + 0.5f, y + SPAWN_Y_OFFSET, z + 0.5f);
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
        if (!isSafeSpawnAt(x, y, z)) {
            return false;
        }

        out.set(x + 0.5f, y + SPAWN_Y_OFFSET, z + 0.5f);
        return true;
    }

    // 共用安全出生點判定，避免森林可見性檢查先 trySpawnAt 後又重掃一次地表高度。
    private boolean isSafeSpawnAt(int x, int y, int z) {
        if (y <= seaLevel()) {
            return false;
        }

        BlockType floor = getBlock(x, y, z);
        if (!floor.isSolid() || floor == BlockType.LEAVES || floor == BlockType.WATER) {
            return false;
        }

        BlockType feet = getBlock(x, y + 1, z);
        BlockType head = getBlock(x, y + 2, z);
        return feet == BlockType.AIR && head == BlockType.AIR;
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

        // 使用類似 3D DDA 的方式，沿著方塊格子一步一步前進；水（含流動水）不會被準星選中。
        while (traveled <= maxDistance) {
            BlockType block = getBlock(x, y, z);
            if (block != BlockType.AIR && !block.isWaterBlock()) {
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

            // 版本 3 起保存世界時間。
            out.writeFloat(timeOfDay);

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

            if (magic != SAVE_MAGIC || version < 1 || version > SAVE_VERSION) {
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

            // 版本 3 之後才有世界時間資料。
            if (version >= 3) {
                float loadedTime = in.readFloat();
                timeOfDay = (loadedTime >= 0.0f && loadedTime < 1.0f) ? loadedTime : DEFAULT_TIME_OF_DAY;
            } else {
                timeOfDay = DEFAULT_TIME_OF_DAY;
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

            // 全部 Chunk 就位後再計算光照，鄰居邊界的光才能正確互相流入。
            for (Chunk chunk : chunks.values()) {
                lightEngine.onChunkLoaded(chunk);
            }

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

    // 世界座標轉 Chunk 座標；集中使用同一套換算，避免熱路徑重複 floorDiv/floorMod。
    static int chunkCoord(int worldCoord) {
        return Math.floorDiv(worldCoord, GameConfig.CHUNK_SIZE);
    }

    // chunkCoord 已用 floorDiv 處理負座標；這裡用減法得到 0..CHUNK_SIZE-1，等價 floorMod 但少一次除法。
    static int localCoord(int worldCoord, int chunkCoord) {
        return worldCoord - chunkCoord * GameConfig.CHUNK_SIZE;
    }

    // 比 Math.floor 更快地把 float 轉成向下取整的整數。
    private int fastFloor(float value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }
}

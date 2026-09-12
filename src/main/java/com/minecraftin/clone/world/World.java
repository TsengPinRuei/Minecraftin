package com.minecraftin.clone.world;

import com.minecraftin.clone.config.GameConfig;
import org.joml.Vector3f;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.Map;

// 負責管理整個世界的區塊、方塊資料、出生點、射線檢測，以及存檔與讀檔。
// 這個類別是「世界狀態」的邊界：會生成 Chunk、標記 mesh dirty、追蹤需要持久化的變更。
public final class World {

    // 存檔檔頭，用來確認這是不是本遊戲建立的存檔；值為 "MCLN" 的整數形式。
    private static final int SAVE_MAGIC = 0x4D434C4E;

    // 寫出版本 4，仍可讀取版本 1 到 3；欄位演進見 save()。
    private static final int SAVE_VERSION = 4;

    // 新世界的初始時間：清晨剛過日出。
    private static final float DEFAULT_TIME_OF_DAY = 0.03f;

    // 玩家出生時，腳底上方的偏移量，避免卡進地面。
    private static final float SPAWN_Y_OFFSET = 1.05f;

    // 水流模擬的 tick 間隔；Minecraft 的水每 5 game tick（0.25 秒）更新一次。
    private static final float WATER_TICK_SECONDS = 0.25f;

    // 每個 tick 最多處理多少格水，避免大規模改動造成單幀卡頓；剩餘格子留到下一個 tick。
    private static final int WATER_MAX_CELLS_PER_TICK = 4096;

    // 搜尋大片森林出生區域時，最遠搜尋半徑。
    private static final int FOREST_SPAWN_SEARCH_MAX_RADIUS = 1536;

    // 搜尋大片森林時，每次移動的距離。
    private static final int FOREST_SPAWN_SEARCH_STEP = 64;

    // 找到森林中心後，在附近細部搜尋安全出生點的最大半徑。
    private static final int FOREST_LOCAL_SPAWN_RADIUS = 56;

    // 在森林中心附近搜尋出生點時，每次移動的距離。
    private static final int FOREST_LOCAL_SPAWN_STEP = 8;

    // 已載入的所有 Chunk，key 是 Chunk 座標。
    // World 由遊戲執行緒存取；背景 mesher 只讀取 ChunkSnapshot。
    private final Map<ChunkPos, Chunk> chunks = new HashMap<>();

    // 維護天空光與方塊光的引擎；區塊載入與方塊變更時負責增量更新。
    private final LightEngine lightEngine = new LightEngine(this);

    // 世界時間，0 到 1 為一天；0 是日出、0.25 是正午、0.5 是日落。
    private float timeOfDay = DEFAULT_TIME_OF_DAY;

    // 待更新的水格排程（去重、保留插入順序）；方塊變更會把自己與鄰居排進來，由固定 tick 處理。
    private final LinkedHashSet<Long> scheduledWaterCells = new LinkedHashSet<>();

    // 水流 tick 每次最多處理固定數量的格子；重複使用批次陣列，避免每個 tick 都配置 long[]。
    private final long[] waterTickBatch = new long[WATER_MAX_CELLS_PER_TICK];

    private final Path worldFile;

    private long seed;

    private TerrainGenerator terrainGenerator;

    private final Vector3f savedRespawnPosition = new Vector3f();

    private boolean hasSavedRespawnPosition;

    private boolean respawnPositionDirty;

    private float waterTickTimer;

    public World(Path worldFile, long defaultSeed) {
        this.worldFile = worldFile;
        this.seed = defaultSeed;
        this.terrainGenerator = new TerrainGenerator(seed, 62);
    }

    // 只有缺少存檔時建立新世界；讀檔錯誤交給呼叫端，避免覆蓋既有資料。
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

    public long seed() {
        return seed;
    }

    public int seaLevel() {
        return terrainGenerator.seaLevel();
    }

    public void update(float deltaSeconds) {
        if (deltaSeconds > 0.0f) {
            timeOfDay = (timeOfDay + deltaSeconds / GameConfig.DAY_LENGTH_SECONDS) % 1.0f;
        }
        updateWaterSimulation(deltaSeconds);
    }

    public float timeOfDay() {
        return timeOfDay;
    }

    public int skyLightAt(int x, int y, int z) {
        return lightEngine.skyLightAt(x, y, z);
    }

    public int blockLightAt(int x, int y, int z) {
        return lightEngine.blockLightAt(x, y, z);
    }

    public int chunkCount() {
        return chunks.size();
    }

    public boolean hasModifiedChunks() {
        for (Chunk chunk : chunks.values()) {
            if (chunk.isModified()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPendingSave() {
        return respawnPositionDirty || hasModifiedChunks();
    }

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

    public boolean tryGetSavedRespawnPosition(Vector3f out) {
        if (!hasSavedRespawnPosition) {
            return false;
        }
        out.set(savedRespawnPosition);
        return true;
    }

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
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                markChunkMeshDirty(chunkX + dx, chunkZ + dz);
            }
        }
        scheduleWaterBorders(chunk);

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
    // 光照與水流只看已載入狀態，避免查詢本身造成地形生成。
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

    // 水流行為不需要特例：setBlockInternal 會把鄰近水格排進水流模擬，
    // 破壞方塊時水自然流入、擋住水源時失去支撐的流動水會自動退去。
    public boolean setBlock(int worldX, int y, int worldZ, BlockType type) {
        return setBlockInternal(worldX, y, worldZ, type, true);
    }

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
        lightEngine.onBlockChanged(worldX, y, worldZ, existing, type);

        // 任何方塊變更都喚醒自己與六個鄰格的水流更新；水流模擬本身的寫入也靠這裡形成連鎖。
        scheduleWaterNeighborhood(worldX, y, worldZ);

        // 如果修改位置在 Chunk 邊界，鄰近 Chunk 的 mesh 也要重建。
        markNeighborMeshesDirty(chunk, localX, localZ);

        return true;
    }

    // ====== 水流模擬 ======
    // 簡化的水流模型：WATER 是水源（強度 8），WATER_FLOW_7..1 是流動水。
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
        if (y < 0 || y >= GameConfig.CHUNK_HEIGHT || !peekBlock(x, y, z).isWaterBlock()) {
            return;
        }
        scheduledWaterCells.add(packWaterCell(x, y, z));
    }

    // 將座標壓進一個 long：x 與 z 各 26 bits（二補數含符號）、y 12 bits。
    // v4 存檔也使用此格式；unpack 依賴位移做符號還原，不能拿來存放任意大座標。
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

    // AO 與平滑光照會取樣對角格，角落變動需同時更新對角 Chunk。
    void markNeighborMeshesDirty(Chunk chunk, int localX, int localZ) {
        int minDx = localX == 0 ? -1 : 0;
        int maxDx = localX == GameConfig.CHUNK_SIZE - 1 ? 1 : 0;
        int minDz = localZ == 0 ? -1 : 0;
        int maxDz = localZ == GameConfig.CHUNK_SIZE - 1 ? 1 : 0;
        for (int dz = minDz; dz <= maxDz; dz++) {
            for (int dx = minDx; dx <= maxDx; dx++) {
                if (dx != 0 || dz != 0) {
                    markChunkMeshDirty(chunk.chunkX() + dx, chunk.chunkZ() + dz);
                }
            }
        }
    }

    // 原本流向未載入區域的水，在邊界兩側到齊後才能繼續。
    private void scheduleWaterBorders(Chunk chunk) {
        scheduleWaterBorder(chunk, getChunkIfLoaded(chunk.chunkX() - 1, chunk.chunkZ()), -1, 0);
        scheduleWaterBorder(chunk, getChunkIfLoaded(chunk.chunkX() + 1, chunk.chunkZ()), 1, 0);
        scheduleWaterBorder(chunk, getChunkIfLoaded(chunk.chunkX(), chunk.chunkZ() - 1), 0, -1);
        scheduleWaterBorder(chunk, getChunkIfLoaded(chunk.chunkX(), chunk.chunkZ() + 1), 0, 1);
    }

    private void scheduleWaterBorder(Chunk chunk, Chunk neighbor, int dx, int dz) {
        if (neighbor == null) {
            return;
        }
        int last = GameConfig.CHUNK_SIZE - 1;
        for (int y = 0; y < GameConfig.CHUNK_HEIGHT; y++) {
            for (int i = 0; i < GameConfig.CHUNK_SIZE; i++) {
                int x = dx == 0 ? i : dx < 0 ? 0 : last;
                int z = dz == 0 ? i : dz < 0 ? 0 : last;
                int nx = dx == 0 ? i : last - x;
                int nz = dz == 0 ? i : last - z;
                if (chunk.get(x, y, z).isWaterBlock()) {
                    scheduledWaterCells.add(packWaterCell(chunk.worldMinX() + x, y, chunk.worldMinZ() + z));
                }
                if (neighbor.get(nx, y, nz).isWaterBlock()) {
                    scheduledWaterCells.add(packWaterCell(neighbor.worldMinX() + nx, y, neighbor.worldMinZ() + nz));
                }
            }
        }
    }

    private void markChunkMeshDirty(int chunkX, int chunkZ) {
        Chunk chunk = getChunkIfLoaded(chunkX, chunkZ);
        if (chunk != null) {
            chunk.markMeshDirty();
        }
    }

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

    // DDA 篩選候選格，再與實際幾何盒求交；半磚空隙不應擋住射線。
    public RaycastHit raycast(Vector3f origin, Vector3f direction, float maxDistance) {
        if (!origin.isFinite() || !direction.isFinite() || !Float.isFinite(maxDistance) || maxDistance < 0) {
            return null;
        }
        double length = Math.sqrt((double) direction.x * direction.x
                + (double) direction.y * direction.y + (double) direction.z * direction.z);
        if (length == 0) {
            return null;
        }
        float dx = (float) (direction.x / length);
        float dy = (float) (direction.y / length);
        float dz = (float) (direction.z / length);

        int x = fastFloor(origin.x);
        int y = fastFloor(origin.y);
        int z = fastFloor(origin.z);

        int stepX = dx > 0 ? 1 : (dx < 0 ? -1 : 0);
        int stepY = dy > 0 ? 1 : (dy < 0 ? -1 : 0);
        int stepZ = dz > 0 ? 1 : (dz < 0 ? -1 : 0);

        float invDx = dx == 0.0f ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dx);
        float invDy = dy == 0.0f ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dy);
        float invDz = dz == 0.0f ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dz);

        float tx = dx == 0.0f ? Float.POSITIVE_INFINITY
                : (dx > 0 ? (x + 1 - origin.x) * invDx : (origin.x - x) * invDx);
        float ty = dy == 0.0f ? Float.POSITIVE_INFINITY
                : (dy > 0 ? (y + 1 - origin.y) * invDy : (origin.y - y) * invDy);
        float tz = dz == 0.0f ? Float.POSITIVE_INFINITY
                : (dz > 0 ? (z + 1 - origin.z) * invDz : (origin.z - z) * invDz);

        float traveled = 0.0f;

        // 使用類似 3D DDA 的方式，沿著方塊格子一步一步前進；水（含流動水）不會被準星選中。
        while (traveled <= maxDistance) {
            BlockType block = getBlock(x, y, z);
            float segmentEnd = Math.min(maxDistance, Math.min(tx, Math.min(ty, tz)));
            RaycastHit hit = intersectBlock(x, y, z, block, origin, dx, dy, dz, segmentEnd);
            // 柵欄的盒子高於一格，因此也檢查射線目前格正下方的方塊。
            if (y > 0 && y <= GameConfig.CHUNK_HEIGHT) {
                RaycastHit below = intersectBlock(x, y - 1, z, getBlock(x, y - 1, z),
                        origin, dx, dy, dz, segmentEnd);
                if (below != null && (hit == null || below.distance() < hit.distance())) {
                    hit = below;
                }
            }
            if (hit != null) {
                return hit;
            }

            if (tx < ty) {
                if (tx < tz) {
                    x += stepX;
                    traveled = tx;
                    tx += invDx;
                } else {
                    z += stepZ;
                    traveled = tz;
                    tz += invDz;
                }
            } else {
                if (ty < tz) {
                    y += stepY;
                    traveled = ty;
                    ty += invDy;
                } else {
                    z += stepZ;
                    traveled = tz;
                    tz += invDz;
                }
            }
        }

        return null;
    }

    private static RaycastHit intersectBlock(int x, int y, int z, BlockType block, Vector3f origin,
            float dx, float dy, float dz, float maxDistance) {
        if (block == BlockType.AIR || block.isWaterBlock()) {
            return null;
        }
        RaycastHit closest = null;
        for (BlockBounds bounds : block.renderBoxes()) {
            float near = 0.0f;
            float far = maxDistance;
            int normalAxis = -1;
            int normalSign = 0;
            boolean missed = false;
            for (int axis = 0; axis < 3; axis++) {
                float start = axis == 0 ? origin.x : axis == 1 ? origin.y : origin.z;
                float direction = axis == 0 ? dx : axis == 1 ? dy : dz;
                float min = axis == 0 ? x + bounds.minX() : axis == 1 ? y + bounds.minY() : z + bounds.minZ();
                float max = axis == 0 ? x + bounds.maxX() : axis == 1 ? y + bounds.maxY() : z + bounds.maxZ();
                if (direction == 0.0f) {
                    if (start < min || start > max) {
                        missed = true;
                        break;
                    }
                    continue;
                }
                float entry = (min - start) / direction;
                float exit = (max - start) / direction;
                float axisNear = Math.min(entry, exit);
                if (axisNear > near) {
                    near = axisNear;
                    normalAxis = axis;
                    normalSign = direction > 0 ? -1 : 1;
                }
                far = Math.min(far, Math.max(entry, exit));
                if (near > far) {
                    missed = true;
                    break;
                }
            }
            if (!missed && (closest == null || near < closest.distance())) {
                closest = new RaycastHit(x, y, z,
                        normalAxis == 0 ? normalSign : 0,
                        normalAxis == 1 ? normalSign : 0,
                        normalAxis == 2 ? normalSign : 0, near, block);
            }
        }
        return closest;
    }

    // v1: seed/chunks；v2: respawn；v3: time；v4: 待處理水流與 tick 計時。
    // 先完成暫存檔、flush/force/close，再原子替換；替換失敗時保留原檔和 dirty 狀態。
    public void save() {
        Path target = worldFile.toAbsolutePath();
        Path temporary = null;
        try {
            Files.createDirectories(target.getParent());
            temporary = Files.createTempFile(target.getParent(), "world-", ".tmp");
            try (DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(temporary)))) {
                out.writeInt(SAVE_MAGIC);
                out.writeInt(SAVE_VERSION);
                out.writeLong(seed);
                out.writeBoolean(hasSavedRespawnPosition);
                if (hasSavedRespawnPosition) {
                    out.writeFloat(savedRespawnPosition.x);
                    out.writeFloat(savedRespawnPosition.y);
                    out.writeFloat(savedRespawnPosition.z);
                }
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
                }
                out.writeFloat(waterTickTimer);
                out.writeInt(scheduledWaterCells.size());
                for (long cell : scheduledWaterCells) {
                    out.writeLong(cell);
                }
            }
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            temporary = null;
            for (Chunk chunk : chunks.values()) {
                chunk.clearModified();
            }
            respawnPositionDirty = false;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save world to " + worldFile, e);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException e) {
                    System.err.println("Failed to remove temporary save " + temporary + ": " + e.getMessage());
                }
            }
        }
    }

    // 只有檔案不存在才建立新世界。損壞、不支援或不可讀的存檔必須停止載入，避免離開時覆蓋原始資料。
    private boolean load() {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(worldFile)))) {
            long fileSize = Files.size(worldFile);
            int magic = in.readInt();
            int version = in.readInt();
            if (magic != SAVE_MAGIC || version < 1 || version > SAVE_VERSION) {
                throw new IOException("Invalid save header or unsupported version: " + version);
            }

            long loadedSeed = in.readLong();
            boolean loadedRespawn = version >= 2 && in.readBoolean();
            Vector3f loadedPosition = new Vector3f();
            if (loadedRespawn) {
                loadedPosition.set(in.readFloat(), in.readFloat(), in.readFloat());
                if (!loadedPosition.isFinite()) {
                    throw new IOException("Invalid respawn position");
                }
            }
            float loadedTime = version >= 3 ? in.readFloat() : DEFAULT_TIME_OF_DAY;
            if (!(loadedTime >= 0.0f && loadedTime < 1.0f)) {
                throw new IOException("Invalid world time");
            }

            int blockCount = GameConfig.CHUNK_SIZE * GameConfig.CHUNK_HEIGHT * GameConfig.CHUNK_SIZE;
            int count = in.readInt();
            if (count < 0 || count > fileSize / (12L + blockCount * Short.BYTES)) {
                throw new IOException("Invalid chunk count: " + count);
            }
            Map<ChunkPos, Chunk> loadedChunks = new HashMap<>();
            for (int i = 0; i < count; i++) {
                int chunkX = in.readInt();
                int chunkZ = in.readInt();
                int length = in.readInt();
                if (length != blockCount) {
                    throw new IOException("Invalid chunk size: " + length);
                }
                Chunk chunk = new Chunk(chunkX, chunkZ);
                short[] data = chunk.rawBlocks();
                for (int j = 0; j < length; j++) {
                    short id = in.readShort();
                    if (BlockType.byId(id).id() != id) {
                        throw new IOException("Unknown block ID: " + id);
                    }
                    data[j] = id;
                }
                if (loadedChunks.putIfAbsent(chunk.pos(), chunk) != null) {
                    throw new IOException("Duplicate chunk: " + chunk.pos());
                }
            }

            LinkedHashSet<Long> loadedWater = new LinkedHashSet<>();
            float loadedWaterTimer = 0.0f;
            if (version >= 4) {
                loadedWaterTimer = in.readFloat();
                if (!Float.isFinite(loadedWaterTimer) || loadedWaterTimer < 0.0f) {
                    throw new IOException("Invalid water tick timer");
                }
                int waterCount = in.readInt();
                if (waterCount < 0 || waterCount > fileSize / Long.BYTES) {
                    throw new IOException("Invalid water queue size: " + waterCount);
                }
                for (int i = 0; i < waterCount; i++) {
                    long cell = in.readLong();
                    if (unpackWaterY(cell) >= GameConfig.CHUNK_HEIGHT) {
                        throw new IOException("Invalid water cell height");
                    }
                    loadedWater.add(cell);
                }
            }
            if (in.read() != -1) {
                throw new IOException("Unexpected data after world save");
            }

            // 全檔驗證完成後才更新世界；光照重建需要所有 Chunk 先到齊。
            seed = loadedSeed;
            terrainGenerator = new TerrainGenerator(seed, 62);
            savedRespawnPosition.set(loadedPosition);
            hasSavedRespawnPosition = loadedRespawn;
            respawnPositionDirty = false;
            timeOfDay = loadedTime;
            chunks.clear();
            chunks.putAll(loadedChunks);
            scheduledWaterCells.clear();
            scheduledWaterCells.addAll(loadedWater);
            waterTickTimer = loadedWaterTimer;
            for (Chunk chunk : chunks.values()) {
                lightEngine.onChunkLoaded(chunk);
            }
            if (version < 4) {
                restoreLegacyWaterSimulation();
            }
            return true;
        } catch (NoSuchFileException e) {
            return false;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load world from " + worldFile, e);
        }
    }

    // 舊版未記錄排程，只恢復流動水與仍可流出的水源，避免把整片靜止海水排入佇列。
    private void restoreLegacyWaterSimulation() {
        for (Chunk chunk : chunks.values()) {
            for (int y = 0; y < GameConfig.CHUNK_HEIGHT; y++) {
                for (int z = 0; z < GameConfig.CHUNK_SIZE; z++) {
                    for (int x = 0; x < GameConfig.CHUNK_SIZE; x++) {
                        BlockType block = chunk.get(x, y, z);
                        if (!block.isWaterBlock()) {
                            continue;
                        }
                        int wx = chunk.worldMinX() + x;
                        int wz = chunk.worldMinZ() + z;
                        if (block.isFlowingWater()
                                || canWaterFlowInto(peekBlock(wx, y - 1, wz), 7)
                                || canWaterFlowInto(peekBlock(wx + 1, y, wz), 7)
                                || canWaterFlowInto(peekBlock(wx - 1, y, wz), 7)
                                || canWaterFlowInto(peekBlock(wx, y, wz + 1), 7)
                                || canWaterFlowInto(peekBlock(wx, y, wz - 1), 7)) {
                            scheduleWaterCell(wx, y, wz);
                        }
                    }
                }
            }
        }
    }

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

    private int fastFloor(float value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }
}

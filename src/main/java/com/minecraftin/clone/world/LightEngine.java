package com.minecraftin.clone.world;

import com.minecraftin.clone.config.GameConfig;
import com.minecraftin.clone.util.LongQueue;

// 與 Minecraft 同款的增量光照引擎：天空光與方塊光兩個通道，各 0 到 15。
// 新增光源用 BFS 擴散，移除光源用「先拆除再從邊界重新擴散」的 removal BFS，
// 只更新真正受影響的格子，並把被改到的 Chunk 標記為需要重建 mesh。
public final class LightEngine {

    public static final int MAX_LIGHT = 15;

    // 兩個光照通道。
    private static final int CHANNEL_SKY = 0;
    private static final int CHANNEL_BLOCK = 1;

    // 六個傳播方向；索引 1 是「往下」，天空光直射的特殊規則需要辨識它。
    private static final int[][] DIRECTIONS = {
            { 0, 1, 0 },
            { 0, -1, 0 },
            { -1, 0, 0 },
            { 1, 0, 0 },
            { 0, 0, -1 },
            { 0, 0, 1 }
    };
    private static final int DIRECTION_DOWN = 1;

    private final World world;

    // BFS 佇列重複使用，避免每次更新都重新配置。
    private final LongQueue spreadQueue = new LongQueue(4096);
    private final LongQueue removalQueue = new LongQueue(1024);
    private final LongQueue neighborSeedQueue = new LongQueue(1024);
    private final LongQueue blockSeedQueue = new LongQueue(1024);

    public LightEngine(World world) {
        this.world = world;
    }

    // 新 Chunk 載入（生成或讀檔）後計算初始光照：
    // 直射天空光、本身的發光方塊，以及從已載入鄰居流入的光；同時把光擴散回鄰居。
    public void onChunkLoaded(Chunk chunk) {
        int minX = chunk.worldMinX();
        int minZ = chunk.worldMinZ();

        // 天空光直射：從最高處往下，被不透明方塊擋住後歸零，水與樹葉逐步衰減。
        for (int z = 0; z < GameConfig.CHUNK_SIZE; z++) {
            for (int x = 0; x < GameConfig.CHUNK_SIZE; x++) {
                int level = MAX_LIGHT;
                for (int y = GameConfig.CHUNK_HEIGHT - 1; y >= 0; y--) {
                    if (level > 0) {
                        level = Math.max(0, level - chunk.get(x, y, z).lightOpacity());
                    }
                    chunk.setSkyLight(x, y, z, level);
                }
            }
        }

        // 把可往外擴散的光排入佇列：天空光所有非零格、發光方塊。
        for (int y = 0; y < GameConfig.CHUNK_HEIGHT; y++) {
            for (int z = 0; z < GameConfig.CHUNK_SIZE; z++) {
                for (int x = 0; x < GameConfig.CHUNK_SIZE; x++) {
                    int sky = chunk.skyLight(x, y, z);
                    if (sky > 1) {
                        spreadQueue.add(pack(minX + x, y, minZ + z));
                    }

                    int emission = chunk.get(x, y, z).lightEmission();
                    if (emission > 0) {
                        chunk.setBlockLight(x, y, z, emission);
                    }
                }
            }
        }
        spread(CHANNEL_SKY);

        for (int y = 0; y < GameConfig.CHUNK_HEIGHT; y++) {
            for (int z = 0; z < GameConfig.CHUNK_SIZE; z++) {
                for (int x = 0; x < GameConfig.CHUNK_SIZE; x++) {
                    if (chunk.blockLight(x, y, z) > 1) {
                        spreadQueue.add(pack(minX + x, y, minZ + z));
                    }
                }
            }
        }
        spread(CHANNEL_BLOCK);

        // 從已載入鄰居的邊界把光引進來。
        seedFromNeighborBorders(chunk);
    }

    // 方塊內容改變時的增量更新；World.setBlockInternal 在寫入後呼叫。
    public void onBlockChanged(int x, int y, int z, BlockType newBlock) {
        // 方塊光：先拆掉舊光（removal BFS 會把邊界光源排入 spreadQueue），再放入新光源。
        removeLight(CHANNEL_BLOCK, x, y, z);
        int emission = newBlock.lightEmission();
        if (emission > 0) {
            setLight(CHANNEL_BLOCK, x, y, z, emission);
            spreadQueue.add(pack(x, y, z));
        }
        enqueueNeighbors(x, y, z);
        spread(CHANNEL_BLOCK);

        // 天空光：同樣先拆除再從鄰居與上方重新擴散；「往下直射保持 15」規則會自動重建陽光柱。
        removeLight(CHANNEL_SKY, x, y, z);
        enqueueNeighbors(x, y, z);
        spread(CHANNEL_SKY);
    }

    // 取得世界座標的天空光；未載入區塊視為全亮，避免渲染邊緣一片黑。
    public int skyLightAt(int x, int y, int z) {
        if (y >= GameConfig.CHUNK_HEIGHT) {
            return MAX_LIGHT;
        }
        if (y < 0) {
            return 0;
        }
        Chunk chunk = chunkAt(x, z);
        if (chunk == null) {
            return MAX_LIGHT;
        }
        return chunk.skyLight(Math.floorMod(x, GameConfig.CHUNK_SIZE), y, Math.floorMod(z, GameConfig.CHUNK_SIZE));
    }

    // 取得世界座標的方塊光；未載入區塊視為無光。
    public int blockLightAt(int x, int y, int z) {
        if (y < 0 || y >= GameConfig.CHUNK_HEIGHT) {
            return 0;
        }
        Chunk chunk = chunkAt(x, z);
        if (chunk == null) {
            return 0;
        }
        return chunk.blockLight(Math.floorMod(x, GameConfig.CHUNK_SIZE), y, Math.floorMod(z, GameConfig.CHUNK_SIZE));
    }

    // 把鄰居 Chunk 邊界上一圈的光當作種子，讓既有世界的光流入新 Chunk。
    private void seedFromNeighborBorders(Chunk chunk) {
        int minX = chunk.worldMinX();
        int minZ = chunk.worldMinZ();
        int maxX = minX + GameConfig.CHUNK_SIZE - 1;
        int maxZ = minZ + GameConfig.CHUNK_SIZE - 1;

        // 先收集種子座標；spread() 會重新讀取當下亮度，所以同一份種子可供兩個通道使用。
        neighborSeedQueue.clear();
        blockSeedQueue.clear();
        for (int y = 0; y < GameConfig.CHUNK_HEIGHT; y++) {
            for (int x = minX; x <= maxX; x++) {
                enqueueIfLoaded(neighborSeedQueue, x, y, minZ - 1);
                enqueueIfLoaded(neighborSeedQueue, x, y, maxZ + 1);
            }
            for (int z = minZ; z <= maxZ; z++) {
                enqueueIfLoaded(neighborSeedQueue, minX - 1, y, z);
                enqueueIfLoaded(neighborSeedQueue, maxX + 1, y, z);
            }
        }

        while (!neighborSeedQueue.isEmpty()) {
            long value = neighborSeedQueue.poll();
            spreadQueue.add(value);
            blockSeedQueue.add(value);
        }
        spread(CHANNEL_SKY);

        while (!blockSeedQueue.isEmpty()) {
            spreadQueue.add(blockSeedQueue.poll());
        }
        spread(CHANNEL_BLOCK);
    }

    private void enqueueIfLoaded(LongQueue out, int x, int y, int z) {
        Chunk chunk = chunkAt(x, z);
        if (chunk == null) {
            return;
        }
        int localX = Math.floorMod(x, GameConfig.CHUNK_SIZE);
        int localZ = Math.floorMod(z, GameConfig.CHUNK_SIZE);
        if (chunk.skyLight(localX, y, localZ) > 1 || chunk.blockLight(localX, y, localZ) > 1) {
            out.add(pack(x, y, z));
        }
    }

    private void enqueueNeighbors(int x, int y, int z) {
        for (int[] dir : DIRECTIONS) {
            spreadQueue.add(pack(x + dir[0], y + dir[1], z + dir[2]));
        }
    }

    // 擴散 BFS：佇列存座標，處理時重新讀取目前亮度，因此重複入列不會出錯。
    private void spread(int channel) {
        while (!spreadQueue.isEmpty()) {
            long packed = spreadQueue.poll();
            int x = unpackX(packed);
            int y = unpackY(packed);
            int z = unpackZ(packed);

            // 亮度 1 以下不可能再讓任何鄰居變亮（衰減後歸零），直接略過。
            int level = getLight(channel, x, y, z);
            if (level <= 1) {
                continue;
            }

            for (int i = 0; i < DIRECTIONS.length; i++) {
                int nx = x + DIRECTIONS[i][0];
                int ny = y + DIRECTIONS[i][1];
                int nz = z + DIRECTIONS[i][2];

                if (ny < 0 || ny >= GameConfig.CHUNK_HEIGHT) {
                    continue;
                }

                Chunk chunk = chunkAt(nx, nz);
                if (chunk == null) {
                    continue;
                }

                int localX = Math.floorMod(nx, GameConfig.CHUNK_SIZE);
                int localZ = Math.floorMod(nz, GameConfig.CHUNK_SIZE);
                int opacity = chunk.get(localX, ny, localZ).lightOpacity();

                int target;
                if (channel == CHANNEL_SKY && i == DIRECTION_DOWN && level == MAX_LIGHT && opacity == 0) {
                    // 滿級天空光向下直射不衰減，這是陽光柱能照進垂直洞穴的關鍵。
                    target = MAX_LIGHT;
                } else {
                    target = level - 1 - opacity;
                }

                if (target <= 0) {
                    continue;
                }

                int current = channel == CHANNEL_SKY
                        ? chunk.skyLight(localX, ny, localZ)
                        : chunk.blockLight(localX, ny, localZ);
                if (target <= current) {
                    continue;
                }

                writeLight(channel, chunk, localX, ny, localZ, nx, nz, target);
                spreadQueue.add(pack(nx, ny, nz));
            }
        }
    }

    // 移除 BFS：把依賴指定格的光全部歸零，並把外圍仍有效的光源排入 spreadQueue 等待重新擴散。
    private void removeLight(int channel, int x, int y, int z) {
        int old = getLight(channel, x, y, z);
        setLight(channel, x, y, z, 0);
        if (old <= 0) {
            return;
        }

        removalQueue.clear();
        removalQueue.add(packWithLevel(x, y, z, old));

        while (!removalQueue.isEmpty()) {
            long packed = removalQueue.poll();
            int cx = unpackX(packed);
            int cy = unpackY(packed);
            int cz = unpackZ(packed);
            int level = unpackLevel(packed);

            for (int i = 0; i < DIRECTIONS.length; i++) {
                int nx = cx + DIRECTIONS[i][0];
                int ny = cy + DIRECTIONS[i][1];
                int nz = cz + DIRECTIONS[i][2];

                int neighborLevel = getLight(channel, nx, ny, nz);
                if (neighborLevel <= 0) {
                    continue;
                }

                boolean dependsOnRemoved = neighborLevel < level
                        || (channel == CHANNEL_SKY && i == DIRECTION_DOWN
                                && level == MAX_LIGHT && neighborLevel == MAX_LIGHT);

                if (dependsOnRemoved) {
                    setLight(channel, nx, ny, nz, 0);
                    removalQueue.add(packWithLevel(nx, ny, nz, neighborLevel));
                } else {
                    // 這格的光來自其他來源，之後由它把光擴散回被清空的區域。
                    spreadQueue.add(pack(nx, ny, nz));
                }
            }
        }
    }

    private int getLight(int channel, int x, int y, int z) {
        return channel == CHANNEL_SKY ? skyLightAtInternal(x, y, z) : blockLightAtInternal(x, y, z);
    }

    // 內部讀取不把未載入區塊當成全亮，避免 BFS 誤判邊界有光。
    private int skyLightAtInternal(int x, int y, int z) {
        if (y < 0 || y >= GameConfig.CHUNK_HEIGHT) {
            return 0;
        }
        Chunk chunk = chunkAt(x, z);
        if (chunk == null) {
            return 0;
        }
        return chunk.skyLight(Math.floorMod(x, GameConfig.CHUNK_SIZE), y, Math.floorMod(z, GameConfig.CHUNK_SIZE));
    }

    private int blockLightAtInternal(int x, int y, int z) {
        if (y < 0 || y >= GameConfig.CHUNK_HEIGHT) {
            return 0;
        }
        Chunk chunk = chunkAt(x, z);
        if (chunk == null) {
            return 0;
        }
        return chunk.blockLight(Math.floorMod(x, GameConfig.CHUNK_SIZE), y, Math.floorMod(z, GameConfig.CHUNK_SIZE));
    }

    private void setLight(int channel, int x, int y, int z, int value) {
        if (y < 0 || y >= GameConfig.CHUNK_HEIGHT) {
            return;
        }
        Chunk chunk = chunkAt(x, z);
        if (chunk == null) {
            return;
        }
        writeLight(channel, chunk, Math.floorMod(x, GameConfig.CHUNK_SIZE), y,
                Math.floorMod(z, GameConfig.CHUNK_SIZE), x, z, value);
    }

    // 寫入光照並標記受影響 Chunk 的 mesh；邊界格子也會影響鄰居 mesh 的取樣結果。
    private void writeLight(int channel, Chunk chunk, int localX, int y, int localZ, int worldX, int worldZ,
            int value) {
        boolean changed = channel == CHANNEL_SKY
                ? chunk.setSkyLight(localX, y, localZ, value)
                : chunk.setBlockLight(localX, y, localZ, value);
        if (!changed) {
            return;
        }

        chunk.markMeshDirty();

        // 平滑光照取樣會跨一格邊界，所以貼邊的光照變化也要重建鄰近 Chunk。
        if (localX == 0) {
            markMeshDirtyAt(worldX - 1, worldZ);
        } else if (localX == GameConfig.CHUNK_SIZE - 1) {
            markMeshDirtyAt(worldX + 1, worldZ);
        }
        if (localZ == 0) {
            markMeshDirtyAt(worldX, worldZ - 1);
        } else if (localZ == GameConfig.CHUNK_SIZE - 1) {
            markMeshDirtyAt(worldX, worldZ + 1);
        }
    }

    private void markMeshDirtyAt(int worldX, int worldZ) {
        Chunk chunk = chunkAt(worldX, worldZ);
        if (chunk != null) {
            chunk.markMeshDirty();
        }
    }

    private Chunk chunkAt(int worldX, int worldZ) {
        return world.getChunkIfLoaded(
                Math.floorDiv(worldX, GameConfig.CHUNK_SIZE),
                Math.floorDiv(worldZ, GameConfig.CHUNK_SIZE));
    }

    // 把座標壓進一個 long：x 與 z 各 24 bits（含符號）、y 8 bits、亮度 4 bits。
    private static long pack(int x, int y, int z) {
        return packWithLevel(x, y, z, 0);
    }

    private static long packWithLevel(int x, int y, int z, int level) {
        return ((long) (x & 0xFFFFFF) << 36)
                | ((long) (z & 0xFFFFFF) << 12)
                | ((long) (y & 0xFF) << 4)
                | (level & 0xF);
    }

    private static int unpackX(long packed) {
        return (int) (packed << 4 >> 40);
    }

    private static int unpackZ(long packed) {
        return (int) (packed << 28 >> 40);
    }

    private static int unpackY(long packed) {
        return (int) ((packed >> 4) & 0xFF);
    }

    private static int unpackLevel(long packed) {
        return (int) (packed & 0xF);
    }
}

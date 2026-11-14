package com.minecraftin.clone.world;

import com.minecraftin.clone.config.GameConfig;
import org.joml.Vector3f;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Own mutable world state on the game thread, coordinating generation, lighting, water, targeting,
// and persistence.
public final class World implements AutoCloseable {

    // The binary save magic is the ASCII sequence MCLN.
    private static final int SAVE_MAGIC = 0x4D434C4E;

    // Write version 4 and accept versions 1-4; save() documents the added fields.
    private static final int SAVE_VERSION = 4;

    // Start new worlds shortly after sunrise.
    private static final float DEFAULT_TIME_OF_DAY = 0.03f;

    // Place the player's feet 1.05 blocks above the floor cell's base: one block of height plus a
    // small clearance.
    private static final float SPAWN_Y_OFFSET = 1.05f;

    // Advance scheduled water every 0.25 simulation seconds.
    private static final float WATER_TICK_SECONDS = 0.25f;

    // Limit work per water tick; leave excess scheduled cells queued for later ticks.
    private static final int WATER_MAX_CELLS_PER_TICK = 4096;

    // Limit the coarse forest search radius in world blocks.
    private static final int FOREST_SPAWN_SEARCH_MAX_RADIUS = 1536;

    // Spacing between coarse forest search samples, in world blocks.
    private static final int FOREST_SPAWN_SEARCH_STEP = 64;

    // Limit the local safe-spawn search around the selected forest center.
    private static final int FOREST_LOCAL_SPAWN_RADIUS = 56;

    // Spacing between local safe-spawn candidates, in world blocks.
    private static final int FOREST_LOCAL_SPAWN_STEP = 8;

    // Access mutable chunks only on the game thread; background meshing reads ChunkSnapshot
    // instead.
    private final Map<ChunkPos, Chunk> chunks = new HashMap<>();

    private final LightEngine lightEngine = new LightEngine(this);

    // One day is [0, 1): sunrise at 0, noon at 0.25, and sunset at 0.5.
    private float timeOfDay = DEFAULT_TIME_OF_DAY;

    // Deduplicate scheduled water cells while preserving insertion order for repeatable tick
    // processing.
    private final LinkedHashSet<Long> scheduledWaterCells = new LinkedHashSet<>();

    // Reuse a bounded batch array so each water tick does not allocate a new one.
    private final long[] waterTickBatch = new long[WATER_MAX_CELLS_PER_TICK];

    // Do not open a second channel for a same-JVM session: closing such a channel can release
    // this process's native locks on some platforms, even after tryLock rejects overlap.
    private static final Set<Path> OPEN_LOCK_PATHS = ConcurrentHashMap.newKeySet();

    private Path worldFile;
    private boolean registeredSessionPath;
    private Path sessionLockPath;

    // Lock a stable sidecar for the whole loaded session, not the atomically replaced save inode.
    private FileChannel sessionLockChannel;
    private FileLock sessionLock;
    private boolean initialized;
    private boolean closed;

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

    // Create a new world only when the save is missing; propagate other load failures to avoid
    // overwriting existing data.
    public void initialize() {
        if (initialized || closed) {
            throw new IllegalStateException("World session is already initialized or closed");
        }
        try {
            Path target = worldFile.toAbsolutePath().normalize();
            Files.createDirectories(target.getParent());
            // Resolve existing file symlinks as well as aliases of the parent directory. Reject
            // dangling save symlinks instead of replacing the link with a new, unrelated world.
            worldFile = Files.exists(target) || Files.isSymbolicLink(target)
                    ? target.toRealPath()
                    : target.getParent().toRealPath().resolve(target.getFileName());
            Path lockFile = worldFile.resolveSibling(worldFile.getFileName() + ".lock");
            try {
                Files.createFile(lockFile);
            } catch (FileAlreadyExistsException e) {
                // The stable sidecar remains after a session exits; existence is not ownership.
            }
            // Canonicalize the existing sidecar too, including case aliases on case-insensitive
            // filesystems when the world itself has not been saved yet.
            sessionLockPath = lockFile.toRealPath();
            if (!OPEN_LOCK_PATHS.add(sessionLockPath)) {
                throw new IllegalStateException("World is already open in another session: " + worldFile);
            }
            registeredSessionPath = true;
            sessionLockChannel = FileChannel.open(sessionLockPath, StandardOpenOption.WRITE);
            try {
                sessionLock = sessionLockChannel.tryLock();
            } catch (OverlappingFileLockException e) {
                throw new IllegalStateException("World is already open in another session: " + worldFile, e);
            }
            if (sessionLock == null) {
                throw new IllegalStateException("World is already open in another session: " + worldFile);
            }

            if (!load()) {
                chunks.clear();
                terrainGenerator = new TerrainGenerator(seed, 62);
                clearSavedRespawnPosition();
                timeOfDay = DEFAULT_TIME_OF_DAY;
            }
            initialized = true;
        } catch (IOException e) {
            IllegalStateException failure = new IllegalStateException("Failed to open world session: " + worldFile, e);
            closeAfterFailedInitialization(failure);
            throw failure;
        } catch (RuntimeException | Error e) {
            closeAfterFailedInitialization(e);
            throw e;
        }
    }

    private void closeAfterFailedInitialization(Throwable failure) {
        try {
            close();
        } catch (RuntimeException e) {
            failure.addSuppressed(e);
        }
    }

    // Closing releases ownership only; callers must finish their final save before closing.
    // Keep the sidecar on disk: deleting it could let another process lock a different inode.
    @Override
    public void close() {
        closed = true;
        IOException failure = null;
        try {
            if (sessionLock != null) {
                sessionLock.release();
            }
        } catch (IOException e) {
            failure = e;
        } finally {
            sessionLock = null;
            try {
                if (sessionLockChannel != null) {
                    sessionLockChannel.close();
                }
            } catch (IOException e) {
                if (failure == null) failure = e;
                else failure.addSuppressed(e);
            } finally {
                sessionLockChannel = null;
                if (registeredSessionPath) {
                    OPEN_LOCK_PATHS.remove(sessionLockPath);
                    registeredSessionPath = false;
                }
            }
        }
        if (failure != null) {
            throw new IllegalStateException("Failed to close world session: " + worldFile, failure);
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

    // Only block edits and respawn changes trigger autosave.
    // Time and water timer changes alone wait for another save trigger or shutdown.
    public boolean hasPendingSave() {
        return respawnPositionDirty || hasModifiedChunks();
    }

    public void setRespawnPosition(float x, float y, float z) {
        WorldBounds.requirePlayerPosition(x, y, z);
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
        WorldBounds.requireChunk(centerChunkX, centerChunkZ);
        if (radius < 0 || radius > WorldBounds.MAX_CHUNK - WorldBounds.MIN_CHUNK) {
            throw new IllegalArgumentException("Invalid chunk radius: " + radius);
        }
        int radiusSq = radius * radius;
        int minX = Math.max(WorldBounds.MIN_CHUNK, centerChunkX - radius);
        int maxX = Math.min(WorldBounds.MAX_CHUNK, centerChunkX + radius);
        int minZ = Math.max(WorldBounds.MIN_CHUNK, centerChunkZ - radius);
        int maxZ = Math.min(WorldBounds.MAX_CHUNK, centerChunkZ + radius);
        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                int dx = x - centerChunkX;
                int dz = z - centerChunkZ;
                if (dx * dx + dz * dz <= radiusSq) getOrCreateChunk(x, z);
            }
        }
    }

    public Chunk getChunkIfLoaded(int chunkX, int chunkZ) {
        if (!WorldBounds.containsChunk(chunkX, chunkZ)) return null;
        return chunks.get(new ChunkPos(chunkX, chunkZ));
    }

    // Generate missing terrain and add it to the world; this lookup can change world state.
    public Chunk getOrCreateChunk(int chunkX, int chunkZ) {
        WorldBounds.requireChunk(chunkX, chunkZ);
        ChunkPos key = new ChunkPos(chunkX, chunkZ);
        Chunk existing = chunks.get(key);
        if (existing != null) {
            return existing;
        }

        Chunk chunk = new Chunk(chunkX, chunkZ);
        terrainGenerator.generate(chunk);
        chunks.put(key, chunk);

        // Initialize lighting and exchange light with already loaded neighbors.
        lightEngine.onChunkLoaded(chunk);

        // Invalidate surrounding meshes because a newly loaded chunk changes face and corner
        // samples.
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                markChunkMeshDirty(chunkX + dx, chunkZ + dz);
            }
        }
        scheduleWaterBorders(chunk);

        return chunk;
    }

    // Below the world return BEDROCK, above it return AIR; valid-height reads generate missing
    // chunks.
    public BlockType getBlock(int worldX, int y, int worldZ) {
        WorldBounds.requireBlock(worldX, worldZ);
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

    // Read without generating chunks; missing chunks return AIR.
    // Lighting and water must use loaded state so simulation queries cannot create terrain.
    public BlockType peekBlock(int worldX, int y, int worldZ) {
        if (!WorldBounds.containsBlock(worldX, worldZ)) return BlockType.AIR;
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

    // Block edits schedule nearby water so opened spaces fill and unsupported flows can recede.
    public boolean setBlock(int worldX, int y, int worldZ, BlockType type) {
        return setBlockInternal(worldX, y, worldZ, type, true);
    }

    // Water simulation passes false to avoid generating terrain while writing across unloaded
    // boundaries.
    private boolean setBlockInternal(int worldX, int y, int worldZ, BlockType type, boolean createMissingChunk) {
        if (!WorldBounds.containsCell(worldX, y, worldZ)) {
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

        // Recompute affected light and invalidate any meshes whose light samples change.
        lightEngine.onBlockChanged(worldX, y, worldZ, existing, type);

        // Wake nearby water after every block change, including changes made by water itself.
        scheduleWaterNeighborhood(worldX, y, worldZ);

        // Invalidate neighboring meshes when a boundary block changes.
        markNeighborMeshesDirty(chunk, localX, localZ);

        return true;
    }

    // Water sources have strength 8; flowing states use 7 down to 1.
    // Scheduled ticks recompute support and spread downward first, then horizontally with
    // decreasing strength.
    // Process at most four overdue ticks per update; retain the remaining accumulated time for
    // later calls.
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

    // Extract the current batch before updating it so newly queued work waits for a later tick.
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

    private void updateWaterCell(int x, int y, int z) {
        BlockType block = peekBlock(x, y, z);
        if (!block.isWaterBlock()) {
            return;
        }

        int strength = block.waterStrength();

        // Recompute flowing water from neighboring supplies; it may strengthen, become a source, or
        // recede.
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

    private int computeWaterStrength(int x, int y, int z) {
        // Any water directly above supplies a falling flow of strength 7.
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

        // Two horizontal source neighbors create a source only when supported by a solid block or
        // source water below.
        if (sourceNeighbors >= 2) {
            BlockType below = peekBlock(x, y - 1, z);
            if (below.isSolid() || below == BlockType.WATER) {
                return 8;
            }
        }

        return Math.min(7, bestHorizontal - 1);
    }

    // Try downward flow first; only sources or supported water can then spread sideways.
    private void spreadWater(int x, int y, int z, int strength) {
        BlockType below = y > 0 ? peekBlock(x, y - 1, z) : BlockType.BEDROCK;

        if (canWaterFlowInto(below, 7)) {
            setBlockInternal(x, y - 1, z, BlockType.flowingWaterOfStrength(7), false);
            return;
        }

        // Allow horizontal spreading for sources, water above solids, or water above a source
        // surface.
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

    // Flow replaces only air or weaker flowing water; it does not wash away torches, doors, or
    // other blocks.
    private boolean canWaterFlowInto(BlockType target, int incomingStrength) {
        if (target == BlockType.AIR) {
            return true;
        }
        return target.isFlowingWater() && target.waterStrength() < incomingStrength;
    }

    // Wake water in the changed cell and its six direct neighbors.
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
        if (!WorldBounds.containsCell(x, y, z) || !peekBlock(x, y, z).isWaterBlock()) {
            return;
        }
        scheduledWaterCells.add(packWaterCell(x, y, z));
    }

    // Pack signed 26-bit X/Z and unsigned 12-bit Y; version 4 saves store this exact format.
    // Coordinates outside those ranges cannot be represented without truncation.
    private static long packWaterCell(int x, int y, int z) {
        if (!WorldBounds.containsCell(x, y, z)) {
            throw new IllegalArgumentException("Water cell outside supported world bounds");
        }
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

    // Corner changes also affect diagonal chunk meshes because smooth light and AO sample diagonal
    // cells.
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

    // Reschedule both sides when a neighbor loads so water previously stopped at that boundary can
    // continue.
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
        WorldBounds.requireBlock(worldX, worldZ);
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

    // Prefer a broad forest region, then search for safe ground; these checks may generate chunks.
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

        // Last-resort fallback above the origin's top solid cell; this position has not passed the
        // safe-spawn checks.
        int fallbackY = topSolidY(fallbackX, fallbackZ);
        out.set(fallbackX + 0.5f, fallbackY + SPAWN_Y_OFFSET, fallbackZ + 0.5f);
        return out;
    }

    // Choose a broad forest region from surface samples before performing local standability
    // checks.
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

            // Stop searching once the score reaches the preferred-region threshold.
            if (foundForestRegion[0] && bestScore[0] >= 290) {
                break;
            }
        }

        if (!foundForestRegion[0]) {
            return false;
        }

        return trySpawnNearForestCenter(out, bestX[0], bestZ[0]);
    }

    // Use one-element arrays so this helper can update the best candidate across calls.
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

    // Search near the forest center for safe ground with nearby sampled tree cover.
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

        // If no candidate has enough sampled tree cover, accept safe ground without that cover
        // requirement.
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

    // Require safe ground and at least six sampled columns containing logs or leaves.
    // This estimates nearby tree cover; it does not test line of sight.
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

    // Reuse the already computed floor height when checking spawn clearance.
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

    // Step through candidate cells, then intersect actual render boxes so gaps in partial blocks do
    // not block the ray.
    public RaycastHit raycast(Vector3f origin, Vector3f direction, float maxDistance) {
        if (!WorldBounds.containsPosition(origin.x, origin.y, origin.z) || !direction.isFinite()
                || !Float.isFinite(maxDistance) || maxDistance < 0 || maxDistance > WorldBounds.MAX_RAY_DISTANCE) {
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

        double invDx = dx == 0.0f ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dx);
        double invDy = dy == 0.0f ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dy);
        double invDz = dz == 0.0f ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dz);

        double tx = dx == 0.0f ? Double.POSITIVE_INFINITY
                : (dx > 0 ? (x + 1.0 - origin.x) * invDx : (origin.x - x) * invDx);
        double ty = dy == 0.0f ? Double.POSITIVE_INFINITY
                : (dy > 0 ? (y + 1.0 - origin.y) * invDy : (origin.y - y) * invDy);
        double tz = dz == 0.0f ? Double.POSITIVE_INFINITY
                : (dz > 0 ? (z + 1.0 - origin.z) * invDz : (origin.z - z) * invDz);

        double traveled = 0.0;
        // Each axis crosses at most distance*abs(direction) cell boundaries, plus the start cell.
        // The hard iteration bound also protects the loop if future arithmetic stops advancing.
        int remainingSteps = (int) Math.ceil(maxDistance * ((double) Math.abs(dx) + Math.abs(dy) + Math.abs(dz))) + 4;

        // Stop at the finite world boundary, without generating any out-of-range chunks.
        while (remainingSteps-- > 0 && traveled <= maxDistance && WorldBounds.containsBlock(x, z)
                && y >= WorldBounds.MIN_Y && y <= WorldBounds.MAX_Y) {
            BlockType block = getBlock(x, y, z);
            float segmentEnd = (float) Math.min(maxDistance, Math.min(tx, Math.min(ty, tz)));
            RaycastHit hit = intersectBlock(x, y, z, block, origin, dx, dy, dz, segmentEnd);
            // Also test the cell below because fence render boxes extend above their own cell.
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

    // Intersect the ray intervals allowed by each axis-aligned box face; reject the box if near
    // exceeds far.
    // The latest entry axis supplies the hit normal. Starting inside a box leaves distance and
    // normal at zero.
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

    // Version additions: v1 seed/chunks, v2 respawn, v3 time, v4 water queue and tick timer.
    // Write and force a temporary file before atomic replacement; clear dirty flags only after
    // replacement succeeds.
    public void save() {
        if (!initialized || closed || sessionLock == null || !sessionLock.isValid()) {
            throw new IllegalStateException("Saving requires an open world session: " + worldFile);
        }
        Path target = worldFile;
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
            // Require atomic replacement; filesystems that do not support it cause the save to
            // fail.
            // Do not fall back to overwriting the existing file in place.
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

    // Only a missing file means a new world; corrupt, unsupported, or unreadable saves must fail
    // instead of being replaced.
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
                if (!WorldBounds.containsPlayerPosition(loadedPosition.x, loadedPosition.y, loadedPosition.z)) {
                    throw new IOException("Respawn position outside supported world bounds: " + loadedPosition);
                }
            }
            float loadedTime = version >= 3 ? in.readFloat() : DEFAULT_TIME_OF_DAY;
            if (!(loadedTime >= 0.0f && loadedTime < 1.0f)) {
                throw new IOException("Invalid world time");
            }

            int blockCount = GameConfig.CHUNK_SIZE * GameConfig.CHUNK_HEIGHT * GameConfig.CHUNK_SIZE;
            int count = in.readInt();
            // Bound the declared count by the minimum record size before reading chunks.
            // Each record needs 12 bytes for X, Z, and block count, plus its block IDs.
            if (count < 0 || count > fileSize / (12L + blockCount * Short.BYTES)) {
                throw new IOException("Invalid chunk count: " + count);
            }
            Map<ChunkPos, Chunk> loadedChunks = new HashMap<>();
            for (int i = 0; i < count; i++) {
                int chunkX = in.readInt();
                int chunkZ = in.readInt();
                if (!WorldBounds.containsChunk(chunkX, chunkZ)) {
                    throw new IOException("Chunk outside supported world bounds: " + chunkX + ", " + chunkZ);
                }
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
                    if (!WorldBounds.containsCell(unpackWaterX(cell), unpackWaterY(cell), unpackWaterZ(cell))) {
                        throw new IOException("Water cell outside supported world bounds");
                    }
                    loadedWater.add(cell);
                }
            }
            if (in.read() != -1) {
                throw new IOException("Unexpected data after world save");
            }

            // Apply loaded state only after validating the whole file; restore all chunks before
            // rebuilding cross-chunk lighting.
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

    // Older saves lack a water queue; schedule flowing water and sources that can spread instead of
    // queuing every ocean cell.
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

    // Use floor division so negative world coordinates map to the chunk containing them.
    static int chunkCoord(int worldCoord) {
        return Math.floorDiv(worldCoord, GameConfig.CHUNK_SIZE);
    }

    // With a floor-divided chunk coordinate, subtraction produces a local coordinate in [0,
    // CHUNK_SIZE).
    static int localCoord(int worldCoord, int chunkCoord) {
        return worldCoord - chunkCoord * GameConfig.CHUNK_SIZE;
    }

    private int fastFloor(float value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }
}

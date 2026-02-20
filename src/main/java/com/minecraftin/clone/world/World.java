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
import java.util.HashMap;
import java.util.Map;

public final class World {
    private static final int SAVE_MAGIC = 0x4D434C4E; // MCLN
    private static final int SAVE_VERSION = 1;
    private static final float SPAWN_Y_OFFSET = 1.05f;

    private final Map<ChunkPos, Chunk> chunks = new HashMap<>();
    private final Path worldFile;

    private long seed;
    private TerrainGenerator terrainGenerator;

    public World(Path worldFile, long defaultSeed) {
        this.worldFile = worldFile;
        this.seed = defaultSeed;
        this.terrainGenerator = new TerrainGenerator(seed, 62);
    }

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
        }
    }

    public long seed() {
        return seed;
    }

    public int seaLevel() {
        return terrainGenerator.seaLevel();
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

    public Chunk getOrCreateChunk(int chunkX, int chunkZ) {
        ChunkPos key = new ChunkPos(chunkX, chunkZ);
        Chunk existing = chunks.get(key);
        if (existing != null) {
            return existing;
        }

        Chunk chunk = new Chunk(chunkX, chunkZ);
        terrainGenerator.generate(chunk);
        chunks.put(key, chunk);
        markChunkMeshDirty(chunkX - 1, chunkZ);
        markChunkMeshDirty(chunkX + 1, chunkZ);
        markChunkMeshDirty(chunkX, chunkZ - 1);
        markChunkMeshDirty(chunkX, chunkZ + 1);
        return chunk;
    }

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

    public boolean setBlock(int worldX, int y, int worldZ, BlockType type) {
        if (y < 0 || y >= GameConfig.CHUNK_HEIGHT) {
            return false;
        }

        int chunkX = Math.floorDiv(worldX, GameConfig.CHUNK_SIZE);
        int chunkZ = Math.floorDiv(worldZ, GameConfig.CHUNK_SIZE);
        int localX = Math.floorMod(worldX, GameConfig.CHUNK_SIZE);
        int localZ = Math.floorMod(worldZ, GameConfig.CHUNK_SIZE);

        Chunk chunk = getOrCreateChunk(chunkX, chunkZ);
        BlockType existing = chunk.get(localX, y, localZ);
        if (existing == type) {
            return false;
        }

        chunk.set(localX, y, localZ, type);

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

    private void markChunkMeshDirty(int chunkX, int chunkZ) {
        Chunk chunk = getChunkIfLoaded(chunkX, chunkZ);
        if (chunk != null) {
            chunk.markMeshDirty();
        }
    }

    public int topSolidY(int worldX, int worldZ) {
        for (int y = GameConfig.CHUNK_HEIGHT - 1; y >= 1; y--) {
            BlockType block = getBlock(worldX, y, worldZ);
            if (block.isSolid() && block != BlockType.LEAVES && block != BlockType.WATER) {
                return y;
            }
        }
        return 1;
    }

    public Vector3f defaultSpawn(Vector3f out) {
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
        int fallbackY = topSolidY(fallbackX, fallbackZ);
        out.set(fallbackX + 0.5f, fallbackY + SPAWN_Y_OFFSET, fallbackZ + 0.5f);
        return out;
    }

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

        float tx = dx == 0.0f ? Float.POSITIVE_INFINITY : (dx > 0 ? (x + 1 - origin.x) * invDx : (origin.x - x) * invDx);
        float ty = dy == 0.0f ? Float.POSITIVE_INFINITY : (dy > 0 ? (y + 1 - origin.y) * invDy : (origin.y - y) * invDy);
        float tz = dz == 0.0f ? Float.POSITIVE_INFINITY : (dz > 0 ? (z + 1 - origin.z) * invDz : (origin.z - z) * invDz);

        float traveled = 0.0f;
        int normalX = 0;
        int normalY = 0;
        int normalZ = 0;

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

    public void save() {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(worldFile)))) {
            out.writeInt(SAVE_MAGIC);
            out.writeInt(SAVE_VERSION);
            out.writeLong(seed);
            out.writeInt(chunks.size());

            for (Chunk chunk : chunks.values()) {
                out.writeInt(chunk.chunkX());
                out.writeInt(chunk.chunkZ());

                short[] data = chunk.rawBlocks();
                out.writeInt(data.length);
                for (short value : data) {
                    out.writeShort(value);
                }

                chunk.clearModified();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save world to " + worldFile, e);
        }
    }

    private boolean load() {
        if (!Files.exists(worldFile)) {
            return false;
        }

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(worldFile)))) {
            int magic = in.readInt();
            int version = in.readInt();

            if (magic != SAVE_MAGIC || version != SAVE_VERSION) {
                return false;
            }

            seed = in.readLong();
            terrainGenerator = new TerrainGenerator(seed, 62);

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

                chunk.markMeshDirty();
                chunk.clearModified();
                loadedChunks.put(new ChunkPos(chunkX, chunkZ), chunk);
            }

            chunks.clear();
            chunks.putAll(loadedChunks);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private int fastFloor(float value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }
}

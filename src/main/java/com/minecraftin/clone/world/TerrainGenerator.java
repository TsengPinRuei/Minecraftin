package com.minecraftin.clone.world;

import com.minecraftin.clone.config.GameConfig;
import com.minecraftin.clone.util.Noise;

public final class TerrainGenerator {
    private enum Biome {
        PLAINS,
        FOREST,
        DESERT,
        SNOW,
        MOUNTAIN,
        BADLANDS
    }

    private final long seed;
    private final int seaLevel;

    public TerrainGenerator(long seed, int seaLevel) {
        this.seed = seed;
        this.seaLevel = seaLevel;
    }

    public void generate(Chunk chunk) {
        int worldMinX = chunk.worldMinX();
        int worldMinZ = chunk.worldMinZ();

        for (int lx = 0; lx < GameConfig.CHUNK_SIZE; lx++) {
            for (int lz = 0; lz < GameConfig.CHUNK_SIZE; lz++) {
                int worldX = worldMinX + lx;
                int worldZ = worldMinZ + lz;

                float continental = Noise.fbm2(worldX * 0.0019f, worldZ * 0.0019f, 5, 2.0f, 0.5f, seed + 17);
                float erosion = Noise.fbm2(worldX * 0.0036f, worldZ * 0.0036f, 4, 2.0f, 0.53f, seed + 23);
                float detail = Noise.fbm2(worldX * 0.0105f, worldZ * 0.0105f, 4, 2.1f, 0.50f, seed + 29);
                float ridges = Math.abs(Noise.fbm2(worldX * 0.0026f, worldZ * 0.0026f, 4, 2.0f, 0.5f, seed + 37));
                float temperature = Noise.fbm2(worldX * 0.0013f, worldZ * 0.0013f, 4, 2.0f, 0.5f, seed + 41);
                float moisture = Noise.fbm2(worldX * 0.0013f, worldZ * 0.0013f, 4, 2.0f, 0.5f, seed + 47);
                float weirdness = Noise.fbm2(worldX * 0.0048f, worldZ * 0.0048f, 3, 2.0f, 0.5f, seed + 53);

                float baseHeight = 57.0f
                        + continental * 20.0f
                        + detail * 7.5f
                        - Math.max(0.0f, -continental) * 8.0f
                        - Math.max(0.0f, -erosion) * 3.0f;

                float mountainMask = Math.max(0.0f, ridges - 0.27f) / 0.73f;
                float mountainBoost = mountainMask * mountainMask * (30.0f + Math.max(0.0f, weirdness) * 18.0f);
                int height = Math.round(baseHeight + mountainBoost);
                height = Math.max(6, Math.min(GameConfig.CHUNK_HEIGHT - 4, height));

                Biome biome = pickBiome(height, continental, ridges, temperature, moisture);
                int topDepth = biome == Biome.DESERT || biome == Biome.BADLANDS ? 5 : 4;

                for (int y = 0; y < GameConfig.CHUNK_HEIGHT; y++) {
                    BlockType block;

                    if (y == 0) {
                        block = BlockType.BEDROCK;
                    } else if (y > height) {
                        block = y <= seaLevel ? BlockType.WATER : BlockType.AIR;
                    } else {
                        float cave = Noise.fbm3(worldX * 0.055f, y * 0.090f, worldZ * 0.055f, 3, 2.0f, 0.5f, seed + 73);
                        float caveThreshold = biome == Biome.MOUNTAIN ? 0.58f : 0.64f;
                        if (y > 5 && y < height - 3 && cave > caveThreshold) {
                            block = BlockType.AIR;
                        } else {
                            block = selectStrataBlock(biome, y, height, topDepth, seaLevel);
                        }
                    }

                    chunk.setRaw(lx, y, lz, (short) block.id());
                }

                if (height > seaLevel + 1) {
                    maybePlaceTree(chunk, biome, lx, height + 1, lz, worldX, worldZ);
                }
            }
        }

        chunk.markMeshDirty();
        chunk.clearModified();
    }

    public int seaLevel() {
        return seaLevel;
    }

    private Biome pickBiome(int height, float continental, float ridges, float temperature, float moisture) {
        if (ridges > 0.72f && continental > -0.20f) {
            return Biome.MOUNTAIN;
        }
        if (temperature > 0.45f && moisture < -0.05f) {
            return Biome.DESERT;
        }
        if (temperature < -0.40f && continental > -0.25f) {
            return Biome.SNOW;
        }
        if (temperature > 0.18f && moisture < -0.24f && height > seaLevel + 3) {
            return Biome.BADLANDS;
        }
        if (moisture > 0.26f) {
            return Biome.FOREST;
        }
        return Biome.PLAINS;
    }

    private BlockType selectStrataBlock(Biome biome, int y, int height, int topDepth, int seaLevel) {
        if (y == height) {
            return switch (biome) {
                case DESERT, BADLANDS -> BlockType.SAND;
                case SNOW -> height > seaLevel + 1 ? BlockType.SNOW : BlockType.SAND;
                case MOUNTAIN -> height > 88 ? BlockType.SNOW : BlockType.STONE;
                default -> height <= seaLevel + 1 ? BlockType.SAND : BlockType.GRASS;
            };
        }

        if (y >= height - topDepth) {
            return switch (biome) {
                case DESERT, BADLANDS -> BlockType.SAND;
                case MOUNTAIN -> (height > 82 && y >= height - 2) ? BlockType.STONE : BlockType.DIRT;
                default -> BlockType.DIRT;
            };
        }

        return switch (biome) {
            case BADLANDS -> (y % 6 == 0) ? BlockType.DIRT : BlockType.STONE;
            default -> BlockType.STONE;
        };
    }

    private void maybePlaceTree(Chunk chunk, Biome biome, int lx, int baseY, int lz, int worldX, int worldZ) {
        if (biome == Biome.DESERT || biome == Biome.BADLANDS || biome == Biome.MOUNTAIN) {
            return;
        }

        int hash = Noise.hashInt(worldX, 0, worldZ, seed + 191);
        int chance = switch (biome) {
            case FOREST -> 56;
            case SNOW -> 10;
            case PLAINS -> 18;
            default -> 0;
        };

        if ((hash & 0xFF) > chance) {
            return;
        }

        int trunkHeight = 4 + Math.abs(hash % 3);
        int topY = baseY + trunkHeight;

        if (topY + 3 >= GameConfig.CHUNK_HEIGHT) {
            return;
        }

        if (lx < 2 || lx >= GameConfig.CHUNK_SIZE - 2 || lz < 2 || lz >= GameConfig.CHUNK_SIZE - 2) {
            return;
        }

        for (int y = 0; y < trunkHeight; y++) {
            if (chunk.get(lx, baseY + y, lz) != BlockType.AIR) {
                return;
            }
        }

        for (int y = 0; y < trunkHeight; y++) {
            chunk.setRaw(lx, baseY + y, lz, (short) BlockType.LOG.id());
        }

        int radius = 2;
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int tx = lx + dx;
                    int ty = topY + dy;
                    int tz = lz + dz;

                    if (tx < 0 || tx >= GameConfig.CHUNK_SIZE || tz < 0 || tz >= GameConfig.CHUNK_SIZE) {
                        continue;
                    }

                    if (ty < 1 || ty >= GameConfig.CHUNK_HEIGHT) {
                        continue;
                    }

                    int dist = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                    if (dist > 4) {
                        continue;
                    }

                    if (chunk.get(tx, ty, tz) == BlockType.AIR) {
                        chunk.setRaw(tx, ty, tz, (short) BlockType.LEAVES.id());
                    }
                }
            }
        }
    }
}

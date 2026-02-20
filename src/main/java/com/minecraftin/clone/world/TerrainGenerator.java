package com.minecraftin.clone.world;

import com.minecraftin.clone.config.GameConfig;
import com.minecraftin.clone.util.Noise;

import java.util.ArrayList;
import java.util.List;

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
        int[][] heights = new int[GameConfig.CHUNK_SIZE][GameConfig.CHUNK_SIZE];
        Biome[][] biomes = new Biome[GameConfig.CHUNK_SIZE][GameConfig.CHUNK_SIZE];
        List<TreeSpec> plannedTrees = new ArrayList<>();

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
                heights[lx][lz] = height;
                biomes[lx][lz] = biome;

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
            }
        }

        for (int lx = 0; lx < GameConfig.CHUNK_SIZE; lx++) {
            for (int lz = 0; lz < GameConfig.CHUNK_SIZE; lz++) {
                int height = heights[lx][lz];
                if (height <= seaLevel + 1) {
                    continue;
                }

                int worldX = worldMinX + lx;
                int worldZ = worldMinZ + lz;
                TreeSpec tree = planTree(chunk, biomes[lx][lz], lx, height + 1, lz, worldX, worldZ);
                if (tree != null) {
                    plannedTrees.add(tree);
                }
            }
        }

        for (TreeSpec tree : plannedTrees) {
            placeTrunk(chunk, tree);
        }
        for (TreeSpec tree : plannedTrees) {
            placeCanopy(chunk, tree);
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

    private TreeSpec planTree(Chunk chunk, Biome biome, int lx, int baseY, int lz, int worldX, int worldZ) {
        if (biome == Biome.DESERT || biome == Biome.BADLANDS || biome == Biome.MOUNTAIN) {
            return null;
        }

        int hash = Noise.hashInt(worldX, 0, worldZ, seed + 191);
        int chance = switch (biome) {
            case FOREST -> 56;
            case SNOW -> 10;
            case PLAINS -> 18;
            default -> 0;
        };

        if ((hash & 0xFF) > chance) {
            return null;
        }

        int trunkHeight = 4 + Math.abs(hash % 3);
        int topY = baseY + trunkHeight;

        if (topY + 3 >= GameConfig.CHUNK_HEIGHT) {
            return null;
        }

        if (lx < 2 || lx >= GameConfig.CHUNK_SIZE - 2 || lz < 2 || lz >= GameConfig.CHUNK_SIZE - 2) {
            return null;
        }

        for (int y = 0; y < trunkHeight; y++) {
            if (chunk.get(lx, baseY + y, lz) != BlockType.AIR) {
                return null;
            }
        }

        return new TreeSpec(lx, lz, baseY, trunkHeight);
    }

    private void placeTrunk(Chunk chunk, TreeSpec tree) {
        for (int y = 0; y < tree.trunkHeight(); y++) {
            chunk.setRaw(tree.lx(), tree.baseY() + y, tree.lz(), (short) BlockType.LOG.id());
        }
    }

    private void placeCanopy(Chunk chunk, TreeSpec tree) {
        int topY = tree.topY();
        int canopyBaseY = tree.baseY() + Math.max(1, tree.trunkHeight() - 4);

        for (int y = canopyBaseY; y <= topY + 2; y++) {
            int rel = y - topY;
            int radius;
            boolean trimCorners;

            if (rel <= -3) {
                radius = 1;
                trimCorners = false;
            } else if (rel <= 0) {
                radius = 2;
                trimCorners = rel == 0;
            } else if (rel == 1) {
                radius = 1;
                trimCorners = false;
            } else {
                radius = 0;
                trimCorners = false;
            }

            placeLeafLayer(chunk, tree.lx(), y, tree.lz(), radius, trimCorners);
        }

        int wrapStart = Math.max(tree.baseY() + 1, topY - 3);
        for (int y = wrapStart; y <= topY - 1; y++) {
            trySetLeaf(chunk, tree.lx() + 1, y, tree.lz());
            trySetLeaf(chunk, tree.lx() - 1, y, tree.lz());
            trySetLeaf(chunk, tree.lx(), y, tree.lz() + 1);
            trySetLeaf(chunk, tree.lx(), y, tree.lz() - 1);
        }
    }

    private void placeLeafLayer(Chunk chunk, int centerX, int y, int centerZ, int radius, boolean trimCorners) {
        if (radius <= 0) {
            trySetLeaf(chunk, centerX, y, centerZ);
            return;
        }

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (trimCorners && Math.abs(dx) == radius && Math.abs(dz) == radius) {
                    continue;
                }
                trySetLeaf(chunk, centerX + dx, y, centerZ + dz);
            }
        }
    }

    private void trySetLeaf(Chunk chunk, int x, int y, int z) {
        if (x < 0 || x >= GameConfig.CHUNK_SIZE || z < 0 || z >= GameConfig.CHUNK_SIZE) {
            return;
        }
        if (y < 1 || y >= GameConfig.CHUNK_HEIGHT) {
            return;
        }

        if (chunk.get(x, y, z) == BlockType.AIR) {
            chunk.setRaw(x, y, z, (short) BlockType.LEAVES.id());
        }
    }

    private record TreeSpec(int lx, int lz, int baseY, int trunkHeight) {
        int topY() {
            return baseY + trunkHeight;
        }
    }
}

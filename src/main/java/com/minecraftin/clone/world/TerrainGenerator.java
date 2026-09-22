package com.minecraftin.clone.world;

import com.minecraftin.clone.config.GameConfig;
import com.minecraftin.clone.util.Noise;

import java.util.ArrayList;
import java.util.List;

// Generate repeatable terrain and trees from world coordinates, seed, and fixed generator settings.
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

    // Finish terrain before planning and placing trees; place all trunks before leaves so foliage
    // cannot overwrite terrain or trunks.
    public void generate(Chunk chunk) {
        int worldMinX = chunk.worldMinX();
        int worldMinZ = chunk.worldMinZ();

        int columnCount = GameConfig.CHUNK_SIZE * GameConfig.CHUNK_SIZE;

        int[] heights = new int[columnCount];

        Biome[] biomes = new Biome[columnCount];

        // Collect tree plans before placing any tree so earlier trees cannot change later placement
        // checks.
        List<TreeSpec> plannedTrees = new ArrayList<>(16);

        for (int lx = 0; lx < GameConfig.CHUNK_SIZE; lx++) {
            for (int lz = 0; lz < GameConfig.CHUNK_SIZE; lz++) {
                int worldX = worldMinX + lx;
                int worldZ = worldMinZ + lz;

                SurfaceSample surface = sampleSurface(worldX, worldZ);
                int height = surface.height();
                Biome biome = surface.biome();

                // Use a thicker near-surface layer in desert and badlands biomes.
                int topDepth = biome == Biome.DESERT || biome == Biome.BADLANDS ? 5 : 4;

                int columnIndex = columnIndex(lx, lz);
                heights[columnIndex] = height;
                biomes[columnIndex] = biome;

                for (int y = 0; y < GameConfig.CHUNK_HEIGHT; y++) {
                    BlockType block;

                    // Keep the bottom layer as bedrock.
                    if (y == 0) {
                        block = BlockType.BEDROCK;

                    } else if (y > height) {
                        // Above the terrain surface, fill through sea level with water and leave
                        // higher cells as air.
                        block = y <= seaLevel ? BlockType.WATER : BlockType.AIR;

                    } else {
                        // Keep a solid layer near bedrock and the terrain surface while carving
                        // caves below.
                        float caveThreshold = biome == Biome.MOUNTAIN ? 0.58f : 0.64f;
                        if (y > 5 && y < height - 3
                                && Noise.fbm3(worldX * 0.055f, y * 0.090f, worldZ * 0.055f,
                                        3, 2.0f, 0.5f, seed + 73) > caveThreshold) {
                            block = y <= seaLevel ? BlockType.WATER : BlockType.AIR;
                        } else {
                            block = selectStrataBlock(biome, y, height, topDepth);
                        }
                    }

                    chunk.setRaw(lx, y, lz, (short) block.id());
                }
            }
        }

        for (int lx = 0; lx < GameConfig.CHUNK_SIZE; lx++) {
            for (int lz = 0; lz < GameConfig.CHUNK_SIZE; lz++) {
                int columnIndex = columnIndex(lx, lz);
                int height = heights[columnIndex];

                // Skip trees where terrain is at or below sea level plus one block.
                if (height <= seaLevel + 1) {
                    continue;
                }

                int worldX = worldMinX + lx;
                int worldZ = worldMinZ + lz;

                TreeSpec tree = planTree(chunk, biomes[columnIndex], lx, height + 1, lz, worldX, worldZ);
                if (tree != null) {
                    plannedTrees.add(tree);
                }
            }
        }

        // Place every trunk before any leaves so overlapping canopies cannot replace trunks.
        for (TreeSpec tree : plannedTrees) {
            placeTrunk(chunk, tree);
        }
        for (TreeSpec tree : plannedTrees) {
            placeCanopy(chunk, tree);
        }

        // New terrain needs a mesh but is not treated as an unsaved gameplay edit.
        chunk.markMeshDirty();
        chunk.clearModified();
    }

    public int seaLevel() {
        return seaLevel;
    }

    // Use a flat column cache with Z varying fastest.
    private static int columnIndex(int localX, int localZ) {
        return localX * GameConfig.CHUNK_SIZE + localZ;
    }

    public int forestSpawnRegionScore(int centerX, int centerZ) {
        SurfaceSample center = sampleSurface(centerX, centerZ);

        // Reject non-forest centers and centers too close to sea level.
        if (center.height() <= seaLevel + 3 || center.biome() != Biome.FOREST) {
            return Integer.MIN_VALUE;
        }

        int sampleRadius = 48;
        int sampleStep = 24;

        int forestCount = 0;
        int landCount = 0;
        int coastCount = 0;
        int mountainCount = 0;
        int minHeight = Integer.MAX_VALUE;
        int maxHeight = Integer.MIN_VALUE;

        // Sample surrounding terrain to favor broad, inland forest regions with limited height
        // variation.
        for (int dz = -sampleRadius; dz <= sampleRadius; dz += sampleStep) {
            for (int dx = -sampleRadius; dx <= sampleRadius; dx += sampleStep) {
                SurfaceSample sample = sampleSurface(centerX + dx, centerZ + dz);

                minHeight = Math.min(minHeight, sample.height());
                maxHeight = Math.max(maxHeight, sample.height());

                if (sample.height() > seaLevel + 2) {
                    landCount++;
                } else {
                    coastCount++;
                }

                if (sample.biome() == Biome.FOREST) {
                    forestCount++;
                }

                if (sample.biome() == Biome.MOUNTAIN) {
                    mountainCount++;
                }
            }
        }

        // Reject regions without enough land and forest samples.
        if (landCount < 20 || forestCount < 15) {
            return Integer.MIN_VALUE;
        }

        int heightRange = maxHeight - minHeight;

        // Reward higher continental noise as a proxy for inland terrain.
        int inlandBonus = Math.round(Math.max(0.0f, center.continental() + 0.12f) * 60.0f);

        return forestCount * 9
                + landCount * 6
                + inlandBonus
                - coastCount * 14
                - mountainCount * 8
                - Math.max(0, heightRange - 10) * 3;
    }

    // Noise seed offsets are part of the generation contract.
    // Changing them affects newly generated chunks and can create seams beside terrain stored in
    // existing saves.
    private SurfaceSample sampleSurface(int worldX, int worldZ) {
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
        return new SurfaceSample(height, biome, continental);
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

    private BlockType selectStrataBlock(Biome biome, int y, int height, int topDepth) {
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

        // Use periodic dirt bands in deep badlands strata; other deep layers are stone.
        return switch (biome) {
            case BADLANDS -> (y % 6 == 0) ? BlockType.DIRT : BlockType.STONE;
            default -> BlockType.STONE;
        };
    }

    // Keep each tree inside one chunk so generation order cannot truncate or duplicate cross-chunk
    // canopies.
    private TreeSpec planTree(Chunk chunk, Biome biome, int lx, int baseY, int lz, int worldX, int worldZ) {
        if (biome == Biome.DESERT || biome == Biome.BADLANDS || biome == Biome.MOUNTAIN) {
            return null;
        }

        // Allow only one candidate slot per grid cell to control tree spacing.
        if (!matchesTreeGridSlot(biome, worldX, worldZ)) {
            return null;
        }

        int hash = Noise.hashInt(worldX, 0, worldZ, seed + 191);

        // Apply a biome-specific threshold to the deterministic placement hash.
        int chance = switch (biome) {
            case FOREST -> 34;
            case SNOW -> 10;
            case PLAINS -> 12;
            default -> 0;
        };

        if ((hash & 0xFF) > chance) {
            return null;
        }

        int trunkHeight = 3 + Math.abs(hash % 3);
        int topY = baseY + trunkHeight;

        // Reserve space above the trunk for the canopy.
        if (topY + 3 >= GameConfig.CHUNK_HEIGHT) {
            return null;
        }

        // Leave room for the widest leaf layer at chunk edges.
        if (lx < 2 || lx >= GameConfig.CHUNK_SIZE - 2 || lz < 2 || lz >= GameConfig.CHUNK_SIZE - 2) {
            return null;
        }

        // Reject a tree if its planned trunk would replace existing terrain.
        for (int y = 0; y < trunkHeight; y++) {
            if (chunk.get(lx, baseY + y, lz) != BlockType.AIR) {
                return null;
            }
        }

        return new TreeSpec(lx, lz, baseY, trunkHeight);
    }

    // Use a seeded offset within each world-space grid cell to distribute tree candidates
    // consistently, including at negative coordinates.
    private boolean matchesTreeGridSlot(Biome biome, int worldX, int worldZ) {
        int cellSize = biome == Biome.FOREST ? 5 : 6;

        int cellX = Math.floorDiv(worldX, cellSize);
        int cellZ = Math.floorDiv(worldZ, cellSize);
        int localX = Math.floorMod(worldX, cellSize);
        int localZ = Math.floorMod(worldZ, cellSize);

        int cellHash = Noise.hashInt(cellX, 97, cellZ, seed + 173);
        int slotX = Math.floorMod(cellHash, cellSize);
        int slotZ = Math.floorMod(cellHash >>> 8, cellSize);

        return localX == slotX && localZ == slotZ;
    }

    private void placeTrunk(Chunk chunk, TreeSpec tree) {
        for (int y = 0; y < tree.trunkHeight(); y++) {
            chunk.setRaw(tree.lx(), tree.baseY() + y, tree.lz(), (short) BlockType.LOG.id());
        }
    }

    private void placeCanopy(Chunk chunk, TreeSpec tree) {
        int topY = tree.topY();

        // Leave at least the lowest trunk block exposed before starting the canopy.
        int canopyBaseY = tree.baseY() + Math.max(1, tree.trunkHeight() - 4);

        for (int y = canopyBaseY; y <= topY + 2; y++) {
            int rel = y - topY;
            int radius;
            boolean trimCorners;

            // Vary leaf radius by height to shape the canopy.
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

    }

    private void placeLeafLayer(Chunk chunk, int centerX, int y, int centerZ, int radius, boolean trimCorners) {
        if (radius <= 0) {
            trySetLeaf(chunk, centerX, y, centerZ);
            return;
        }

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // Trim selected layer corners to soften the square canopy outline.
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

        // Place leaves only in air so terrain and previously placed trunks remain intact.
        if (chunk.get(x, y, z) == BlockType.AIR) {
            chunk.setRaw(x, y, z, (short) BlockType.LEAVES.id());
        }
    }

    private record TreeSpec(int lx, int lz, int baseY, int trunkHeight) {

        int topY() {
            return baseY + trunkHeight;
        }
    }

    private record SurfaceSample(int height, Biome biome, float continental) {
    }
}

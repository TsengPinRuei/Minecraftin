// 宣告此檔案所屬的套件。
package com.minecraftin.clone.world;

// 匯入後續會使用到的型別或函式。
import com.minecraftin.clone.config.GameConfig;
// 匯入後續會使用到的型別或函式。
import com.minecraftin.clone.util.Noise;

// 匯入後續會使用到的型別或函式。
import java.util.ArrayList;
// 匯入後續會使用到的型別或函式。
import java.util.List;

// 定義主要型別與其結構。
public final class TerrainGenerator {
    // 定義主要型別與其結構。
    private enum Biome {
        // 下一行程式碼負責執行目前步驟。
        PLAINS,
        // 下一行程式碼負責執行目前步驟。
        FOREST,
        // 下一行程式碼負責執行目前步驟。
        DESERT,
        // 下一行程式碼負責執行目前步驟。
        SNOW,
        // 下一行程式碼負責執行目前步驟。
        MOUNTAIN,
        // 下一行程式碼負責執行目前步驟。
        BADLANDS
    }

    // 下一行程式碼負責執行目前步驟。
    private final long seed;
    // 下一行程式碼負責執行目前步驟。
    private final int seaLevel;

    // 定義對外可呼叫的方法。
    public TerrainGenerator(long seed, int seaLevel) {
        // 設定或更新變數的值。
        this.seed = seed;
        // 設定或更新變數的值。
        this.seaLevel = seaLevel;
    }

    // 定義對外可呼叫的方法。
    public void generate(Chunk chunk) {
        // 宣告並初始化變數。
        int worldMinX = chunk.worldMinX();
        // 宣告並初始化變數。
        int worldMinZ = chunk.worldMinZ();
        // 宣告並初始化變數。
        int[][] heights = new int[GameConfig.CHUNK_SIZE][GameConfig.CHUNK_SIZE];
        // 宣告並初始化變數。
        Biome[][] biomes = new Biome[GameConfig.CHUNK_SIZE][GameConfig.CHUNK_SIZE];
        // 宣告並初始化變數。
        List<TreeSpec> plannedTrees = new ArrayList<>();

        // 使用迴圈逐一處理每個元素或區間。
        for (int lx = 0; lx < GameConfig.CHUNK_SIZE; lx++) {
            // 使用迴圈逐一處理每個元素或區間。
            for (int lz = 0; lz < GameConfig.CHUNK_SIZE; lz++) {
                // 宣告並初始化變數。
                int worldX = worldMinX + lx;
                // 宣告並初始化變數。
                int worldZ = worldMinZ + lz;

                // 宣告並初始化變數。
                float continental = Noise.fbm2(worldX * 0.0019f, worldZ * 0.0019f, 5, 2.0f, 0.5f, seed + 17);
                // 宣告並初始化變數。
                float erosion = Noise.fbm2(worldX * 0.0036f, worldZ * 0.0036f, 4, 2.0f, 0.53f, seed + 23);
                // 宣告並初始化變數。
                float detail = Noise.fbm2(worldX * 0.0105f, worldZ * 0.0105f, 4, 2.1f, 0.50f, seed + 29);
                // 宣告並初始化變數。
                float ridges = Math.abs(Noise.fbm2(worldX * 0.0026f, worldZ * 0.0026f, 4, 2.0f, 0.5f, seed + 37));
                // 宣告並初始化變數。
                float temperature = Noise.fbm2(worldX * 0.0013f, worldZ * 0.0013f, 4, 2.0f, 0.5f, seed + 41);
                // 宣告並初始化變數。
                float moisture = Noise.fbm2(worldX * 0.0013f, worldZ * 0.0013f, 4, 2.0f, 0.5f, seed + 47);
                // 宣告並初始化變數。
                float weirdness = Noise.fbm2(worldX * 0.0048f, worldZ * 0.0048f, 3, 2.0f, 0.5f, seed + 53);

                // 宣告並初始化變數。
                float baseHeight = 57.0f
                        // 下一行程式碼負責執行目前步驟。
                        + continental * 20.0f
                        // 下一行程式碼負責執行目前步驟。
                        + detail * 7.5f
                        // 下一行程式碼負責執行目前步驟。
                        - Math.max(0.0f, -continental) * 8.0f
                        // 呼叫方法執行對應功能。
                        - Math.max(0.0f, -erosion) * 3.0f;

                // 宣告並初始化變數。
                float mountainMask = Math.max(0.0f, ridges - 0.27f) / 0.73f;
                // 宣告並初始化變數。
                float mountainBoost = mountainMask * mountainMask * (30.0f + Math.max(0.0f, weirdness) * 18.0f);
                // 宣告並初始化變數。
                int height = Math.round(baseHeight + mountainBoost);
                // 設定或更新變數的值。
                height = Math.max(6, Math.min(GameConfig.CHUNK_HEIGHT - 4, height));

                // 宣告並初始化變數。
                Biome biome = pickBiome(height, continental, ridges, temperature, moisture);
                // 宣告並初始化變數。
                int topDepth = biome == Biome.DESERT || biome == Biome.BADLANDS ? 5 : 4;
                // 設定或更新變數的值。
                heights[lx][lz] = height;
                // 設定或更新變數的值。
                biomes[lx][lz] = biome;

                // 使用迴圈逐一處理每個元素或區間。
                for (int y = 0; y < GameConfig.CHUNK_HEIGHT; y++) {
                    // 下一行程式碼負責執行目前步驟。
                    BlockType block;

                    // 根據條件決定是否進入此邏輯分支。
                    if (y == 0) {
                        // 設定或更新變數的值。
                        block = BlockType.BEDROCK;
                    // 下一行程式碼負責執行目前步驟。
                    } else if (y > height) {
                        // 設定或更新變數的值。
                        block = y <= seaLevel ? BlockType.WATER : BlockType.AIR;
                    // 下一行程式碼負責執行目前步驟。
                    } else {
                        // 宣告並初始化變數。
                        float cave = Noise.fbm3(worldX * 0.055f, y * 0.090f, worldZ * 0.055f, 3, 2.0f, 0.5f, seed + 73);
                        // 宣告並初始化變數。
                        float caveThreshold = biome == Biome.MOUNTAIN ? 0.58f : 0.64f;
                        // 根據條件決定是否進入此邏輯分支。
                        if (y > 5 && y < height - 3 && cave > caveThreshold) {
                            // 設定或更新變數的值。
                            block = y <= seaLevel ? BlockType.WATER : BlockType.AIR;
                        // 下一行程式碼負責執行目前步驟。
                        } else {
                            // 設定或更新變數的值。
                            block = selectStrataBlock(biome, y, height, topDepth, seaLevel);
                        }
                    }

                    // 呼叫方法執行對應功能。
                    chunk.setRaw(lx, y, lz, (short) block.id());
                }
            }
        }

        // 使用迴圈逐一處理每個元素或區間。
        for (int lx = 0; lx < GameConfig.CHUNK_SIZE; lx++) {
            // 使用迴圈逐一處理每個元素或區間。
            for (int lz = 0; lz < GameConfig.CHUNK_SIZE; lz++) {
                // 宣告並初始化變數。
                int height = heights[lx][lz];
                // 根據條件決定是否進入此邏輯分支。
                if (height <= seaLevel + 1) {
                    // 跳過本次迴圈剩餘邏輯，直接進入下一次迭代。
                    continue;
                }

                // 宣告並初始化變數。
                int worldX = worldMinX + lx;
                // 宣告並初始化變數。
                int worldZ = worldMinZ + lz;
                // 宣告並初始化變數。
                TreeSpec tree = planTree(chunk, biomes[lx][lz], lx, height + 1, lz, worldX, worldZ);
                // 根據條件決定是否進入此邏輯分支。
                if (tree != null) {
                    // 呼叫方法執行對應功能。
                    plannedTrees.add(tree);
                }
            }
        }

        // 使用迴圈逐一處理每個元素或區間。
        for (TreeSpec tree : plannedTrees) {
            // 呼叫方法執行對應功能。
            placeTrunk(chunk, tree);
        }
        // 使用迴圈逐一處理每個元素或區間。
        for (TreeSpec tree : plannedTrees) {
            // 呼叫方法執行對應功能。
            placeCanopy(chunk, tree);
        }

        // 呼叫方法執行對應功能。
        chunk.markMeshDirty();
        // 呼叫方法執行對應功能。
        chunk.clearModified();
    }

    // 定義對外可呼叫的方法。
    public int seaLevel() {
        // 下一行程式碼負責執行目前步驟。
        return seaLevel;
    }

    // 定義對外可呼叫的方法。
    public int surfaceHeightAt(int worldX, int worldZ) {
        // 呼叫方法執行對應功能。
        return sampleSurface(worldX, worldZ).height();
    }

    // 定義對外可呼叫的方法。
    public int forestSpawnRegionScore(int centerX, int centerZ) {
        // 宣告並初始化變數。
        SurfaceSample center = sampleSurface(centerX, centerZ);
        // 根據條件決定是否進入此邏輯分支。
        if (center.height() <= seaLevel + 3 || center.biome() != Biome.FOREST) {
            // 下一行程式碼負責執行目前步驟。
            return Integer.MIN_VALUE;
        }

        // 宣告並初始化變數。
        int sampleRadius = 48;
        // 宣告並初始化變數。
        int sampleStep = 24;
        // 宣告並初始化變數。
        int sampleCount = 0;
        // 宣告並初始化變數。
        int forestCount = 0;
        // 宣告並初始化變數。
        int landCount = 0;
        // 宣告並初始化變數。
        int coastCount = 0;
        // 宣告並初始化變數。
        int mountainCount = 0;
        // 宣告並初始化變數。
        int minHeight = Integer.MAX_VALUE;
        // 宣告並初始化變數。
        int maxHeight = Integer.MIN_VALUE;

        // 使用粗網格估算「大片森林且遠離海岸」的程度。
        for (int dz = -sampleRadius; dz <= sampleRadius; dz += sampleStep) {
            // 使用迴圈逐一處理每個元素或區間。
            for (int dx = -sampleRadius; dx <= sampleRadius; dx += sampleStep) {
                // 宣告並初始化變數。
                SurfaceSample sample = sampleSurface(centerX + dx, centerZ + dz);
                // 設定或更新變數的值。
                sampleCount++;
                // 設定或更新變數的值。
                minHeight = Math.min(minHeight, sample.height());
                // 設定或更新變數的值。
                maxHeight = Math.max(maxHeight, sample.height());

                // 根據條件決定是否進入此邏輯分支。
                if (sample.height() > seaLevel + 2) {
                    // 設定或更新變數的值。
                    landCount++;
                // 下一行程式碼負責執行目前步驟。
                } else {
                    // 設定或更新變數的值。
                    coastCount++;
                }

                // 根據條件決定是否進入此邏輯分支。
                if (sample.biome() == Biome.FOREST) {
                    // 設定或更新變數的值。
                    forestCount++;
                }
                // 根據條件決定是否進入此邏輯分支。
                if (sample.biome() == Biome.MOUNTAIN) {
                    // 設定或更新變數的值。
                    mountainCount++;
                }
            }
        }

        // 大片森林重生點需要大多數樣本都是陸地且森林比例夠高。
        if (landCount < 20 || forestCount < 15) {
            // 下一行程式碼負責執行目前步驟。
            return Integer.MIN_VALUE;
        }

        // 宣告並初始化變數。
        int heightRange = maxHeight - minHeight;
        // 宣告並初始化變數。
        int inlandBonus = Math.round(Math.max(0.0f, center.continental() + 0.12f) * 60.0f);

        // 分數越高表示越像大片內陸森林，適合作為出生區中心。
        return forestCount * 9
                // 下一行程式碼負責執行目前步驟。
                + landCount * 6
                // 下一行程式碼負責執行目前步驟。
                + inlandBonus
                // 下一行程式碼負責執行目前步驟。
                - coastCount * 14
                // 下一行程式碼負責執行目前步驟。
                - mountainCount * 8
                // 下一行程式碼負責執行目前步驟。
                - Math.max(0, heightRange - 10) * 3;
    }

    // 定義類別內部使用的方法。
    private SurfaceSample sampleSurface(int worldX, int worldZ) {
        // 宣告並初始化變數。
        float continental = Noise.fbm2(worldX * 0.0019f, worldZ * 0.0019f, 5, 2.0f, 0.5f, seed + 17);
        // 宣告並初始化變數。
        float erosion = Noise.fbm2(worldX * 0.0036f, worldZ * 0.0036f, 4, 2.0f, 0.53f, seed + 23);
        // 宣告並初始化變數。
        float detail = Noise.fbm2(worldX * 0.0105f, worldZ * 0.0105f, 4, 2.1f, 0.50f, seed + 29);
        // 宣告並初始化變數。
        float ridges = Math.abs(Noise.fbm2(worldX * 0.0026f, worldZ * 0.0026f, 4, 2.0f, 0.5f, seed + 37));
        // 宣告並初始化變數。
        float temperature = Noise.fbm2(worldX * 0.0013f, worldZ * 0.0013f, 4, 2.0f, 0.5f, seed + 41);
        // 宣告並初始化變數。
        float moisture = Noise.fbm2(worldX * 0.0013f, worldZ * 0.0013f, 4, 2.0f, 0.5f, seed + 47);
        // 宣告並初始化變數。
        float weirdness = Noise.fbm2(worldX * 0.0048f, worldZ * 0.0048f, 3, 2.0f, 0.5f, seed + 53);

        // 宣告並初始化變數。
        float baseHeight = 57.0f
                // 下一行程式碼負責執行目前步驟。
                + continental * 20.0f
                // 下一行程式碼負責執行目前步驟。
                + detail * 7.5f
                // 下一行程式碼負責執行目前步驟。
                - Math.max(0.0f, -continental) * 8.0f
                // 呼叫方法執行對應功能。
                - Math.max(0.0f, -erosion) * 3.0f;
        // 宣告並初始化變數。
        float mountainMask = Math.max(0.0f, ridges - 0.27f) / 0.73f;
        // 宣告並初始化變數。
        float mountainBoost = mountainMask * mountainMask * (30.0f + Math.max(0.0f, weirdness) * 18.0f);
        // 宣告並初始化變數。
        int height = Math.round(baseHeight + mountainBoost);
        // 設定或更新變數的值。
        height = Math.max(6, Math.min(GameConfig.CHUNK_HEIGHT - 4, height));

        // 宣告並初始化變數。
        Biome biome = pickBiome(height, continental, ridges, temperature, moisture);
        // 呼叫方法執行對應功能。
        return new SurfaceSample(height, biome, continental);
    }

    // 定義類別內部使用的方法。
    private Biome pickBiome(int height, float continental, float ridges, float temperature, float moisture) {
        // 根據條件決定是否進入此邏輯分支。
        if (ridges > 0.72f && continental > -0.20f) {
            // 下一行程式碼負責執行目前步驟。
            return Biome.MOUNTAIN;
        }
        // 根據條件決定是否進入此邏輯分支。
        if (temperature > 0.45f && moisture < -0.05f) {
            // 下一行程式碼負責執行目前步驟。
            return Biome.DESERT;
        }
        // 根據條件決定是否進入此邏輯分支。
        if (temperature < -0.40f && continental > -0.25f) {
            // 下一行程式碼負責執行目前步驟。
            return Biome.SNOW;
        }
        // 根據條件決定是否進入此邏輯分支。
        if (temperature > 0.18f && moisture < -0.24f && height > seaLevel + 3) {
            // 下一行程式碼負責執行目前步驟。
            return Biome.BADLANDS;
        }
        // 根據條件決定是否進入此邏輯分支。
        if (moisture > 0.26f) {
            // 下一行程式碼負責執行目前步驟。
            return Biome.FOREST;
        }
        // 下一行程式碼負責執行目前步驟。
        return Biome.PLAINS;
    }

    // 定義類別內部使用的方法。
    private BlockType selectStrataBlock(Biome biome, int y, int height, int topDepth, int seaLevel) {
        // 根據條件決定是否進入此邏輯分支。
        if (y == height) {
            // 下一行程式碼負責執行目前步驟。
            return switch (biome) {
                // 宣告 switch 的其中一個分支。
                case DESERT, BADLANDS -> BlockType.SAND;
                // 宣告 switch 的其中一個分支。
                case SNOW -> height > seaLevel + 1 ? BlockType.SNOW : BlockType.SAND;
                // 宣告 switch 的其中一個分支。
                case MOUNTAIN -> height > 88 ? BlockType.SNOW : BlockType.STONE;
                // 設定或更新變數的值。
                default -> height <= seaLevel + 1 ? BlockType.SAND : BlockType.GRASS;
            };
        }

        // 根據條件決定是否進入此邏輯分支。
        if (y >= height - topDepth) {
            // 下一行程式碼負責執行目前步驟。
            return switch (biome) {
                // 宣告 switch 的其中一個分支。
                case DESERT, BADLANDS -> BlockType.SAND;
                // 宣告 switch 的其中一個分支。
                case MOUNTAIN -> (height > 82 && y >= height - 2) ? BlockType.STONE : BlockType.DIRT;
                // 下一行程式碼負責執行目前步驟。
                default -> BlockType.DIRT;
            };
        }

        // 下一行程式碼負責執行目前步驟。
        return switch (biome) {
            // 宣告 switch 的其中一個分支。
            case BADLANDS -> (y % 6 == 0) ? BlockType.DIRT : BlockType.STONE;
            // 下一行程式碼負責執行目前步驟。
            default -> BlockType.STONE;
        };
    }

    // 定義類別內部使用的方法。
    private TreeSpec planTree(Chunk chunk, Biome biome, int lx, int baseY, int lz, int worldX, int worldZ) {
        // 根據條件決定是否進入此邏輯分支。
        if (biome == Biome.DESERT || biome == Biome.BADLANDS || biome == Biome.MOUNTAIN) {
            // 下一行程式碼負責執行目前步驟。
            return null;
        }

        // 用格網抖動控制樹木間距，避免森林樹冠過度重疊看起來像超大樹。
        if (!matchesTreeGridSlot(biome, worldX, worldZ)) {
            // 下一行程式碼負責執行目前步驟。
            return null;
        }

        // 宣告並初始化變數。
        int hash = Noise.hashInt(worldX, 0, worldZ, seed + 191);
        // 宣告並初始化變數。
        int chance = switch (biome) {
            // 宣告 switch 的其中一個分支。
            case FOREST -> 34;
            // 宣告 switch 的其中一個分支。
            case SNOW -> 10;
            // 宣告 switch 的其中一個分支。
            case PLAINS -> 12;
            // 下一行程式碼負責執行目前步驟。
            default -> 0;
        };

        // 根據條件決定是否進入此邏輯分支。
        if ((hash & 0xFF) > chance) {
            // 下一行程式碼負責執行目前步驟。
            return null;
        }

        // 宣告並初始化變數。
        int trunkHeight = 3 + Math.abs(hash % 3);
        // 宣告並初始化變數。
        int topY = baseY + trunkHeight;

        // 根據條件決定是否進入此邏輯分支。
        if (topY + 3 >= GameConfig.CHUNK_HEIGHT) {
            // 下一行程式碼負責執行目前步驟。
            return null;
        }

        // 根據條件決定是否進入此邏輯分支。
        if (lx < 2 || lx >= GameConfig.CHUNK_SIZE - 2 || lz < 2 || lz >= GameConfig.CHUNK_SIZE - 2) {
            // 下一行程式碼負責執行目前步驟。
            return null;
        }

        // 使用迴圈逐一處理每個元素或區間。
        for (int y = 0; y < trunkHeight; y++) {
            // 根據條件決定是否進入此邏輯分支。
            if (chunk.get(lx, baseY + y, lz) != BlockType.AIR) {
                // 下一行程式碼負責執行目前步驟。
                return null;
            }
        }

        // 呼叫方法執行對應功能。
        return new TreeSpec(lx, lz, baseY, trunkHeight);
    }

    // 定義類別內部使用的方法。
    private boolean matchesTreeGridSlot(Biome biome, int worldX, int worldZ) {
        // 森林使用較大格網降低密度，其他地形保留較小格網。
        int cellSize = biome == Biome.FOREST ? 5 : 6;
        // 宣告並初始化變數。
        int cellX = Math.floorDiv(worldX, cellSize);
        // 宣告並初始化變數。
        int cellZ = Math.floorDiv(worldZ, cellSize);
        // 宣告並初始化變數。
        int localX = Math.floorMod(worldX, cellSize);
        // 宣告並初始化變數。
        int localZ = Math.floorMod(worldZ, cellSize);
        // 宣告並初始化變數。
        int cellHash = Noise.hashInt(cellX, 97, cellZ, seed + 173);
        // 宣告並初始化變數。
        int slotX = Math.floorMod(cellHash, cellSize);
        // 宣告並初始化變數。
        int slotZ = Math.floorMod(cellHash >>> 8, cellSize);
        // 下一行程式碼負責執行目前步驟。
        return localX == slotX && localZ == slotZ;
    }

    // 定義類別內部使用的方法。
    private void placeTrunk(Chunk chunk, TreeSpec tree) {
        // 使用迴圈逐一處理每個元素或區間。
        for (int y = 0; y < tree.trunkHeight(); y++) {
            // 呼叫方法執行對應功能。
            chunk.setRaw(tree.lx(), tree.baseY() + y, tree.lz(), (short) BlockType.LOG.id());
        }
    }

    // 定義類別內部使用的方法。
    private void placeCanopy(Chunk chunk, TreeSpec tree) {
        // 宣告並初始化變數。
        int topY = tree.topY();
        // 宣告並初始化變數。
        int canopyBaseY = tree.baseY() + Math.max(1, tree.trunkHeight() - 4);

        // 使用迴圈逐一處理每個元素或區間。
        for (int y = canopyBaseY; y <= topY + 2; y++) {
            // 宣告並初始化變數。
            int rel = y - topY;
            // 下一行程式碼負責執行目前步驟。
            int radius;
            // 下一行程式碼負責執行目前步驟。
            boolean trimCorners;

            // 根據條件決定是否進入此邏輯分支。
            if (rel <= -3) {
                // 設定或更新變數的值。
                radius = 1;
                // 設定或更新變數的值。
                trimCorners = false;
            // 下一行程式碼負責執行目前步驟。
            } else if (rel <= 0) {
                // 設定或更新變數的值。
                radius = 2;
                // 設定或更新變數的值。
                trimCorners = rel == 0;
            // 下一行程式碼負責執行目前步驟。
            } else if (rel == 1) {
                // 設定或更新變數的值。
                radius = 1;
                // 設定或更新變數的值。
                trimCorners = false;
            // 下一行程式碼負責執行目前步驟。
            } else {
                // 設定或更新變數的值。
                radius = 0;
                // 設定或更新變數的值。
                trimCorners = false;
            }

            // 呼叫方法執行對應功能。
            placeLeafLayer(chunk, tree.lx(), y, tree.lz(), radius, trimCorners);
        }

        // 宣告並初始化變數。
        int wrapStart = Math.max(tree.baseY() + 1, topY - 3);
        // 使用迴圈逐一處理每個元素或區間。
        for (int y = wrapStart; y <= topY - 1; y++) {
            // 呼叫方法執行對應功能。
            trySetLeaf(chunk, tree.lx() + 1, y, tree.lz());
            // 呼叫方法執行對應功能。
            trySetLeaf(chunk, tree.lx() - 1, y, tree.lz());
            // 呼叫方法執行對應功能。
            trySetLeaf(chunk, tree.lx(), y, tree.lz() + 1);
            // 呼叫方法執行對應功能。
            trySetLeaf(chunk, tree.lx(), y, tree.lz() - 1);
        }
    }

    // 定義類別內部使用的方法。
    private void placeLeafLayer(Chunk chunk, int centerX, int y, int centerZ, int radius, boolean trimCorners) {
        // 根據條件決定是否進入此邏輯分支。
        if (radius <= 0) {
            // 呼叫方法執行對應功能。
            trySetLeaf(chunk, centerX, y, centerZ);
            // 下一行程式碼負責執行目前步驟。
            return;
        }

        // 使用迴圈逐一處理每個元素或區間。
        for (int dx = -radius; dx <= radius; dx++) {
            // 使用迴圈逐一處理每個元素或區間。
            for (int dz = -radius; dz <= radius; dz++) {
                // 根據條件決定是否進入此邏輯分支。
                if (trimCorners && Math.abs(dx) == radius && Math.abs(dz) == radius) {
                    // 跳過本次迴圈剩餘邏輯，直接進入下一次迭代。
                    continue;
                }
                // 呼叫方法執行對應功能。
                trySetLeaf(chunk, centerX + dx, y, centerZ + dz);
            }
        }
    }

    // 定義類別內部使用的方法。
    private void trySetLeaf(Chunk chunk, int x, int y, int z) {
        // 根據條件決定是否進入此邏輯分支。
        if (x < 0 || x >= GameConfig.CHUNK_SIZE || z < 0 || z >= GameConfig.CHUNK_SIZE) {
            // 下一行程式碼負責執行目前步驟。
            return;
        }
        // 根據條件決定是否進入此邏輯分支。
        if (y < 1 || y >= GameConfig.CHUNK_HEIGHT) {
            // 下一行程式碼負責執行目前步驟。
            return;
        }

        // 根據條件決定是否進入此邏輯分支。
        if (chunk.get(x, y, z) == BlockType.AIR) {
            // 呼叫方法執行對應功能。
            chunk.setRaw(x, y, z, (short) BlockType.LEAVES.id());
        }
    }

    // 定義主要型別與其結構。
    private record TreeSpec(int lx, int lz, int baseY, int trunkHeight) {
        // 下一行程式碼負責執行目前步驟。
        int topY() {
            // 下一行程式碼負責執行目前步驟。
            return baseY + trunkHeight;
        }
    }

    // 定義地表採樣結果，供出生點搜尋等邏輯重複使用。
    private record SurfaceSample(int height, Biome biome, float continental) {
    }
}

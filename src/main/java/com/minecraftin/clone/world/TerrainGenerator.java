// 說明：宣告此檔案所屬的套件。
package com.minecraftin.clone.world;

// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.config.GameConfig;
// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.util.Noise;

// 說明：匯入後續會使用到的型別或函式。
import java.util.ArrayList;
// 說明：匯入後續會使用到的型別或函式。
import java.util.List;

// 說明：定義主要型別與其結構。
public final class TerrainGenerator {
    // 說明：定義主要型別與其結構。
    private enum Biome {
        // 說明：下一行程式碼負責執行目前步驟。
        PLAINS,
        // 說明：下一行程式碼負責執行目前步驟。
        FOREST,
        // 說明：下一行程式碼負責執行目前步驟。
        DESERT,
        // 說明：下一行程式碼負責執行目前步驟。
        SNOW,
        // 說明：下一行程式碼負責執行目前步驟。
        MOUNTAIN,
        // 說明：下一行程式碼負責執行目前步驟。
        BADLANDS
    }

    // 說明：下一行程式碼負責執行目前步驟。
    private final long seed;
    // 說明：下一行程式碼負責執行目前步驟。
    private final int seaLevel;

    // 說明：定義對外可呼叫的方法。
    public TerrainGenerator(long seed, int seaLevel) {
        // 說明：設定或更新變數的值。
        this.seed = seed;
        // 說明：設定或更新變數的值。
        this.seaLevel = seaLevel;
    }

    // 說明：定義對外可呼叫的方法。
    public void generate(Chunk chunk) {
        // 說明：宣告並初始化變數。
        int worldMinX = chunk.worldMinX();
        // 說明：宣告並初始化變數。
        int worldMinZ = chunk.worldMinZ();
        // 說明：宣告並初始化變數。
        int[][] heights = new int[GameConfig.CHUNK_SIZE][GameConfig.CHUNK_SIZE];
        // 說明：宣告並初始化變數。
        Biome[][] biomes = new Biome[GameConfig.CHUNK_SIZE][GameConfig.CHUNK_SIZE];
        // 說明：宣告並初始化變數。
        List<TreeSpec> plannedTrees = new ArrayList<>();

        // 說明：使用迴圈逐一處理每個元素或區間。
        for (int lx = 0; lx < GameConfig.CHUNK_SIZE; lx++) {
            // 說明：使用迴圈逐一處理每個元素或區間。
            for (int lz = 0; lz < GameConfig.CHUNK_SIZE; lz++) {
                // 說明：宣告並初始化變數。
                int worldX = worldMinX + lx;
                // 說明：宣告並初始化變數。
                int worldZ = worldMinZ + lz;

                // 說明：宣告並初始化變數。
                float continental = Noise.fbm2(worldX * 0.0019f, worldZ * 0.0019f, 5, 2.0f, 0.5f, seed + 17);
                // 說明：宣告並初始化變數。
                float erosion = Noise.fbm2(worldX * 0.0036f, worldZ * 0.0036f, 4, 2.0f, 0.53f, seed + 23);
                // 說明：宣告並初始化變數。
                float detail = Noise.fbm2(worldX * 0.0105f, worldZ * 0.0105f, 4, 2.1f, 0.50f, seed + 29);
                // 說明：宣告並初始化變數。
                float ridges = Math.abs(Noise.fbm2(worldX * 0.0026f, worldZ * 0.0026f, 4, 2.0f, 0.5f, seed + 37));
                // 說明：宣告並初始化變數。
                float temperature = Noise.fbm2(worldX * 0.0013f, worldZ * 0.0013f, 4, 2.0f, 0.5f, seed + 41);
                // 說明：宣告並初始化變數。
                float moisture = Noise.fbm2(worldX * 0.0013f, worldZ * 0.0013f, 4, 2.0f, 0.5f, seed + 47);
                // 說明：宣告並初始化變數。
                float weirdness = Noise.fbm2(worldX * 0.0048f, worldZ * 0.0048f, 3, 2.0f, 0.5f, seed + 53);

                // 說明：宣告並初始化變數。
                float baseHeight = 57.0f
                        // 說明：下一行程式碼負責執行目前步驟。
                        + continental * 20.0f
                        // 說明：下一行程式碼負責執行目前步驟。
                        + detail * 7.5f
                        // 說明：下一行程式碼負責執行目前步驟。
                        - Math.max(0.0f, -continental) * 8.0f
                        // 說明：呼叫方法執行對應功能。
                        - Math.max(0.0f, -erosion) * 3.0f;

                // 說明：宣告並初始化變數。
                float mountainMask = Math.max(0.0f, ridges - 0.27f) / 0.73f;
                // 說明：宣告並初始化變數。
                float mountainBoost = mountainMask * mountainMask * (30.0f + Math.max(0.0f, weirdness) * 18.0f);
                // 說明：宣告並初始化變數。
                int height = Math.round(baseHeight + mountainBoost);
                // 說明：設定或更新變數的值。
                height = Math.max(6, Math.min(GameConfig.CHUNK_HEIGHT - 4, height));

                // 說明：宣告並初始化變數。
                Biome biome = pickBiome(height, continental, ridges, temperature, moisture);
                // 說明：宣告並初始化變數。
                int topDepth = biome == Biome.DESERT || biome == Biome.BADLANDS ? 5 : 4;
                // 說明：設定或更新變數的值。
                heights[lx][lz] = height;
                // 說明：設定或更新變數的值。
                biomes[lx][lz] = biome;

                // 說明：使用迴圈逐一處理每個元素或區間。
                for (int y = 0; y < GameConfig.CHUNK_HEIGHT; y++) {
                    // 說明：下一行程式碼負責執行目前步驟。
                    BlockType block;

                    // 說明：根據條件決定是否進入此邏輯分支。
                    if (y == 0) {
                        // 說明：設定或更新變數的值。
                        block = BlockType.BEDROCK;
                    // 說明：下一行程式碼負責執行目前步驟。
                    } else if (y > height) {
                        // 說明：設定或更新變數的值。
                        block = y <= seaLevel ? BlockType.WATER : BlockType.AIR;
                    // 說明：下一行程式碼負責執行目前步驟。
                    } else {
                        // 說明：宣告並初始化變數。
                        float cave = Noise.fbm3(worldX * 0.055f, y * 0.090f, worldZ * 0.055f, 3, 2.0f, 0.5f, seed + 73);
                        // 說明：宣告並初始化變數。
                        float caveThreshold = biome == Biome.MOUNTAIN ? 0.58f : 0.64f;
                        // 說明：根據條件決定是否進入此邏輯分支。
                        if (y > 5 && y < height - 3 && cave > caveThreshold) {
                            // 說明：設定或更新變數的值。
                            block = y <= seaLevel ? BlockType.WATER : BlockType.AIR;
                        // 說明：下一行程式碼負責執行目前步驟。
                        } else {
                            // 說明：設定或更新變數的值。
                            block = selectStrataBlock(biome, y, height, topDepth, seaLevel);
                        }
                    }

                    // 說明：呼叫方法執行對應功能。
                    chunk.setRaw(lx, y, lz, (short) block.id());
                }
            }
        }

        // 說明：使用迴圈逐一處理每個元素或區間。
        for (int lx = 0; lx < GameConfig.CHUNK_SIZE; lx++) {
            // 說明：使用迴圈逐一處理每個元素或區間。
            for (int lz = 0; lz < GameConfig.CHUNK_SIZE; lz++) {
                // 說明：宣告並初始化變數。
                int height = heights[lx][lz];
                // 說明：根據條件決定是否進入此邏輯分支。
                if (height <= seaLevel + 1) {
                    // 說明：跳過本次迴圈剩餘邏輯，直接進入下一次迭代。
                    continue;
                }

                // 說明：宣告並初始化變數。
                int worldX = worldMinX + lx;
                // 說明：宣告並初始化變數。
                int worldZ = worldMinZ + lz;
                // 說明：宣告並初始化變數。
                TreeSpec tree = planTree(chunk, biomes[lx][lz], lx, height + 1, lz, worldX, worldZ);
                // 說明：根據條件決定是否進入此邏輯分支。
                if (tree != null) {
                    // 說明：呼叫方法執行對應功能。
                    plannedTrees.add(tree);
                }
            }
        }

        // 說明：使用迴圈逐一處理每個元素或區間。
        for (TreeSpec tree : plannedTrees) {
            // 說明：呼叫方法執行對應功能。
            placeTrunk(chunk, tree);
        }
        // 說明：使用迴圈逐一處理每個元素或區間。
        for (TreeSpec tree : plannedTrees) {
            // 說明：呼叫方法執行對應功能。
            placeCanopy(chunk, tree);
        }

        // 說明：呼叫方法執行對應功能。
        chunk.markMeshDirty();
        // 說明：呼叫方法執行對應功能。
        chunk.clearModified();
    }

    // 說明：定義對外可呼叫的方法。
    public int seaLevel() {
        // 說明：下一行程式碼負責執行目前步驟。
        return seaLevel;
    }

    // 說明：定義類別內部使用的方法。
    private Biome pickBiome(int height, float continental, float ridges, float temperature, float moisture) {
        // 說明：根據條件決定是否進入此邏輯分支。
        if (ridges > 0.72f && continental > -0.20f) {
            // 說明：下一行程式碼負責執行目前步驟。
            return Biome.MOUNTAIN;
        }
        // 說明：根據條件決定是否進入此邏輯分支。
        if (temperature > 0.45f && moisture < -0.05f) {
            // 說明：下一行程式碼負責執行目前步驟。
            return Biome.DESERT;
        }
        // 說明：根據條件決定是否進入此邏輯分支。
        if (temperature < -0.40f && continental > -0.25f) {
            // 說明：下一行程式碼負責執行目前步驟。
            return Biome.SNOW;
        }
        // 說明：根據條件決定是否進入此邏輯分支。
        if (temperature > 0.18f && moisture < -0.24f && height > seaLevel + 3) {
            // 說明：下一行程式碼負責執行目前步驟。
            return Biome.BADLANDS;
        }
        // 說明：根據條件決定是否進入此邏輯分支。
        if (moisture > 0.26f) {
            // 說明：下一行程式碼負責執行目前步驟。
            return Biome.FOREST;
        }
        // 說明：下一行程式碼負責執行目前步驟。
        return Biome.PLAINS;
    }

    // 說明：定義類別內部使用的方法。
    private BlockType selectStrataBlock(Biome biome, int y, int height, int topDepth, int seaLevel) {
        // 說明：根據條件決定是否進入此邏輯分支。
        if (y == height) {
            // 說明：下一行程式碼負責執行目前步驟。
            return switch (biome) {
                // 說明：宣告 switch 的其中一個分支。
                case DESERT, BADLANDS -> BlockType.SAND;
                // 說明：宣告 switch 的其中一個分支。
                case SNOW -> height > seaLevel + 1 ? BlockType.SNOW : BlockType.SAND;
                // 說明：宣告 switch 的其中一個分支。
                case MOUNTAIN -> height > 88 ? BlockType.SNOW : BlockType.STONE;
                // 說明：設定或更新變數的值。
                default -> height <= seaLevel + 1 ? BlockType.SAND : BlockType.GRASS;
            };
        }

        // 說明：根據條件決定是否進入此邏輯分支。
        if (y >= height - topDepth) {
            // 說明：下一行程式碼負責執行目前步驟。
            return switch (biome) {
                // 說明：宣告 switch 的其中一個分支。
                case DESERT, BADLANDS -> BlockType.SAND;
                // 說明：宣告 switch 的其中一個分支。
                case MOUNTAIN -> (height > 82 && y >= height - 2) ? BlockType.STONE : BlockType.DIRT;
                // 說明：下一行程式碼負責執行目前步驟。
                default -> BlockType.DIRT;
            };
        }

        // 說明：下一行程式碼負責執行目前步驟。
        return switch (biome) {
            // 說明：宣告 switch 的其中一個分支。
            case BADLANDS -> (y % 6 == 0) ? BlockType.DIRT : BlockType.STONE;
            // 說明：下一行程式碼負責執行目前步驟。
            default -> BlockType.STONE;
        };
    }

    // 說明：定義類別內部使用的方法。
    private TreeSpec planTree(Chunk chunk, Biome biome, int lx, int baseY, int lz, int worldX, int worldZ) {
        // 說明：根據條件決定是否進入此邏輯分支。
        if (biome == Biome.DESERT || biome == Biome.BADLANDS || biome == Biome.MOUNTAIN) {
            // 說明：下一行程式碼負責執行目前步驟。
            return null;
        }

        // 說明：宣告並初始化變數。
        int hash = Noise.hashInt(worldX, 0, worldZ, seed + 191);
        // 說明：宣告並初始化變數。
        int chance = switch (biome) {
            // 說明：宣告 switch 的其中一個分支。
            case FOREST -> 56;
            // 說明：宣告 switch 的其中一個分支。
            case SNOW -> 10;
            // 說明：宣告 switch 的其中一個分支。
            case PLAINS -> 18;
            // 說明：下一行程式碼負責執行目前步驟。
            default -> 0;
        };

        // 說明：根據條件決定是否進入此邏輯分支。
        if ((hash & 0xFF) > chance) {
            // 說明：下一行程式碼負責執行目前步驟。
            return null;
        }

        // 說明：宣告並初始化變數。
        int trunkHeight = 4 + Math.abs(hash % 3);
        // 說明：宣告並初始化變數。
        int topY = baseY + trunkHeight;

        // 說明：根據條件決定是否進入此邏輯分支。
        if (topY + 3 >= GameConfig.CHUNK_HEIGHT) {
            // 說明：下一行程式碼負責執行目前步驟。
            return null;
        }

        // 說明：根據條件決定是否進入此邏輯分支。
        if (lx < 2 || lx >= GameConfig.CHUNK_SIZE - 2 || lz < 2 || lz >= GameConfig.CHUNK_SIZE - 2) {
            // 說明：下一行程式碼負責執行目前步驟。
            return null;
        }

        // 說明：使用迴圈逐一處理每個元素或區間。
        for (int y = 0; y < trunkHeight; y++) {
            // 說明：根據條件決定是否進入此邏輯分支。
            if (chunk.get(lx, baseY + y, lz) != BlockType.AIR) {
                // 說明：下一行程式碼負責執行目前步驟。
                return null;
            }
        }

        // 說明：呼叫方法執行對應功能。
        return new TreeSpec(lx, lz, baseY, trunkHeight);
    }

    // 說明：定義類別內部使用的方法。
    private void placeTrunk(Chunk chunk, TreeSpec tree) {
        // 說明：使用迴圈逐一處理每個元素或區間。
        for (int y = 0; y < tree.trunkHeight(); y++) {
            // 說明：呼叫方法執行對應功能。
            chunk.setRaw(tree.lx(), tree.baseY() + y, tree.lz(), (short) BlockType.LOG.id());
        }
    }

    // 說明：定義類別內部使用的方法。
    private void placeCanopy(Chunk chunk, TreeSpec tree) {
        // 說明：宣告並初始化變數。
        int topY = tree.topY();
        // 說明：宣告並初始化變數。
        int canopyBaseY = tree.baseY() + Math.max(1, tree.trunkHeight() - 4);

        // 說明：使用迴圈逐一處理每個元素或區間。
        for (int y = canopyBaseY; y <= topY + 2; y++) {
            // 說明：宣告並初始化變數。
            int rel = y - topY;
            // 說明：下一行程式碼負責執行目前步驟。
            int radius;
            // 說明：下一行程式碼負責執行目前步驟。
            boolean trimCorners;

            // 說明：根據條件決定是否進入此邏輯分支。
            if (rel <= -3) {
                // 說明：設定或更新變數的值。
                radius = 1;
                // 說明：設定或更新變數的值。
                trimCorners = false;
            // 說明：下一行程式碼負責執行目前步驟。
            } else if (rel <= 0) {
                // 說明：設定或更新變數的值。
                radius = 2;
                // 說明：設定或更新變數的值。
                trimCorners = rel == 0;
            // 說明：下一行程式碼負責執行目前步驟。
            } else if (rel == 1) {
                // 說明：設定或更新變數的值。
                radius = 1;
                // 說明：設定或更新變數的值。
                trimCorners = false;
            // 說明：下一行程式碼負責執行目前步驟。
            } else {
                // 說明：設定或更新變數的值。
                radius = 0;
                // 說明：設定或更新變數的值。
                trimCorners = false;
            }

            // 說明：呼叫方法執行對應功能。
            placeLeafLayer(chunk, tree.lx(), y, tree.lz(), radius, trimCorners);
        }

        // 說明：宣告並初始化變數。
        int wrapStart = Math.max(tree.baseY() + 1, topY - 3);
        // 說明：使用迴圈逐一處理每個元素或區間。
        for (int y = wrapStart; y <= topY - 1; y++) {
            // 說明：呼叫方法執行對應功能。
            trySetLeaf(chunk, tree.lx() + 1, y, tree.lz());
            // 說明：呼叫方法執行對應功能。
            trySetLeaf(chunk, tree.lx() - 1, y, tree.lz());
            // 說明：呼叫方法執行對應功能。
            trySetLeaf(chunk, tree.lx(), y, tree.lz() + 1);
            // 說明：呼叫方法執行對應功能。
            trySetLeaf(chunk, tree.lx(), y, tree.lz() - 1);
        }
    }

    // 說明：定義類別內部使用的方法。
    private void placeLeafLayer(Chunk chunk, int centerX, int y, int centerZ, int radius, boolean trimCorners) {
        // 說明：根據條件決定是否進入此邏輯分支。
        if (radius <= 0) {
            // 說明：呼叫方法執行對應功能。
            trySetLeaf(chunk, centerX, y, centerZ);
            // 說明：下一行程式碼負責執行目前步驟。
            return;
        }

        // 說明：使用迴圈逐一處理每個元素或區間。
        for (int dx = -radius; dx <= radius; dx++) {
            // 說明：使用迴圈逐一處理每個元素或區間。
            for (int dz = -radius; dz <= radius; dz++) {
                // 說明：根據條件決定是否進入此邏輯分支。
                if (trimCorners && Math.abs(dx) == radius && Math.abs(dz) == radius) {
                    // 說明：跳過本次迴圈剩餘邏輯，直接進入下一次迭代。
                    continue;
                }
                // 說明：呼叫方法執行對應功能。
                trySetLeaf(chunk, centerX + dx, y, centerZ + dz);
            }
        }
    }

    // 說明：定義類別內部使用的方法。
    private void trySetLeaf(Chunk chunk, int x, int y, int z) {
        // 說明：根據條件決定是否進入此邏輯分支。
        if (x < 0 || x >= GameConfig.CHUNK_SIZE || z < 0 || z >= GameConfig.CHUNK_SIZE) {
            // 說明：下一行程式碼負責執行目前步驟。
            return;
        }
        // 說明：根據條件決定是否進入此邏輯分支。
        if (y < 1 || y >= GameConfig.CHUNK_HEIGHT) {
            // 說明：下一行程式碼負責執行目前步驟。
            return;
        }

        // 說明：根據條件決定是否進入此邏輯分支。
        if (chunk.get(x, y, z) == BlockType.AIR) {
            // 說明：呼叫方法執行對應功能。
            chunk.setRaw(x, y, z, (short) BlockType.LEAVES.id());
        }
    }

    // 說明：定義主要型別與其結構。
    private record TreeSpec(int lx, int lz, int baseY, int trunkHeight) {
        // 說明：下一行程式碼負責執行目前步驟。
        int topY() {
            // 說明：下一行程式碼負責執行目前步驟。
            return baseY + trunkHeight;
        }
    }
}

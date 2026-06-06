package com.minecraftin.clone.world;

import com.minecraftin.clone.config.GameConfig;
import com.minecraftin.clone.util.Noise;

import java.util.ArrayList;
import java.util.List;

// 負責依照種子產生地形、地表材質與樹木。
// 所有取樣都必須只依賴世界座標與 seed，確保同一個 Chunk 不論何時生成都得到相同結果。
public final class TerrainGenerator {

    // 表示不同的地形區域種類。
    private enum Biome {
        PLAINS,
        FOREST,
        DESERT,
        SNOW,
        MOUNTAIN,
        BADLANDS
    }

    // 讓同一個世界可重現相同地形。
    private final long seed;

    // 海平面高度。
    private final int seaLevel;

    // 建立地形產生器。
    public TerrainGenerator(long seed, int seaLevel) {
        this.seed = seed;
        this.seaLevel = seaLevel;
    }

    // 產生一個 Chunk 的所有地形資料。
    // 流程分成地層、水/洞穴、樹木規劃、樹幹、樹葉，避免樹葉先放後被地形覆蓋。
    public void generate(Chunk chunk) {
        int worldMinX = chunk.worldMinX();
        int worldMinZ = chunk.worldMinZ();

        int columnCount = GameConfig.CHUNK_SIZE * GameConfig.CHUNK_SIZE;

        // 記錄每個位置的地表高度。
        int[] heights = new int[columnCount];

        // 記錄每個位置的生態區。
        Biome[] biomes = new Biome[columnCount];

        // 先記下要生成的樹，等地形都完成後再放置；這樣樹木不會干擾同一 Chunk 的地表高度判斷。
        List<TreeSpec> plannedTrees = new ArrayList<>(16);

        // 先產生方塊地形。
        for (int lx = 0; lx < GameConfig.CHUNK_SIZE; lx++) {
            for (int lz = 0; lz < GameConfig.CHUNK_SIZE; lz++) {
                int worldX = worldMinX + lx;
                int worldZ = worldMinZ + lz;

                // 取得這個座標的地表資訊。
                SurfaceSample surface = sampleSurface(worldX, worldZ);
                int height = surface.height();
                Biome biome = surface.biome();

                // 沙漠與惡地的表層較厚。
                int topDepth = biome == Biome.DESERT || biome == Biome.BADLANDS ? 5 : 4;

                int columnIndex = columnIndex(lx, lz);
                heights[columnIndex] = height;
                biomes[columnIndex] = biome;

                for (int y = 0; y < GameConfig.CHUNK_HEIGHT; y++) {
                    BlockType block;

                    // 最底層固定為基岩。
                    if (y == 0) {
                        block = BlockType.BEDROCK;

                        // 高於地表時，海平面以下填水，海平面以上填空氣。
                    } else if (y > height) {
                        block = y <= seaLevel ? BlockType.WATER : BlockType.AIR;

                    } else {
                        // 在地底加入洞穴噪音。
                        float cave = Noise.fbm3(worldX * 0.055f, y * 0.090f, worldZ * 0.055f, 3, 2.0f, 0.5f, seed + 73);
                        float caveThreshold = biome == Biome.MOUNTAIN ? 0.58f : 0.64f;

                        // 符合條件時把該位置挖空成洞穴。
                        if (y > 5 && y < height - 3 && cave > caveThreshold) {
                            block = y <= seaLevel ? BlockType.WATER : BlockType.AIR;
                        } else {
                            // 根據生態區與深度決定地層方塊。
                            block = selectStrataBlock(biome, y, height, topDepth, seaLevel);
                        }
                    }

                    chunk.setRaw(lx, y, lz, (short) block.id());
                }
            }
        }

        // 再規劃樹木要長在哪裡。
        for (int lx = 0; lx < GameConfig.CHUNK_SIZE; lx++) {
            for (int lz = 0; lz < GameConfig.CHUNK_SIZE; lz++) {
                int columnIndex = columnIndex(lx, lz);
                int height = heights[columnIndex];

                // 海平面太低的地方不生成樹。
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

        // 先放樹幹，再放樹葉。
        for (TreeSpec tree : plannedTrees) {
            placeTrunk(chunk, tree);
        }
        for (TreeSpec tree : plannedTrees) {
            placeCanopy(chunk, tree);
        }

        // 生成完後要重新建立模型，但不算玩家修改。
        chunk.markMeshDirty();
        chunk.clearModified();
    }

    // 回傳海平面高度。
    public int seaLevel() {
        return seaLevel;
    }

    // 將 Chunk 內 X/Z 欄位轉成一維暫存陣列索引，避免每次生成 Chunk 都建立多個小陣列。
    private static int columnIndex(int localX, int localZ) {
        return localX * GameConfig.CHUNK_SIZE + localZ;
    }

    // 回傳指定世界座標的地表高度。
    public int surfaceHeightAt(int worldX, int worldZ) {
        return sampleSurface(worldX, worldZ).height();
    }

    // 計算某個位置是否適合當作大片森林出生區中心。
    public int forestSpawnRegionScore(int centerX, int centerZ) {
        SurfaceSample center = sampleSurface(centerX, centerZ);

        // 中心點必須是森林，且不能太接近海面。
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

        // 在周圍取樣，評估這片區域是否夠大、夠平穩、夠像森林內陸。
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

        // 陸地與森林比例不足時，直接視為不適合。
        if (landCount < 20 || forestCount < 15) {
            return Integer.MIN_VALUE;
        }

        int heightRange = maxHeight - minHeight;

        // 大陸性越高，代表越偏內陸，分數越高。
        int inlandBonus = Math.round(Math.max(0.0f, center.continental() + 0.12f) * 60.0f);

        return forestCount * 9
                + landCount * 6
                + inlandBonus
                - coastCount * 14
                - mountainCount * 8
                - Math.max(0, heightRange - 10) * 3;
    }

    // 取樣某個座標的地表高度、生態區與大陸性。
    // 這是地形生成的核心函式，任何 seed 偏移值調整都會改變整個世界外觀。
    private SurfaceSample sampleSurface(int worldX, int worldZ) {
        float continental = Noise.fbm2(worldX * 0.0019f, worldZ * 0.0019f, 5, 2.0f, 0.5f, seed + 17);
        float erosion = Noise.fbm2(worldX * 0.0036f, worldZ * 0.0036f, 4, 2.0f, 0.53f, seed + 23);
        float detail = Noise.fbm2(worldX * 0.0105f, worldZ * 0.0105f, 4, 2.1f, 0.50f, seed + 29);
        float ridges = Math.abs(Noise.fbm2(worldX * 0.0026f, worldZ * 0.0026f, 4, 2.0f, 0.5f, seed + 37));
        float temperature = Noise.fbm2(worldX * 0.0013f, worldZ * 0.0013f, 4, 2.0f, 0.5f, seed + 41);
        float moisture = Noise.fbm2(worldX * 0.0013f, worldZ * 0.0013f, 4, 2.0f, 0.5f, seed + 47);
        float weirdness = Noise.fbm2(worldX * 0.0048f, worldZ * 0.0048f, 3, 2.0f, 0.5f, seed + 53);

        // 先用多組噪音決定基礎高度。
        float baseHeight = 57.0f
                + continental * 20.0f
                + detail * 7.5f
                - Math.max(0.0f, -continental) * 8.0f
                - Math.max(0.0f, -erosion) * 3.0f;

        // 山地地形會額外抬高高度。
        float mountainMask = Math.max(0.0f, ridges - 0.27f) / 0.73f;
        float mountainBoost = mountainMask * mountainMask * (30.0f + Math.max(0.0f, weirdness) * 18.0f);

        int height = Math.round(baseHeight + mountainBoost);
        height = Math.max(6, Math.min(GameConfig.CHUNK_HEIGHT - 4, height));

        Biome biome = pickBiome(height, continental, ridges, temperature, moisture);
        return new SurfaceSample(height, biome, continental);
    }

    // 根據高度與多種噪音值決定這裡屬於哪種生態區。
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

    // 根據生態區與深度決定某一層應該用哪種方塊。
    private BlockType selectStrataBlock(Biome biome, int y, int height, int topDepth, int seaLevel) {
        // 最上層通常是地表。
        if (y == height) {
            return switch (biome) {
                case DESERT, BADLANDS -> BlockType.SAND;
                case SNOW -> height > seaLevel + 1 ? BlockType.SNOW : BlockType.SAND;
                case MOUNTAIN -> height > 88 ? BlockType.SNOW : BlockType.STONE;
                default -> height <= seaLevel + 1 ? BlockType.SAND : BlockType.GRASS;
            };
        }

        // 接近地表的幾層通常是泥土或沙。
        if (y >= height - topDepth) {
            return switch (biome) {
                case DESERT, BADLANDS -> BlockType.SAND;
                case MOUNTAIN -> (height > 82 && y >= height - 2) ? BlockType.STONE : BlockType.DIRT;
                default -> BlockType.DIRT;
            };
        }

        // 更深處大多是石頭，惡地偶爾混入泥土。
        return switch (biome) {
            case BADLANDS -> (y % 6 == 0) ? BlockType.DIRT : BlockType.STONE;
            default -> BlockType.STONE;
        };
    }

    // 規劃某個位置是否要長一棵樹，以及樹的基本資訊。
    // 目前只允許樹完整落在單一 Chunk 內，避免跨 Chunk 生成順序造成樹冠缺口或重複。
    private TreeSpec planTree(Chunk chunk, Biome biome, int lx, int baseY, int lz, int worldX, int worldZ) {
        // 某些生態區不長樹。
        if (biome == Biome.DESERT || biome == Biome.BADLANDS || biome == Biome.MOUNTAIN) {
            return null;
        }

        // 只有符合格網位置的座標才有機會生成樹，避免太密。
        if (!matchesTreeGridSlot(biome, worldX, worldZ)) {
            return null;
        }

        int hash = Noise.hashInt(worldX, 0, worldZ, seed + 191);

        // 不同生態區有不同機率長樹。
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

        // 樹太高會超出世界高度時，不生成。
        if (topY + 3 >= GameConfig.CHUNK_HEIGHT) {
            return null;
        }

        // 太靠近 Chunk 邊界時不生成，避免樹冠被切掉。
        if (lx < 2 || lx >= GameConfig.CHUNK_SIZE - 2 || lz < 2 || lz >= GameConfig.CHUNK_SIZE - 2) {
            return null;
        }

        // 樹幹位置必須是空氣。
        for (int y = 0; y < trunkHeight; y++) {
            if (chunk.get(lx, baseY + y, lz) != BlockType.AIR) {
                return null;
            }
        }

        return new TreeSpec(lx, lz, baseY, trunkHeight);
    }

    // 用格網加上隨機偏移，控制樹木分布位置；比逐格純機率更能避免樹木成片擠在一起。
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

    // 放置樹幹。
    private void placeTrunk(Chunk chunk, TreeSpec tree) {
        for (int y = 0; y < tree.trunkHeight(); y++) {
            chunk.setRaw(tree.lx(), tree.baseY() + y, tree.lz(), (short) BlockType.LOG.id());
        }
    }

    // 放置樹冠。
    private void placeCanopy(Chunk chunk, TreeSpec tree) {
        int topY = tree.topY();

        // 樹葉從樹幹上半部開始包覆。
        int canopyBaseY = tree.baseY() + Math.max(1, tree.trunkHeight() - 4);

        for (int y = canopyBaseY; y <= topY + 2; y++) {
            int rel = y - topY;
            int radius;
            boolean trimCorners;

            // 不同高度使用不同的葉層半徑，讓樹冠看起來較自然。
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

        // 在樹幹周圍再補一圈樹葉，讓外觀看起來更厚實。
        int wrapStart = Math.max(tree.baseY() + 1, topY - 3);
        for (int y = wrapStart; y <= topY - 1; y++) {
            trySetLeaf(chunk, tree.lx() + 1, y, tree.lz());
            trySetLeaf(chunk, tree.lx() - 1, y, tree.lz());
            trySetLeaf(chunk, tree.lx(), y, tree.lz() + 1);
            trySetLeaf(chunk, tree.lx(), y, tree.lz() - 1);
        }
    }

    // 放置某一層的樹葉。
    private void placeLeafLayer(Chunk chunk, int centerX, int y, int centerZ, int radius, boolean trimCorners) {
        if (radius <= 0) {
            trySetLeaf(chunk, centerX, y, centerZ);
            return;
        }

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // 需要時把四個角落去掉，避免樹冠太方。
                if (trimCorners && Math.abs(dx) == radius && Math.abs(dz) == radius) {
                    continue;
                }
                trySetLeaf(chunk, centerX + dx, y, centerZ + dz);
            }
        }
    }

    // 嘗試在指定位置放置樹葉。
    private void trySetLeaf(Chunk chunk, int x, int y, int z) {
        if (x < 0 || x >= GameConfig.CHUNK_SIZE || z < 0 || z >= GameConfig.CHUNK_SIZE) {
            return;
        }
        if (y < 1 || y >= GameConfig.CHUNK_HEIGHT) {
            return;
        }

        // 只在空氣位置放樹葉，不覆蓋其他方塊。
        if (chunk.get(x, y, z) == BlockType.AIR) {
            chunk.setRaw(x, y, z, (short) BlockType.LEAVES.id());
        }
    }

    // 記錄一棵樹的基本生成資訊。
    private record TreeSpec(int lx, int lz, int baseY, int trunkHeight) {

        // 回傳樹幹頂端高度。
        int topY() {
            return baseY + trunkHeight;
        }
    }

    // 記錄某個地表位置的取樣結果。
    private record SurfaceSample(int height, Biome biome, float continental) {
    }
}

package com.minecraftin.clone.engine;

import com.minecraftin.clone.render.AtlasTiles;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL33C.*;

// Generate block textures in code so the game does not require external image assets.
// Keep tile indices aligned with AtlasTiles and BlockType.tileForFace().
public final class TextureAtlas implements AutoCloseable {
    // Tile generators use fixed 16-pixel coordinates; the mipmap limit also assumes a power-of-two
    // tile size.
    public static final int TILE_SIZE = 16;

    public static final int TILES_PER_ROW = 16;

    private static final int WIDTH = TILE_SIZE * TILES_PER_ROW;

    private static final int HEIGHT = TILE_SIZE * TILES_PER_ROW;

    // Precompute UV bounds for the fixed atlas grid so meshing can use array lookups.
    private static final int TILE_COUNT = TILES_PER_ROW * TILES_PER_ROW;
    private static final float[] U0 = new float[TILE_COUNT];
    private static final float[] V0 = new float[TILE_COUNT];
    private static final float[] U1 = new float[TILE_COUNT];
    private static final float[] V1 = new float[TILE_COUNT];

    static {
        for (int tile = 0; tile < TILE_COUNT; tile++) {
            U0[tile] = computeU0(tile);
            V0[tile] = computeV0(tile);
            U1[tile] = computeU1(tile);
            V1[tile] = computeV1(tile);
        }
    }

    private final int textureId;

    public TextureAtlas() {
        try (ResourceScope resources = new ResourceScope()) {
            textureId = glGenTextures();
            resources.add(() -> glDeleteTextures(textureId));
            glBindTexture(GL_TEXTURE_2D, textureId);
            ByteBuffer pixels = null;
            try {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                // Stop mipmaps when each tile is one texel; smaller levels would mix neighboring
                // tiles.
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, Integer.numberOfTrailingZeros(TILE_SIZE));
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
                pixels = MemoryUtil.memAlloc(WIDTH * HEIGHT * 4);
                fillAtlas(pixels);
                pixels.flip();
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, WIDTH, HEIGHT, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
                glGenerateMipmap(GL_TEXTURE_2D);
            } finally {
                MemoryUtil.memFree(pixels);
                glBindTexture(GL_TEXTURE_2D, 0);
            }
            resources.release();
        }
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    // Inset each UV edge by one percent of a tile to reduce sampling from adjacent tiles.
    public static float u0(int tile) {
        return tile >= 0 && tile < TILE_COUNT ? U0[tile] : computeU0(tile);
    }

    public static float v0(int tile) {
        return tile >= 0 && tile < TILE_COUNT ? V0[tile] : computeV0(tile);
    }

    public static float u1(int tile) {
        return tile >= 0 && tile < TILE_COUNT ? U1[tile] : computeU1(tile);
    }

    public static float v1(int tile) {
        return tile >= 0 && tile < TILE_COUNT ? V1[tile] : computeV1(tile);
    }

    private static float computeU0(int tile) {
        return ((tile % TILES_PER_ROW) + 0.01f) / TILES_PER_ROW;
    }

    private static float computeV0(int tile) {
        return ((tile / TILES_PER_ROW) + 0.01f) / TILES_PER_ROW;
    }

    private static float computeU1(int tile) {
        return ((tile % TILES_PER_ROW) + 0.99f) / TILES_PER_ROW;
    }

    private static float computeV1(int tile) {
        return ((tile / TILES_PER_ROW) + 0.99f) / TILES_PER_ROW;
    }

    @Override
    public void close() {
        glDeleteTextures(textureId);
    }

    private void fillAtlas(ByteBuffer buffer) {
        // Use packed ARGB pixels while generating the atlas; convert to RGBA bytes for upload.
        int[] colors = new int[WIDTH * HEIGHT];

        fillGrassSideTile(colors, AtlasTiles.GRASS_SIDE);
        fillGrassTopTile(colors, AtlasTiles.GRASS_TOP);
        fillDirtTile(colors, AtlasTiles.DIRT);
        fillStoneTile(colors, AtlasTiles.STONE, 0x858585, 0x5F5F5F);
        fillSandTile(colors, AtlasTiles.SAND);
        fillPlanksTile(colors, AtlasTiles.PLANKS, 0xBA8A52, 0x7F552F);
        fillLogSideTile(colors, AtlasTiles.LOG_SIDE);
        fillLogTopTile(colors, AtlasTiles.LOG_TOP);
        fillLeavesTile(colors, AtlasTiles.LEAVES);
        fillCobbleTile(colors, AtlasTiles.COBBLE);
        fillWaterTile(colors, AtlasTiles.WATER);
        fillGlassTile(colors, AtlasTiles.GLASS);
        fillBrickTile(colors, AtlasTiles.BRICKS, 0xA14E43, 0x6F302B);
        fillBedrockTile(colors, AtlasTiles.BEDROCK);
        fillSnowTile(colors, AtlasTiles.SNOW);
        fillTile(colors, AtlasTiles.RED_BLOCK, 0xD84141, 0xBD2F2F, false);
        fillTile(colors, AtlasTiles.ORANGE_BLOCK, 0xE68A2E, 0xCC7420, false);
        fillTile(colors, AtlasTiles.YELLOW_BLOCK, 0xF2D53C, 0xD6BB2A, false);
        fillTile(colors, AtlasTiles.GREEN_BLOCK, 0x4DAA45, 0x3A8D34, false);
        fillTile(colors, AtlasTiles.BLUE_BLOCK, 0x3E78D8, 0x2E60BE, false);
        fillTile(colors, AtlasTiles.PURPLE_BLOCK, 0x8448CC, 0x6B36AD, false);

        fillWoolTile(colors, AtlasTiles.WHITE_WOOL, 0xEFEFE8, 0xD8D8D0);
        fillWoolTile(colors, AtlasTiles.LIGHT_GRAY_WOOL, 0xA9A9A9, 0x8E8E8E);
        fillWoolTile(colors, AtlasTiles.GRAY_WOOL, 0x666666, 0x4C4C4C);
        fillWoolTile(colors, AtlasTiles.BLACK_WOOL, 0x262626, 0x171717);
        fillWoolTile(colors, AtlasTiles.BROWN_WOOL, 0x805334, 0x62402A);
        fillWoolTile(colors, AtlasTiles.RED_WOOL, 0xB73731, 0x8E2A26);
        fillWoolTile(colors, AtlasTiles.ORANGE_WOOL, 0xD07D31, 0xA75E24);
        fillWoolTile(colors, AtlasTiles.YELLOW_WOOL, 0xD6BC3A, 0xB1982C);
        fillWoolTile(colors, AtlasTiles.LIME_WOOL, 0x6CBF3B, 0x4F9430);
        fillWoolTile(colors, AtlasTiles.GREEN_WOOL, 0x4C8B36, 0x356B2A);
        fillWoolTile(colors, AtlasTiles.CYAN_WOOL, 0x3AA0A6, 0x2C787E);
        fillWoolTile(colors, AtlasTiles.BLUE_WOOL, 0x3A5EAA, 0x2B4688);
        fillWoolTile(colors, AtlasTiles.PURPLE_WOOL, 0x794BA8, 0x5D3786);
        fillWoolTile(colors, AtlasTiles.MAGENTA_WOOL, 0xB34AA8, 0x8E367F);
        fillWoolTile(colors, AtlasTiles.PINK_WOOL, 0xD98AA8, 0xBF6790);

        fillPlanksTile(colors, AtlasTiles.BIRCH_PLANKS, 0xD5BF75, 0xBFA55F);
        fillPlanksTile(colors, AtlasTiles.SPRUCE_PLANKS, 0x70502F, 0x523A25);
        fillPlanksTile(colors, AtlasTiles.DARK_OAK_PLANKS, 0x4E321E, 0x352112);

        fillBrickTile(colors, AtlasTiles.STONE_BRICKS, 0x7E7E7E, 0x565656);
        fillBrickTile(colors, AtlasTiles.CHISELED_STONE_BRICKS, 0x858585, 0x525252);
        fillMossyBrickTile(colors, AtlasTiles.MOSSY_STONE_BRICKS, 0x777C6E, 0x3F5B31);
        fillTile(colors, AtlasTiles.GRANITE, 0xA87563, 0x7F5147, false);
        fillTile(colors, AtlasTiles.POLISHED_GRANITE, 0xAD7D6D, 0x8E5F52, true);
        fillTile(colors, AtlasTiles.DIORITE, 0xD6D6D6, 0xA8A8A8, false);
        fillTile(colors, AtlasTiles.POLISHED_DIORITE, 0xDCDCDC, 0xB8B8B8, true);
        fillTile(colors, AtlasTiles.ANDESITE, 0x858585, 0x686868, false);
        fillTile(colors, AtlasTiles.POLISHED_ANDESITE, 0x8D8D8D, 0x707070, true);
        fillTile(colors, AtlasTiles.DEEPSLATE, 0x4B4D52, 0x313338, false);
        fillTile(colors, AtlasTiles.POLISHED_DEEPSLATE, 0x55565B, 0x3D3E43, true);
        fillBrickTile(colors, AtlasTiles.DEEPSLATE_BRICKS, 0x4C4E55, 0x2D2F35);
        fillTile(colors, AtlasTiles.QUARTZ_BLOCK, 0xE9E4D8, 0xCFC7B6, false);
        fillPillarSideTile(colors, AtlasTiles.QUARTZ_PILLAR_SIDE, 0xE9E4D8, 0xBFB6A6);
        fillPillarTopTile(colors, AtlasTiles.QUARTZ_PILLAR_TOP, 0xEAE5D9, 0xC6BDAE);
        fillTile(colors, AtlasTiles.SMOOTH_QUARTZ, 0xEFEADF, 0xDDD5C8, true);
        fillTile(colors, AtlasTiles.OBSIDIAN, 0x211A2E, 0x0F0B18, false);
        fillTile(colors, AtlasTiles.NETHERRACK, 0x7C2E2E, 0x4D1B1F, false);
        fillBrickTile(colors, AtlasTiles.NETHER_BRICKS, 0x4C1F2A, 0x271018);
        fillTile(colors, AtlasTiles.END_STONE, 0xD8D19A, 0xB8B072, false);
        fillGlowTile(colors, AtlasTiles.GLOWSTONE, 0xF3C85B, 0xA87824);
        fillGlowTile(colors, AtlasTiles.SEA_LANTERN, 0xCDEDE8, 0x5AA8A4);

        fillPlanksTile(colors, AtlasTiles.OAK_FENCE, 0xBA8A52, 0x8D6236);
        fillDoorUpperTile(colors, AtlasTiles.OAK_DOOR_UPPER, 0xA97943, 0x5F3A1F);
        fillDoorLowerTile(colors, AtlasTiles.OAK_DOOR_LOWER, 0xA97943, 0x5F3A1F);
        fillTrapdoorTile(colors, AtlasTiles.OAK_TRAPDOOR, 0xA97943, 0x5F3A1F);
        fillLadderTile(colors, AtlasTiles.LADDER, 0xA97943, 0x5F3A1F);
        fillTorchTile(colors, AtlasTiles.TORCH, 0xF0B84A, 0x70451F);
        fillCraftingSideTile(colors, AtlasTiles.CRAFTING_TABLE_SIDE, 0x9A6A3B, 0x5C3A20);
        fillCraftingTopTile(colors, AtlasTiles.CRAFTING_TABLE_TOP, 0xB9824A, 0x614027);
        fillTile(colors, AtlasTiles.FURNACE_SIDE, 0x727272, 0x4B4B4B, false);
        fillFurnaceFrontTile(colors, AtlasTiles.FURNACE_FRONT, 0x6E6E6E, 0x323232);
        fillTile(colors, AtlasTiles.FURNACE_TOP, 0x777777, 0x555555, true);
        fillChestSideTile(colors, AtlasTiles.CHEST_SIDE, 0xB47A37, 0x6B441F);
        fillChestTopTile(colors, AtlasTiles.CHEST_TOP, 0xC08A42, 0x775025);
        fillBookshelfTile(colors, AtlasTiles.BOOKSHELF, 0xA56C32, 0x3D5D9A);
        fillSolidTile(colors, AtlasTiles.WHITE, 0xFFFFFF);

        // Upload bytes in RGBA order even though the temporary pixel array uses packed ARGB.
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int argb = colors[y * WIDTH + x];
                int a = (argb >>> 24) & 0xFF;
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;

                buffer.put((byte) r).put((byte) g).put((byte) b).put((byte) a);
            }
        }
    }

    private void fillGrassSideTile(int[] pixels, int tile) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int bladeDrop = 3 + (hash(tile + 7, x / 2, 0) & 0x03);
                boolean grass = y < 4 || (y < bladeDrop + 3 && ((x + hash(tile, x / 2, 1)) & 0x03) != 0);
                int color;
                if (grass) {
                    float blend = blockyBlend(tile, x, y) * 0.72f;
                    color = mix(0x69B948, 0x3E7D2E, blend);
                } else {
                    color = dirtColor(tile, x, y);
                    if (y == 4 || y == 5) {
                        color = mix(color, 0x2E5E25, 0.18f);
                    }
                }
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | color;
            }
        }
    }

    private void fillGrassTopTile(int[] pixels, int tile) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                float blend = blockyBlend(tile, x, y);
                int color = mix(0x67B94B, 0x3F842D, blend);
                if (((hash(tile + 19, x / 2, y / 2) >>> 3) & 0x07) == 0) {
                    color = mix(color, 0x9AD05D, 0.18f);
                }
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | color;
            }
        }
    }

    private void fillDirtTile(int[] pixels, int tile) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | dirtColor(tile, x, y);
            }
        }
    }

    private int dirtColor(int tile, int x, int y) {
        float blend = blockyBlend(tile, x, y);
        int color = mix(0x8A5A32, 0x5F3B21, blend);
        int detail = hash(tile + 41, x, y) & 0xFF;
        if (detail > 244) {
            return mix(color, 0x9A9587, 0.38f);
        }
        if (detail < 10) {
            return mix(color, 0x3D2617, 0.26f);
        }
        return color;
    }

    private void fillStoneTile(int[] pixels, int tile, int colorA, int colorB) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                float blend = blockyBlend(tile, x, y);
                int color = mix(colorA, colorB, blend);
                boolean vein = (x + y + (hash(tile + 13, x / 4, y / 4) & 0x03)) % 11 == 0;
                if (vein) {
                    color = mix(color, 0x464646, 0.18f);
                }
                if ((hash(tile + 29, x, y) & 0xFF) > 248) {
                    color = mix(color, 0xD0D0D0, 0.14f);
                }
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | color;
            }
        }
    }

    private void fillSandTile(int[] pixels, int tile) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                float blend = blockyBlend(tile, x, y) * 0.72f;
                int color = mix(0xDDCC8A, 0xBDA969, blend);
                int detail = hash(tile + 5, x, y) & 0xFF;
                if (detail > 238) {
                    color = mix(color, 0xFFF3B6, 0.30f);
                } else if (detail < 12) {
                    color = mix(color, 0x9E8B55, 0.20f);
                }
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | color;
            }
        }
    }

    private void fillLogSideTile(int[] pixels, int tile) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                boolean groove = x == 2 || x == 7 || x == 13 || ((x + y / 3) % 9 == 0);
                float blend = groove ? 0.78f : blockyBlend(tile, x, y) * 0.70f;
                int color = mix(0x9A6A3B, 0x4E2F1B, blend);
                if ((hash(tile + 23, x / 2, y / 2) & 0x0F) == 0) {
                    color = mix(color, 0xC18D57, 0.18f);
                }
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | color;
            }
        }
    }

    private void fillLogTopTile(int[] pixels, int tile) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int dx = Math.abs(x - 7);
                int dy = Math.abs(y - 7);
                int ring = Math.max(dx, dy);
                float blend = (ring % 3 == 0) ? 0.50f : blockyBlend(tile, x, y) * 0.52f;
                int color = mix(0xC18D57, 0x8A5A31, blend);
                if (x == 0 || y == 0 || x == 15 || y == 15) {
                    color = mix(color, 0x4E2F1B, 0.45f);
                }
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | color;
            }
        }
    }

    private void fillLeavesTile(int[] pixels, int tile) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                float blend = blockyBlend(tile, x, y);
                int color = mix(0x3E9342, 0x1F5F2C, blend);
                if (((hash(tile + 37, x / 2, y / 2) >>> 1) & 0x07) == 0) {
                    color = mix(color, 0x73B94A, 0.22f);
                }
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | color;
            }
        }
    }

    private void fillCobbleTile(int[] pixels, int tile) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int row = y / 4;
                int shiftedX = x + (row % 2) * 2;
                boolean seam = y % 4 == 0 || shiftedX % 5 == 0;
                float blend = seam ? 0.82f : blockyBlend(tile, x, y) * 0.80f;
                int color = mix(0x777777, 0x444444, blend);
                if (!seam && (hash(tile + 11, x, y) & 0xFF) > 246) {
                    color = mix(color, 0xB7B7B7, 0.18f);
                }
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | color;
            }
        }
    }

    private void fillWaterTile(int[] pixels, int tile) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                boolean ripple = (x + y * 2 + (hash(tile, x / 4, y / 2) & 0x03)) % 9 == 0;
                float blend = blockyBlend(tile, x, y) * 0.45f + (ripple ? 0.0f : 0.20f);
                int color = mix(0x3F86DF, 0x1E5EBD, blend);
                if (ripple) {
                    color = mix(color, 0x8DD8FF, 0.26f);
                }
                pixels[(tileY + y) * WIDTH + tileX + x] = (150 << 24) | color;
            }
        }
    }

    private void fillGlassTile(int[] pixels, int tile) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                boolean edge = x == 0 || y == 0 || x == 15 || y == 15;
                boolean streak = x - y == 4 || x - y == 8 || x - y == -6;
                int color = edge ? 0xC7F3FF : mix(0xA9D8F4, 0x73A9D6, blockyBlend(tile, x, y) * 0.35f);
                if (streak) {
                    color = mix(color, 0xFFFFFF, 0.55f);
                }
                int alpha = edge || streak ? 150 : 92;
                pixels[(tileY + y) * WIDTH + tileX + x] = (alpha << 24) | color;
            }
        }
    }

    private void fillBedrockTile(int[] pixels, int tile) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                float blend = blockyBlend(tile, x, y);
                int color = mix(0x444444, 0x151515, blend);
                if ((hash(tile + 17, x / 2, y / 2) & 0x0F) == 0) {
                    color = mix(color, 0x777777, 0.22f);
                }
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | color;
            }
        }
    }

    private void fillSnowTile(int[] pixels, int tile) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                float blend = blockyBlend(tile, x, y) * 0.55f;
                int color = mix(0xF4F8FF, 0xC9D6EA, blend);
                if ((hash(tile + 29, x, y) & 0xFF) > 248) {
                    color = mix(color, 0xFFFFFF, 0.30f);
                }
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | color;
            }
        }
    }

    private void fillTile(int[] pixels, int tile, int colorA, int colorB, boolean stripe) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                // Favor coarse patches over strong per-pixel variation to keep textures readable at
                // a distance.
                float blend = blockyBlend(tile, x, y);

                // The stripe option adds crossed grooves to distinguish polished tiles.
                if (stripe) {
                    boolean groove = x % 5 == 0 || y % 5 == 0;
                    blend = groove ? Math.min(1.0f, blend + 0.30f) : blend * 0.68f;
                }

                int color = mix(colorA, colorB, blend);
                int accent = hash(tile + 53, x, y) & 0xFF;
                if (accent > 248) {
                    color = mix(color, 0xFFFFFF, 0.08f);
                } else if (accent < 8) {
                    color = mix(color, 0x000000, 0.08f);
                }

                int alpha = 255;

                int pixel = (alpha << 24) | color;

                int px = tileX + x;
                int py = tileY + y;
                pixels[py * WIDTH + px] = pixel;
            }
        }
    }

    // Combine coarse four-pixel and two-pixel patches with a weaker per-pixel accent.
    private float blockyBlend(int tile, int x, int y) {
        int broad = hash(tile, x / 4, y / 4) & 0x1F;
        int mid = hash(tile + 17, x / 2, y / 2) & 0x1F;
        int detail = hash(tile + 31, x, y) & 0x07;
        float blend = 0.10f + broad / 31.0f * 0.26f + mid / 31.0f * 0.30f + detail / 7.0f * 0.08f;
        return Math.min(0.86f, blend);
    }

    // Use diagonal threads and low-contrast variation to distinguish wool from concrete.
    private void fillWoolTile(int[] pixels, int tile, int colorA, int colorB) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int noise = hash(tile, x, y) & 0x0F;
                float blend = noise / 22.0f;
                if ((x + y) % 7 == 0 || (x - y + TILE_SIZE) % 9 == 0) {
                    blend = Math.min(1.0f, blend + 0.22f);
                }
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | mix(colorA, colorB, blend);
            }
        }
    }

    // Horizontal seams and wood grain distinguish planks from logs and plain color blocks.
    private void fillPlanksTile(int[] pixels, int tile, int colorA, int colorB) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                boolean seam = y == 4 || y == 9 || y == 14;
                int noise = hash(tile, x * 3, y) & 0x1F;
                float blend = seam ? 0.95f : noise / 42.0f;
                if ((x + (y / 5) * 3) % 11 == 0) {
                    blend = Math.min(1.0f, blend + 0.18f);
                }
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | mix(colorA, colorB, blend);
            }
        }
    }

    // Offset the mortar lines between rows to produce a brick pattern.
    private void fillBrickTile(int[] pixels, int tile, int colorA, int colorB) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int row = y / 4;
                int shiftedX = x + (row % 2) * 4;
                boolean mortar = y % 4 == 0 || shiftedX % 8 == 0;
                int noise = hash(tile, x, y) & 0x17;
                float blend = mortar ? 0.92f : noise / 50.0f;
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | mix(colorA, colorB, blend);
            }
        }
    }

    // Match ordinary brick joints before adding moss so adjacent brick variants align.
    private void fillMossyBrickTile(int[] pixels, int tile, int colorA, int mossColor) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int row = y / 4;
                int shiftedX = x + (row % 2) * 4;
                boolean mortar = y % 4 == 0 || shiftedX % 8 == 0;
                boolean moss = (hash(tile + 31, x, y) & 0x1F) > 24 || (x < 3 && y > 7);
                int base = moss ? mossColor : colorA;
                int shade = moss ? 0x294220 : 0x565656;
                float blend = mortar ? 0.86f : ((hash(tile, x, y) & 0x17) / 50.0f);
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | mix(base, shade, blend);
            }
        }
    }

    // Use vertical grooves and horizontal bands to distinguish pillar sides from their tops.
    private void fillPillarSideTile(int[] pixels, int tile, int colorA, int colorB) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                boolean groove = x == 3 || x == 12;
                boolean band = y == 1 || y == 14;
                float blend = groove ? 0.72f : (band ? 0.44f : ((hash(tile, x, y) & 0x0F) / 72.0f));
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | mix(colorA, colorB, blend);
            }
        }
    }

    private void fillPillarTopTile(int[] pixels, int tile, int colorA, int colorB) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int dist = Math.abs(x - 7) + Math.abs(y - 7);
                float blend = dist > 8 ? 0.48f : ((hash(tile, x, y) & 0x0F) / 80.0f);
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | mix(colorA, colorB, blend);
            }
        }
    }

    private void fillGlowTile(int[] pixels, int tile, int colorA, int colorB) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                boolean grid = x % 5 == 0 || y % 5 == 0;
                float blend = grid ? 0.65f : ((hash(tile, x, y) & 0x1F) / 64.0f);
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | mix(colorA, colorB, blend);
            }
        }
    }

    // Draw framed windows and a handle on the upper door tile; the lower tile supplies the solid
    // panels.
    private void fillDoorUpperTile(int[] pixels, int tile, int wood, int dark) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                boolean frame = x == 1 || x == 14 || y == 1 || y == 14;
                boolean stile = (x == 7 || x == 8) && y > 1 && y < 14;
                boolean window = (x >= 3 && x <= 6 || x >= 9 && x <= 12) && y >= 4 && y <= 8;
                boolean knob = (x == 11 || x == 12) && (y == 11 || y == 12);
                int color;
                if (knob) {
                    color = 0xD6B15A;
                } else if (frame || stile) {
                    color = mix(dark, wood, (hash(tile, x, y) & 0x07) / 28.0f);
                } else if (window) {
                    color = mix(0xC9E6F0, 0x8FB8CF, (hash(tile, x, y) & 0x0F) / 30.0f);
                } else {
                    color = mix(wood, dark, (hash(tile, x, y) & 0x1F) / 48.0f);
                }
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | color;
            }
        }
    }

    // Use framed inset panels for the lower half of the door.
    private void fillDoorLowerTile(int[] pixels, int tile, int wood, int dark) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                boolean frame = x == 1 || x == 14 || y == 1 || y == 14;
                boolean stile = (x == 7 || x == 8) && y > 1 && y < 14;
                boolean leftPanel = ((x == 3 || x == 6) && y >= 3 && y <= 12)
                        || ((y == 3 || y == 12) && x >= 3 && x <= 6);
                boolean rightPanel = ((x == 9 || x == 12) && y >= 3 && y <= 12)
                        || ((y == 3 || y == 12) && x >= 9 && x <= 12);
                int color;
                if (frame || stile) {
                    color = mix(dark, wood, (hash(tile, x, y) & 0x07) / 28.0f);
                } else if (leftPanel || rightPanel) {
                    color = mix(wood, dark, 0.55f);
                } else {
                    color = mix(wood, dark, (hash(tile, x, y) & 0x1F) / 48.0f);
                }
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | color;
            }
        }
    }

    private void fillTrapdoorTile(int[] pixels, int tile, int wood, int dark) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                boolean frame = x == 1 || x == 14 || y == 1 || y == 14 || x == 7 || y == 7;
                boolean cutout = (x == 4 || x == 11) && (y == 4 || y == 11);
                float blend = frame ? 0.75f : ((hash(tile, x, y) & 0x1F) / 52.0f);
                int color = cutout ? dark : mix(wood, dark, blend);
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | color;
            }
        }
    }

    // Ladder and torch tiles include transparent pixels; world.frag discards them before depth is
    // written.
    private void fillLadderTile(int[] pixels, int tile, int wood, int dark) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                boolean rung = y == 3 || y == 8 || y == 13;
                boolean rail = x == 4 || x == 11;
                int color = (rung || rail) ? mix(wood, dark, (hash(tile, x, y) & 0x0F) / 44.0f) : 0x000000;
                int alpha = (rung || rail) ? 255 : 0;
                pixels[(tileY + y) * WIDTH + tileX + x] = (alpha << 24) | color;
            }
        }
    }

    // At the base mip level, the world torch's side UVs cover columns 6-9 and rows 6-15.
    // Only flame rows 6-8 appear on those sides; HUD icons use the full tile, including the wider
    // flame.
    private void fillTorchTile(int[] pixels, int tile, int flame, int handle) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                boolean stick = x >= 6 && x <= 9 && y >= 9;
                boolean fire = x >= 5 && x <= 10 && y >= 3 && y < 9;
                int alpha = stick || fire ? 255 : 0;
                int color;
                if (fire) {
                    boolean core = x >= 6 && x <= 9 && y >= 4 && y < 8;
                    color = core ? mix(0xFFE36A, flame, (hash(tile, x, y) & 0x07) / 14.0f)
                            : mix(flame, 0xD8741F, (hash(tile, x, y) & 0x0F) / 24.0f);
                } else {
                    color = mix(handle, 0x4A2C12, (hash(tile, x, y) & 0x0F) / 40.0f);
                }
                pixels[(tileY + y) * WIDTH + tileX + x] = (alpha << 24) | color;
            }
        }
    }

    private void fillCraftingSideTile(int[] pixels, int tile, int wood, int dark) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                boolean grid = x == 4 || x == 11 || y == 4 || y == 11;
                boolean tool = (x >= 6 && x <= 9 && y >= 2 && y <= 3) || (x == 8 && y >= 3 && y <= 7);
                int color = tool ? 0xC7C7C7 : mix(wood, dark, grid ? 0.82f : ((hash(tile, x, y) & 0x1F) / 50.0f));
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | color;
            }
        }
    }

    private void fillCraftingTopTile(int[] pixels, int tile, int wood, int dark) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                boolean grid = x == 1 || x == 7 || x == 14 || y == 1 || y == 7 || y == 14;
                float blend = grid ? 0.88f : ((hash(tile, x, y) & 0x1F) / 58.0f);
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | mix(wood, dark, blend);
            }
        }
    }

    private void fillFurnaceFrontTile(int[] pixels, int tile, int stone, int dark) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                boolean rim = x == 2 || x == 13 || y == 2 || y == 13;
                boolean mouth = x >= 4 && x <= 11 && y >= 6 && y <= 11;
                int base = mouth ? dark : stone;
                float blend = rim ? 0.72f : ((hash(tile, x, y) & 0x1F) / 50.0f);
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | mix(base, dark, blend);
            }
        }
    }

    private void fillChestSideTile(int[] pixels, int tile, int wood, int dark) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                boolean band = y == 7 || x == 1 || x == 14 || y == 1 || y == 14;
                boolean latch = x >= 7 && x <= 9 && y >= 6 && y <= 9;
                int color = latch ? 0xD7B861 : mix(wood, dark, band ? 0.80f : ((hash(tile, x, y) & 0x1F) / 56.0f));
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | color;
            }
        }
    }

    private void fillChestTopTile(int[] pixels, int tile, int wood, int dark) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                boolean rim = x == 1 || x == 14 || y == 1 || y == 14;
                float blend = rim ? 0.75f : ((hash(tile, x, y) & 0x1F) / 54.0f);
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | mix(wood, dark, blend);
            }
        }
    }

    private void fillBookshelfTile(int[] pixels, int tile, int wood, int bookColor) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;
        int[] books = { 0xB83A32, 0x3D5D9A, 0x2E7D4F, 0xD2B047, 0x8D4DA2 };

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                boolean shelf = y == 1 || y == 7 || y == 14;
                int color;
                if (shelf || x == 0 || x == 15) {
                    color = mix(wood, 0x5A351C, 0.35f);
                } else {
                    int book = books[Math.floorMod((x / 3) + (y / 8) * 2, books.length)];
                    color = mix(book, bookColor, (hash(tile, x, y) & 0x0F) / 48.0f);
                }
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | color;
            }
        }
    }

    // Provide a white tile so the HUD can draw solid colors with the same textured shader.
    private void fillSolidTile(int[] pixels, int tile, int color) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                pixels[(tileY + y) * WIDTH + tileX + x] = 0xFF000000 | color;
            }
        }
    }

    private int mix(int a, int b, float t) {
        int ar = (a >>> 16) & 0xFF;
        int ag = (a >>> 8) & 0xFF;
        int ab = a & 0xFF;

        int br = (b >>> 16) & 0xFF;
        int bg = (b >>> 8) & 0xFF;
        int bb = b & 0xFF;

        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bCh = (int) (ab + (bb - ab) * t);

        return (r << 16) | (g << 8) | bCh;
    }

    // Deterministic visual variation only; this hash is not suitable for security use.
    private int hash(int tile, int x, int y) {
        int h = tile * 0x9E3779B9;
        h ^= x * 0x85EBCA77;
        h ^= y * 0xC2B2AE3D;
        h ^= h >>> 16;
        h *= 0x7FEB352D;
        h ^= h >>> 15;
        return h;
    }
}

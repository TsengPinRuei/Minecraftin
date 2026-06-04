package com.minecraftin.clone.engine;

import com.minecraftin.clone.render.AtlasTiles;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL33C.*;

// 以程式方式產生方塊材質圖集，避免專案額外依賴圖片資產。
// tile 編號需與 AtlasTiles 與 BlockType.tileForFace() 保持一致。
public final class TextureAtlas implements AutoCloseable {
    // 每個小貼圖的寬高都是 16 像素。
    public static final int TILE_SIZE = 16;

    // 每一列放 16 個小貼圖。
    public static final int TILES_PER_ROW = 16;

    // 整張材質圖集的總寬度。
    private static final int WIDTH = TILE_SIZE * TILES_PER_ROW;

    // 整張材質圖集的總高度。
    private static final int HEIGHT = TILE_SIZE * TILES_PER_ROW;

    // OpenGL 材質物件的 ID。
    private final int textureId;

    // 建立整張材質圖集，並把像素資料上傳到顯示卡。
    public TextureAtlas() {
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        // 縮小時使用最近鄰 mipmap，放大時使用最近鄰取樣，保留方塊像素風格。
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        // 超出貼圖座標時，使用重複方式取樣。
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        // 配置一塊記憶體來存放整張圖集的 RGBA 像素資料。
        ByteBuffer pixels = MemoryUtil.memAlloc(WIDTH * HEIGHT * 4);

        try {
            // 先把圖集像素內容填進 buffer。
            fillAtlas(pixels);

            // 切換成讀取模式，準備交給 OpenGL 使用。
            pixels.flip();

            // 把像素資料上傳成 2D 材質。
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, WIDTH, HEIGHT, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

            // 產生 mipmap，讓遠處貼圖顯示更穩定。
            glGenerateMipmap(GL_TEXTURE_2D);
        } finally {
            // 無論成功或失敗，都要釋放手動配置的記憶體。
            MemoryUtil.memFree(pixels);
        }

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    // 把這張材質綁定到指定的 texture unit。
    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    // 回傳指定 tile 左上角的 U 座標。
    // 0.01f 是留一點邊界，減少 mipmap 或浮點誤差取樣到隔壁 tile。
    public float u0(int tile) {
        return ((tile % TILES_PER_ROW) + 0.01f) / TILES_PER_ROW;
    }

    // 回傳指定 tile 左上角的 V 座標。
    public float v0(int tile) {
        return ((tile / TILES_PER_ROW) + 0.01f) / TILES_PER_ROW;
    }

    // 回傳指定 tile 右下角的 U 座標。
    public float u1(int tile) {
        return ((tile % TILES_PER_ROW) + 0.99f) / TILES_PER_ROW;
    }

    // 回傳指定 tile 右下角的 V 座標。
    public float v1(int tile) {
        return ((tile / TILES_PER_ROW) + 0.99f) / TILES_PER_ROW;
    }

    // 刪除顯示卡中的材質資源。
    @Override
    public void close() {
        glDeleteTextures(textureId);
    }

    // 產生整張圖集的像素內容，並寫入 ByteBuffer。
    private void fillAtlas(ByteBuffer buffer) {
        // 先用 int 陣列暫存整張圖的 ARGB 顏色值。
        int[] colors = new int[WIDTH * HEIGHT];

        // 依序把各種方塊材質畫到圖集中的對應位置。
        fillTile(colors, AtlasTiles.GRASS_SIDE, 0x5B913B, 0x4B7E30, true);
        fillTile(colors, AtlasTiles.GRASS_TOP, 0x63AF45, 0x4F9132, false);
        fillTile(colors, AtlasTiles.DIRT, 0x8B5A2B, 0x754820, false);
        fillTile(colors, AtlasTiles.STONE, 0x7E7E7E, 0x676767, false);
        fillTile(colors, AtlasTiles.SAND, 0xDCCB8A, 0xCDBB79, false);
        fillTile(colors, AtlasTiles.PLANKS, 0xBA8A52, 0xA37543, true);
        fillTile(colors, AtlasTiles.LOG_SIDE, 0x9A6A3B, 0x7D522E, true);
        fillTile(colors, AtlasTiles.LOG_TOP, 0xC18D57, 0x9B7040, true);
        fillTile(colors, AtlasTiles.LEAVES, 0x3B8A3E, 0x2F6E33, false);
        fillTile(colors, AtlasTiles.COBBLE, 0x737373, 0x565656, false);
        fillTile(colors, AtlasTiles.WATER, 0x3D76D1, 0x285FC2, false);
        fillTile(colors, AtlasTiles.GLASS, 0xA4D1FF, 0x86B9F0, false);
        fillTile(colors, AtlasTiles.BRICKS, 0xA14E43, 0x8C3F35, true);
        fillTile(colors, AtlasTiles.BEDROCK, 0x3B3B3B, 0x1F1F1F, false);
        fillTile(colors, AtlasTiles.SNOW, 0xF2F6FF, 0xDCE5F4, false);
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
        fillDoorTile(colors, AtlasTiles.OAK_DOOR, 0xA97943, 0x5F3A1F);
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

        // 把 ARGB 轉成 OpenGL 需要的 RGBA 順序後寫進 buffer。
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

    // 在圖集中填入一個 tile 的像素內容。
    private void fillTile(int[] pixels, int tile, int colorA, int colorB, boolean stripe) {
        // 算出這個 tile 在整張圖集中的左上角位置。
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                // 利用 hash 產生一點隨機感，讓材質不要看起來太平。
                int noise = hash(tile, x, y) & 0x1F;
                float blend = (noise / 31.0f);

                // 如果需要條紋效果，就依照 x 的區段調整混色比例。
                if (stripe) {
                    blend = ((x / 4) % 2 == 0) ? blend * 0.55f : 1.0f - blend * 0.35f;
                }

                // 用兩種顏色混合出最後顏色。
                int color = mix(colorA, colorB, blend);

                // 水和玻璃可設定不同透明度，其餘材質維持不透明。
                int alpha = tile == AtlasTiles.WATER ? 150 : (tile == AtlasTiles.GLASS ? 120 : 255);

                // 組成 ARGB 顏色值。
                int pixel = (alpha << 24) | color;

                // 寫入圖集對應位置。
                int px = tileX + x;
                int py = tileY + y;
                pixels[py * WIDTH + px] = pixel;
            }
        }
    }

    // 羊毛用交錯線條與低對比雜訊，避免看起來像單純混凝土。
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

    // 木板用水平縫線與木紋雜訊，讓它和原木、一般色塊分開。
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

    // 石磚使用錯位磚縫，讓建築牆面有 Minecraft 常見的砌磚語彙。
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

    private void fillDoorTile(int[] pixels, int tile, int wood, int dark) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                boolean rail = x == 1 || x == 14 || y == 1 || y == 14 || y == 7;
                boolean inset = (x == 5 || x == 10) && y > 2 && y < 13;
                boolean knob = x >= 11 && x <= 12 && y >= 7 && y <= 8;
                float blend = rail || inset ? 0.80f : ((hash(tile, x, y) & 0x1F) / 48.0f);
                int color = knob ? 0xD6B15A : mix(wood, dark, blend);
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

    private void fillTorchTile(int[] pixels, int tile, int flame, int handle) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                boolean stick = x >= 6 && x <= 9 && y >= 5;
                boolean fire = x >= 5 && x <= 10 && y <= 5;
                int alpha = stick || fire ? 255 : 0;
                int color = fire ? mix(0xFFE36A, flame, (hash(tile, x, y) & 0x0F) / 30.0f) : handle;
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

    // 依照比例 t，把兩個顏色混合成一個新顏色。
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

    // 根據 tile 編號與像素座標，產生一個穩定的雜訊值。
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

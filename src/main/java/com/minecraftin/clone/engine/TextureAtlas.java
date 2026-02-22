// 宣告此檔案所屬的套件。
package com.minecraftin.clone.engine;

// 匯入後續會使用到的型別或函式。
import com.minecraftin.clone.render.AtlasTiles;
// 匯入後續會使用到的型別或函式。
import org.lwjgl.system.MemoryUtil;

// 匯入後續會使用到的型別或函式。
import java.nio.ByteBuffer;

// 匯入後續會使用到的型別或函式。
import static org.lwjgl.opengl.GL33C.*;

// 定義主要型別與其結構。
public final class TextureAtlas implements AutoCloseable {
    // 設定或更新變數的值。
    public static final int TILE_SIZE = 16;
    // 設定或更新變數的值。
    public static final int TILES_PER_ROW = 16;

    // 設定或更新變數的值。
    private static final int WIDTH = TILE_SIZE * TILES_PER_ROW;
    // 設定或更新變數的值。
    private static final int HEIGHT = TILE_SIZE * TILES_PER_ROW;

    // 下一行程式碼負責執行目前步驟。
    private final int textureId;

    // 定義對外可呼叫的方法。
    public TextureAtlas() {
        // 設定或更新變數的值。
        textureId = glGenTextures();
        // 呼叫方法執行對應功能。
        glBindTexture(GL_TEXTURE_2D, textureId);
        // 呼叫方法執行對應功能。
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
        // 呼叫方法執行對應功能。
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        // 呼叫方法執行對應功能。
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        // 呼叫方法執行對應功能。
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        // 宣告並初始化變數。
        ByteBuffer pixels = MemoryUtil.memAlloc(WIDTH * HEIGHT * 4);
        // 下一行程式碼負責執行目前步驟。
        try {
            // 呼叫方法執行對應功能。
            fillAtlas(pixels);
            // 呼叫方法執行對應功能。
            pixels.flip();
            // 呼叫方法執行對應功能。
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, WIDTH, HEIGHT, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            // 呼叫方法執行對應功能。
            glGenerateMipmap(GL_TEXTURE_2D);
        // 下一行程式碼負責執行目前步驟。
        } finally {
            // 呼叫方法執行對應功能。
            MemoryUtil.memFree(pixels);
        }

        // 呼叫方法執行對應功能。
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    // 定義對外可呼叫的方法。
    public void bind(int unit) {
        // 呼叫方法執行對應功能。
        glActiveTexture(GL_TEXTURE0 + unit);
        // 呼叫方法執行對應功能。
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    // 定義對外可呼叫的方法。
    public float u0(int tile) {
        // 呼叫方法執行對應功能。
        return ((tile % TILES_PER_ROW) + 0.01f) / TILES_PER_ROW;
    }

    // 定義對外可呼叫的方法。
    public float v0(int tile) {
        // 呼叫方法執行對應功能。
        return ((tile / TILES_PER_ROW) + 0.01f) / TILES_PER_ROW;
    }

    // 定義對外可呼叫的方法。
    public float u1(int tile) {
        // 呼叫方法執行對應功能。
        return ((tile % TILES_PER_ROW) + 0.99f) / TILES_PER_ROW;
    }

    // 定義對外可呼叫的方法。
    public float v1(int tile) {
        // 呼叫方法執行對應功能。
        return ((tile / TILES_PER_ROW) + 0.99f) / TILES_PER_ROW;
    }

    // 宣告註解標記，提供編譯器或框架額外資訊。
    @Override
    // 定義對外可呼叫的方法。
    public void close() {
        // 呼叫方法執行對應功能。
        glDeleteTextures(textureId);
    }

    // 定義類別內部使用的方法。
    private void fillAtlas(ByteBuffer buffer) {
        // 宣告並初始化變數。
        int[] colors = new int[WIDTH * HEIGHT];

        // 呼叫方法執行對應功能。
        fillTile(colors, AtlasTiles.GRASS_SIDE, 0x5B913B, 0x4B7E30, true);
        // 呼叫方法執行對應功能。
        fillTile(colors, AtlasTiles.GRASS_TOP, 0x63AF45, 0x4F9132, false);
        // 呼叫方法執行對應功能。
        fillTile(colors, AtlasTiles.DIRT, 0x8B5A2B, 0x754820, false);
        // 呼叫方法執行對應功能。
        fillTile(colors, AtlasTiles.STONE, 0x7E7E7E, 0x676767, false);
        // 呼叫方法執行對應功能。
        fillTile(colors, AtlasTiles.SAND, 0xDCCB8A, 0xCDBB79, false);
        // 呼叫方法執行對應功能。
        fillTile(colors, AtlasTiles.PLANKS, 0xBA8A52, 0xA37543, true);
        // 呼叫方法執行對應功能。
        fillTile(colors, AtlasTiles.LOG_SIDE, 0x9A6A3B, 0x7D522E, true);
        // 呼叫方法執行對應功能。
        fillTile(colors, AtlasTiles.LOG_TOP, 0xC18D57, 0x9B7040, true);
        // 呼叫方法執行對應功能。
        fillTile(colors, AtlasTiles.LEAVES, 0x3B8A3E, 0x2F6E33, false);
        // 呼叫方法執行對應功能。
        fillTile(colors, AtlasTiles.COBBLE, 0x737373, 0x565656, false);
        // 呼叫方法執行對應功能。
        fillTile(colors, AtlasTiles.WATER, 0x3D76D1, 0x285FC2, false);
        // 呼叫方法執行對應功能。
        fillTile(colors, AtlasTiles.GLASS, 0xA4D1FF, 0x86B9F0, false);
        // 呼叫方法執行對應功能。
        fillTile(colors, AtlasTiles.BRICKS, 0xA14E43, 0x8C3F35, true);
        // 呼叫方法執行對應功能。
        fillTile(colors, AtlasTiles.BEDROCK, 0x3B3B3B, 0x1F1F1F, false);
        // 呼叫方法執行對應功能。
        fillTile(colors, AtlasTiles.SNOW, 0xF2F6FF, 0xDCE5F4, false);
        // 呼叫方法執行對應功能。
        fillTile(colors, AtlasTiles.RED_BLOCK, 0xD84141, 0xBD2F2F, false);
        // 呼叫方法執行對應功能。
        fillTile(colors, AtlasTiles.ORANGE_BLOCK, 0xE68A2E, 0xCC7420, false);
        // 呼叫方法執行對應功能。
        fillTile(colors, AtlasTiles.YELLOW_BLOCK, 0xF2D53C, 0xD6BB2A, false);
        // 呼叫方法執行對應功能。
        fillTile(colors, AtlasTiles.GREEN_BLOCK, 0x4DAA45, 0x3A8D34, false);
        // 呼叫方法執行對應功能。
        fillTile(colors, AtlasTiles.BLUE_BLOCK, 0x3E78D8, 0x2E60BE, false);
        // 呼叫方法執行對應功能。
        fillTile(colors, AtlasTiles.PURPLE_BLOCK, 0x8448CC, 0x6B36AD, false);

        // 使用迴圈逐一處理每個元素或區間。
        for (int y = 0; y < HEIGHT; y++) {
            // 使用迴圈逐一處理每個元素或區間。
            for (int x = 0; x < WIDTH; x++) {
                // 宣告並初始化變數。
                int argb = colors[y * WIDTH + x];
                // 宣告並初始化變數。
                int a = (argb >>> 24) & 0xFF;
                // 宣告並初始化變數。
                int r = (argb >>> 16) & 0xFF;
                // 宣告並初始化變數。
                int g = (argb >>> 8) & 0xFF;
                // 宣告並初始化變數。
                int b = argb & 0xFF;
                // 呼叫方法執行對應功能。
                buffer.put((byte) r).put((byte) g).put((byte) b).put((byte) a);
            }
        }
    }

    // 定義類別內部使用的方法。
    private void fillTile(int[] pixels, int tile, int colorA, int colorB, boolean stripe) {
        // 宣告並初始化變數。
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        // 宣告並初始化變數。
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        // 使用迴圈逐一處理每個元素或區間。
        for (int y = 0; y < TILE_SIZE; y++) {
            // 使用迴圈逐一處理每個元素或區間。
            for (int x = 0; x < TILE_SIZE; x++) {
                // 宣告並初始化變數。
                int noise = hash(tile, x, y) & 0x1F;
                // 宣告並初始化變數。
                float blend = (noise / 31.0f);
                // 根據條件決定是否進入此邏輯分支。
                if (stripe) {
                    // 設定或更新變數的值。
                    blend = ((x / 4) % 2 == 0) ? blend * 0.55f : 1.0f - blend * 0.35f;
                }

                // 宣告並初始化變數。
                int color = mix(colorA, colorB, blend);
                // 宣告並初始化變數。
                int alpha = tile == AtlasTiles.WATER ? 255 : (tile == AtlasTiles.GLASS ? 140 : 255);
                // 宣告並初始化變數。
                int pixel = (alpha << 24) | color;
                // 宣告並初始化變數。
                int px = tileX + x;
                // 宣告並初始化變數。
                int py = tileY + y;
                // 設定或更新變數的值。
                pixels[py * WIDTH + px] = pixel;
            }
        }
    }

    // 定義類別內部使用的方法。
    private int mix(int a, int b, float t) {
        // 宣告並初始化變數。
        int ar = (a >>> 16) & 0xFF;
        // 宣告並初始化變數。
        int ag = (a >>> 8) & 0xFF;
        // 宣告並初始化變數。
        int ab = a & 0xFF;

        // 宣告並初始化變數。
        int br = (b >>> 16) & 0xFF;
        // 宣告並初始化變數。
        int bg = (b >>> 8) & 0xFF;
        // 宣告並初始化變數。
        int bb = b & 0xFF;

        // 宣告並初始化變數。
        int r = (int) (ar + (br - ar) * t);
        // 宣告並初始化變數。
        int g = (int) (ag + (bg - ag) * t);
        // 宣告並初始化變數。
        int bCh = (int) (ab + (bb - ab) * t);
        // 呼叫方法執行對應功能。
        return (r << 16) | (g << 8) | bCh;
    }

    // 定義類別內部使用的方法。
    private int hash(int tile, int x, int y) {
        // 宣告並初始化變數。
        int h = tile * 0x9E3779B9;
        // 設定或更新變數的值。
        h ^= x * 0x85EBCA77;
        // 設定或更新變數的值。
        h ^= y * 0xC2B2AE3D;
        // 設定或更新變數的值。
        h ^= h >>> 16;
        // 設定或更新變數的值。
        h *= 0x7FEB352D;
        // 設定或更新變數的值。
        h ^= h >>> 15;
        // 下一行程式碼負責執行目前步驟。
        return h;
    }
}

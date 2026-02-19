package com.minecraftin.clone.engine;

import com.minecraftin.clone.render.AtlasTiles;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL33C.*;

public final class TextureAtlas implements AutoCloseable {
    public static final int TILE_SIZE = 16;
    public static final int TILES_PER_ROW = 16;

    private static final int WIDTH = TILE_SIZE * TILES_PER_ROW;
    private static final int HEIGHT = TILE_SIZE * TILES_PER_ROW;

    private final int textureId;

    public TextureAtlas() {
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        ByteBuffer pixels = MemoryUtil.memAlloc(WIDTH * HEIGHT * 4);
        try {
            fillAtlas(pixels);
            pixels.flip();
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, WIDTH, HEIGHT, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            glGenerateMipmap(GL_TEXTURE_2D);
        } finally {
            MemoryUtil.memFree(pixels);
        }

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public float u0(int tile) {
        return ((tile % TILES_PER_ROW) + 0.01f) / TILES_PER_ROW;
    }

    public float v0(int tile) {
        return ((tile / TILES_PER_ROW) + 0.01f) / TILES_PER_ROW;
    }

    public float u1(int tile) {
        return ((tile % TILES_PER_ROW) + 0.99f) / TILES_PER_ROW;
    }

    public float v1(int tile) {
        return ((tile / TILES_PER_ROW) + 0.99f) / TILES_PER_ROW;
    }

    @Override
    public void close() {
        glDeleteTextures(textureId);
    }

    private void fillAtlas(ByteBuffer buffer) {
        int[] colors = new int[WIDTH * HEIGHT];

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

    private void fillTile(int[] pixels, int tile, int colorA, int colorB, boolean stripe) {
        int tileX = (tile % TILES_PER_ROW) * TILE_SIZE;
        int tileY = (tile / TILES_PER_ROW) * TILE_SIZE;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int noise = hash(tile, x, y) & 0x1F;
                float blend = (noise / 31.0f);
                if (stripe) {
                    blend = ((x / 4) % 2 == 0) ? blend * 0.55f : 1.0f - blend * 0.35f;
                }

                int color = mix(colorA, colorB, blend);
                int alpha = tile == AtlasTiles.WATER ? 180 : (tile == AtlasTiles.GLASS ? 140 : 255);
                int pixel = (alpha << 24) | color;
                int px = tileX + x;
                int py = tileY + y;
                pixels[py * WIDTH + px] = pixel;
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

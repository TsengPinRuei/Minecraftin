package com.minecraftin.clone.render;

import com.minecraftin.clone.engine.Mesh;
import com.minecraftin.clone.engine.ShaderProgram;
import com.minecraftin.clone.util.FloatArrayBuilder;
import com.minecraftin.clone.world.BlockType;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33C.*;

public final class HudRenderer implements AutoCloseable {
    private static final int STRIDE = 7; // position xyz + color rgba
    private static final float TEXT_PIXEL_SIZE = 0.0062f;
    private static final int FONT_GLYPH_WIDTH = 5;
    private static final int FONT_GLYPH_HEIGHT = 7;
    private static final int FONT_GLYPH_SPACING = 1;

    private static final float[] BORDER_COLOR_SELECTED = new float[]{0.95f, 0.95f, 0.95f, 0.95f};
    private static final float[] BORDER_COLOR_NORMAL = new float[]{0.16f, 0.16f, 0.16f, 0.82f};
    private static final float[] SLOT_COLOR_SELECTED = new float[]{0.20f, 0.20f, 0.20f, 0.92f};
    private static final float[] SLOT_COLOR_NORMAL = new float[]{0.10f, 0.10f, 0.10f, 0.75f};
    private static final float[] TEXT_COLOR = new float[]{0.97f, 0.97f, 0.97f, 0.98f};
    private static final float[] TEXT_BG_COLOR = new float[]{0.04f, 0.04f, 0.04f, 0.72f};

    private static final float[] COLOR_RED_BLOCK = new float[]{0.85f, 0.25f, 0.25f, 0.95f};
    private static final float[] COLOR_ORANGE_BLOCK = new float[]{0.90f, 0.54f, 0.18f, 0.95f};
    private static final float[] COLOR_YELLOW_BLOCK = new float[]{0.95f, 0.84f, 0.23f, 0.95f};
    private static final float[] COLOR_GREEN_BLOCK = new float[]{0.30f, 0.66f, 0.27f, 0.95f};
    private static final float[] COLOR_BLUE_BLOCK = new float[]{0.24f, 0.47f, 0.85f, 0.95f};
    private static final float[] COLOR_PURPLE_BLOCK = new float[]{0.51f, 0.28f, 0.80f, 0.95f};
    private static final float[] COLOR_GRASS = new float[]{0.36f, 0.69f, 0.29f, 0.95f};
    private static final float[] COLOR_DIRT = new float[]{0.53f, 0.34f, 0.19f, 0.95f};
    private static final float[] COLOR_STONE = new float[]{0.53f, 0.53f, 0.53f, 0.95f};
    private static final float[] COLOR_SAND = new float[]{0.85f, 0.78f, 0.56f, 0.95f};
    private static final float[] COLOR_WATER = new float[]{0.29f, 0.48f, 0.84f, 0.95f};
    private static final float[] COLOR_LOG = new float[]{0.59f, 0.41f, 0.24f, 0.95f};
    private static final float[] COLOR_LEAVES = new float[]{0.23f, 0.54f, 0.26f, 0.95f};
    private static final float[] COLOR_COBBLE = new float[]{0.45f, 0.45f, 0.45f, 0.95f};
    private static final float[] COLOR_PLANKS = new float[]{0.72f, 0.54f, 0.30f, 0.95f};
    private static final float[] COLOR_GLASS = new float[]{0.67f, 0.84f, 0.98f, 0.95f};
    private static final float[] COLOR_BRICKS = new float[]{0.63f, 0.31f, 0.28f, 0.95f};
    private static final float[] COLOR_BEDROCK = new float[]{0.22f, 0.22f, 0.22f, 0.95f};
    private static final float[] COLOR_SNOW = new float[]{0.95f, 0.97f, 1.00f, 0.95f};
    private static final float[] COLOR_DEFAULT = new float[]{0.20f, 0.20f, 0.20f, 0.95f};
    private static final String[] GLYPH_EMPTY = new String[]{
            "00000",
            "00000",
            "00000",
            "00000",
            "00000",
            "00000",
            "00000"
    };

    private static final Map<Character, String[]> FONT = createFont();

    private final ShaderProgram shader;
    private final Mesh crosshair;
    private final Mesh hotbarMesh;
    private final FloatArrayBuilder hotbarVertices = new FloatArrayBuilder(32768);

    public HudRenderer() {
        shader = new ShaderProgram("/shaders/hud.vert", "/shaders/hud.frag");

        float s = 0.015f;
        crosshair = new Mesh(new float[]{
                -s, 0.0f, 0.0f, 0.05f, 0.05f, 0.05f, 0.92f,
                s, 0.0f, 0.0f, 0.05f, 0.05f, 0.05f, 0.92f,
                0.0f, -s, 0.0f, 0.05f, 0.05f, 0.05f, 0.92f,
                0.0f, s, 0.0f, 0.05f, 0.05f, 0.05f, 0.92f
        }, GL_LINES, 3, 4);

        hotbarMesh = new Mesh(new float[0], GL_TRIANGLES, 3, 4);
    }

    public void render(BlockType[] hotbar, int selectedIndex) {
        glDisable(GL_DEPTH_TEST);
        shader.use();
        updateHotbarMesh(hotbar, selectedIndex);
        hotbarMesh.draw();
        crosshair.draw();
        glEnable(GL_DEPTH_TEST);
    }

    @Override
    public void close() {
        hotbarMesh.close();
        crosshair.close();
        shader.close();
    }

    private void updateHotbarMesh(BlockType[] hotbar, int selectedIndex) {
        hotbarVertices.clear();

        int slots = hotbar.length;
        float slotSize = 0.105f;
        float gap = 0.014f;
        float totalWidth = slots * slotSize + (slots - 1) * gap;
        float startX = -totalWidth * 0.5f;
        float y = -0.90f;

        for (int i = 0; i < slots; i++) {
            float x = startX + i * (slotSize + gap);
            boolean selected = i == selectedIndex;

            float border = selected ? 0.007f : 0.004f;
            float[] borderColor = selected ? BORDER_COLOR_SELECTED : BORDER_COLOR_NORMAL;
            float[] slotColor = selected ? SLOT_COLOR_SELECTED : SLOT_COLOR_NORMAL;

            addRect(hotbarVertices, x - border, y - border, slotSize + border * 2.0f, slotSize + border * 2.0f, borderColor);
            addRect(hotbarVertices, x, y, slotSize, slotSize, slotColor);

            float cubeSize = slotSize * 0.44f;
            float depthX = cubeSize * 0.30f;
            float depthY = cubeSize * 0.22f;
            float cubeX = x + (slotSize - (cubeSize + depthX)) * 0.5f;
            float cubeY = y + (slotSize - (cubeSize + depthY)) * 0.5f;
            addCubeIcon(hotbarVertices, cubeX, cubeY, cubeSize, depthX, depthY, blockColor(hotbar[i]));
        }

        if (selectedIndex >= 0 && selectedIndex < hotbar.length) {
            String label = selectedBlockName(hotbar[selectedIndex]);
            addCenteredText(hotbarVertices, label, -0.735f);
        }

        hotbarMesh.update(hotbarVertices.toArray(), STRIDE);
    }

    private void addRect(FloatArrayBuilder out, float x, float y, float width, float height, float[] color) {
        float x2 = x + width;
        float y2 = y + height;
        addQuad(out, x, y, x2, y, x2, y2, x, y2, color[0], color[1], color[2], color[3]);
    }

    private void addCubeIcon(FloatArrayBuilder out, float x, float y, float size, float depthX, float depthY, float[] baseColor) {
        float x0 = x;
        float y0 = y;
        float x1 = x + size;
        float y1 = y + size;

        float r = baseColor[0];
        float g = baseColor[1];
        float b = baseColor[2];
        float a = baseColor[3];

        addQuad(out, x0, y0, x1, y0, x1, y1, x0, y1, r, g, b, a);
        addQuad(out, x0, y1, x1, y1, x1 + depthX, y1 + depthY, x0 + depthX, y1 + depthY,
                lighten(r), lighten(g), lighten(b), a);
        addQuad(out, x1, y0, x1 + depthX, y0 + depthY, x1 + depthX, y1 + depthY, x1, y1,
                darken(r), darken(g), darken(b), a);
    }

    private float lighten(float channel) {
        return Math.min(channel * 1.20f + 0.03f, 1.0f);
    }

    private float darken(float channel) {
        return Math.max(channel * 0.72f, 0.0f);
    }

    private void addCenteredText(FloatArrayBuilder out, String text, float y) {
        if (text == null || text.isBlank()) {
            return;
        }

        int charCount = text.length();
        int pixelColumns = charCount * FONT_GLYPH_WIDTH + Math.max(0, charCount - 1) * FONT_GLYPH_SPACING;
        float textWidth = pixelColumns * TEXT_PIXEL_SIZE;
        float textHeight = FONT_GLYPH_HEIGHT * TEXT_PIXEL_SIZE;
        float startX = -textWidth * 0.5f;

        float padX = TEXT_PIXEL_SIZE * 2.2f;
        float padY = TEXT_PIXEL_SIZE * 1.5f;
        addRect(out, startX - padX, y - padY, textWidth + padX * 2.0f, textHeight + padY * 2.0f, TEXT_BG_COLOR);

        float cursorX = startX;
        for (int i = 0; i < text.length(); i++) {
            String[] glyph = glyphFor(text.charAt(i));
            addGlyph(out, cursorX, y, glyph, TEXT_COLOR);
            cursorX += (FONT_GLYPH_WIDTH + FONT_GLYPH_SPACING) * TEXT_PIXEL_SIZE;
        }
    }

    private void addGlyph(FloatArrayBuilder out, float x, float y, String[] glyph, float[] color) {
        for (int row = 0; row < FONT_GLYPH_HEIGHT; row++) {
            String line = glyph[row];
            for (int col = 0; col < FONT_GLYPH_WIDTH; col++) {
                if (line.charAt(col) != '1') {
                    continue;
                }
                float px = x + col * TEXT_PIXEL_SIZE;
                float py = y + (FONT_GLYPH_HEIGHT - 1 - row) * TEXT_PIXEL_SIZE;
                addRect(out, px, py, TEXT_PIXEL_SIZE, TEXT_PIXEL_SIZE, color);
            }
        }
    }

    private String[] glyphFor(char c) {
        String[] glyph = FONT.get(c);
        if (glyph != null) {
            return glyph;
        }

        glyph = FONT.get(Character.toUpperCase(c));
        return glyph != null ? glyph : GLYPH_EMPTY;
    }

    private static Map<Character, String[]> createFont() {
        Map<Character, String[]> font = new HashMap<>();
        putGlyph(font, ' ', GLYPH_EMPTY);

        putGlyph(font, 'A', "01110", "10001", "10001", "11111", "10001", "10001", "10001");
        putGlyph(font, 'B', "11110", "10001", "10001", "11110", "10001", "10001", "11110");
        putGlyph(font, 'D', "11110", "10001", "10001", "10001", "10001", "10001", "11110");
        putGlyph(font, 'E', "11111", "10000", "10000", "11110", "10000", "10000", "11111");
        putGlyph(font, 'G', "01110", "10001", "10000", "10111", "10001", "10001", "01110");
        putGlyph(font, 'I', "11111", "00100", "00100", "00100", "00100", "00100", "11111");
        putGlyph(font, 'L', "10000", "10000", "10000", "10000", "10000", "10000", "11111");
        putGlyph(font, 'N', "10001", "11001", "10101", "10011", "10001", "10001", "10001");
        putGlyph(font, 'O', "01110", "10001", "10001", "10001", "10001", "10001", "01110");
        putGlyph(font, 'P', "11110", "10001", "10001", "11110", "10000", "10000", "10000");
        putGlyph(font, 'R', "11110", "10001", "10001", "11110", "10100", "10010", "10001");
        putGlyph(font, 'S', "01111", "10000", "10000", "01110", "00001", "00001", "11110");
        putGlyph(font, 'T', "11111", "00100", "00100", "00100", "00100", "00100", "00100");
        putGlyph(font, 'U', "10001", "10001", "10001", "10001", "10001", "10001", "01110");
        putGlyph(font, 'W', "10001", "10001", "10001", "10101", "10101", "10101", "01010");
        putGlyph(font, 'Y', "10001", "10001", "01010", "00100", "00100", "00100", "00100");

        putGlyph(font, 'a', "00000", "00000", "01110", "00001", "01111", "10001", "01111");
        putGlyph(font, 'b', "10000", "10000", "10110", "11001", "10001", "11001", "10110");
        putGlyph(font, 'd', "00001", "00001", "01101", "10011", "10001", "10011", "01101");
        putGlyph(font, 'e', "00000", "00000", "01110", "10001", "11111", "10000", "01111");
        putGlyph(font, 'g', "00000", "00000", "01110", "10001", "01111", "00001", "01110");
        putGlyph(font, 'i', "00100", "00000", "01100", "00100", "00100", "00100", "01110");
        putGlyph(font, 'l', "01100", "00100", "00100", "00100", "00100", "00100", "01110");
        putGlyph(font, 'n', "00000", "00000", "10110", "11001", "10001", "10001", "10001");
        putGlyph(font, 'o', "00000", "00000", "01110", "10001", "10001", "10001", "01110");
        putGlyph(font, 'p', "00000", "00000", "11110", "10001", "11110", "10000", "10000");
        putGlyph(font, 'r', "00000", "00000", "10110", "11001", "10000", "10000", "10000");
        putGlyph(font, 's', "00000", "00000", "01111", "10000", "01110", "00001", "11110");
        putGlyph(font, 't', "00100", "00100", "11111", "00100", "00100", "00101", "00010");
        putGlyph(font, 'u', "00000", "00000", "10001", "10001", "10001", "10011", "01101");
        putGlyph(font, 'w', "00000", "00000", "10001", "10001", "10101", "10101", "01010");
        putGlyph(font, 'y', "00000", "00000", "10001", "10001", "01111", "00001", "01110");
        return font;
    }

    private static void putGlyph(Map<Character, String[]> font, char c, String... rows) {
        font.put(c, rows);
    }

    private String selectedBlockName(BlockType type) {
        return switch (type) {
            case RED_BLOCK -> "Red";
            case ORANGE_BLOCK -> "Orange";
            case YELLOW_BLOCK -> "Yellow";
            case GREEN_BLOCK -> "Green";
            case BLUE_BLOCK -> "Blue";
            case PURPLE_BLOCK -> "Purple";
            case DIRT -> "Dirt";
            case STONE -> "Stone";
            case GLASS -> "Glass";
            default -> type.name();
        };
    }

    private void addQuad(
            FloatArrayBuilder out,
            float ax, float ay,
            float bx, float by,
            float cx, float cy,
            float dx, float dy,
            float r, float g, float b, float a
    ) {
        float z = 0.0f;
        addVertex(out, ax, ay, z, r, g, b, a);
        addVertex(out, bx, by, z, r, g, b, a);
        addVertex(out, cx, cy, z, r, g, b, a);

        addVertex(out, cx, cy, z, r, g, b, a);
        addVertex(out, dx, dy, z, r, g, b, a);
        addVertex(out, ax, ay, z, r, g, b, a);
    }

    private void addVertex(FloatArrayBuilder out, float x, float y, float z, float r, float g, float b, float a) {
        out.add(x, y, z, r, g, b, a);
    }

    private float[] blockColor(BlockType type) {
        return switch (type) {
            case RED_BLOCK -> COLOR_RED_BLOCK;
            case ORANGE_BLOCK -> COLOR_ORANGE_BLOCK;
            case YELLOW_BLOCK -> COLOR_YELLOW_BLOCK;
            case GREEN_BLOCK -> COLOR_GREEN_BLOCK;
            case BLUE_BLOCK -> COLOR_BLUE_BLOCK;
            case PURPLE_BLOCK -> COLOR_PURPLE_BLOCK;
            case GRASS -> COLOR_GRASS;
            case DIRT -> COLOR_DIRT;
            case STONE -> COLOR_STONE;
            case SAND -> COLOR_SAND;
            case WATER -> COLOR_WATER;
            case LOG -> COLOR_LOG;
            case LEAVES -> COLOR_LEAVES;
            case COBBLESTONE -> COLOR_COBBLE;
            case PLANKS -> COLOR_PLANKS;
            case GLASS -> COLOR_GLASS;
            case BRICKS -> COLOR_BRICKS;
            case BEDROCK -> COLOR_BEDROCK;
            case SNOW -> COLOR_SNOW;
            default -> COLOR_DEFAULT;
        };
    }
}

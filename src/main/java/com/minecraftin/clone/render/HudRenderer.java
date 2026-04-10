package com.minecraftin.clone.render;

import com.minecraftin.clone.engine.Mesh;
import com.minecraftin.clone.engine.ShaderProgram;
import com.minecraftin.clone.util.FloatArrayBuilder;
import com.minecraftin.clone.world.BlockType;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33C.*;

// 負責繪製 HUD，例如準星、hotbar，以及目前選取方塊的名稱。
public final class HudRenderer implements AutoCloseable {

    // 每個頂點包含 7 個 float：位置 xyz + 顏色 rgba。
    private static final int STRIDE = 7;

    // 單一文字像素在 NDC 座標中的寬度。
    private static final float TEXT_PIXEL_WIDTH = 0.005f;

    // 單一文字像素在 NDC 座標中的高度。
    private static final float TEXT_PIXEL_HEIGHT = 0.01f;

    // 每個字元的寬度為 5 格像素。
    private static final int FONT_GLYPH_WIDTH = 5;

    // 每個字元的高度為 7 格像素。
    private static final int FONT_GLYPH_HEIGHT = 7;

    // 字元與字元之間保留 1 格間距。
    private static final int FONT_GLYPH_SPACING = 1;

    // hotbar 單一格子的寬度。
    private static final float HOTBAR_SLOT_WIDTH = 0.1f;

    // hotbar 單一格子的高度。
    private static final float HOTBAR_SLOT_HEIGHT = 0.15f;

    // hotbar 各格子之間的間距。
    private static final float HOTBAR_GAP = 0.014f;

    // hotbar 底部在畫面中的 Y 座標。
    private static final float HOTBAR_Y = -0.90f;

    // 目前選取方塊名稱與 hotbar 頂部之間的距離。
    private static final float HOTBAR_LABEL_MARGIN = 0.032f;

    // 被選取格子的外框顏色。
    private static final float[] BORDER_COLOR_SELECTED = new float[] { 0.95f, 0.95f, 0.95f, 0.95f };

    // 一般格子的外框顏色。
    private static final float[] BORDER_COLOR_NORMAL = new float[] { 0.16f, 0.16f, 0.16f, 0.82f };

    // 被選取格子的背景顏色。
    private static final float[] SLOT_COLOR_SELECTED = new float[] { 0.20f, 0.20f, 0.20f, 0.92f };

    // 一般格子的背景顏色。
    private static final float[] SLOT_COLOR_NORMAL = new float[] { 0.10f, 0.10f, 0.10f, 0.75f };

    // 文字顏色。
    private static final float[] TEXT_COLOR = new float[] { 0.97f, 0.97f, 0.97f, 0.98f };

    // 文字背景顏色。
    private static final float[] TEXT_BG_COLOR = new float[] { 0.04f, 0.04f, 0.04f, 0.72f };

    // 各種方塊在 hotbar 中的代表顏色。
    private static final float[] COLOR_RED_BLOCK = new float[] { 0.85f, 0.25f, 0.25f, 0.95f };
    private static final float[] COLOR_ORANGE_BLOCK = new float[] { 0.90f, 0.54f, 0.18f, 0.95f };
    private static final float[] COLOR_YELLOW_BLOCK = new float[] { 0.95f, 0.84f, 0.23f, 0.95f };
    private static final float[] COLOR_GREEN_BLOCK = new float[] { 0.30f, 0.66f, 0.27f, 0.95f };
    private static final float[] COLOR_BLUE_BLOCK = new float[] { 0.24f, 0.47f, 0.85f, 0.95f };
    private static final float[] COLOR_PURPLE_BLOCK = new float[] { 0.51f, 0.28f, 0.80f, 0.95f };
    private static final float[] COLOR_GRASS = new float[] { 0.36f, 0.69f, 0.29f, 0.95f };
    private static final float[] COLOR_DIRT = new float[] { 0.53f, 0.34f, 0.19f, 0.95f };
    private static final float[] COLOR_STONE = new float[] { 0.53f, 0.53f, 0.53f, 0.95f };
    private static final float[] COLOR_SAND = new float[] { 0.85f, 0.78f, 0.56f, 0.95f };
    private static final float[] COLOR_WATER = new float[] { 0.29f, 0.48f, 0.84f, 0.95f };
    private static final float[] COLOR_LOG = new float[] { 0.59f, 0.41f, 0.24f, 0.95f };
    private static final float[] COLOR_LEAVES = new float[] { 0.23f, 0.54f, 0.26f, 0.95f };
    private static final float[] COLOR_COBBLE = new float[] { 0.45f, 0.45f, 0.45f, 0.95f };
    private static final float[] COLOR_PLANKS = new float[] { 0.72f, 0.54f, 0.30f, 0.95f };
    private static final float[] COLOR_GLASS = new float[] { 0.67f, 0.84f, 0.98f, 0.95f };
    private static final float[] COLOR_BRICKS = new float[] { 0.63f, 0.31f, 0.28f, 0.95f };
    private static final float[] COLOR_BEDROCK = new float[] { 0.22f, 0.22f, 0.22f, 0.95f };
    private static final float[] COLOR_SNOW = new float[] { 0.95f, 0.97f, 1.00f, 0.95f };
    private static final float[] COLOR_DEFAULT = new float[] { 0.20f, 0.20f, 0.20f, 0.95f };

    // 空白字元的點陣資料。
    private static final String[] GLYPH_EMPTY = new String[] {
            "00000",
            "00000",
            "00000",
            "00000",
            "00000",
            "00000",
            "00000"
    };

    // 字型資料表，將字元對應到 5x7 的點陣圖。
    private static final Map<Character, String[]> FONT = createFont();

    // HUD 專用 shader。
    private final ShaderProgram shader;

    // 畫面中央的準星 mesh。
    private final Mesh crosshair;

    // hotbar 與文字共用的 mesh。
    private final Mesh hotbarMesh;

    // 用來累積 hotbar 與文字頂點資料。
    private final FloatArrayBuilder hotbarVertices = new FloatArrayBuilder(32768);

    // 暫存 viewport 資訊，避免每次重新建立陣列。
    private final int[] viewport = new int[4];

    // 快取上一次選取的格子索引，若沒變就可避免重建 mesh。
    private int cachedSelectedIndex = Integer.MIN_VALUE;

    // 快取上一次 hotbar 內容的簽章。
    private int cachedHotbarSignature = Integer.MIN_VALUE;

    // 快取上一次 viewport 寬度。
    private int cachedViewportWidth = Integer.MIN_VALUE;

    // 快取上一次 viewport 高度。
    private int cachedViewportHeight = Integer.MIN_VALUE;

    // 建立 HUD 所需的 shader 與基本 mesh。
    public HudRenderer() {
        shader = new ShaderProgram("/shaders/hud.vert", "/shaders/hud.frag");

        float s = 0.015f;

        // 建立中央準星。
        crosshair = new Mesh(new float[] {
                -s, 0.0f, 0.0f, 0.05f, 0.05f, 0.05f, 0.92f,
                s, 0.0f, 0.0f, 0.05f, 0.05f, 0.05f, 0.92f,
                0.0f, -s, 0.0f, 0.05f, 0.05f, 0.05f, 0.92f,
                0.0f, s, 0.0f, 0.05f, 0.05f, 0.05f, 0.92f
        }, GL_LINES, 3, 4);

        // hotbar mesh 一開始先建立空資料，之後再動態更新。
        hotbarMesh = new Mesh(new float[0], GL_TRIANGLES, 3, 4);
    }

    // 繪製 HUD。
    public void render(BlockType[] hotbar, int selectedIndex) {
        glDisable(GL_DEPTH_TEST);
        shader.use();

        // 取得目前視窗大小，讓圖示在不同畫面比例下維持正常外觀。
        glGetIntegerv(GL_VIEWPORT, viewport);
        int viewportWidth = Math.max(1, viewport[2]);
        int viewportHeight = Math.max(1, viewport[3]);

        boolean viewportChanged = viewportWidth != cachedViewportWidth || viewportHeight != cachedViewportHeight;
        int hotbarSignature = hotbarSignature(hotbar);

        // 只有在 hotbar 內容、選取狀態或視窗大小變動時才重建 mesh。
        if (viewportChanged || hotbarSignature != cachedHotbarSignature || selectedIndex != cachedSelectedIndex) {
            updateHotbarMesh(hotbar, selectedIndex, (float) viewportWidth / (float) viewportHeight);
            cachedHotbarSignature = hotbarSignature;
            cachedSelectedIndex = selectedIndex;
            cachedViewportWidth = viewportWidth;
            cachedViewportHeight = viewportHeight;
        }

        hotbarMesh.draw();
        crosshair.draw();

        glEnable(GL_DEPTH_TEST);
    }

    // 釋放 HUD 相關的 OpenGL 資源。
    @Override
    public void close() {
        hotbarMesh.close();
        crosshair.close();
        shader.close();
    }

    // 依照 hotbar 內容與目前選取狀態，重新建立 hotbar mesh。
    private void updateHotbarMesh(BlockType[] hotbar, int selectedIndex, float viewportAspect) {
        hotbarVertices.clear();

        int slots = hotbar.length;
        float totalWidth = slots * HOTBAR_SLOT_WIDTH + (slots - 1) * HOTBAR_GAP;
        float startX = -totalWidth * 0.5f;
        float y = HOTBAR_Y;

        for (int i = 0; i < slots; i++) {
            float x = startX + i * (HOTBAR_SLOT_WIDTH + HOTBAR_GAP);
            boolean selected = i == selectedIndex;

            float border = selected ? 0.007f : 0.004f;
            float[] borderColor = selected ? BORDER_COLOR_SELECTED : BORDER_COLOR_NORMAL;
            float[] slotColor = selected ? SLOT_COLOR_SELECTED : SLOT_COLOR_NORMAL;

            // 畫出格子的外框與背景。
            addRect(hotbarVertices, x - border, y - border,
                    HOTBAR_SLOT_WIDTH + border * 2.0f, HOTBAR_SLOT_HEIGHT + border * 2.0f, borderColor);
            addRect(hotbarVertices, x, y, HOTBAR_SLOT_WIDTH, HOTBAR_SLOT_HEIGHT, slotColor);

            // 根據畫面比例調整圖示寬度，避免寬螢幕下被拉扁。
            float pixelAspect = Math.max(0.5f, viewportAspect);
            float cubeHeight = Math.min(HOTBAR_SLOT_WIDTH, HOTBAR_SLOT_HEIGHT) * 0.44f;
            float cubeWidth = cubeHeight / pixelAspect;
            float depthX = cubeWidth * 0.30f;
            float depthY = cubeHeight * 0.22f;
            float cubeX = x + (HOTBAR_SLOT_WIDTH - (cubeWidth + depthX)) * 0.5f;
            float cubeY = y + (HOTBAR_SLOT_HEIGHT - (cubeHeight + depthY)) * 0.5f;

            // 畫出代表方塊的小立方體圖示。
            addCubeIcon(hotbarVertices, cubeX, cubeY, cubeWidth, cubeHeight, depthX, depthY, blockColor(hotbar[i]));
        }

        // 在 hotbar 上方顯示目前選取方塊的名稱。
        if (selectedIndex >= 0 && selectedIndex < hotbar.length) {
            String label = hotbar[selectedIndex].displayName();
            addCenteredText(hotbarVertices, label, HOTBAR_Y + HOTBAR_SLOT_HEIGHT + HOTBAR_LABEL_MARGIN);
        }

        hotbarMesh.update(hotbarVertices.toArray(), STRIDE);
    }

    // 加入一個矩形。
    private void addRect(FloatArrayBuilder out, float x, float y, float width, float height, float[] color) {
        float x2 = x + width;
        float y2 = y + height;
        addQuad(out, x, y, x2, y, x2, y2, x, y2, color[0], color[1], color[2], color[3]);
    }

    // 用三個面模擬一個簡化的立方體圖示。
    private void addCubeIcon(FloatArrayBuilder out, float x, float y, float width, float height, float depthX,
            float depthY,
            float[] baseColor) {
        float x0 = x;
        float y0 = y;
        float x1 = x + width;
        float y1 = y + height;

        float r = baseColor[0];
        float g = baseColor[1];
        float b = baseColor[2];
        float a = baseColor[3];

        // 正面
        addQuad(out, x0, y0, x1, y0, x1, y1, x0, y1, r, g, b, a);

        // 上面，用較亮的顏色製造立體感。
        addQuad(out, x0, y1, x1, y1, x1 + depthX, y1 + depthY, x0 + depthX, y1 + depthY,
                lighten(r), lighten(g), lighten(b), a);

        // 右側面，用較暗的顏色製造陰影感。
        addQuad(out, x1, y0, x1 + depthX, y0 + depthY, x1 + depthX, y1 + depthY, x1, y1,
                darken(r), darken(g), darken(b), a);
    }

    // 讓顏色稍微變亮。
    private float lighten(float channel) {
        return Math.min(channel * 1.20f + 0.03f, 1.0f);
    }

    // 讓顏色稍微變暗。
    private float darken(float channel) {
        return Math.max(channel * 0.72f, 0.0f);
    }

    // 在畫面中央加入一段置中的文字。
    private void addCenteredText(FloatArrayBuilder out, String text, float y) {
        if (text == null || text.isBlank()) {
            return;
        }

        int charCount = text.length();
        int pixelColumns = charCount * FONT_GLYPH_WIDTH + Math.max(0, charCount - 1) * FONT_GLYPH_SPACING;
        float textWidth = pixelColumns * TEXT_PIXEL_WIDTH;
        float textHeight = FONT_GLYPH_HEIGHT * TEXT_PIXEL_HEIGHT;
        float startX = -textWidth * 0.5f;

        // 先畫文字背景框。
        float padX = TEXT_PIXEL_WIDTH * 2.2f;
        float padY = TEXT_PIXEL_HEIGHT * 1.5f;
        addRect(out, startX - padX, y - padY, textWidth + padX * 2.0f, textHeight + padY * 2.0f, TEXT_BG_COLOR);

        // 再逐字畫出文字。
        float cursorX = startX;
        for (int i = 0; i < text.length(); i++) {
            String[] glyph = glyphFor(text.charAt(i));
            addGlyph(out, cursorX, y, glyph, TEXT_COLOR);
            cursorX += (FONT_GLYPH_WIDTH + FONT_GLYPH_SPACING) * TEXT_PIXEL_WIDTH;
        }
    }

    // 根據點陣資料畫出單一字元。
    private void addGlyph(FloatArrayBuilder out, float x, float y, String[] glyph, float[] color) {
        for (int row = 0; row < FONT_GLYPH_HEIGHT; row++) {
            String line = glyph[row];
            for (int col = 0; col < FONT_GLYPH_WIDTH; col++) {
                if (line.charAt(col) != '1') {
                    continue;
                }

                float px = x + col * TEXT_PIXEL_WIDTH;
                float py = y + (FONT_GLYPH_HEIGHT - 1 - row) * TEXT_PIXEL_HEIGHT;
                addRect(out, px, py, TEXT_PIXEL_WIDTH, TEXT_PIXEL_HEIGHT, color);
            }
        }
    }

    // 取得某個字元對應的點陣資料。
    // 若找不到，先嘗試轉成大寫，再不行就回傳空白字元。
    private String[] glyphFor(char c) {
        String[] glyph = FONT.get(c);
        if (glyph != null) {
            return glyph;
        }

        glyph = FONT.get(Character.toUpperCase(c));
        return glyph != null ? glyph : GLYPH_EMPTY;
    }

    // 建立內建點陣字型表。
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

    // 將單一字元與其點陣資料加入字型表。
    private static void putGlyph(Map<Character, String[]> font, char c, String... rows) {
        font.put(c, rows);
    }

    // 加入一個四邊形，內部會拆成兩個三角形。
    private void addQuad(
            FloatArrayBuilder out,
            float ax, float ay,
            float bx, float by,
            float cx, float cy,
            float dx, float dy,
            float r, float g, float b, float a) {
        float z = 0.0f;

        addVertex(out, ax, ay, z, r, g, b, a);
        addVertex(out, bx, by, z, r, g, b, a);
        addVertex(out, cx, cy, z, r, g, b, a);

        addVertex(out, cx, cy, z, r, g, b, a);
        addVertex(out, dx, dy, z, r, g, b, a);
        addVertex(out, ax, ay, z, r, g, b, a);
    }

    // 加入單一頂點資料。
    private void addVertex(FloatArrayBuilder out, float x, float y, float z, float r, float g, float b, float a) {
        out.add(x, y, z, r, g, b, a);
    }

    // 根據 hotbar 內容產生簡單簽章，用來判斷內容是否改變。
    private int hotbarSignature(BlockType[] hotbar) {
        int hash = 1;
        for (BlockType blockType : hotbar) {
            hash = 31 * hash + (blockType != null ? blockType.id() : -1);
        }
        return hash;
    }

    // 回傳某種方塊在 HUD 中要使用的代表顏色。
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
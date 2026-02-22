// 宣告此檔案所屬的套件。
package com.minecraftin.clone.render;

// 匯入後續會使用到的型別或函式。
import com.minecraftin.clone.engine.Mesh;
// 匯入後續會使用到的型別或函式。
import com.minecraftin.clone.engine.ShaderProgram;
// 匯入後續會使用到的型別或函式。
import com.minecraftin.clone.util.FloatArrayBuilder;
// 匯入後續會使用到的型別或函式。
import com.minecraftin.clone.world.BlockType;

// 匯入後續會使用到的型別或函式。
import java.util.HashMap;
// 匯入後續會使用到的型別或函式。
import java.util.Map;

// 匯入後續會使用到的型別或函式。
import static org.lwjgl.opengl.GL33C.*;

// 定義主要型別與其結構。
public final class HudRenderer implements AutoCloseable {
    // 下一行程式碼負責執行目前步驟。
    private static final int STRIDE = 7; // position xyz + color rgba
    // 設定文字像素寬度（NDC）。
    private static final float TEXT_PIXEL_WIDTH = 0.005f;
    // 設定文字像素高度（NDC）。
    private static final float TEXT_PIXEL_HEIGHT = 0.01f;
    // 設定或更新變數的值。
    private static final int FONT_GLYPH_WIDTH = 5;
    // 設定或更新變數的值。
    private static final int FONT_GLYPH_HEIGHT = 7;
    // 設定或更新變數的值。
    private static final int FONT_GLYPH_SPACING = 1;
    // 設定 hotbar 槽位寬度。
    private static final float HOTBAR_SLOT_WIDTH = 0.1f;
    // 設定 hotbar 槽位高度。
    private static final float HOTBAR_SLOT_HEIGHT = 0.15f;
    // 設定 hotbar 槽位間距。
    private static final float HOTBAR_GAP = 0.014f;
    // 設定 hotbar 左下角基準 Y（第一格底部）。
    private static final float HOTBAR_Y = -0.90f;
    // 設定 hotbar 上方名稱標籤與槽位頂部的距離。
    private static final float HOTBAR_LABEL_MARGIN = 0.032f;

    // 設定或更新變數的值。
    private static final float[] BORDER_COLOR_SELECTED = new float[] { 0.95f, 0.95f, 0.95f, 0.95f };
    // 設定或更新變數的值。
    private static final float[] BORDER_COLOR_NORMAL = new float[] { 0.16f, 0.16f, 0.16f, 0.82f };
    // 設定或更新變數的值。
    private static final float[] SLOT_COLOR_SELECTED = new float[] { 0.20f, 0.20f, 0.20f, 0.92f };
    // 設定或更新變數的值。
    private static final float[] SLOT_COLOR_NORMAL = new float[] { 0.10f, 0.10f, 0.10f, 0.75f };
    // 設定或更新變數的值。
    private static final float[] TEXT_COLOR = new float[] { 0.97f, 0.97f, 0.97f, 0.98f };
    // 設定或更新變數的值。
    private static final float[] TEXT_BG_COLOR = new float[] { 0.04f, 0.04f, 0.04f, 0.72f };

    // 設定或更新變數的值。
    private static final float[] COLOR_RED_BLOCK = new float[] { 0.85f, 0.25f, 0.25f, 0.95f };
    // 設定或更新變數的值。
    private static final float[] COLOR_ORANGE_BLOCK = new float[] { 0.90f, 0.54f, 0.18f, 0.95f };
    // 設定或更新變數的值。
    private static final float[] COLOR_YELLOW_BLOCK = new float[] { 0.95f, 0.84f, 0.23f, 0.95f };
    // 設定或更新變數的值。
    private static final float[] COLOR_GREEN_BLOCK = new float[] { 0.30f, 0.66f, 0.27f, 0.95f };
    // 設定或更新變數的值。
    private static final float[] COLOR_BLUE_BLOCK = new float[] { 0.24f, 0.47f, 0.85f, 0.95f };
    // 設定或更新變數的值。
    private static final float[] COLOR_PURPLE_BLOCK = new float[] { 0.51f, 0.28f, 0.80f, 0.95f };
    // 設定或更新變數的值。
    private static final float[] COLOR_GRASS = new float[] { 0.36f, 0.69f, 0.29f, 0.95f };
    // 設定或更新變數的值。
    private static final float[] COLOR_DIRT = new float[] { 0.53f, 0.34f, 0.19f, 0.95f };
    // 設定或更新變數的值。
    private static final float[] COLOR_STONE = new float[] { 0.53f, 0.53f, 0.53f, 0.95f };
    // 設定或更新變數的值。
    private static final float[] COLOR_SAND = new float[] { 0.85f, 0.78f, 0.56f, 0.95f };
    // 設定或更新變數的值。
    private static final float[] COLOR_WATER = new float[] { 0.29f, 0.48f, 0.84f, 0.95f };
    // 設定或更新變數的值。
    private static final float[] COLOR_LOG = new float[] { 0.59f, 0.41f, 0.24f, 0.95f };
    // 設定或更新變數的值。
    private static final float[] COLOR_LEAVES = new float[] { 0.23f, 0.54f, 0.26f, 0.95f };
    // 設定或更新變數的值。
    private static final float[] COLOR_COBBLE = new float[] { 0.45f, 0.45f, 0.45f, 0.95f };
    // 設定或更新變數的值。
    private static final float[] COLOR_PLANKS = new float[] { 0.72f, 0.54f, 0.30f, 0.95f };
    // 設定或更新變數的值。
    private static final float[] COLOR_GLASS = new float[] { 0.67f, 0.84f, 0.98f, 0.95f };
    // 設定或更新變數的值。
    private static final float[] COLOR_BRICKS = new float[] { 0.63f, 0.31f, 0.28f, 0.95f };
    // 設定或更新變數的值。
    private static final float[] COLOR_BEDROCK = new float[] { 0.22f, 0.22f, 0.22f, 0.95f };
    // 設定或更新變數的值。
    private static final float[] COLOR_SNOW = new float[] { 0.95f, 0.97f, 1.00f, 0.95f };
    // 設定或更新變數的值。
    private static final float[] COLOR_DEFAULT = new float[] { 0.20f, 0.20f, 0.20f, 0.95f };
    // 下一行程式碼負責執行目前步驟。
    private static final String[] GLYPH_EMPTY = new String[] {
            // 下一行程式碼負責執行目前步驟。
            "00000",
            // 下一行程式碼負責執行目前步驟。
            "00000",
            // 下一行程式碼負責執行目前步驟。
            "00000",
            // 下一行程式碼負責執行目前步驟。
            "00000",
            // 下一行程式碼負責執行目前步驟。
            "00000",
            // 下一行程式碼負責執行目前步驟。
            "00000",
            // 下一行程式碼負責執行目前步驟。
            "00000"
    };

    // 設定或更新變數的值。
    private static final Map<Character, String[]> FONT = createFont();

    // 下一行程式碼負責執行目前步驟。
    private final ShaderProgram shader;
    // 下一行程式碼負責執行目前步驟。
    private final Mesh crosshair;
    // 下一行程式碼負責執行目前步驟。
    private final Mesh hotbarMesh;
    // 設定或更新變數的值。
    private final FloatArrayBuilder hotbarVertices = new FloatArrayBuilder(32768);
    // 暫存 OpenGL viewport，避免每次查詢時都重新配置陣列。
    private final int[] viewport = new int[4];
    // 設定或更新變數的值。
    private int cachedSelectedIndex = Integer.MIN_VALUE;
    // 設定或更新變數的值。
    private int cachedHotbarSignature = Integer.MIN_VALUE;
    // 快取 viewport 尺寸，尺寸變更時需要重建 hotbar mesh。
    private int cachedViewportWidth = Integer.MIN_VALUE;
    // 快取 viewport 尺寸，尺寸變更時需要重建 hotbar mesh。
    private int cachedViewportHeight = Integer.MIN_VALUE;

    // 定義對外可呼叫的方法。
    public HudRenderer() {
        // 設定或更新變數的值。
        shader = new ShaderProgram("/shaders/hud.vert", "/shaders/hud.frag");

        // 宣告並初始化變數。
        float s = 0.015f;
        // 下一行程式碼負責執行目前步驟。
        crosshair = new Mesh(new float[] {
                // 下一行程式碼負責執行目前步驟。
                -s, 0.0f, 0.0f, 0.05f, 0.05f, 0.05f, 0.92f,
                // 下一行程式碼負責執行目前步驟。
                s, 0.0f, 0.0f, 0.05f, 0.05f, 0.05f, 0.92f,
                // 下一行程式碼負責執行目前步驟。
                0.0f, -s, 0.0f, 0.05f, 0.05f, 0.05f, 0.92f,
                // 下一行程式碼負責執行目前步驟。
                0.0f, s, 0.0f, 0.05f, 0.05f, 0.05f, 0.92f
                // 下一行程式碼負責執行目前步驟。
        }, GL_LINES, 3, 4);

        // 設定或更新變數的值。
        hotbarMesh = new Mesh(new float[0], GL_TRIANGLES, 3, 4);
    }

    // 定義對外可呼叫的方法。
    public void render(BlockType[] hotbar, int selectedIndex) {
        // 呼叫方法執行對應功能。
        glDisable(GL_DEPTH_TEST);
        // 呼叫方法執行對應功能。
        shader.use();
        // 讀取目前視窗尺寸，用於修正 HUD 圖示在寬螢幕下的水平拉伸。
        glGetIntegerv(GL_VIEWPORT, viewport);
        // 宣告並初始化變數。
        int viewportWidth = Math.max(1, viewport[2]);
        // 宣告並初始化變數。
        int viewportHeight = Math.max(1, viewport[3]);
        // 宣告並初始化變數。
        boolean viewportChanged = viewportWidth != cachedViewportWidth || viewportHeight != cachedViewportHeight;
        // 宣告並初始化變數。
        int hotbarSignature = hotbarSignature(hotbar);
        // 根據條件決定是否進入此邏輯分支。
        if (viewportChanged || hotbarSignature != cachedHotbarSignature || selectedIndex != cachedSelectedIndex) {
            // 呼叫方法執行對應功能。
            updateHotbarMesh(hotbar, selectedIndex, (float) viewportWidth / (float) viewportHeight);
            // 設定或更新變數的值。
            cachedHotbarSignature = hotbarSignature;
            // 設定或更新變數的值。
            cachedSelectedIndex = selectedIndex;
            // 設定或更新變數的值。
            cachedViewportWidth = viewportWidth;
            // 設定或更新變數的值。
            cachedViewportHeight = viewportHeight;
        }
        // 呼叫方法執行對應功能。
        hotbarMesh.draw();
        // 呼叫方法執行對應功能。
        crosshair.draw();
        // 呼叫方法執行對應功能。
        glEnable(GL_DEPTH_TEST);
    }

    // 宣告註解標記，提供編譯器或框架額外資訊。
    @Override
    // 定義對外可呼叫的方法。
    public void close() {
        // 呼叫方法執行對應功能。
        hotbarMesh.close();
        // 呼叫方法執行對應功能。
        crosshair.close();
        // 呼叫方法執行對應功能。
        shader.close();
    }

    // 定義類別內部使用的方法。
    private void updateHotbarMesh(BlockType[] hotbar, int selectedIndex, float viewportAspect) {
        // 呼叫方法執行對應功能。
        hotbarVertices.clear();

        // 宣告並初始化變數。
        int slots = hotbar.length;
        // 宣告並初始化變數。
        float totalWidth = slots * HOTBAR_SLOT_WIDTH + (slots - 1) * HOTBAR_GAP;
        // 宣告並初始化變數。
        float startX = -totalWidth * 0.5f;
        // 宣告並初始化變數。
        float y = HOTBAR_Y;

        // 使用迴圈逐一處理每個元素或區間。
        for (int i = 0; i < slots; i++) {
            // 宣告並初始化變數。
            float x = startX + i * (HOTBAR_SLOT_WIDTH + HOTBAR_GAP);
            // 宣告並初始化變數。
            boolean selected = i == selectedIndex;

            // 宣告並初始化變數。
            float border = selected ? 0.007f : 0.004f;
            // 宣告並初始化變數。
            float[] borderColor = selected ? BORDER_COLOR_SELECTED : BORDER_COLOR_NORMAL;
            // 宣告並初始化變數。
            float[] slotColor = selected ? SLOT_COLOR_SELECTED : SLOT_COLOR_NORMAL;

            // 呼叫方法執行對應功能。
            addRect(hotbarVertices, x - border, y - border,
                    HOTBAR_SLOT_WIDTH + border * 2.0f, HOTBAR_SLOT_HEIGHT + border * 2.0f, borderColor);
            // 呼叫方法執行對應功能。
            addRect(hotbarVertices, x, y, HOTBAR_SLOT_WIDTH, HOTBAR_SLOT_HEIGHT, slotColor);

            // 以像素比例修正圖示寬度，讓寬螢幕下看起來仍接近正方體。
            float pixelAspect = Math.max(0.5f, viewportAspect);
            // 宣告並初始化變數。
            float cubeHeight = Math.min(HOTBAR_SLOT_WIDTH, HOTBAR_SLOT_HEIGHT) * 0.44f;
            // 宣告並初始化變數。
            float cubeWidth = cubeHeight / pixelAspect;
            // 宣告並初始化變數。
            float depthX = cubeWidth * 0.30f;
            // 宣告並初始化變數。
            float depthY = cubeHeight * 0.22f;
            // 宣告並初始化變數。
            float cubeX = x + (HOTBAR_SLOT_WIDTH - (cubeWidth + depthX)) * 0.5f;
            // 宣告並初始化變數。
            float cubeY = y + (HOTBAR_SLOT_HEIGHT - (cubeHeight + depthY)) * 0.5f;
            // 呼叫方法執行對應功能。
            addCubeIcon(hotbarVertices, cubeX, cubeY, cubeWidth, cubeHeight, depthX, depthY, blockColor(hotbar[i]));
        }

        // 根據條件決定是否進入此邏輯分支。
        if (selectedIndex >= 0 && selectedIndex < hotbar.length) {
            // 宣告並初始化變數。
            String label = hotbar[selectedIndex].displayName();
            // 呼叫方法執行對應功能。
            addCenteredText(hotbarVertices, label, HOTBAR_Y + HOTBAR_SLOT_HEIGHT + HOTBAR_LABEL_MARGIN);
        }

        // 呼叫方法執行對應功能。
        hotbarMesh.update(hotbarVertices.toArray(), STRIDE);
    }

    // 定義類別內部使用的方法。
    private void addRect(FloatArrayBuilder out, float x, float y, float width, float height, float[] color) {
        // 宣告並初始化變數。
        float x2 = x + width;
        // 宣告並初始化變數。
        float y2 = y + height;
        // 呼叫方法執行對應功能。
        addQuad(out, x, y, x2, y, x2, y2, x, y2, color[0], color[1], color[2], color[3]);
    }

    // 定義類別內部使用的方法。
    private void addCubeIcon(FloatArrayBuilder out, float x, float y, float width, float height, float depthX, float depthY,
            float[] baseColor) {
        // 宣告並初始化變數。
        float x0 = x;
        // 宣告並初始化變數。
        float y0 = y;
        // 宣告並初始化變數。
        float x1 = x + width;
        // 宣告並初始化變數。
        float y1 = y + height;

        // 宣告並初始化變數。
        float r = baseColor[0];
        // 宣告並初始化變數。
        float g = baseColor[1];
        // 宣告並初始化變數。
        float b = baseColor[2];
        // 宣告並初始化變數。
        float a = baseColor[3];

        // 呼叫方法執行對應功能。
        addQuad(out, x0, y0, x1, y0, x1, y1, x0, y1, r, g, b, a);
        // 下一行程式碼負責執行目前步驟。
        addQuad(out, x0, y1, x1, y1, x1 + depthX, y1 + depthY, x0 + depthX, y1 + depthY,
                // 呼叫方法執行對應功能。
                lighten(r), lighten(g), lighten(b), a);
        // 下一行程式碼負責執行目前步驟。
        addQuad(out, x1, y0, x1 + depthX, y0 + depthY, x1 + depthX, y1 + depthY, x1, y1,
                // 呼叫方法執行對應功能。
                darken(r), darken(g), darken(b), a);
    }

    // 定義類別內部使用的方法。
    private float lighten(float channel) {
        // 呼叫方法執行對應功能。
        return Math.min(channel * 1.20f + 0.03f, 1.0f);
    }

    // 定義類別內部使用的方法。
    private float darken(float channel) {
        // 呼叫方法執行對應功能。
        return Math.max(channel * 0.72f, 0.0f);
    }

    // 定義類別內部使用的方法。
    private void addCenteredText(FloatArrayBuilder out, String text, float y) {
        // 根據條件決定是否進入此邏輯分支。
        if (text == null || text.isBlank()) {
            // 下一行程式碼負責執行目前步驟。
            return;
        }

        // 宣告並初始化變數。
        int charCount = text.length();
        // 宣告並初始化變數。
        int pixelColumns = charCount * FONT_GLYPH_WIDTH + Math.max(0, charCount - 1) * FONT_GLYPH_SPACING;
        // 宣告並初始化變數。
        float textWidth = pixelColumns * TEXT_PIXEL_WIDTH;
        // 宣告並初始化變數。
        float textHeight = FONT_GLYPH_HEIGHT * TEXT_PIXEL_HEIGHT;
        // 宣告並初始化變數。
        float startX = -textWidth * 0.5f;

        // 宣告並初始化變數。
        float padX = TEXT_PIXEL_WIDTH * 2.2f;
        // 宣告並初始化變數。
        float padY = TEXT_PIXEL_HEIGHT * 1.5f;
        // 呼叫方法執行對應功能。
        addRect(out, startX - padX, y - padY, textWidth + padX * 2.0f, textHeight + padY * 2.0f, TEXT_BG_COLOR);

        // 宣告並初始化變數。
        float cursorX = startX;
        // 使用迴圈逐一處理每個元素或區間。
        for (int i = 0; i < text.length(); i++) {
            // 宣告並初始化變數。
            String[] glyph = glyphFor(text.charAt(i));
            // 呼叫方法執行對應功能。
            addGlyph(out, cursorX, y, glyph, TEXT_COLOR);
            // 設定或更新變數的值。
            cursorX += (FONT_GLYPH_WIDTH + FONT_GLYPH_SPACING) * TEXT_PIXEL_WIDTH;
        }
    }

    // 定義類別內部使用的方法。
    private void addGlyph(FloatArrayBuilder out, float x, float y, String[] glyph, float[] color) {
        // 使用迴圈逐一處理每個元素或區間。
        for (int row = 0; row < FONT_GLYPH_HEIGHT; row++) {
            // 宣告並初始化變數。
            String line = glyph[row];
            // 使用迴圈逐一處理每個元素或區間。
            for (int col = 0; col < FONT_GLYPH_WIDTH; col++) {
                // 根據條件決定是否進入此邏輯分支。
                if (line.charAt(col) != '1') {
                    // 跳過本次迴圈剩餘邏輯，直接進入下一次迭代。
                    continue;
                }
                // 宣告並初始化變數。
                float px = x + col * TEXT_PIXEL_WIDTH;
                // 宣告並初始化變數。
                float py = y + (FONT_GLYPH_HEIGHT - 1 - row) * TEXT_PIXEL_HEIGHT;
                // 呼叫方法執行對應功能。
                addRect(out, px, py, TEXT_PIXEL_WIDTH, TEXT_PIXEL_HEIGHT, color);
            }
        }
    }

    // 定義類別內部使用的方法。
    private String[] glyphFor(char c) {
        // 宣告並初始化變數。
        String[] glyph = FONT.get(c);
        // 根據條件決定是否進入此邏輯分支。
        if (glyph != null) {
            // 下一行程式碼負責執行目前步驟。
            return glyph;
        }

        // 設定或更新變數的值。
        glyph = FONT.get(Character.toUpperCase(c));
        // 設定或更新變數的值。
        return glyph != null ? glyph : GLYPH_EMPTY;
    }

    // 定義類別內部使用的方法。
    private static Map<Character, String[]> createFont() {
        // 設定或更新變數的值。
        Map<Character, String[]> font = new HashMap<>();
        // 呼叫方法執行對應功能。
        putGlyph(font, ' ', GLYPH_EMPTY);

        // 呼叫方法執行對應功能。
        putGlyph(font, 'A', "01110", "10001", "10001", "11111", "10001", "10001", "10001");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'B', "11110", "10001", "10001", "11110", "10001", "10001", "11110");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'D', "11110", "10001", "10001", "10001", "10001", "10001", "11110");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'E', "11111", "10000", "10000", "11110", "10000", "10000", "11111");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'G', "01110", "10001", "10000", "10111", "10001", "10001", "01110");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'I', "11111", "00100", "00100", "00100", "00100", "00100", "11111");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'L', "10000", "10000", "10000", "10000", "10000", "10000", "11111");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'N', "10001", "11001", "10101", "10011", "10001", "10001", "10001");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'O', "01110", "10001", "10001", "10001", "10001", "10001", "01110");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'P', "11110", "10001", "10001", "11110", "10000", "10000", "10000");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'R', "11110", "10001", "10001", "11110", "10100", "10010", "10001");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'S', "01111", "10000", "10000", "01110", "00001", "00001", "11110");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'T', "11111", "00100", "00100", "00100", "00100", "00100", "00100");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'U', "10001", "10001", "10001", "10001", "10001", "10001", "01110");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'W', "10001", "10001", "10001", "10101", "10101", "10101", "01010");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'Y', "10001", "10001", "01010", "00100", "00100", "00100", "00100");

        // 呼叫方法執行對應功能。
        putGlyph(font, 'a', "00000", "00000", "01110", "00001", "01111", "10001", "01111");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'b', "10000", "10000", "10110", "11001", "10001", "11001", "10110");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'd', "00001", "00001", "01101", "10011", "10001", "10011", "01101");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'e', "00000", "00000", "01110", "10001", "11111", "10000", "01111");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'g', "00000", "00000", "01110", "10001", "01111", "00001", "01110");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'i', "00100", "00000", "01100", "00100", "00100", "00100", "01110");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'l', "01100", "00100", "00100", "00100", "00100", "00100", "01110");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'n', "00000", "00000", "10110", "11001", "10001", "10001", "10001");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'o', "00000", "00000", "01110", "10001", "10001", "10001", "01110");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'p', "00000", "00000", "11110", "10001", "11110", "10000", "10000");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'r', "00000", "00000", "10110", "11001", "10000", "10000", "10000");
        // 呼叫方法執行對應功能。
        putGlyph(font, 's', "00000", "00000", "01111", "10000", "01110", "00001", "11110");
        // 呼叫方法執行對應功能。
        putGlyph(font, 't', "00100", "00100", "11111", "00100", "00100", "00101", "00010");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'u', "00000", "00000", "10001", "10001", "10001", "10011", "01101");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'w', "00000", "00000", "10001", "10001", "10101", "10101", "01010");
        // 呼叫方法執行對應功能。
        putGlyph(font, 'y', "00000", "00000", "10001", "10001", "01111", "00001", "01110");
        // 下一行程式碼負責執行目前步驟。
        return font;
    }

    // 定義類別內部使用的方法。
    private static void putGlyph(Map<Character, String[]> font, char c, String... rows) {
        // 呼叫方法執行對應功能。
        font.put(c, rows);
    }

    // 下一行程式碼負責執行目前步驟。
    private void addQuad(
            // 下一行程式碼負責執行目前步驟。
            FloatArrayBuilder out,
            // 下一行程式碼負責執行目前步驟。
            float ax, float ay,
            // 下一行程式碼負責執行目前步驟。
            float bx, float by,
            // 下一行程式碼負責執行目前步驟。
            float cx, float cy,
            // 下一行程式碼負責執行目前步驟。
            float dx, float dy,
            // 下一行程式碼負責執行目前步驟。
            float r, float g, float b, float a
    // 下一行程式碼負責執行目前步驟。
    ) {
        // 宣告並初始化變數。
        float z = 0.0f;
        // 呼叫方法執行對應功能。
        addVertex(out, ax, ay, z, r, g, b, a);
        // 呼叫方法執行對應功能。
        addVertex(out, bx, by, z, r, g, b, a);
        // 呼叫方法執行對應功能。
        addVertex(out, cx, cy, z, r, g, b, a);

        // 呼叫方法執行對應功能。
        addVertex(out, cx, cy, z, r, g, b, a);
        // 呼叫方法執行對應功能。
        addVertex(out, dx, dy, z, r, g, b, a);
        // 呼叫方法執行對應功能。
        addVertex(out, ax, ay, z, r, g, b, a);
    }

    // 定義類別內部使用的方法。
    private void addVertex(FloatArrayBuilder out, float x, float y, float z, float r, float g, float b, float a) {
        // 呼叫方法執行對應功能。
        out.add(x, y, z, r, g, b, a);
    }

    // 定義類別內部使用的方法。
    private int hotbarSignature(BlockType[] hotbar) {
        // 宣告並初始化變數。
        int hash = 1;
        // 使用迴圈逐一處理每個元素或區間。
        for (BlockType blockType : hotbar) {
            // 設定或更新變數的值。
            hash = 31 * hash + (blockType != null ? blockType.id() : -1);
        }
        // 下一行程式碼負責執行目前步驟。
        return hash;
    }

    // 定義類別內部使用的方法。
    private float[] blockColor(BlockType type) {
        // 下一行程式碼負責執行目前步驟。
        return switch (type) {
            // 宣告 switch 的其中一個分支。
            case RED_BLOCK -> COLOR_RED_BLOCK;
            // 宣告 switch 的其中一個分支。
            case ORANGE_BLOCK -> COLOR_ORANGE_BLOCK;
            // 宣告 switch 的其中一個分支。
            case YELLOW_BLOCK -> COLOR_YELLOW_BLOCK;
            // 宣告 switch 的其中一個分支。
            case GREEN_BLOCK -> COLOR_GREEN_BLOCK;
            // 宣告 switch 的其中一個分支。
            case BLUE_BLOCK -> COLOR_BLUE_BLOCK;
            // 宣告 switch 的其中一個分支。
            case PURPLE_BLOCK -> COLOR_PURPLE_BLOCK;
            // 宣告 switch 的其中一個分支。
            case GRASS -> COLOR_GRASS;
            // 宣告 switch 的其中一個分支。
            case DIRT -> COLOR_DIRT;
            // 宣告 switch 的其中一個分支。
            case STONE -> COLOR_STONE;
            // 宣告 switch 的其中一個分支。
            case SAND -> COLOR_SAND;
            // 宣告 switch 的其中一個分支。
            case WATER -> COLOR_WATER;
            // 宣告 switch 的其中一個分支。
            case LOG -> COLOR_LOG;
            // 宣告 switch 的其中一個分支。
            case LEAVES -> COLOR_LEAVES;
            // 宣告 switch 的其中一個分支。
            case COBBLESTONE -> COLOR_COBBLE;
            // 宣告 switch 的其中一個分支。
            case PLANKS -> COLOR_PLANKS;
            // 宣告 switch 的其中一個分支。
            case GLASS -> COLOR_GLASS;
            // 宣告 switch 的其中一個分支。
            case BRICKS -> COLOR_BRICKS;
            // 宣告 switch 的其中一個分支。
            case BEDROCK -> COLOR_BEDROCK;
            // 宣告 switch 的其中一個分支。
            case SNOW -> COLOR_SNOW;
            // 下一行程式碼負責執行目前步驟。
            default -> COLOR_DEFAULT;
        };
    }
}

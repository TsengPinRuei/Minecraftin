package com.minecraftin.clone.render;

import com.minecraftin.clone.engine.Mesh;
import com.minecraftin.clone.engine.ShaderProgram;
import com.minecraftin.clone.util.FloatArrayBuilder;
import com.minecraftin.clone.world.BlockType;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33C.*;

// 負責繪製 HUD，例如準星、hotbar，以及目前選取方塊的名稱。
// HUD 使用 NDC 座標直接繪製 2D mesh，不經由相機矩陣，因此頂點資料與螢幕比例處理都在這裡完成。
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

    // 創造模式背包的格子配置，接近 Minecraft 的 9 欄方塊頁。
    public static final int CREATIVE_COLUMNS = 9;
    public static final int CREATIVE_ROWS = 5;
    public static final int CREATIVE_SLOTS_PER_PAGE = CREATIVE_COLUMNS * CREATIVE_ROWS;

    private static final float CREATIVE_SLOT_WIDTH = 0.105f;
    private static final float CREATIVE_SLOT_HEIGHT = 0.125f;
    private static final float CREATIVE_GAP = 0.012f;
    private static final float CREATIVE_GRID_TOP = 0.56f;
    private static final float CREATIVE_PANEL_PAD_X = 0.065f;
    private static final float CREATIVE_PANEL_PAD_TOP = 0.13f;
    private static final float CREATIVE_PANEL_PAD_BOTTOM = 0.11f;

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

    private static final float[] INVENTORY_PANEL_COLOR = new float[] { 0.62f, 0.62f, 0.58f, 0.94f };
    private static final float[] INVENTORY_PANEL_SHADOW = new float[] { 0.05f, 0.05f, 0.05f, 0.55f };
    private static final float[] INVENTORY_SLOT_COLOR = new float[] { 0.30f, 0.30f, 0.28f, 0.88f };
    private static final float[] INVENTORY_SLOT_BORDER = new float[] { 0.13f, 0.13f, 0.12f, 0.82f };
    private static final float[] INVENTORY_SLOT_SELECTED = new float[] { 0.98f, 0.95f, 0.70f, 0.96f };

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
    private static final float[] COLOR_WHITE_WOOL = new float[] { 0.92f, 0.92f, 0.88f, 0.95f };
    private static final float[] COLOR_LIGHT_GRAY_WOOL = new float[] { 0.66f, 0.66f, 0.66f, 0.95f };
    private static final float[] COLOR_GRAY_WOOL = new float[] { 0.38f, 0.38f, 0.38f, 0.95f };
    private static final float[] COLOR_BLACK_WOOL = new float[] { 0.11f, 0.11f, 0.11f, 0.95f };
    private static final float[] COLOR_BROWN_WOOL = new float[] { 0.50f, 0.32f, 0.20f, 0.95f };
    private static final float[] COLOR_RED_WOOL = new float[] { 0.72f, 0.22f, 0.20f, 0.95f };
    private static final float[] COLOR_ORANGE_WOOL = new float[] { 0.82f, 0.49f, 0.19f, 0.95f };
    private static final float[] COLOR_YELLOW_WOOL = new float[] { 0.84f, 0.74f, 0.23f, 0.95f };
    private static final float[] COLOR_LIME_WOOL = new float[] { 0.42f, 0.75f, 0.23f, 0.95f };
    private static final float[] COLOR_GREEN_WOOL = new float[] { 0.30f, 0.55f, 0.21f, 0.95f };
    private static final float[] COLOR_CYAN_WOOL = new float[] { 0.23f, 0.63f, 0.65f, 0.95f };
    private static final float[] COLOR_BLUE_WOOL = new float[] { 0.23f, 0.37f, 0.67f, 0.95f };
    private static final float[] COLOR_PURPLE_WOOL = new float[] { 0.47f, 0.30f, 0.66f, 0.95f };
    private static final float[] COLOR_MAGENTA_WOOL = new float[] { 0.70f, 0.29f, 0.66f, 0.95f };
    private static final float[] COLOR_PINK_WOOL = new float[] { 0.85f, 0.54f, 0.66f, 0.95f };
    private static final float[] COLOR_BIRCH_PLANKS = new float[] { 0.83f, 0.74f, 0.45f, 0.95f };
    private static final float[] COLOR_SPRUCE_PLANKS = new float[] { 0.44f, 0.31f, 0.18f, 0.95f };
    private static final float[] COLOR_DARK_OAK_PLANKS = new float[] { 0.30f, 0.19f, 0.12f, 0.95f };
    private static final float[] COLOR_STONE_BRICKS = new float[] { 0.49f, 0.49f, 0.49f, 0.95f };
    private static final float[] COLOR_CHISELED_STONE_BRICKS = new float[] { 0.52f, 0.52f, 0.52f, 0.95f };
    private static final float[] COLOR_MOSSY_STONE_BRICKS = new float[] { 0.38f, 0.48f, 0.32f, 0.95f };
    private static final float[] COLOR_GRANITE = new float[] { 0.66f, 0.46f, 0.39f, 0.95f };
    private static final float[] COLOR_POLISHED_GRANITE = new float[] { 0.68f, 0.49f, 0.43f, 0.95f };
    private static final float[] COLOR_DIORITE = new float[] { 0.82f, 0.82f, 0.82f, 0.95f };
    private static final float[] COLOR_POLISHED_DIORITE = new float[] { 0.86f, 0.86f, 0.86f, 0.95f };
    private static final float[] COLOR_ANDESITE = new float[] { 0.52f, 0.52f, 0.52f, 0.95f };
    private static final float[] COLOR_POLISHED_ANDESITE = new float[] { 0.56f, 0.56f, 0.56f, 0.95f };
    private static final float[] COLOR_DEEPSLATE = new float[] { 0.29f, 0.30f, 0.32f, 0.95f };
    private static final float[] COLOR_POLISHED_DEEPSLATE = new float[] { 0.33f, 0.34f, 0.36f, 0.95f };
    private static final float[] COLOR_DEEPSLATE_BRICKS = new float[] { 0.30f, 0.31f, 0.34f, 0.95f };
    private static final float[] COLOR_QUARTZ_BLOCK = new float[] { 0.91f, 0.89f, 0.84f, 0.95f };
    private static final float[] COLOR_QUARTZ_PILLAR = new float[] { 0.90f, 0.88f, 0.82f, 0.95f };
    private static final float[] COLOR_SMOOTH_QUARTZ = new float[] { 0.94f, 0.92f, 0.87f, 0.95f };
    private static final float[] COLOR_OBSIDIAN = new float[] { 0.13f, 0.10f, 0.18f, 0.95f };
    private static final float[] COLOR_NETHERRACK = new float[] { 0.49f, 0.18f, 0.18f, 0.95f };
    private static final float[] COLOR_NETHER_BRICKS = new float[] { 0.30f, 0.12f, 0.17f, 0.95f };
    private static final float[] COLOR_END_STONE = new float[] { 0.84f, 0.81f, 0.59f, 0.95f };
    private static final float[] COLOR_GLOWSTONE = new float[] { 0.95f, 0.78f, 0.36f, 0.95f };
    private static final float[] COLOR_SEA_LANTERN = new float[] { 0.80f, 0.93f, 0.91f, 0.95f };
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

    // hotbar 與文字共用的 mesh；只有內容、選取狀態或 viewport 改變時才重建。
    private final Mesh hotbarMesh;

    // 用來累積 hotbar 與文字頂點資料。
    private final FloatArrayBuilder hotbarVertices = new FloatArrayBuilder(32768);

    // 暫存 viewport 資訊，避免每次重新建立陣列。
    private final int[] viewport = new int[4];

    // 快取上一次選取的格子索引，若沒變就可避免重建 mesh。
    private int cachedSelectedIndex = Integer.MIN_VALUE;

    // 快取上一次 hotbar 內容的簽章，避免每幀重建完全相同的 HUD 頂點資料。
    private int cachedHotbarSignature = Integer.MIN_VALUE;

    // 快取上一次創造背包內容與狀態。
    private int cachedCreativeSignature = Integer.MIN_VALUE;
    private int cachedCreativePage = Integer.MIN_VALUE;
    private int cachedCreativeTotalPages = Integer.MIN_VALUE;
    private boolean cachedCreativeOpen;

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
    public void render(BlockType[] hotbar, int selectedIndex, boolean creativeInventoryOpen, BlockType[] creativeBlocks,
            int creativePage, int creativeTotalPages) {
        glDisable(GL_DEPTH_TEST);
        shader.use();

        // 取得目前 viewport 大小，讓圖示在不同畫面比例下維持正常外觀；HUD 不依賴 Window 尺寸參數。
        glGetIntegerv(GL_VIEWPORT, viewport);
        int viewportWidth = Math.max(1, viewport[2]);
        int viewportHeight = Math.max(1, viewport[3]);

        boolean viewportChanged = viewportWidth != cachedViewportWidth || viewportHeight != cachedViewportHeight;
        int hotbarSignature = hotbarSignature(hotbar);
        int creativeSignature = creativeInventoryOpen && creativeBlocks != null ? hotbarSignature(creativeBlocks) : 0;

        // 只有在 hotbar 內容、選取狀態或視窗大小變動時才重建 mesh。
        if (viewportChanged
                || hotbarSignature != cachedHotbarSignature
                || selectedIndex != cachedSelectedIndex
                || creativeSignature != cachedCreativeSignature
                || creativePage != cachedCreativePage
                || creativeTotalPages != cachedCreativeTotalPages
                || creativeInventoryOpen != cachedCreativeOpen) {
            updateHotbarMesh(hotbar, selectedIndex, (float) viewportWidth / (float) viewportHeight,
                    creativeInventoryOpen, creativeBlocks, creativePage, creativeTotalPages);
            cachedHotbarSignature = hotbarSignature;
            cachedSelectedIndex = selectedIndex;
            cachedCreativeSignature = creativeSignature;
            cachedCreativePage = creativePage;
            cachedCreativeTotalPages = creativeTotalPages;
            cachedCreativeOpen = creativeInventoryOpen;
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

    public static int creativeTotalPages(int blockCount) {
        return Math.max(1, (blockCount + CREATIVE_SLOTS_PER_PAGE - 1) / CREATIVE_SLOTS_PER_PAGE);
    }

    public static int creativeSlotAt(double mouseX, double mouseY, int windowWidth, int windowHeight, int page,
            int blockCount) {
        if (windowWidth <= 0 || windowHeight <= 0 || blockCount <= 0) {
            return -1;
        }

        double ndcX = (mouseX / (double) windowWidth) * 2.0 - 1.0;
        double ndcY = 1.0 - (mouseY / (double) windowHeight) * 2.0;

        float startX = creativeGridStartX();
        float cellWidth = CREATIVE_SLOT_WIDTH + CREATIVE_GAP;
        float cellHeight = CREATIVE_SLOT_HEIGHT + CREATIVE_GAP;

        for (int row = 0; row < CREATIVE_ROWS; row++) {
            float slotY = CREATIVE_GRID_TOP - row * cellHeight - CREATIVE_SLOT_HEIGHT;
            if (ndcY < slotY || ndcY > slotY + CREATIVE_SLOT_HEIGHT) {
                continue;
            }

            for (int col = 0; col < CREATIVE_COLUMNS; col++) {
                float slotX = startX + col * cellWidth;
                if (ndcX < slotX || ndcX > slotX + CREATIVE_SLOT_WIDTH) {
                    continue;
                }

                int index = page * CREATIVE_SLOTS_PER_PAGE + row * CREATIVE_COLUMNS + col;
                return index < blockCount ? index : -1;
            }
        }

        return -1;
    }

    // 依照 hotbar 內容與目前選取狀態，重新建立 hotbar mesh。
    private void updateHotbarMesh(BlockType[] hotbar, int selectedIndex, float viewportAspect,
            boolean creativeInventoryOpen, BlockType[] creativeBlocks, int creativePage, int creativeTotalPages) {
        hotbarVertices.clear();

        if (creativeInventoryOpen && creativeBlocks != null) {
            addCreativeInventoryPanel(hotbarVertices, hotbar, selectedIndex, creativeBlocks, creativePage,
                    creativeTotalPages, viewportAspect);
        }

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

    private void addCreativeInventoryPanel(FloatArrayBuilder out, BlockType[] hotbar, int selectedIndex,
            BlockType[] creativeBlocks, int page, int totalPages, float viewportAspect) {
        float gridWidth = creativeGridWidth();
        float gridHeight = CREATIVE_ROWS * CREATIVE_SLOT_HEIGHT + (CREATIVE_ROWS - 1) * CREATIVE_GAP;
        float startX = creativeGridStartX();
        float gridBottom = CREATIVE_GRID_TOP - gridHeight;
        float panelX = startX - CREATIVE_PANEL_PAD_X;
        float panelY = gridBottom - CREATIVE_PANEL_PAD_BOTTOM;
        float panelWidth = gridWidth + CREATIVE_PANEL_PAD_X * 2.0f;
        float panelHeight = gridHeight + CREATIVE_PANEL_PAD_TOP + CREATIVE_PANEL_PAD_BOTTOM;

        addRect(out, panelX + 0.018f, panelY - 0.018f, panelWidth, panelHeight, INVENTORY_PANEL_SHADOW);
        addRect(out, panelX, panelY, panelWidth, panelHeight, INVENTORY_PANEL_COLOR);
        addCenteredText(out, "Creative", CREATIVE_GRID_TOP + 0.046f);

        BlockType selected = selectedIndex >= 0 && selectedIndex < hotbar.length ? hotbar[selectedIndex] : null;
        float cellWidth = CREATIVE_SLOT_WIDTH + CREATIVE_GAP;
        float cellHeight = CREATIVE_SLOT_HEIGHT + CREATIVE_GAP;
        int firstBlock = Math.max(0, page) * CREATIVE_SLOTS_PER_PAGE;

        for (int row = 0; row < CREATIVE_ROWS; row++) {
            for (int col = 0; col < CREATIVE_COLUMNS; col++) {
                int blockIndex = firstBlock + row * CREATIVE_COLUMNS + col;
                float x = startX + col * cellWidth;
                float y = CREATIVE_GRID_TOP - row * cellHeight - CREATIVE_SLOT_HEIGHT;
                boolean hasBlock = blockIndex < creativeBlocks.length;
                boolean selectedBlock = hasBlock && creativeBlocks[blockIndex] == selected;

                float[] borderColor = selectedBlock ? INVENTORY_SLOT_SELECTED : INVENTORY_SLOT_BORDER;
                addRect(out, x - 0.004f, y - 0.004f, CREATIVE_SLOT_WIDTH + 0.008f,
                        CREATIVE_SLOT_HEIGHT + 0.008f, borderColor);
                addRect(out, x, y, CREATIVE_SLOT_WIDTH, CREATIVE_SLOT_HEIGHT, INVENTORY_SLOT_COLOR);

                if (!hasBlock) {
                    continue;
                }

                float pixelAspect = Math.max(0.5f, viewportAspect);
                float cubeHeight = Math.min(CREATIVE_SLOT_WIDTH, CREATIVE_SLOT_HEIGHT) * 0.46f;
                float cubeWidth = cubeHeight / pixelAspect;
                float depthX = cubeWidth * 0.30f;
                float depthY = cubeHeight * 0.22f;
                float cubeX = x + (CREATIVE_SLOT_WIDTH - (cubeWidth + depthX)) * 0.5f;
                float cubeY = y + (CREATIVE_SLOT_HEIGHT - (cubeHeight + depthY)) * 0.5f;

                addCubeIcon(out, cubeX, cubeY, cubeWidth, cubeHeight, depthX, depthY,
                        blockColor(creativeBlocks[blockIndex]));
            }
        }

        String pageLabel = "Page " + Math.min(page + 1, Math.max(1, totalPages)) + "/" + Math.max(1, totalPages);
        addCenteredText(out, pageLabel, panelY + 0.035f);
    }

    private static float creativeGridWidth() {
        return CREATIVE_COLUMNS * CREATIVE_SLOT_WIDTH + (CREATIVE_COLUMNS - 1) * CREATIVE_GAP;
    }

    private static float creativeGridStartX() {
        return -creativeGridWidth() * 0.5f;
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

    // 建立內建點陣字型表；目前只涵蓋 hotbar 顯示名稱需要的字元。
    private static Map<Character, String[]> createFont() {
        Map<Character, String[]> font = new HashMap<>();
        putGlyph(font, ' ', GLYPH_EMPTY);

        putGlyph(font, 'A', "01110", "10001", "10001", "11111", "10001", "10001", "10001");
        putGlyph(font, 'B', "11110", "10001", "10001", "11110", "10001", "10001", "11110");
        putGlyph(font, 'C', "01111", "10000", "10000", "10000", "10000", "10000", "01111");
        putGlyph(font, 'D', "11110", "10001", "10001", "10001", "10001", "10001", "11110");
        putGlyph(font, 'E', "11111", "10000", "10000", "11110", "10000", "10000", "11111");
        putGlyph(font, 'F', "11111", "10000", "10000", "11110", "10000", "10000", "10000");
        putGlyph(font, 'G', "01110", "10001", "10000", "10111", "10001", "10001", "01110");
        putGlyph(font, 'H', "10001", "10001", "10001", "11111", "10001", "10001", "10001");
        putGlyph(font, 'I', "11111", "00100", "00100", "00100", "00100", "00100", "11111");
        putGlyph(font, 'J', "00111", "00010", "00010", "00010", "10010", "10010", "01100");
        putGlyph(font, 'K', "10001", "10010", "10100", "11000", "10100", "10010", "10001");
        putGlyph(font, 'L', "10000", "10000", "10000", "10000", "10000", "10000", "11111");
        putGlyph(font, 'M', "10001", "11011", "10101", "10101", "10001", "10001", "10001");
        putGlyph(font, 'N', "10001", "11001", "10101", "10011", "10001", "10001", "10001");
        putGlyph(font, 'O', "01110", "10001", "10001", "10001", "10001", "10001", "01110");
        putGlyph(font, 'P', "11110", "10001", "10001", "11110", "10000", "10000", "10000");
        putGlyph(font, 'Q', "01110", "10001", "10001", "10001", "10101", "10010", "01101");
        putGlyph(font, 'R', "11110", "10001", "10001", "11110", "10100", "10010", "10001");
        putGlyph(font, 'S', "01111", "10000", "10000", "01110", "00001", "00001", "11110");
        putGlyph(font, 'T', "11111", "00100", "00100", "00100", "00100", "00100", "00100");
        putGlyph(font, 'U', "10001", "10001", "10001", "10001", "10001", "10001", "01110");
        putGlyph(font, 'V', "10001", "10001", "10001", "10001", "10001", "01010", "00100");
        putGlyph(font, 'W', "10001", "10001", "10001", "10101", "10101", "10101", "01010");
        putGlyph(font, 'X', "10001", "10001", "01010", "00100", "01010", "10001", "10001");
        putGlyph(font, 'Y', "10001", "10001", "01010", "00100", "00100", "00100", "00100");
        putGlyph(font, 'Z', "11111", "00001", "00010", "00100", "01000", "10000", "11111");

        putGlyph(font, 'a', "00000", "00000", "01110", "00001", "01111", "10001", "01111");
        putGlyph(font, 'b', "10000", "10000", "10110", "11001", "10001", "11001", "10110");
        putGlyph(font, 'c', "00000", "00000", "01110", "10000", "10000", "10000", "01110");
        putGlyph(font, 'd', "00001", "00001", "01101", "10011", "10001", "10011", "01101");
        putGlyph(font, 'e', "00000", "00000", "01110", "10001", "11111", "10000", "01111");
        putGlyph(font, 'f', "00110", "01001", "01000", "11100", "01000", "01000", "01000");
        putGlyph(font, 'g', "00000", "00000", "01110", "10001", "01111", "00001", "01110");
        putGlyph(font, 'h', "10000", "10000", "10110", "11001", "10001", "10001", "10001");
        putGlyph(font, 'i', "00100", "00000", "01100", "00100", "00100", "00100", "01110");
        putGlyph(font, 'j', "00010", "00000", "00110", "00010", "00010", "10010", "01100");
        putGlyph(font, 'k', "10000", "10000", "10010", "10100", "11000", "10100", "10010");
        putGlyph(font, 'l', "01100", "00100", "00100", "00100", "00100", "00100", "01110");
        putGlyph(font, 'm', "00000", "00000", "11010", "10101", "10101", "10101", "10101");
        putGlyph(font, 'n', "00000", "00000", "10110", "11001", "10001", "10001", "10001");
        putGlyph(font, 'o', "00000", "00000", "01110", "10001", "10001", "10001", "01110");
        putGlyph(font, 'p', "00000", "00000", "11110", "10001", "11110", "10000", "10000");
        putGlyph(font, 'q', "00000", "00000", "01101", "10011", "01111", "00001", "00001");
        putGlyph(font, 'r', "00000", "00000", "10110", "11001", "10000", "10000", "10000");
        putGlyph(font, 's', "00000", "00000", "01111", "10000", "01110", "00001", "11110");
        putGlyph(font, 't', "00100", "00100", "11111", "00100", "00100", "00101", "00010");
        putGlyph(font, 'u', "00000", "00000", "10001", "10001", "10001", "10011", "01101");
        putGlyph(font, 'v', "00000", "00000", "10001", "10001", "10001", "01010", "00100");
        putGlyph(font, 'w', "00000", "00000", "10001", "10001", "10101", "10101", "01010");
        putGlyph(font, 'x', "00000", "00000", "10001", "01010", "00100", "01010", "10001");
        putGlyph(font, 'y', "00000", "00000", "10001", "10001", "01111", "00001", "01110");
        putGlyph(font, 'z', "00000", "00000", "11111", "00010", "00100", "01000", "11111");

        putGlyph(font, '0', "01110", "10001", "10011", "10101", "11001", "10001", "01110");
        putGlyph(font, '1', "00100", "01100", "00100", "00100", "00100", "00100", "01110");
        putGlyph(font, '2', "01110", "10001", "00001", "00010", "00100", "01000", "11111");
        putGlyph(font, '3', "11110", "00001", "00001", "01110", "00001", "00001", "11110");
        putGlyph(font, '4', "00010", "00110", "01010", "10010", "11111", "00010", "00010");
        putGlyph(font, '5', "11111", "10000", "10000", "11110", "00001", "00001", "11110");
        putGlyph(font, '6', "01110", "10000", "10000", "11110", "10001", "10001", "01110");
        putGlyph(font, '7', "11111", "00001", "00010", "00100", "01000", "01000", "01000");
        putGlyph(font, '8', "01110", "10001", "10001", "01110", "10001", "10001", "01110");
        putGlyph(font, '9', "01110", "10001", "10001", "01111", "00001", "00001", "01110");
        putGlyph(font, '/', "00001", "00010", "00010", "00100", "01000", "01000", "10000");

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
            case WHITE_WOOL -> COLOR_WHITE_WOOL;
            case LIGHT_GRAY_WOOL -> COLOR_LIGHT_GRAY_WOOL;
            case GRAY_WOOL -> COLOR_GRAY_WOOL;
            case BLACK_WOOL -> COLOR_BLACK_WOOL;
            case BROWN_WOOL -> COLOR_BROWN_WOOL;
            case RED_WOOL -> COLOR_RED_WOOL;
            case ORANGE_WOOL -> COLOR_ORANGE_WOOL;
            case YELLOW_WOOL -> COLOR_YELLOW_WOOL;
            case LIME_WOOL -> COLOR_LIME_WOOL;
            case GREEN_WOOL -> COLOR_GREEN_WOOL;
            case CYAN_WOOL -> COLOR_CYAN_WOOL;
            case BLUE_WOOL -> COLOR_BLUE_WOOL;
            case PURPLE_WOOL -> COLOR_PURPLE_WOOL;
            case MAGENTA_WOOL -> COLOR_MAGENTA_WOOL;
            case PINK_WOOL -> COLOR_PINK_WOOL;
            case BIRCH_PLANKS -> COLOR_BIRCH_PLANKS;
            case SPRUCE_PLANKS -> COLOR_SPRUCE_PLANKS;
            case DARK_OAK_PLANKS -> COLOR_DARK_OAK_PLANKS;
            case STONE_BRICKS -> COLOR_STONE_BRICKS;
            case CHISELED_STONE_BRICKS -> COLOR_CHISELED_STONE_BRICKS;
            case MOSSY_STONE_BRICKS -> COLOR_MOSSY_STONE_BRICKS;
            case GRANITE -> COLOR_GRANITE;
            case POLISHED_GRANITE -> COLOR_POLISHED_GRANITE;
            case DIORITE -> COLOR_DIORITE;
            case POLISHED_DIORITE -> COLOR_POLISHED_DIORITE;
            case ANDESITE -> COLOR_ANDESITE;
            case POLISHED_ANDESITE -> COLOR_POLISHED_ANDESITE;
            case DEEPSLATE -> COLOR_DEEPSLATE;
            case POLISHED_DEEPSLATE -> COLOR_POLISHED_DEEPSLATE;
            case DEEPSLATE_BRICKS -> COLOR_DEEPSLATE_BRICKS;
            case QUARTZ_BLOCK -> COLOR_QUARTZ_BLOCK;
            case QUARTZ_PILLAR -> COLOR_QUARTZ_PILLAR;
            case SMOOTH_QUARTZ -> COLOR_SMOOTH_QUARTZ;
            case OBSIDIAN -> COLOR_OBSIDIAN;
            case NETHERRACK -> COLOR_NETHERRACK;
            case NETHER_BRICKS -> COLOR_NETHER_BRICKS;
            case END_STONE -> COLOR_END_STONE;
            case GLOWSTONE -> COLOR_GLOWSTONE;
            case SEA_LANTERN -> COLOR_SEA_LANTERN;
            default -> COLOR_DEFAULT;
        };
    }
}

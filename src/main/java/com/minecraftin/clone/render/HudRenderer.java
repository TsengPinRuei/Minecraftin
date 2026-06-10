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

    // hotbar 單一格子的高度；寬度依視窗比例換算成正方形，與 Minecraft 的方形格子一致。
    private static final float HOTBAR_SLOT_HEIGHT = 0.15f;

    // hotbar 底部在畫面中的 Y 座標；Minecraft 的 hotbar 貼齊畫面底部。
    private static final float HOTBAR_Y = -0.97f;

    // 目前選取方塊名稱與 hotbar 頂部之間的距離。
    private static final float HOTBAR_LABEL_MARGIN = 0.045f;

    // 創造模式背包的格子配置，接近 Minecraft 的 9 欄方塊頁。
    public static final int CREATIVE_COLUMNS = 9;
    public static final int CREATIVE_ROWS = 5;
    public static final int CREATIVE_SLOTS_PER_PAGE = CREATIVE_COLUMNS * CREATIVE_ROWS;

    // 創造背包與箱子面板也使用 NDC 尺寸；格子為正方形（寬度依比例換算），與 Minecraft 的緊密網格一致。
    private static final float CREATIVE_SLOT_HEIGHT = 0.125f;
    private static final float CREATIVE_GAP = 0.0f;
    private static final float CREATIVE_GRID_TOP = 0.56f;
    private static final float CREATIVE_PANEL_PAD_X = 0.065f;
    private static final float CREATIVE_PANEL_PAD_TOP = 0.13f;
    private static final float CREATIVE_PANEL_PAD_BOTTOM = 0.11f;

    // Minecraft 風格的灰階 UI 色票：#C6C6C6 面板、#8B8B8B 凹陷格、#373737 暗緣、白色高光與選取框。
    private static final float[] UI_BLACK_BORDER = new float[] { 0.0f, 0.0f, 0.0f, 1.0f };
    private static final float[] UI_PANEL_FACE = new float[] { 0.776f, 0.776f, 0.776f, 1.0f };
    private static final float[] UI_PANEL_LIGHT = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
    private static final float[] UI_PANEL_DARK = new float[] { 0.333f, 0.333f, 0.333f, 1.0f };
    private static final float[] UI_SLOT_FACE = new float[] { 0.545f, 0.545f, 0.545f, 1.0f };
    private static final float[] UI_SLOT_DARK = new float[] { 0.216f, 0.216f, 0.216f, 1.0f };
    private static final float[] UI_SLOT_LIGHT = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };

    // 開啟容器 UI 時的全螢幕變暗效果，模擬 Minecraft 的背景遮罩。
    private static final float[] UI_SCREEN_DIM = new float[] { 0.0f, 0.0f, 0.0f, 0.62f };

    // hotbar 使用半透明深色底與灰色格線，選取格用白色粗框，貼近 Minecraft 原版外觀。
    private static final float[] HOTBAR_BG = new float[] { 0.0f, 0.0f, 0.0f, 0.5f };
    private static final float[] HOTBAR_BORDER = new float[] { 0.55f, 0.55f, 0.55f, 0.75f };
    private static final float[] HOTBAR_SELECTION = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };

    // 選取中的背包格子用半透明白色覆蓋，類似 Minecraft 的格子 hover 高亮。
    private static final float[] SLOT_HIGHLIGHT = new float[] { 1.0f, 1.0f, 1.0f, 0.45f };

    // 一般文字為白色加深色陰影；面板標題用 Minecraft 的 #404040 深灰、不加陰影。
    private static final float[] TEXT_COLOR = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
    private static final float[] TEXT_SHADOW_COLOR = new float[] { 0.25f, 0.25f, 0.25f, 1.0f };
    private static final float[] TITLE_COLOR = new float[] { 0.25f, 0.25f, 0.25f, 1.0f };

    // F3 偵錯文字每行的半透明底色。
    private static final float[] DEBUG_BG_COLOR = new float[] { 0.0f, 0.0f, 0.0f, 0.35f };

    // 圖示細節沿用的暗色邊。
    private static final float[] INVENTORY_SLOT_BORDER = UI_SLOT_DARK;

    // Minecraft 的準星是淺色小十字。
    private static final float[] CROSSHAIR_COLOR = new float[] { 0.92f, 0.92f, 0.92f, 0.9f };

    // 面板與格子的立體邊厚度（NDC Y 單位；X 方向依視窗比例換算）。
    private static final float PANEL_BORDER_Y = 0.010f;
    private static final float BEVEL_Y = 0.008f;

    private static final int CHEST_COLUMNS = 9;
    private static final int CHEST_ROWS = 3;
    private static final float CHEST_SLOT_HEIGHT = 0.125f;
    private static final float CHEST_GAP = 0.0f;
    private static final float CHEST_GRID_TOP = 0.42f;
    private static final float CHEST_PANEL_PAD_X = 0.065f;
    private static final float CHEST_PANEL_PAD_TOP = 0.13f;
    private static final float CHEST_PANEL_PAD_BOTTOM = 0.10f;

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
    private static final float[] COLOR_OAK_STAIRS = new float[] { 0.70f, 0.50f, 0.27f, 0.95f };
    private static final float[] COLOR_OAK_SLAB = new float[] { 0.73f, 0.55f, 0.32f, 0.95f };
    private static final float[] COLOR_STONE_SLAB = new float[] { 0.57f, 0.57f, 0.57f, 0.95f };
    private static final float[] COLOR_OAK_FENCE = new float[] { 0.62f, 0.42f, 0.22f, 0.95f };
    private static final float[] COLOR_OAK_DOOR = new float[] { 0.66f, 0.43f, 0.21f, 0.95f };
    private static final float[] COLOR_OAK_TRAPDOOR = new float[] { 0.64f, 0.41f, 0.20f, 0.95f };
    private static final float[] COLOR_LADDER = new float[] { 0.68f, 0.46f, 0.23f, 0.95f };
    private static final float[] COLOR_TORCH = new float[] { 0.95f, 0.70f, 0.24f, 0.95f };
    private static final float[] COLOR_CRAFTING_TABLE = new float[] { 0.61f, 0.38f, 0.20f, 0.95f };
    private static final float[] COLOR_FURNACE = new float[] { 0.43f, 0.43f, 0.43f, 0.95f };
    private static final float[] COLOR_CHEST = new float[] { 0.73f, 0.48f, 0.20f, 0.95f };
    private static final float[] COLOR_BOOKSHELF = new float[] { 0.62f, 0.33f, 0.23f, 0.95f };
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

    // 圖示重建時的臨時色彩，避免 lightenColor/darkenColor 每次建立短生命週期陣列。
    private final float[] tmpLightColor = new float[4];
    private final float[] tmpDarkColor = new float[4];

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
    private boolean cachedChestOpen;

    // 快取上一次方塊名稱提示是否顯示。
    private boolean cachedLabelVisible;

    // 快取上一次 F3 偵錯文字的內容簽章。
    private int cachedDebugSignature;

    // 快取上一次 viewport 寬度。
    private int cachedViewportWidth = Integer.MIN_VALUE;

    // 快取上一次 viewport 高度。
    private int cachedViewportHeight = Integer.MIN_VALUE;

    // 建立 HUD 所需的 shader 與基本 mesh。
    public HudRenderer() {
        shader = new ShaderProgram("/shaders/hud.vert", "/shaders/hud.frag");

        // 準星會依 viewport 比例重建，確保實際畫面上是置中的正十字。
        crosshair = new Mesh(new float[0], GL_TRIANGLES, 3, 4);

        // hotbar mesh 一開始先建立空資料，之後再動態更新。
        hotbarMesh = new Mesh(new float[0], GL_TRIANGLES, 3, 4);
    }

    // 繪製 HUD。debugLines 為 null 時不顯示 F3 偵錯文字。
    public void render(BlockType[] hotbar, int selectedIndex, boolean showHotbarLabel, boolean creativeInventoryOpen,
            BlockType[] creativeBlocks, int creativePage, int creativeTotalPages, boolean chestOpen,
            String[] debugLines) {
        glDisable(GL_DEPTH_TEST);
        shader.use();

        // 取得目前 viewport 大小，讓圖示在不同畫面比例下維持正常外觀；HUD 不依賴 Window 尺寸參數。
        glGetIntegerv(GL_VIEWPORT, viewport);
        int viewportWidth = Math.max(1, viewport[2]);
        int viewportHeight = Math.max(1, viewport[3]);
        float viewportAspect = (float) viewportWidth / (float) viewportHeight;

        boolean viewportChanged = viewportWidth != cachedViewportWidth || viewportHeight != cachedViewportHeight;
        int hotbarSignature = hotbarSignature(hotbar);
        int creativeSignature = creativeInventoryOpen && creativeBlocks != null ? hotbarSignature(creativeBlocks) : 0;
        int debugSignature = debugLines != null ? java.util.Arrays.hashCode(debugLines) : 0;

        if (viewportChanged) {
            updateCrosshairMesh(viewportAspect);
        }

        // 只有在 hotbar 內容、選取狀態或視窗大小變動時才重建 mesh。
        if (viewportChanged
                || hotbarSignature != cachedHotbarSignature
                || selectedIndex != cachedSelectedIndex
                || showHotbarLabel != cachedLabelVisible
                || creativeSignature != cachedCreativeSignature
                || creativePage != cachedCreativePage
                || creativeTotalPages != cachedCreativeTotalPages
                || creativeInventoryOpen != cachedCreativeOpen
                || chestOpen != cachedChestOpen
                || debugSignature != cachedDebugSignature) {
            updateHotbarMesh(hotbar, selectedIndex, showHotbarLabel, viewportAspect,
                    creativeInventoryOpen, creativeBlocks, creativePage, creativeTotalPages, chestOpen, debugLines);
            cachedHotbarSignature = hotbarSignature;
            cachedSelectedIndex = selectedIndex;
            cachedLabelVisible = showHotbarLabel;
            cachedCreativeSignature = creativeSignature;
            cachedCreativePage = creativePage;
            cachedCreativeTotalPages = creativeTotalPages;
            cachedCreativeOpen = creativeInventoryOpen;
            cachedChestOpen = chestOpen;
            cachedDebugSignature = debugSignature;
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

    // 依方塊總數計算頁數；至少回傳 1，讓沒有資料時 UI 仍有穩定頁碼與快取 key。
    public static int creativeTotalPages(int blockCount) {
        return Math.max(1, (blockCount + CREATIVE_SLOTS_PER_PAGE - 1) / CREATIVE_SLOTS_PER_PAGE);
    }

    // 將 GLFW 視窗座標轉成 HUD 使用的 NDC 座標，再回推目前滑到的創造背包格子。
    public static int creativeSlotAt(double mouseX, double mouseY, int windowWidth, int windowHeight, int page,
            int blockCount) {
        if (windowWidth <= 0 || windowHeight <= 0 || blockCount <= 0) {
            return -1;
        }

        double ndcX = (mouseX / (double) windowWidth) * 2.0 - 1.0;
        double ndcY = 1.0 - (mouseY / (double) windowHeight) * 2.0;

        float aspect = (float) windowWidth / (float) windowHeight;
        float slotWidth = squareSlotWidth(CREATIVE_SLOT_HEIGHT, aspect);
        float startX = creativeGridStartX(aspect);
        float cellWidth = slotWidth + CREATIVE_GAP;
        float cellHeight = CREATIVE_SLOT_HEIGHT + CREATIVE_GAP;

        for (int row = 0; row < CREATIVE_ROWS; row++) {
            float slotY = CREATIVE_GRID_TOP - row * cellHeight - CREATIVE_SLOT_HEIGHT;
            if (ndcY < slotY || ndcY > slotY + CREATIVE_SLOT_HEIGHT) {
                continue;
            }

            for (int col = 0; col < CREATIVE_COLUMNS; col++) {
                float slotX = startX + col * cellWidth;
                if (ndcX < slotX || ndcX > slotX + slotWidth) {
                    continue;
                }

                int index = page * CREATIVE_SLOTS_PER_PAGE + row * CREATIVE_COLUMNS + col;
                return index < blockCount ? index : -1;
            }
        }

        return -1;
    }

    // 把 NDC 高度換算成在目前視窗比例下看起來是正方形的 NDC 寬度。
    private static float squareSlotWidth(float slotHeight, float aspect) {
        return slotHeight / Math.max(0.1f, aspect);
    }

    // 準星由兩個矩形組成，寬度按 aspect 修正，避免寬螢幕下變成橫向拉長的十字。
    private void updateCrosshairMesh(float viewportAspect) {
        float safeAspect = Math.max(0.1f, viewportAspect);
        float halfLengthY = 0.026f;
        float halfThicknessY = 0.0046f;
        float halfLengthX = halfLengthY / safeAspect;
        float halfThicknessX = halfThicknessY / safeAspect;

        FloatArrayBuilder out = new FloatArrayBuilder(128);
        addRect(out, -halfLengthX, -halfThicknessY, halfLengthX * 2.0f, halfThicknessY * 2.0f, CROSSHAIR_COLOR);
        addRect(out, -halfThicknessX, -halfLengthY, halfThicknessX * 2.0f, halfLengthY * 2.0f, CROSSHAIR_COLOR);
        crosshair.update(out.toArray(), STRIDE);
    }

    // 依照 hotbar 內容與目前選取狀態，重新建立 hotbar mesh。
    private void updateHotbarMesh(BlockType[] hotbar, int selectedIndex, boolean showHotbarLabel,
            float viewportAspect, boolean creativeInventoryOpen, BlockType[] creativeBlocks, int creativePage,
            int creativeTotalPages, boolean chestOpen, String[] debugLines) {
        hotbarVertices.clear();

        if (chestOpen) {
            addChestPanel(hotbarVertices, viewportAspect);
        } else if (creativeInventoryOpen && creativeBlocks != null) {
            addCreativeInventoryPanel(hotbarVertices, hotbar, selectedIndex, creativeBlocks, creativePage,
                    creativeTotalPages, viewportAspect);
        }

        if (debugLines != null) {
            addDebugOverlay(hotbarVertices, debugLines);
        }

        float aspect = Math.max(0.1f, viewportAspect);
        float slotWidth = squareSlotWidth(HOTBAR_SLOT_HEIGHT, aspect);
        int slots = hotbar.length;
        float totalWidth = slots * slotWidth;
        float startX = -totalWidth * 0.5f;
        float y = HOTBAR_Y;
        float lineY = 0.006f;
        float lineX = lineY / aspect;

        // Minecraft 式 hotbar：半透明深色底、灰色外框與格線。
        addRect(hotbarVertices, startX - lineX * 2.0f, y - lineY * 2.0f,
                totalWidth + lineX * 4.0f, HOTBAR_SLOT_HEIGHT + lineY * 4.0f, HOTBAR_BORDER);
        addRect(hotbarVertices, startX - lineX, y - lineY,
                totalWidth + lineX * 2.0f, HOTBAR_SLOT_HEIGHT + lineY * 2.0f, HOTBAR_BG);

        for (int i = 0; i < slots; i++) {
            float slotX = startX + i * slotWidth;
            addFrame(hotbarVertices, slotX, y, slotWidth, HOTBAR_SLOT_HEIGHT, lineX, lineY, HOTBAR_BORDER);
            addHotbarIcon(hotbarVertices, hotbar[i], slotX, y, slotWidth, aspect);
        }

        // 被選取的格子用比格子稍大的白色粗框標示，與 Minecraft 的選取框一致。
        if (selectedIndex >= 0 && selectedIndex < slots) {
            float selX = startX + selectedIndex * slotWidth;
            addFrame(hotbarVertices, selX - lineX * 2.0f, y - lineY * 2.0f,
                    slotWidth + lineX * 4.0f, HOTBAR_SLOT_HEIGHT + lineY * 4.0f,
                    lineX * 2.0f, lineY * 2.0f, HOTBAR_SELECTION);
        }

        // 切換選取後，名稱在 hotbar 上方短暫顯示，逾時後由 Game 收回 showHotbarLabel。
        if (showHotbarLabel && selectedIndex >= 0 && selectedIndex < hotbar.length) {
            String label = hotbar[selectedIndex].displayName();
            addCenteredText(hotbarVertices, label, HOTBAR_Y + HOTBAR_SLOT_HEIGHT + HOTBAR_LABEL_MARGIN,
                    TEXT_COLOR, true);
        }

        hotbarMesh.update(hotbarVertices.toArray(), STRIDE);
    }

    // 在 hotbar 格子內畫出方塊小圖示。
    private void addHotbarIcon(FloatArrayBuilder out, BlockType block, float x, float y, float slotWidth,
            float aspect) {
        float cubeHeight = HOTBAR_SLOT_HEIGHT * 0.44f;
        float cubeWidth = cubeHeight / Math.max(0.5f, aspect);
        float depthX = cubeWidth * 0.30f;
        float depthY = cubeHeight * 0.22f;
        float cubeX = x + (slotWidth - (cubeWidth + depthX)) * 0.5f;
        float cubeY = y + (HOTBAR_SLOT_HEIGHT - (cubeHeight + depthY)) * 0.5f;

        addBlockIcon(out, block, cubeX, cubeY, cubeWidth, cubeHeight, depthX, depthY);
    }

    // 用四條邊框條組成一個空心矩形框。
    private void addFrame(FloatArrayBuilder out, float x, float y, float width, float height,
            float thicknessX, float thicknessY, float[] color) {
        addRect(out, x, y + height - thicknessY, width, thicknessY, color);
        addRect(out, x, y, width, thicknessY, color);
        addRect(out, x, y + thicknessY, thicknessX, height - thicknessY * 2.0f, color);
        addRect(out, x + width - thicknessX, y + thicknessY, thicknessX, height - thicknessY * 2.0f, color);
    }

    // 目前箱子只畫空格介面；未來接入容器資料時可在這裡填入每格物品圖示。
    private void addChestPanel(FloatArrayBuilder out, float viewportAspect) {
        float aspect = Math.max(0.1f, viewportAspect);
        float slotWidth = squareSlotWidth(CHEST_SLOT_HEIGHT, aspect);
        float gridWidth = CHEST_COLUMNS * slotWidth + (CHEST_COLUMNS - 1) * CHEST_GAP;
        float gridHeight = CHEST_ROWS * CHEST_SLOT_HEIGHT + (CHEST_ROWS - 1) * CHEST_GAP;
        float startX = -gridWidth * 0.5f;
        float gridBottom = CHEST_GRID_TOP - gridHeight;
        float panelX = startX - CHEST_PANEL_PAD_X;
        float panelY = gridBottom - CHEST_PANEL_PAD_BOTTOM;
        float panelWidth = gridWidth + CHEST_PANEL_PAD_X * 2.0f;
        float panelHeight = gridHeight + CHEST_PANEL_PAD_TOP + CHEST_PANEL_PAD_BOTTOM;

        // Minecraft 開啟容器時，背後的世界會整體變暗。
        addRect(out, -1.0f, -1.0f, 2.0f, 2.0f, UI_SCREEN_DIM);

        addBlockPanel(out, panelX, panelY, panelWidth, panelHeight, aspect);
        addCenteredText(out, "Chest", CHEST_GRID_TOP + 0.046f, TITLE_COLOR, false);

        for (int row = 0; row < CHEST_ROWS; row++) {
            for (int col = 0; col < CHEST_COLUMNS; col++) {
                float x = startX + col * (slotWidth + CHEST_GAP);
                float y = CHEST_GRID_TOP - row * (CHEST_SLOT_HEIGHT + CHEST_GAP) - CHEST_SLOT_HEIGHT;
                addSlot(out, x, y, slotWidth, CHEST_SLOT_HEIGHT, false, aspect);
            }
        }
    }

    // 建立創造背包面板與當頁方塊圖示；選中狀態以目前 hotbar 方塊反查，不另外保存 UI 狀態。
    private void addCreativeInventoryPanel(FloatArrayBuilder out, BlockType[] hotbar, int selectedIndex,
            BlockType[] creativeBlocks, int page, int totalPages, float viewportAspect) {
        float aspect = Math.max(0.1f, viewportAspect);
        float slotWidth = squareSlotWidth(CREATIVE_SLOT_HEIGHT, aspect);
        float gridWidth = creativeGridWidth(aspect);
        float gridHeight = CREATIVE_ROWS * CREATIVE_SLOT_HEIGHT + (CREATIVE_ROWS - 1) * CREATIVE_GAP;
        float startX = creativeGridStartX(aspect);
        float gridBottom = CREATIVE_GRID_TOP - gridHeight;
        float panelX = startX - CREATIVE_PANEL_PAD_X;
        float panelY = gridBottom - CREATIVE_PANEL_PAD_BOTTOM;
        float panelWidth = gridWidth + CREATIVE_PANEL_PAD_X * 2.0f;
        float panelHeight = gridHeight + CREATIVE_PANEL_PAD_TOP + CREATIVE_PANEL_PAD_BOTTOM;

        // Minecraft 開啟背包時，背後的世界會整體變暗。
        addRect(out, -1.0f, -1.0f, 2.0f, 2.0f, UI_SCREEN_DIM);

        addBlockPanel(out, panelX, panelY, panelWidth, panelHeight, aspect);
        addCenteredText(out, "Creative", CREATIVE_GRID_TOP + 0.046f, TITLE_COLOR, false);

        BlockType selected = selectedIndex >= 0 && selectedIndex < hotbar.length ? hotbar[selectedIndex] : null;
        float cellWidth = slotWidth + CREATIVE_GAP;
        float cellHeight = CREATIVE_SLOT_HEIGHT + CREATIVE_GAP;
        int firstBlock = Math.max(0, page) * CREATIVE_SLOTS_PER_PAGE;

        for (int row = 0; row < CREATIVE_ROWS; row++) {
            for (int col = 0; col < CREATIVE_COLUMNS; col++) {
                int blockIndex = firstBlock + row * CREATIVE_COLUMNS + col;
                float x = startX + col * cellWidth;
                float y = CREATIVE_GRID_TOP - row * cellHeight - CREATIVE_SLOT_HEIGHT;
                boolean hasBlock = blockIndex < creativeBlocks.length;
                boolean selectedBlock = hasBlock && creativeBlocks[blockIndex] == selected;

                addSlot(out, x, y, slotWidth, CREATIVE_SLOT_HEIGHT, selectedBlock, aspect);

                if (!hasBlock) {
                    continue;
                }

                float cubeHeight = CREATIVE_SLOT_HEIGHT * 0.46f;
                float cubeWidth = cubeHeight / Math.max(0.5f, aspect);
                float depthX = cubeWidth * 0.30f;
                float depthY = cubeHeight * 0.22f;
                float cubeX = x + (slotWidth - (cubeWidth + depthX)) * 0.5f;
                float cubeY = y + (CREATIVE_SLOT_HEIGHT - (cubeHeight + depthY)) * 0.5f;

                addBlockIcon(out, creativeBlocks[blockIndex], cubeX, cubeY, cubeWidth, cubeHeight, depthX, depthY);
            }
        }

        String pageLabel = "Page " + Math.min(page + 1, Math.max(1, totalPages)) + "/" + Math.max(1, totalPages);
        addCenteredText(out, pageLabel, panelY + 0.035f, TITLE_COLOR, false);
    }

    private static float creativeGridWidth(float aspect) {
        return CREATIVE_COLUMNS * squareSlotWidth(CREATIVE_SLOT_HEIGHT, aspect)
                + (CREATIVE_COLUMNS - 1) * CREATIVE_GAP;
    }

    private static float creativeGridStartX(float aspect) {
        return -creativeGridWidth(aspect) * 0.5f;
    }

    // Minecraft 式面板：黑色外框、#C6C6C6 灰面、左上白色高光與右下深灰陰影。
    private void addBlockPanel(FloatArrayBuilder out, float x, float y, float width, float height, float aspect) {
        float borderY = PANEL_BORDER_Y;
        float borderX = borderY / Math.max(0.1f, aspect);
        float bevelY = BEVEL_Y;
        float bevelX = bevelY / Math.max(0.1f, aspect);

        addRect(out, x, y, width, height, UI_BLACK_BORDER);

        float faceX = x + borderX;
        float faceY = y + borderY;
        float faceWidth = width - borderX * 2.0f;
        float faceHeight = height - borderY * 2.0f;
        addRect(out, faceX, faceY, faceWidth, faceHeight, UI_PANEL_FACE);

        // 光源來自左上：上與左是白色高光，下與右是深灰陰影。
        addRect(out, faceX, faceY + faceHeight - bevelY, faceWidth, bevelY, UI_PANEL_LIGHT);
        addRect(out, faceX, faceY, bevelX, faceHeight, UI_PANEL_LIGHT);
        addRect(out, faceX + bevelX, faceY, faceWidth - bevelX, bevelY, UI_PANEL_DARK);
        addRect(out, faceX + faceWidth - bevelX, faceY, bevelX, faceHeight - bevelY, UI_PANEL_DARK);
    }

    // Minecraft 式凹陷格子：#8B8B8B 灰面、左上 #373737 暗緣、右下白色亮緣。
    private void addSlot(FloatArrayBuilder out, float x, float y, float width, float height, boolean selected,
            float aspect) {
        float bevelY = BEVEL_Y * 0.75f;
        float bevelX = bevelY / Math.max(0.1f, aspect);

        addRect(out, x, y, width, height, UI_SLOT_FACE);

        // 凹陷面的光影與面板相反：上與左是暗緣，下與右是亮緣。
        addRect(out, x, y + height - bevelY, width, bevelY, UI_SLOT_DARK);
        addRect(out, x, y, bevelX, height - bevelY, UI_SLOT_DARK);
        addRect(out, x + bevelX, y, width - bevelX, bevelY, UI_SLOT_LIGHT);
        addRect(out, x + width - bevelX, y + bevelY, bevelX, height - bevelY * 2.0f, UI_SLOT_LIGHT);

        // 對應目前 hotbar 選取方塊的格子，用半透明白色覆蓋標示。
        if (selected) {
            addRect(out, x + bevelX, y + bevelY, width - bevelX * 2.0f, height - bevelY * 2.0f, SLOT_HIGHLIGHT);
        }
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

    // 針對非完整方塊和基本物品畫出更容易辨識的 2D 輪廓，避免背包裡全部看起來像普通立方體。
    private void addBlockIcon(FloatArrayBuilder out, BlockType type, float x, float y, float width, float height,
            float depthX, float depthY) {
        float[] color = blockColor(type);
        switch (type) {
            case OAK_STAIRS, OAK_STAIRS_NORTH, OAK_STAIRS_EAST, OAK_STAIRS_SOUTH, OAK_STAIRS_WEST ->
                addStairsIcon(out, x, y, width, height, color);
            case OAK_SLAB, STONE_SLAB -> addSlabIcon(out, x, y, width, height, color);
            case OAK_FENCE -> addFenceIcon(out, x, y, width, height, color);
            case OAK_DOOR, OAK_DOOR_NORTH_BOTTOM, OAK_DOOR_NORTH_TOP, OAK_DOOR_EAST_BOTTOM, OAK_DOOR_EAST_TOP,
                    OAK_DOOR_SOUTH_BOTTOM, OAK_DOOR_SOUTH_TOP, OAK_DOOR_WEST_BOTTOM, OAK_DOOR_WEST_TOP,
                    OAK_DOOR_NORTH_OPEN_BOTTOM, OAK_DOOR_NORTH_OPEN_TOP, OAK_DOOR_EAST_OPEN_BOTTOM,
                    OAK_DOOR_EAST_OPEN_TOP, OAK_DOOR_SOUTH_OPEN_BOTTOM, OAK_DOOR_SOUTH_OPEN_TOP,
                    OAK_DOOR_WEST_OPEN_BOTTOM, OAK_DOOR_WEST_OPEN_TOP ->
                addDoorIcon(out, x, y, width, height, color);
            case OAK_TRAPDOOR, OAK_TRAPDOOR_OPEN -> addTrapdoorIcon(out, x, y, width, height, color);
            case LADDER, LADDER_NORTH, LADDER_EAST, LADDER_SOUTH, LADDER_WEST ->
                addLadderIcon(out, x, y, width, height, color);
            case TORCH -> addTorchIcon(out, x, y, width, height, color);
            case CRAFTING_TABLE -> addCraftingTableIcon(out, x, y, width, height, depthX, depthY, color);
            case FURNACE -> addFurnaceIcon(out, x, y, width, height, depthX, depthY, color);
            case CHEST -> addChestIcon(out, x, y, width, height, depthX, depthY, color);
            case BOOKSHELF -> addBookshelfIcon(out, x, y, width, height, depthX, depthY, color);
            default -> addCubeIcon(out, x, y, width, height, depthX, depthY, color);
        }
    }

    private void addStairsIcon(FloatArrayBuilder out, float x, float y, float width, float height, float[] color) {
        float stepW = width / 3.0f;
        float stepH = height / 3.0f;
        addRect(out, x, y, width, stepH, color);
        addRect(out, x + stepW, y + stepH, width - stepW, stepH, color);
        addRect(out, x + stepW * 2.0f, y + stepH * 2.0f, width - stepW * 2.0f, stepH, color);
        addRect(out, x, y - 0.004f, width, 0.004f, INVENTORY_SLOT_BORDER);
    }

    private void addSlabIcon(FloatArrayBuilder out, float x, float y, float width, float height, float[] color) {
        addRect(out, x, y, width, height * 0.42f, color);
        addRect(out, x, y + height * 0.42f, width, height * 0.08f, lightenColor(color));
    }

    private void addFenceIcon(FloatArrayBuilder out, float x, float y, float width, float height, float[] color) {
        float postW = width * 0.16f;
        float railH = height * 0.14f;
        addRect(out, x + width * 0.12f, y, postW, height, color);
        addRect(out, x + width * 0.72f, y, postW, height, color);
        addRect(out, x, y + height * 0.30f, width, railH, color);
        addRect(out, x, y + height * 0.64f, width, railH, color);
    }

    private void addDoorIcon(FloatArrayBuilder out, float x, float y, float width, float height, float[] color) {
        float doorX = x + width * 0.20f;
        float doorW = width * 0.60f;
        addRect(out, doorX, y, doorW, height, color);
        addRect(out, doorX + doorW * 0.12f, y + height * 0.10f, doorW * 0.76f, height * 0.32f, darkenColor(color));
        addRect(out, doorX + doorW * 0.12f, y + height * 0.56f, doorW * 0.76f, height * 0.32f, darkenColor(color));
        addRect(out, doorX + doorW * 0.72f, y + height * 0.47f, doorW * 0.12f, height * 0.08f,
                COLOR_GLOWSTONE);
    }

    private void addTrapdoorIcon(FloatArrayBuilder out, float x, float y, float width, float height, float[] color) {
        float trapY = y + height * 0.15f;
        float trapH = height * 0.70f;
        addRect(out, x, trapY, width, trapH, color);
        addRect(out, x + width * 0.12f, trapY + trapH * 0.12f, width * 0.76f, trapH * 0.14f,
                darkenColor(color));
        addRect(out, x + width * 0.12f, trapY + trapH * 0.43f, width * 0.76f, trapH * 0.14f,
                darkenColor(color));
        addRect(out, x + width * 0.12f, trapY + trapH * 0.74f, width * 0.76f, trapH * 0.14f,
                darkenColor(color));
    }

    private void addLadderIcon(FloatArrayBuilder out, float x, float y, float width, float height, float[] color) {
        float railW = width * 0.14f;
        addRect(out, x + width * 0.22f, y, railW, height, color);
        addRect(out, x + width * 0.64f, y, railW, height, color);
        for (int i = 0; i < 4; i++) {
            float rungY = y + height * (0.14f + i * 0.23f);
            addRect(out, x + width * 0.20f, rungY, width * 0.60f, height * 0.08f, color);
        }
    }

    private void addTorchIcon(FloatArrayBuilder out, float x, float y, float width, float height, float[] color) {
        addRect(out, x + width * 0.43f, y, width * 0.14f, height * 0.68f, darkenColor(color));
        addRect(out, x + width * 0.34f, y + height * 0.58f, width * 0.32f, height * 0.38f, COLOR_GLOWSTONE);
    }

    private void addCraftingTableIcon(FloatArrayBuilder out, float x, float y, float width, float height,
            float depthX, float depthY, float[] color) {
        addCubeIcon(out, x, y, width, height, depthX, depthY, color);
        addRect(out, x + width * 0.30f, y + height * 0.16f, width * 0.08f, height * 0.68f, darkenColor(color));
        addRect(out, x + width * 0.62f, y + height * 0.16f, width * 0.08f, height * 0.68f, darkenColor(color));
        addRect(out, x + width * 0.14f, y + height * 0.42f, width * 0.72f, height * 0.08f, darkenColor(color));
    }

    private void addFurnaceIcon(FloatArrayBuilder out, float x, float y, float width, float height, float depthX,
            float depthY, float[] color) {
        addCubeIcon(out, x, y, width, height, depthX, depthY, color);
        addRect(out, x + width * 0.22f, y + height * 0.30f, width * 0.56f, height * 0.38f, COLOR_OBSIDIAN);
    }

    private void addChestIcon(FloatArrayBuilder out, float x, float y, float width, float height, float depthX,
            float depthY, float[] color) {
        addCubeIcon(out, x, y, width, height, depthX, depthY, color);
        addRect(out, x, y + height * 0.50f, width, height * 0.08f, darkenColor(color));
        addRect(out, x + width * 0.44f, y + height * 0.38f, width * 0.16f, height * 0.20f, COLOR_GLOWSTONE);
    }

    private void addBookshelfIcon(FloatArrayBuilder out, float x, float y, float width, float height, float depthX,
            float depthY, float[] color) {
        addCubeIcon(out, x, y, width, height, depthX, depthY, color);
        float bookW = width * 0.12f;
        for (int i = 0; i < 5; i++) {
            float bx = x + width * 0.14f + i * bookW * 1.25f;
            float[] bookColor = switch (i % 3) {
                case 0 -> COLOR_RED_WOOL;
                case 1 -> COLOR_BLUE_WOOL;
                default -> COLOR_GREEN_WOOL;
            };
            addRect(out, bx, y + height * 0.18f, bookW, height * 0.58f, bookColor);
        }
    }

    private float[] lightenColor(float[] color) {
        tmpLightColor[0] = lighten(color[0]);
        tmpLightColor[1] = lighten(color[1]);
        tmpLightColor[2] = lighten(color[2]);
        tmpLightColor[3] = color[3];
        return tmpLightColor;
    }

    private float[] darkenColor(float[] color) {
        tmpDarkColor[0] = darken(color[0]);
        tmpDarkColor[1] = darken(color[1]);
        tmpDarkColor[2] = darken(color[2]);
        tmpDarkColor[3] = color[3];
        return tmpDarkColor;
    }

    // 讓顏色稍微變亮。
    private float lighten(float channel) {
        return Math.min(channel * 1.20f + 0.03f, 1.0f);
    }

    // 讓顏色稍微變暗。
    private float darken(float channel) {
        return Math.max(channel * 0.72f, 0.0f);
    }

    // 在畫面中央加入一段置中的文字；Minecraft 的文字不畫底框，亮色文字配右下深色陰影。
    private void addCenteredText(FloatArrayBuilder out, String text, float y, float[] color, boolean shadow) {
        if (text == null || text.isBlank()) {
            return;
        }
        addText(out, text, -textWidth(text) * 0.5f, y, color, shadow);
    }

    // 從指定起點往右畫一段文字。
    private void addText(FloatArrayBuilder out, String text, float startX, float y, float[] color, boolean shadow) {
        float advance = (FONT_GLYPH_WIDTH + FONT_GLYPH_SPACING) * TEXT_PIXEL_WIDTH;

        // 陰影向右下偏移一個文字像素，模擬 Minecraft 字型的投影。
        if (shadow) {
            float cursorX = startX + TEXT_PIXEL_WIDTH;
            for (int i = 0; i < text.length(); i++) {
                String[] glyph = glyphFor(text.charAt(i));
                addGlyph(out, cursorX, y - TEXT_PIXEL_HEIGHT, glyph, TEXT_SHADOW_COLOR);
                cursorX += advance;
            }
        }

        float cursorX = startX;
        for (int i = 0; i < text.length(); i++) {
            String[] glyph = glyphFor(text.charAt(i));
            addGlyph(out, cursorX, y, glyph, color);
            cursorX += advance;
        }
    }

    // 計算一段文字的 NDC 寬度。
    private static float textWidth(String text) {
        int charCount = text.length();
        int pixelColumns = charCount * FONT_GLYPH_WIDTH + Math.max(0, charCount - 1) * FONT_GLYPH_SPACING;
        return pixelColumns * TEXT_PIXEL_WIDTH;
    }

    // 左上角的 F3 偵錯文字；每行帶半透明深色底，仿照新版 Minecraft 的 F3 樣式。
    private void addDebugOverlay(FloatArrayBuilder out, String[] lines) {
        float x = -0.985f;
        float lineHeight = FONT_GLYPH_HEIGHT * TEXT_PIXEL_HEIGHT;
        float lineStep = lineHeight + TEXT_PIXEL_HEIGHT * 3.0f;
        float y = 0.96f - lineHeight;

        for (String line : lines) {
            if (line == null || line.isBlank()) {
                y -= lineStep;
                continue;
            }

            float padX = TEXT_PIXEL_WIDTH * 2.0f;
            float padY = TEXT_PIXEL_HEIGHT * 1.2f;
            addRect(out, x - padX, y - padY, textWidth(line) + padX * 2.0f, lineHeight + padY * 2.0f,
                    DEBUG_BG_COLOR);
            addText(out, line, x, y, TEXT_COLOR, true);
            y -= lineStep;
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
        putGlyph(font, ':', "00000", "01100", "01100", "00000", "01100", "01100", "00000");
        putGlyph(font, '.', "00000", "00000", "00000", "00000", "00000", "01100", "01100");
        putGlyph(font, ',', "00000", "00000", "00000", "00000", "01100", "01100", "01000");
        putGlyph(font, '-', "00000", "00000", "00000", "01110", "00000", "00000", "00000");

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
            case OAK_STAIRS, OAK_STAIRS_NORTH, OAK_STAIRS_EAST, OAK_STAIRS_SOUTH, OAK_STAIRS_WEST ->
                COLOR_OAK_STAIRS;
            case OAK_SLAB -> COLOR_OAK_SLAB;
            case STONE_SLAB -> COLOR_STONE_SLAB;
            case OAK_FENCE -> COLOR_OAK_FENCE;
            case OAK_DOOR, OAK_DOOR_NORTH_BOTTOM, OAK_DOOR_NORTH_TOP, OAK_DOOR_EAST_BOTTOM, OAK_DOOR_EAST_TOP,
                    OAK_DOOR_SOUTH_BOTTOM, OAK_DOOR_SOUTH_TOP, OAK_DOOR_WEST_BOTTOM, OAK_DOOR_WEST_TOP,
                    OAK_DOOR_NORTH_OPEN_BOTTOM, OAK_DOOR_NORTH_OPEN_TOP, OAK_DOOR_EAST_OPEN_BOTTOM,
                    OAK_DOOR_EAST_OPEN_TOP, OAK_DOOR_SOUTH_OPEN_BOTTOM, OAK_DOOR_SOUTH_OPEN_TOP,
                    OAK_DOOR_WEST_OPEN_BOTTOM, OAK_DOOR_WEST_OPEN_TOP ->
                COLOR_OAK_DOOR;
            case OAK_TRAPDOOR, OAK_TRAPDOOR_OPEN -> COLOR_OAK_TRAPDOOR;
            case LADDER, LADDER_NORTH, LADDER_EAST, LADDER_SOUTH, LADDER_WEST -> COLOR_LADDER;
            case TORCH -> COLOR_TORCH;
            case CRAFTING_TABLE -> COLOR_CRAFTING_TABLE;
            case FURNACE -> COLOR_FURNACE;
            case CHEST -> COLOR_CHEST;
            case BOOKSHELF -> COLOR_BOOKSHELF;
            default -> COLOR_DEFAULT;
        };
    }
}

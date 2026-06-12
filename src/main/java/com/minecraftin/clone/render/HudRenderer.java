package com.minecraftin.clone.render;

import com.minecraftin.clone.engine.Mesh;
import com.minecraftin.clone.engine.ShaderProgram;
import com.minecraftin.clone.engine.TextureAtlas;
import com.minecraftin.clone.util.FloatArrayBuilder;
import com.minecraftin.clone.world.BlockType;
import com.minecraftin.clone.world.Face;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33C.*;

// 負責繪製 HUD，例如準星、hotbar，以及目前選取方塊的名稱。
// HUD 使用 NDC 座標直接繪製 2D mesh，不經由相機矩陣，因此頂點資料與螢幕比例處理都在這裡完成。
// 方塊縮圖直接取樣世界共用的材質圖集；純色幾何（面板、文字）則取樣圖集中的純白格，讓整個 HUD 一次繪製。
public final class HudRenderer implements AutoCloseable {

    // 每個頂點包含 9 個 float：位置 xyz + 顏色 rgba + 貼圖 uv。
    private static final int STRIDE = 9;

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

    // 一行文字在 NDC 中的高度，用於面板標題與頁碼的垂直置中。
    private static final float TEXT_BLOCK_HEIGHT = FONT_GLYPH_HEIGHT * TEXT_PIXEL_HEIGHT;

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
    // 格子加大到 0.16，讓五列九欄的面板佔據約半個畫面高度，圖示與格距更清楚不擁擠。
    private static final float CREATIVE_SLOT_HEIGHT = 0.16f;
    private static final float CREATIVE_GAP = 0.0f;
    private static final float CREATIVE_GRID_TOP = 0.62f;
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

    // Minecraft 的準星是淺色小十字。
    private static final float[] CROSSHAIR_COLOR = new float[] { 0.92f, 0.92f, 0.92f, 0.9f };

    // 面板與格子的立體邊厚度（NDC Y 單位；X 方向依視窗比例換算）。
    private static final float PANEL_BORDER_Y = 0.010f;
    private static final float BEVEL_Y = 0.008f;

    private static final int CHEST_COLUMNS = 9;
    private static final int CHEST_ROWS = 3;
    private static final float CHEST_SLOT_HEIGHT = 0.16f;
    private static final float CHEST_GAP = 0.0f;
    private static final float CHEST_GRID_TOP = 0.50f;
    private static final float CHEST_PANEL_PAD_X = 0.065f;
    private static final float CHEST_PANEL_PAD_TOP = 0.13f;
    private static final float CHEST_PANEL_PAD_BOTTOM = 0.10f;

    // 方塊縮圖的面亮度：頂面最亮、正面次之、右側最暗，模擬世界中的方向光，讓縮圖有立體感。
    private static final float ICON_TOP_SHADE = 1.0f;
    private static final float ICON_FRONT_SHADE = 0.82f;
    private static final float ICON_SIDE_SHADE = 0.55f;

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

    // 與世界渲染共用的材質圖集；方塊縮圖直接取樣世界貼圖，讓玩家一眼認出方塊。
    private final TextureAtlas atlas;

    // 純白 tile 中心點的 UV；純色幾何固定取樣這個點，UV 無變化也就不會誤取到 mipmap 的混色。
    private final float whiteU;
    private final float whiteV;

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
    private boolean cachedChestOpen;

    // 快取上一次方塊名稱提示是否顯示。
    private boolean cachedLabelVisible;

    // 快取上一次 F3 偵錯文字的內容簽章。
    private int cachedDebugSignature;

    // 快取上一次 viewport 寬度。
    private int cachedViewportWidth = Integer.MIN_VALUE;

    // 快取上一次 viewport 高度。
    private int cachedViewportHeight = Integer.MIN_VALUE;

    // 建立 HUD 所需的 shader 與基本 mesh；atlas 由 WorldRenderer 持有並負責釋放。
    public HudRenderer(TextureAtlas atlas) {
        this.atlas = atlas;
        whiteU = (atlas.u0(AtlasTiles.WHITE) + atlas.u1(AtlasTiles.WHITE)) * 0.5f;
        whiteV = (atlas.v0(AtlasTiles.WHITE) + atlas.v1(AtlasTiles.WHITE)) * 0.5f;

        shader = new ShaderProgram("/shaders/hud.vert", "/shaders/hud.frag");

        // 準星會依 viewport 比例重建，確保實際畫面上是置中的正十字。
        crosshair = new Mesh(new float[0], GL_TRIANGLES, 3, 4, 2);

        // hotbar mesh 一開始先建立空資料，之後再動態更新。
        hotbarMesh = new Mesh(new float[0], GL_TRIANGLES, 3, 4, 2);
    }

    // 繪製 HUD。debugLines 為 null 時不顯示 F3 偵錯文字。
    public void render(BlockType[] hotbar, int selectedIndex, boolean showHotbarLabel, boolean creativeInventoryOpen,
            BlockType[] creativeBlocks, int creativePage, int creativeTotalPages, boolean chestOpen,
            String[] debugLines) {
        glDisable(GL_DEPTH_TEST);
        shader.use();
        shader.setInt("uAtlas", 0);
        atlas.bind(0);

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

        // 標題在「網格頂到面板內緣」這段區帶內垂直置中。
        float titleY = CHEST_GRID_TOP + (CHEST_PANEL_PAD_TOP - PANEL_BORDER_Y - TEXT_BLOCK_HEIGHT) * 0.5f;
        addCenteredText(out, "Chest", titleY, TITLE_COLOR, false);

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

        // 標題在「網格頂到面板內緣」這段區帶內垂直置中。
        float titleY = CREATIVE_GRID_TOP + (CREATIVE_PANEL_PAD_TOP - PANEL_BORDER_Y - TEXT_BLOCK_HEIGHT) * 0.5f;
        addCenteredText(out, "Creative", titleY, TITLE_COLOR, false);

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

        // 頁碼在「面板內緣到網格底」這段區帶內垂直置中。
        float pageY = panelY + PANEL_BORDER_Y + (CREATIVE_PANEL_PAD_BOTTOM - PANEL_BORDER_Y - TEXT_BLOCK_HEIGHT) * 0.5f;
        addCenteredText(out, pageLabel, pageY, TITLE_COLOR, false);
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

    // 方塊縮圖一律取樣世界材質圖集：完整方塊畫成三面立方體（正面用 NORTH 貼圖，爐口、箱扣等特徵朝向玩家），
    // 非完整方塊（火把、梯子、門等）直接用貼圖本身當平面圖示，和 Minecraft 的物品圖示語彙一致。
    private void addBlockIcon(FloatArrayBuilder out, BlockType type, float x, float y, float width, float height,
            float depthX, float depthY) {
        if (type == null || type == BlockType.AIR) {
            return;
        }

        // 平面圖示不需要立體深度，改用整個縮圖範圍置中顯示。
        float flatWidth = width + depthX;
        float flatHeight = height + depthY;

        switch (type) {
            case OAK_STAIRS, OAK_STAIRS_NORTH, OAK_STAIRS_EAST, OAK_STAIRS_SOUTH, OAK_STAIRS_WEST ->
                addStairsIcon(out, type, x, y, width, height, depthX, depthY);
            case OAK_SLAB, STONE_SLAB -> addSlabIcon(out, type, x, y, width, height, depthX, depthY);
            case OAK_FENCE -> addFenceIcon(out, type, x, y, flatWidth, flatHeight);
            case OAK_DOOR, OAK_DOOR_NORTH_BOTTOM, OAK_DOOR_NORTH_TOP, OAK_DOOR_EAST_BOTTOM, OAK_DOOR_EAST_TOP,
                    OAK_DOOR_SOUTH_BOTTOM, OAK_DOOR_SOUTH_TOP, OAK_DOOR_WEST_BOTTOM, OAK_DOOR_WEST_TOP,
                    OAK_DOOR_NORTH_OPEN_BOTTOM, OAK_DOOR_NORTH_OPEN_TOP, OAK_DOOR_EAST_OPEN_BOTTOM,
                    OAK_DOOR_EAST_OPEN_TOP, OAK_DOOR_SOUTH_OPEN_BOTTOM, OAK_DOOR_SOUTH_OPEN_TOP,
                    OAK_DOOR_WEST_OPEN_BOTTOM, OAK_DOOR_WEST_OPEN_TOP ->
                addDoorIcon(out, x, y, flatWidth, flatHeight);
            case OAK_TRAPDOOR, OAK_TRAPDOOR_OPEN -> addFlatIcon(out, AtlasTiles.OAK_TRAPDOOR, x, y, flatWidth,
                    flatHeight);
            case LADDER, LADDER_NORTH, LADDER_EAST, LADDER_SOUTH, LADDER_WEST ->
                addFlatIcon(out, AtlasTiles.LADDER, x, y, flatWidth, flatHeight);
            case TORCH -> addFlatIcon(out, AtlasTiles.TORCH, x, y, flatWidth, flatHeight);
            default -> addPartialCube(out, type, x, y, width, height, depthX, depthY, 0.0f, 1.0f, 0.0f, 1.0f);
        }
    }

    // 階梯畫成「整寬半磚 + 疊在右半的上階」兩個量塊，由左向右升高；貼圖各取對應子區域維持紋理連續。
    private void addStairsIcon(FloatArrayBuilder out, BlockType type, float x, float y, float width, float height,
            float depthX, float depthY) {
        float halfWidth = width * 0.5f;
        float halfHeight = height * 0.5f;
        addPartialCube(out, type, x, y, width, halfHeight, depthX, depthY, 0.0f, 1.0f, 0.5f, 1.0f);
        addPartialCube(out, type, x + halfWidth, y + halfHeight, halfWidth, halfHeight, depthX, depthY,
                0.5f, 1.0f, 0.0f, 0.5f);
    }

    // 半磚畫成高度減半的立方體，頂面位置明顯低於完整方塊。
    private void addSlabIcon(FloatArrayBuilder out, BlockType type, float x, float y, float width, float height,
            float depthX, float depthY) {
        addPartialCube(out, type, x, y, width, height * 0.5f, depthX, depthY, 0.0f, 1.0f, 0.5f, 1.0f);
    }

    // 柵欄維持「兩柱兩欄」的剪影，但改取木頭貼圖的對應條狀區域上色。
    private void addFenceIcon(FloatArrayBuilder out, BlockType type, float x, float y, float width, float height) {
        int tile = type.tileForFace(Face.NORTH);
        float postWidth = width * 0.16f;
        float railHeight = height * 0.14f;
        addTileSubRect(out, tile, x + width * 0.12f, y, postWidth, height,
                0.12f, 0.0f, 0.28f, 1.0f, ICON_FRONT_SHADE);
        addTileSubRect(out, tile, x + width * 0.72f, y, postWidth, height,
                0.72f, 0.0f, 0.88f, 1.0f, ICON_FRONT_SHADE);
        addTileSubRect(out, tile, x, y + height * 0.64f, width, railHeight,
                0.0f, 0.22f, 1.0f, 0.36f, ICON_TOP_SHADE);
        addTileSubRect(out, tile, x, y + height * 0.30f, width, railHeight,
                0.0f, 0.56f, 1.0f, 0.70f, ICON_TOP_SHADE);
    }

    // 直接把貼圖整格畫成平面圖示；火把、梯子的透明像素會自然露出格子底色。
    private void addFlatIcon(FloatArrayBuilder out, int tile, float x, float y, float width, float height) {
        addTileSubRect(out, tile, x, y, width, height, 0.0f, 0.0f, 1.0f, 1.0f, ICON_TOP_SHADE);
    }

    // 門在世界中佔上下兩格，縮圖用同一組上下半貼圖拼出完整門板；水平壓縮維持門的瘦長輪廓。
    private void addDoorIcon(FloatArrayBuilder out, float x, float y, float width, float height) {
        float doorX = x + width * 0.18f;
        float doorWidth = width * 0.64f;
        float halfHeight = height * 0.5f;
        addFlatIcon(out, AtlasTiles.OAK_DOOR_LOWER, doorX, y, doorWidth, halfHeight);
        addFlatIcon(out, AtlasTiles.OAK_DOOR_UPPER, doorX, y + halfHeight, doorWidth, halfHeight);
    }

    // 畫出一個貼圖立方體量塊：正面、頂面、右側面各自取對應貼圖。
    // uFrac/vFrac 指定正面與右側面取樣的貼圖子區域（v = 0 為貼圖上緣），讓半磚、階梯的紋理比例與世界一致。
    private void addPartialCube(FloatArrayBuilder out, BlockType type, float x, float y, float width, float height,
            float depthX, float depthY, float uFrac0, float uFrac1, float vFrac0, float vFrac1) {
        int frontTile = type.tileForFace(Face.NORTH);
        int topTile = type.tileForFace(Face.UP);
        int sideTile = type.tileForFace(Face.EAST);

        float x1 = x + width;
        float y1 = y + height;

        // 正面。
        addTileSubQuad(out, frontTile, x, y, x1, y, x1, y1, x, y1,
                uFrac0, vFrac0, uFrac1, vFrac1, ICON_FRONT_SHADE);

        // 頂面：近邊取貼圖下緣，往右上（遠處）延伸到貼圖上緣。
        addTileSubQuad(out, topTile, x, y1, x1, y1, x1 + depthX, y1 + depthY, x + depthX, y1 + depthY,
                uFrac0, 0.0f, uFrac1, 1.0f, ICON_TOP_SHADE);

        // 右側面。
        addTileSubQuad(out, sideTile, x1, y, x1 + depthX, y + depthY, x1 + depthX, y1 + depthY, x1, y1,
                0.0f, vFrac0, 1.0f, vFrac1, ICON_SIDE_SHADE);
    }

    // 取樣 tile 子區域畫成矩形；uFrac/vFrac 以貼圖影像座標表示（v = 0 為貼圖上緣）。
    private void addTileSubRect(FloatArrayBuilder out, int tile, float x, float y, float width, float height,
            float uFrac0, float vFrac0, float uFrac1, float vFrac1, float shade) {
        addTileSubQuad(out, tile, x, y, x + width, y, x + width, y + height, x, y + height,
                uFrac0, vFrac0, uFrac1, vFrac1, shade);
    }

    // 把 tile 子區域貼到四邊形上；頂點順序為左下、右下、右上、左上，貼圖上緣對齊四邊形上緣。
    private void addTileSubQuad(FloatArrayBuilder out, int tile,
            float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy,
            float uFrac0, float vFrac0, float uFrac1, float vFrac1, float shade) {
        float u0 = atlas.u0(tile);
        float v0 = atlas.v0(tile);
        float uSpan = atlas.u1(tile) - u0;
        float vSpan = atlas.v1(tile) - v0;
        float uLeft = u0 + uSpan * uFrac0;
        float uRight = u0 + uSpan * uFrac1;
        float vTop = v0 + vSpan * vFrac0;
        float vBottom = v0 + vSpan * vFrac1;

        float z = 0.0f;
        addVertex(out, ax, ay, z, shade, shade, shade, 1.0f, uLeft, vBottom);
        addVertex(out, bx, by, z, shade, shade, shade, 1.0f, uRight, vBottom);
        addVertex(out, cx, cy, z, shade, shade, shade, 1.0f, uRight, vTop);

        addVertex(out, cx, cy, z, shade, shade, shade, 1.0f, uRight, vTop);
        addVertex(out, dx, dy, z, shade, shade, shade, 1.0f, uLeft, vTop);
        addVertex(out, ax, ay, z, shade, shade, shade, 1.0f, uLeft, vBottom);
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

    // 加入一個純色四邊形，內部會拆成兩個三角形；UV 固定取樣純白 tile，輸出即為頂點色。
    private void addQuad(
            FloatArrayBuilder out,
            float ax, float ay,
            float bx, float by,
            float cx, float cy,
            float dx, float dy,
            float r, float g, float b, float a) {
        float z = 0.0f;

        addVertex(out, ax, ay, z, r, g, b, a, whiteU, whiteV);
        addVertex(out, bx, by, z, r, g, b, a, whiteU, whiteV);
        addVertex(out, cx, cy, z, r, g, b, a, whiteU, whiteV);

        addVertex(out, cx, cy, z, r, g, b, a, whiteU, whiteV);
        addVertex(out, dx, dy, z, r, g, b, a, whiteU, whiteV);
        addVertex(out, ax, ay, z, r, g, b, a, whiteU, whiteV);
    }

    // 加入單一頂點資料。
    private void addVertex(FloatArrayBuilder out, float x, float y, float z, float r, float g, float b, float a,
            float u, float v) {
        out.add(x, y, z, r, g, b, a, u, v);
    }

    // 根據 hotbar 內容產生簡單簽章，用來判斷內容是否改變。
    private int hotbarSignature(BlockType[] hotbar) {
        int hash = 1;
        for (BlockType blockType : hotbar) {
            hash = 31 * hash + (blockType != null ? blockType.id() : -1);
        }
        return hash;
    }

}

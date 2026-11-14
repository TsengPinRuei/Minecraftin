package com.minecraftin.clone.render;

import com.minecraftin.clone.engine.Mesh;
import com.minecraftin.clone.engine.ResourceScope;
import com.minecraftin.clone.engine.ShaderProgram;
import com.minecraftin.clone.engine.TextureAtlas;
import com.minecraftin.clone.util.FloatArrayBuilder;
import com.minecraftin.clone.world.BlockType;
import com.minecraftin.clone.world.Face;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33C.*;

// Build HUD geometry directly in normalized device coordinates, without camera matrices.
// Panels, icons, and text share one mesh and atlas; the crosshair is drawn separately.
public final class HudRenderer implements AutoCloseable {

    private static final float TEXT_PIXEL_WIDTH = 0.005f;

    private static final float TEXT_PIXEL_HEIGHT = 0.01f;

    private static final int FONT_GLYPH_WIDTH = 5;

    private static final int FONT_GLYPH_HEIGHT = 7;

    private static final int FONT_GLYPH_SPACING = 1;

    // Text height in normalized device coordinates, used to center titles and page labels.
    private static final float TEXT_BLOCK_HEIGHT = FONT_GLYPH_HEIGHT * TEXT_PIXEL_HEIGHT;

    // Specify slot height in NDC and adjust width by aspect ratio to keep slots square.
    private static final float HOTBAR_SLOT_HEIGHT = 0.15f;

    private static final float HOTBAR_Y = -0.97f;

    private static final float HOTBAR_LABEL_MARGIN = 0.045f;

    public static final int CREATIVE_COLUMNS = 9;
    public static final int CREATIVE_ROWS = 5;
    public static final int CREATIVE_SLOTS_PER_PAGE = CREATIVE_COLUMNS * CREATIVE_ROWS;

    // Inventory and chest layouts use NDC dimensions; convert horizontal sizes by the viewport
    // aspect ratio.
    private static final float CREATIVE_SLOT_HEIGHT = 0.16f;
    private static final float CREATIVE_GAP = 0.0f;
    private static final float CREATIVE_GRID_TOP = 0.62f;
    private static final float CREATIVE_PANEL_PAD_X = 0.065f;
    private static final float CREATIVE_PANEL_PAD_TOP = 0.13f;
    private static final float CREATIVE_PANEL_PAD_BOTTOM = 0.11f;

    // Use light upper/left edges and dark lower/right edges to make panels appear raised.
    private static final float[] UI_BLACK_BORDER = new float[] { 0.0f, 0.0f, 0.0f, 1.0f };
    private static final float[] UI_PANEL_FACE = new float[] { 0.776f, 0.776f, 0.776f, 1.0f };
    private static final float[] UI_PANEL_LIGHT = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
    private static final float[] UI_PANEL_DARK = new float[] { 0.333f, 0.333f, 0.333f, 1.0f };
    private static final float[] UI_SLOT_FACE = new float[] { 0.545f, 0.545f, 0.545f, 1.0f };
    private static final float[] UI_SLOT_DARK = new float[] { 0.216f, 0.216f, 0.216f, 1.0f };
    private static final float[] UI_SLOT_LIGHT = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };

    // Dim the world behind an open panel.
    private static final float[] UI_SCREEN_DIM = new float[] { 0.0f, 0.0f, 0.0f, 0.62f };

    // Use a translucent background with a brighter frame around the selected hotbar slot.
    private static final float[] HOTBAR_BG = new float[] { 0.0f, 0.0f, 0.0f, 0.5f };
    private static final float[] HOTBAR_BORDER = new float[] { 0.55f, 0.55f, 0.55f, 0.75f };
    private static final float[] HOTBAR_SELECTION = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };

    // Highlight the inventory entry matching the selected hotbar block; this is not mouse hover.
    private static final float[] SLOT_HIGHLIGHT = new float[] { 1.0f, 1.0f, 1.0f, 0.45f };

    // Use shadowed light text for overlays and unshadowed dark text for panel titles.
    private static final float[] TEXT_COLOR = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
    private static final float[] TEXT_SHADOW_COLOR = new float[] { 0.25f, 0.25f, 0.25f, 1.0f };
    private static final float[] TITLE_COLOR = new float[] { 0.25f, 0.25f, 0.25f, 1.0f };

    private static final float[] DEBUG_BG_COLOR = new float[] { 0.0f, 0.0f, 0.0f, 0.35f };

    private static final float[] CROSSHAIR_COLOR = new float[] { 0.92f, 0.92f, 0.92f, 0.9f };

    // Border thickness is measured in NDC Y units; divide by aspect ratio for X.
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

    // Bake different face shades into icons to suggest depth without world lighting.
    private static final float ICON_TOP_SHADE = 1.0f;
    private static final float ICON_FRONT_SHADE = 0.82f;
    private static final float ICON_SIDE_SHADE = 0.55f;

    private static final String[] GLYPH_EMPTY = new String[] {
            "00000",
            "00000",
            "00000",
            "00000",
            "00000",
            "00000",
            "00000"
    };

    // Each glyph contains seven top-to-bottom rows of five pixels; '1' pixels are drawn.
    private static final Map<Character, String[]> FONT = createFont();

    private final ShaderProgram shader;

    // Borrow the world atlas so HUD icons match world textures; WorldRenderer owns its lifetime.
    private final TextureAtlas atlas;

    // Sample the center of the white tile for solid-color geometry.
    private final float whiteU;
    private final float whiteV;

    private final Mesh crosshair;

    // Cache panels, icons, hotbar labels, and debug text in one mesh; rebuild when any visible
    // content or viewport changes.
    private final Mesh hotbarMesh;

    private final FloatArrayBuilder hotbarVertices = new FloatArrayBuilder(32768);

    private int cachedSelectedIndex = Integer.MIN_VALUE;

    // Compare content snapshots instead of hashes so collisions cannot hide a HUD update.
    private BlockType[] cachedHotbar;

    private BlockType[] cachedCreativeBlocks;
    private int cachedCreativePage = Integer.MIN_VALUE;
    private int cachedCreativeTotalPages = Integer.MIN_VALUE;
    private boolean cachedCreativeOpen;
    private boolean cachedChestOpen;

    private boolean cachedLabelVisible;

    private String[] cachedDebugLines;

    private int cachedViewportWidth = Integer.MIN_VALUE;

    private int cachedViewportHeight = Integer.MIN_VALUE;

    // The atlas is borrowed from WorldRenderer and must outlive this HUD.
    public HudRenderer(TextureAtlas atlas) {
        this.atlas = atlas;
        whiteU = (TextureAtlas.u0(AtlasTiles.WHITE) + TextureAtlas.u1(AtlasTiles.WHITE)) * 0.5f;
        whiteV = (TextureAtlas.v0(AtlasTiles.WHITE) + TextureAtlas.v1(AtlasTiles.WHITE)) * 0.5f;

        try (ResourceScope resources = new ResourceScope()) {
            shader = resources.add(new ShaderProgram("/shaders/hud.vert", "/shaders/hud.frag"));
            crosshair = resources.add(new Mesh(new float[0], GL_TRIANGLES, 3, 4, 2));
            hotbarMesh = resources.add(new Mesh(new float[0], GL_TRIANGLES, 3, 4, 2));
            resources.release();
        }
    }

    public void render(BlockType[] hotbar, int selectedIndex, boolean showHotbarLabel, boolean creativeInventoryOpen,
            BlockType[] creativeBlocks, int creativePage, int creativeTotalPages, boolean chestOpen,
            String[] debugLines, int viewportWidth, int viewportHeight) {
        glDisable(GL_DEPTH_TEST);
        shader.use();
        shader.setInt("uAtlas", 0);
        atlas.bind(0);

        float viewportAspect = (float) viewportWidth / Math.max(1, viewportHeight);
        boolean viewportChanged = viewportWidth != cachedViewportWidth || viewportHeight != cachedViewportHeight;
        BlockType[] visibleCreativeBlocks = creativeInventoryOpen ? creativeBlocks : null;

        if (viewportChanged) {
            updateCrosshairMesh(viewportAspect);
        }

        // Rebuild when any HUD content, panel state, label visibility, debug text, or viewport
        // changes.
        if (viewportChanged
                || !Arrays.equals(hotbar, cachedHotbar)
                || selectedIndex != cachedSelectedIndex
                || showHotbarLabel != cachedLabelVisible
                || !Arrays.equals(visibleCreativeBlocks, cachedCreativeBlocks)
                || creativePage != cachedCreativePage
                || creativeTotalPages != cachedCreativeTotalPages
                || creativeInventoryOpen != cachedCreativeOpen
                || chestOpen != cachedChestOpen
                || !Arrays.equals(debugLines, cachedDebugLines)) {
            updateHotbarMesh(hotbar, selectedIndex, showHotbarLabel, viewportAspect,
                    creativeInventoryOpen, creativeBlocks, creativePage, creativeTotalPages, chestOpen, debugLines);
            cachedHotbar = hotbar.clone();
            cachedSelectedIndex = selectedIndex;
            cachedLabelVisible = showHotbarLabel;
            cachedCreativeBlocks = visibleCreativeBlocks == null ? null : visibleCreativeBlocks.clone();
            cachedCreativePage = creativePage;
            cachedCreativeTotalPages = creativeTotalPages;
            cachedCreativeOpen = creativeInventoryOpen;
            cachedChestOpen = chestOpen;
            cachedDebugLines = debugLines == null ? null : debugLines.clone();
            cachedViewportWidth = viewportWidth;
            cachedViewportHeight = viewportHeight;
        }

        hotbarMesh.draw();
        if (!creativeInventoryOpen && !chestOpen) {
            crosshair.draw();
        }

        glEnable(GL_DEPTH_TEST);
    }

    @Override
    public void close() {
        // The shared atlas is owned and released by WorldRenderer.
        try (shader; crosshair; hotbarMesh) {
        }
    }

    // Return at least one page so an empty inventory still has stable layout and cache state.
    public static int creativeTotalPages(int blockCount) {
        return blockCount <= 0 ? 1 : 1 + (blockCount - 1) / CREATIVE_SLOTS_PER_PAGE;
    }

    // Convert GLFW logical window coordinates to the HUD's NDC layout.
    // Use logical window dimensions here, not high-DPI framebuffer pixels.
    public static int creativeSlotAt(double mouseX, double mouseY, int windowWidth, int windowHeight, int page,
            int blockCount) {
        if (windowWidth <= 0 || windowHeight <= 0 || blockCount <= 0
                || page < 0 || page >= creativeTotalPages(blockCount)
                || !Double.isFinite(mouseX) || !Double.isFinite(mouseY)) {
            return -1;
        }

        double ndcX = (mouseX / (double) windowWidth) * 2.0 - 1.0;
        double ndcY = 1.0 - (mouseY / (double) windowHeight) * 2.0;

        float aspect = (float) windowWidth / (float) windowHeight;
        GridLayout layout = creativeLayout(aspect);
        float slotHeight = layout.slotHeight();
        float slotWidth = layout.slotWidth();
        float startX = layout.startX();
        float cellWidth = slotWidth + CREATIVE_GAP;
        float cellHeight = slotHeight + CREATIVE_GAP;

        for (int row = 0; row < CREATIVE_ROWS; row++) {
            float slotY = CREATIVE_GRID_TOP - row * cellHeight - slotHeight;
            if (ndcY <= slotY || ndcY > slotY + slotHeight) {
                continue;
            }

            for (int col = 0; col < CREATIVE_COLUMNS; col++) {
                float slotX = startX + col * cellWidth;
                if (ndcX < slotX || ndcX >= slotX + slotWidth) {
                    continue;
                }

                int index = page * CREATIVE_SLOTS_PER_PAGE + row * CREATIVE_COLUMNS + col;
                return index < blockCount ? index : -1;
            }
        }

        return -1;
    }

    // Convert NDC height to width using the aspect ratio so slots appear square.
    private static float squareSlotWidth(float slotHeight, float aspect) {
        return slotHeight / Math.max(0.1f, aspect);
    }

    // Correct horizontal crosshair dimensions by aspect ratio so the cross stays square on wide
    // displays.
    private void updateCrosshairMesh(float viewportAspect) {
        float safeAspect = Math.max(0.1f, viewportAspect);
        float halfLengthY = 0.026f;
        float halfThicknessY = 0.0046f;
        float halfLengthX = halfLengthY / safeAspect;
        float halfThicknessX = halfThicknessY / safeAspect;

        FloatArrayBuilder out = new FloatArrayBuilder(128);
        addRect(out, -halfLengthX, -halfThicknessY, halfLengthX * 2.0f, halfThicknessY * 2.0f, CROSSHAIR_COLOR);
        addRect(out, -halfThicknessX, -halfLengthY, halfThicknessX * 2.0f, halfLengthY * 2.0f, CROSSHAIR_COLOR);
        crosshair.update(out.toArray());
    }

    // Rebuild the combined panel, debug, hotbar, and label mesh from the current HUD state.
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
        int slots = hotbar.length;
        float slotHeight = fittedSlotHeight(HOTBAR_SLOT_HEIGHT, slots, aspect, 0.03f);
        float slotWidth = squareSlotWidth(slotHeight, aspect);
        float totalWidth = slots * slotWidth;
        float startX = -totalWidth * 0.5f;
        float y = HOTBAR_Y;
        float lineY = 0.006f * slotHeight / HOTBAR_SLOT_HEIGHT;
        float lineX = lineY / aspect;

        addRect(hotbarVertices, startX - lineX * 2.0f, y - lineY * 2.0f,
                totalWidth + lineX * 4.0f, slotHeight + lineY * 4.0f, HOTBAR_BORDER);
        addRect(hotbarVertices, startX - lineX, y - lineY,
                totalWidth + lineX * 2.0f, slotHeight + lineY * 2.0f, HOTBAR_BG);

        for (int i = 0; i < slots; i++) {
            float slotX = startX + i * slotWidth;
            addFrame(hotbarVertices, slotX, y, slotWidth, slotHeight, lineX, lineY, HOTBAR_BORDER);
            addHotbarIcon(hotbarVertices, hotbar[i], slotX, y, slotWidth, slotHeight, aspect);
        }

        if (selectedIndex >= 0 && selectedIndex < slots) {
            float selX = startX + selectedIndex * slotWidth;
            addFrame(hotbarVertices, selX - lineX * 2.0f, y - lineY * 2.0f,
                    slotWidth + lineX * 4.0f, slotHeight + lineY * 4.0f,
                    lineX * 2.0f, lineY * 2.0f, HOTBAR_SELECTION);
        }

        // Game controls label visibility and clears it when the selection timer expires.
        if (showHotbarLabel && selectedIndex >= 0 && selectedIndex < hotbar.length) {
            String label = hotbar[selectedIndex].displayName();
            addCenteredText(hotbarVertices, label, HOTBAR_Y + slotHeight + HOTBAR_LABEL_MARGIN,
                    TEXT_COLOR, true);
        }

        hotbarMesh.update(hotbarVertices.toArray());
    }

    private void addHotbarIcon(FloatArrayBuilder out, BlockType block, float x, float y, float slotWidth,
            float slotHeight, float aspect) {
        float cubeHeight = slotHeight * 0.44f;
        float cubeWidth = cubeHeight / Math.max(0.5f, aspect);
        float depthX = cubeWidth * 0.30f;
        float depthY = cubeHeight * 0.22f;
        float cubeX = x + (slotWidth - (cubeWidth + depthX)) * 0.5f;
        float cubeY = y + (slotHeight - (cubeHeight + depthY)) * 0.5f;

        addBlockIcon(out, block, cubeX, cubeY, cubeWidth, cubeHeight, depthX, depthY);
    }

    private void addFrame(FloatArrayBuilder out, float x, float y, float width, float height,
            float thicknessX, float thicknessY, float[] color) {
        addRect(out, x, y + height - thicknessY, width, thicknessY, color);
        addRect(out, x, y, width, thicknessY, color);
        addRect(out, x, y + thicknessY, thicknessX, height - thicknessY * 2.0f, color);
        addRect(out, x + width - thicknessX, y + thicknessY, thicknessX, height - thicknessY * 2.0f, color);
    }

    // Draw empty chest slots only; no container item data or slot interaction is implemented.
    private void addChestPanel(FloatArrayBuilder out, float viewportAspect) {
        float aspect = Math.max(0.1f, viewportAspect);
        float slotHeight = fittedSlotHeight(CHEST_SLOT_HEIGHT, CHEST_COLUMNS, aspect, CHEST_PANEL_PAD_X);
        float slotWidth = squareSlotWidth(slotHeight, aspect);
        float gridWidth = CHEST_COLUMNS * slotWidth + (CHEST_COLUMNS - 1) * CHEST_GAP;
        float gridHeight = CHEST_ROWS * slotHeight + (CHEST_ROWS - 1) * CHEST_GAP;
        float startX = -gridWidth * 0.5f;
        float gridBottom = CHEST_GRID_TOP - gridHeight;
        float panelX = startX - CHEST_PANEL_PAD_X;
        float panelY = gridBottom - CHEST_PANEL_PAD_BOTTOM;
        float panelWidth = gridWidth + CHEST_PANEL_PAD_X * 2.0f;
        float panelHeight = gridHeight + CHEST_PANEL_PAD_TOP + CHEST_PANEL_PAD_BOTTOM;

        addRect(out, -1.0f, -1.0f, 2.0f, 2.0f, UI_SCREEN_DIM);

        addBlockPanel(out, panelX, panelY, panelWidth, panelHeight, aspect);

        // Center the title between the grid top and the panel's inner top edge.
        float titleY = CHEST_GRID_TOP + (CHEST_PANEL_PAD_TOP - PANEL_BORDER_Y - TEXT_BLOCK_HEIGHT) * 0.5f;
        addCenteredText(out, "Chest", titleY, TITLE_COLOR, false);

        for (int row = 0; row < CHEST_ROWS; row++) {
            for (int col = 0; col < CHEST_COLUMNS; col++) {
                float x = startX + col * (slotWidth + CHEST_GAP);
                float y = CHEST_GRID_TOP - row * (slotHeight + CHEST_GAP) - slotHeight;
                addSlot(out, x, y, slotWidth, slotHeight, false, aspect);
            }
        }
    }

    // Derive the inventory highlight from the selected hotbar block instead of storing separate
    // selection state.
    private void addCreativeInventoryPanel(FloatArrayBuilder out, BlockType[] hotbar, int selectedIndex,
            BlockType[] creativeBlocks, int page, int totalPages, float viewportAspect) {
        float aspect = Math.max(0.1f, viewportAspect);
        GridLayout layout = creativeLayout(aspect);
        float slotHeight = layout.slotHeight();
        float slotWidth = layout.slotWidth();
        float gridWidth = layout.width();
        float gridHeight = layout.height();
        float startX = layout.startX();
        float gridBottom = CREATIVE_GRID_TOP - gridHeight;
        float panelX = startX - CREATIVE_PANEL_PAD_X;
        float panelY = gridBottom - CREATIVE_PANEL_PAD_BOTTOM;
        float panelWidth = gridWidth + CREATIVE_PANEL_PAD_X * 2.0f;
        float panelHeight = gridHeight + CREATIVE_PANEL_PAD_TOP + CREATIVE_PANEL_PAD_BOTTOM;

        addRect(out, -1.0f, -1.0f, 2.0f, 2.0f, UI_SCREEN_DIM);

        addBlockPanel(out, panelX, panelY, panelWidth, panelHeight, aspect);

        // Center the title between the grid top and the panel's inner top edge.
        float titleY = CREATIVE_GRID_TOP + (CREATIVE_PANEL_PAD_TOP - PANEL_BORDER_Y - TEXT_BLOCK_HEIGHT) * 0.5f;
        addCenteredText(out, "Creative", titleY, TITLE_COLOR, false);

        BlockType selected = selectedIndex >= 0 && selectedIndex < hotbar.length ? hotbar[selectedIndex] : null;
        float cellWidth = slotWidth + CREATIVE_GAP;
        float cellHeight = slotHeight + CREATIVE_GAP;
        int firstBlock = Math.max(0, page) * CREATIVE_SLOTS_PER_PAGE;

        for (int row = 0; row < CREATIVE_ROWS; row++) {
            for (int col = 0; col < CREATIVE_COLUMNS; col++) {
                int blockIndex = firstBlock + row * CREATIVE_COLUMNS + col;
                float x = startX + col * cellWidth;
                float y = CREATIVE_GRID_TOP - row * cellHeight - slotHeight;
                boolean hasBlock = blockIndex < creativeBlocks.length;
                boolean selectedBlock = hasBlock && creativeBlocks[blockIndex] == selected;

                addSlot(out, x, y, slotWidth, slotHeight, selectedBlock, aspect);

                if (!hasBlock) {
                    continue;
                }

                float cubeHeight = slotHeight * 0.46f;
                float cubeWidth = cubeHeight / Math.max(0.5f, aspect);
                float depthX = cubeWidth * 0.30f;
                float depthY = cubeHeight * 0.22f;
                float cubeX = x + (slotWidth - (cubeWidth + depthX)) * 0.5f;
                float cubeY = y + (slotHeight - (cubeHeight + depthY)) * 0.5f;

                addBlockIcon(out, creativeBlocks[blockIndex], cubeX, cubeY, cubeWidth, cubeHeight, depthX, depthY);
            }
        }

        String pageLabel = "Page " + Math.min(page + 1, Math.max(1, totalPages)) + "/" + Math.max(1, totalPages);

        // Center the page label between the inner panel bottom and the grid bottom.
        float pageY = panelY + PANEL_BORDER_Y + (CREATIVE_PANEL_PAD_BOTTOM - PANEL_BORDER_Y - TEXT_BLOCK_HEIGHT) * 0.5f;
        addCenteredText(out, pageLabel, pageY, TITLE_COLOR, false);
    }

    // The same dimensions drive rendering and logical-window hit testing, including HiDPI.
    static GridLayout creativeLayout(float aspect) {
        float slotHeight = fittedSlotHeight(CREATIVE_SLOT_HEIGHT, CREATIVE_COLUMNS, aspect, CREATIVE_PANEL_PAD_X);
        float slotWidth = squareSlotWidth(slotHeight, aspect);
        return new GridLayout(slotHeight, slotWidth,
                CREATIVE_COLUMNS * slotWidth, CREATIVE_ROWS * slotHeight);
    }

    private static float fittedSlotHeight(float preferred, int columns, float aspect, float paddingX) {
        return Math.min(preferred, (1.9f - 2.0f * paddingX) * Math.max(0.1f, aspect) / Math.max(1, columns));
    }

    record GridLayout(float slotHeight, float slotWidth, float width, float height) {
        float startX() { return -width * 0.5f; }
        float slotX(int column) { return startX() + column * slotWidth; }
        float slotY(int row) { return CREATIVE_GRID_TOP - (row + 1) * slotHeight; }
    }

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

        // Use light top/left edges and dark bottom/right edges for a raised panel.
        addRect(out, faceX, faceY + faceHeight - bevelY, faceWidth, bevelY, UI_PANEL_LIGHT);
        addRect(out, faceX, faceY, bevelX, faceHeight, UI_PANEL_LIGHT);
        addRect(out, faceX + bevelX, faceY, faceWidth - bevelX, bevelY, UI_PANEL_DARK);
        addRect(out, faceX + faceWidth - bevelX, faceY, bevelX, faceHeight - bevelY, UI_PANEL_DARK);
    }

    private void addSlot(FloatArrayBuilder out, float x, float y, float width, float height, boolean selected,
            float aspect) {
        float bevelY = BEVEL_Y * 0.75f;
        float bevelX = bevelY / Math.max(0.1f, aspect);

        addRect(out, x, y, width, height, UI_SLOT_FACE);

        // Reverse the panel shading for slots so their interiors appear recessed.
        addRect(out, x, y + height - bevelY, width, bevelY, UI_SLOT_DARK);
        addRect(out, x, y, bevelX, height - bevelY, UI_SLOT_DARK);
        addRect(out, x + bevelX, y, width - bevelX, bevelY, UI_SLOT_LIGHT);
        addRect(out, x + width - bevelX, y + bevelY, bevelX, height - bevelY * 2.0f, UI_SLOT_LIGHT);

        if (selected) {
            addRect(out, x + bevelX, y + bevelY, width - bevelX * 2.0f, height - bevelY * 2.0f, SLOT_HIGHLIGHT);
        }
    }

    private void addRect(FloatArrayBuilder out, float x, float y, float width, float height, float[] color) {
        float x2 = x + width;
        float y2 = y + height;
        addQuad(out, x, y, x2, y, x2, y2, x, y2, color[0], color[1], color[2], color[3]);
    }

    // Use world atlas textures for all icons, with NORTH facing the viewer on cube icons.
    // Stairs and slabs use partial cubes; fences and doors are assembled shapes; ladders, torches,
    // and trapdoors use flat tiles.
    private void addBlockIcon(FloatArrayBuilder out, BlockType type, float x, float y, float width, float height,
            float depthX, float depthY) {
        if (type == null || type == BlockType.AIR) {
            return;
        }

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

    // Build stairs from a lower slab and a raised right half, cropping the texture to match each
    // part.
    private void addStairsIcon(FloatArrayBuilder out, BlockType type, float x, float y, float width, float height,
            float depthX, float depthY) {
        float halfWidth = width * 0.5f;
        float halfHeight = height * 0.5f;
        addPartialCube(out, type, x, y, width, halfHeight, depthX, depthY, 0.0f, 1.0f, 0.5f, 1.0f);
        addPartialCube(out, type, x + halfWidth, y + halfHeight, halfWidth, halfHeight, depthX, depthY,
                0.5f, 1.0f, 0.0f, 0.5f);
    }

    // Use half-height cube geometry for slabs.
    private void addSlabIcon(FloatArrayBuilder out, BlockType type, float x, float y, float width, float height,
            float depthX, float depthY) {
        addPartialCube(out, type, x, y, width, height * 0.5f, depthX, depthY, 0.0f, 1.0f, 0.5f, 1.0f);
    }

    // Assemble a two-post fence silhouette from narrow regions of the wood tile.
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

    // Use the whole tile for flat icons so transparent torch and ladder pixels reveal the slot
    // background.
    private void addFlatIcon(FloatArrayBuilder out, int tile, float x, float y, float width, float height) {
        addTileSubRect(out, tile, x, y, width, height, 0.0f, 0.0f, 1.0f, 1.0f, ICON_TOP_SHADE);
    }

    // Stack the world door's two tiles and narrow the result to retain a tall door silhouette.
    private void addDoorIcon(FloatArrayBuilder out, float x, float y, float width, float height) {
        float doorX = x + width * 0.18f;
        float doorWidth = width * 0.64f;
        float halfHeight = height * 0.5f;
        addFlatIcon(out, AtlasTiles.OAK_DOOR_LOWER, doorX, y, doorWidth, halfHeight);
        addFlatIcon(out, AtlasTiles.OAK_DOOR_UPPER, doorX, y + halfHeight, doorWidth, halfHeight);
    }

    // Use uFrac to crop the front and top horizontally, and vFrac to crop the front and right
    // vertically.
    // The right face uses the full U range, and the top uses the full V range.
    private void addPartialCube(FloatArrayBuilder out, BlockType type, float x, float y, float width, float height,
            float depthX, float depthY, float uFrac0, float uFrac1, float vFrac0, float vFrac1) {
        int frontTile = type.tileForFace(Face.NORTH);
        int topTile = type.tileForFace(Face.UP);
        int sideTile = type.tileForFace(Face.EAST);

        float x1 = x + width;
        float y1 = y + height;

        addTileSubQuad(out, frontTile, x, y, x1, y, x1, y1, x, y1,
                uFrac0, vFrac0, uFrac1, vFrac1, ICON_FRONT_SHADE);

        // Map the near top edge to the tile bottom and the far edge to the tile top.
        addTileSubQuad(out, topTile, x, y1, x1, y1, x1 + depthX, y1 + depthY, x + depthX, y1 + depthY,
                uFrac0, 0.0f, uFrac1, 1.0f, ICON_TOP_SHADE);

        addTileSubQuad(out, sideTile, x1, y, x1 + depthX, y + depthY, x1 + depthX, y1 + depthY, x1, y1,
                0.0f, vFrac0, 1.0f, vFrac1, ICON_SIDE_SHADE);
    }

    // Texture fractions use image coordinates, with V = 0 at the tile's top.
    private void addTileSubRect(FloatArrayBuilder out, int tile, float x, float y, float width, float height,
            float uFrac0, float vFrac0, float uFrac1, float vFrac1, float shade) {
        addTileSubQuad(out, tile, x, y, x + width, y, x + width, y + height, x, y + height,
                uFrac0, vFrac0, uFrac1, vFrac1, shade);
    }

    // Map corners a/b/c/d to texture bottom-left/bottom-right/top-right/top-left.
    private void addTileSubQuad(FloatArrayBuilder out, int tile,
            float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy,
            float uFrac0, float vFrac0, float uFrac1, float vFrac1, float shade) {
        float u0 = TextureAtlas.u0(tile);
        float v0 = TextureAtlas.v0(tile);
        float uSpan = TextureAtlas.u1(tile) - u0;
        float vSpan = TextureAtlas.v1(tile) - v0;
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

    private void addCenteredText(FloatArrayBuilder out, String text, float y, float[] color, boolean shadow) {
        if (text == null || text.isBlank()) {
            return;
        }
        addText(out, text, -textWidth(text) * 0.5f, y, color, shadow);
    }

    private void addText(FloatArrayBuilder out, String text, float startX, float y, float[] color, boolean shadow) {
        float advance = (FONT_GLYPH_WIDTH + FONT_GLYPH_SPACING) * TEXT_PIXEL_WIDTH;

        // Offset text shadows by one glyph pixel to the right and downward.
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

    private static float textWidth(String text) {
        int charCount = text.length();
        int pixelColumns = charCount * FONT_GLYPH_WIDTH + Math.max(0, charCount - 1) * FONT_GLYPH_SPACING;
        return pixelColumns * TEXT_PIXEL_WIDTH;
    }

    static List<String> wrapDebugLine(String line) {
        if (line == null || line.isBlank()) return List.of("");
        List<String> result = new ArrayList<>();
        while (line.length() > 63) {
            int split = line.lastIndexOf(' ', 63);
            if (split <= 0) split = 63;
            result.add(line.substring(0, split));
            line = line.substring(split).stripLeading();
        }
        result.add(line);
        return result;
    }

    private void addDebugOverlay(FloatArrayBuilder out, String[] lines) {
        float x = -0.985f;
        float lineHeight = FONT_GLYPH_HEIGHT * TEXT_PIXEL_HEIGHT;
        float lineStep = lineHeight + TEXT_PIXEL_HEIGHT * 3.0f;
        float y = 0.96f - lineHeight;

        List<String> wrapped = new ArrayList<>();
        for (String line : lines) wrapped.addAll(wrapDebugLine(line));
        for (String line : wrapped) {
            if (line.isBlank()) {
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

    // Try the exact glyph, then uppercase; unsupported characters occupy blank space.
    private String[] glyphFor(char c) {
        String[] glyph = FONT.get(c);
        if (glyph != null) {
            return glyph;
        }

        glyph = FONT.get(Character.toUpperCase(c));
        return glyph != null ? glyph : GLYPH_EMPTY;
    }

    // Built-in five-by-seven glyphs cover current labels and debug text; glyphFor supplies the
    // fallback.
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

    private static void putGlyph(Map<Character, String[]> font, char c, String... rows) {
        font.put(c, rows);
    }

    // Sample the atlas's white tile so this quad renders with its vertex color.
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

    private void addVertex(FloatArrayBuilder out, float x, float y, float z, float r, float g, float b, float a,
            float u, float v) {
        out.add(x, y, z, r, g, b, a, u, v);
    }

}

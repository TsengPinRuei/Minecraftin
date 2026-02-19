package com.minecraftin.clone.render;

import com.minecraftin.clone.engine.Mesh;
import com.minecraftin.clone.engine.ShaderProgram;
import com.minecraftin.clone.util.FloatArrayBuilder;
import com.minecraftin.clone.world.BlockType;

import static org.lwjgl.opengl.GL33C.*;

public final class HudRenderer implements AutoCloseable {
    private static final int STRIDE = 7; // position xyz + color rgba
    private static final float[] BORDER_COLOR_SELECTED = new float[]{0.95f, 0.95f, 0.95f, 0.95f};
    private static final float[] BORDER_COLOR_NORMAL = new float[]{0.16f, 0.16f, 0.16f, 0.82f};
    private static final float[] SLOT_COLOR_SELECTED = new float[]{0.20f, 0.20f, 0.20f, 0.92f};
    private static final float[] SLOT_COLOR_NORMAL = new float[]{0.10f, 0.10f, 0.10f, 0.75f};

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

    private final ShaderProgram shader;
    private final Mesh crosshair;
    private final Mesh hotbarMesh;
    private final FloatArrayBuilder hotbarVertices = new FloatArrayBuilder(4096);

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

            float iconPad = slotSize * 0.20f;
            float[] iconColor = blockColor(hotbar[i]);
            addRect(hotbarVertices, x + iconPad, y + iconPad, slotSize - iconPad * 2.0f, slotSize - iconPad * 2.0f, iconColor);
        }

        hotbarMesh.update(hotbarVertices.toArray(), STRIDE);
    }

    private void addRect(FloatArrayBuilder out, float x, float y, float width, float height, float[] color) {
        float x2 = x + width;
        float y2 = y + height;
        float z = 0.0f;

        addVertex(out, x, y, z, color);
        addVertex(out, x2, y, z, color);
        addVertex(out, x2, y2, z, color);

        addVertex(out, x2, y2, z, color);
        addVertex(out, x, y2, z, color);
        addVertex(out, x, y, z, color);
    }

    private void addVertex(FloatArrayBuilder out, float x, float y, float z, float[] color) {
        out.add(x, y, z, color[0], color[1], color[2], color[3]);
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

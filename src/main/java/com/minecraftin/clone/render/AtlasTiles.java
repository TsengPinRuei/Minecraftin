package com.minecraftin.clone.render;

// Keep these indices synchronized with TextureAtlas.fillAtlas() when adding or moving textures.
public final class AtlasTiles {

    public static final int GRASS_SIDE = 0;

    public static final int GRASS_TOP = 1;

    public static final int DIRT = 2;

    public static final int STONE = 3;

    public static final int SAND = 4;

    public static final int PLANKS = 5;

    public static final int LOG_SIDE = 6;

    public static final int LOG_TOP = 7;

    public static final int LEAVES = 8;

    public static final int COBBLE = 9;

    public static final int WATER = 10;

    public static final int GLASS = 11;

    public static final int BRICKS = 12;

    public static final int BEDROCK = 13;

    public static final int SNOW = 14;

    public static final int RED_BLOCK = 15;

    public static final int ORANGE_BLOCK = 16;

    public static final int YELLOW_BLOCK = 17;

    public static final int GREEN_BLOCK = 18;

    public static final int BLUE_BLOCK = 19;

    public static final int PURPLE_BLOCK = 20;

    public static final int WHITE_WOOL = 21;
    public static final int LIGHT_GRAY_WOOL = 22;
    public static final int GRAY_WOOL = 23;
    public static final int BLACK_WOOL = 24;
    public static final int BROWN_WOOL = 25;
    public static final int RED_WOOL = 26;
    public static final int ORANGE_WOOL = 27;
    public static final int YELLOW_WOOL = 28;
    public static final int LIME_WOOL = 29;
    public static final int GREEN_WOOL = 30;
    public static final int CYAN_WOOL = 31;
    public static final int BLUE_WOOL = 32;
    public static final int PURPLE_WOOL = 33;
    public static final int MAGENTA_WOOL = 34;
    public static final int PINK_WOOL = 35;

    public static final int BIRCH_PLANKS = 36;
    public static final int SPRUCE_PLANKS = 37;
    public static final int DARK_OAK_PLANKS = 38;

    public static final int STONE_BRICKS = 39;
    public static final int CHISELED_STONE_BRICKS = 40;
    public static final int MOSSY_STONE_BRICKS = 41;
    public static final int GRANITE = 42;
    public static final int POLISHED_GRANITE = 43;
    public static final int DIORITE = 44;
    public static final int POLISHED_DIORITE = 45;
    public static final int ANDESITE = 46;
    public static final int POLISHED_ANDESITE = 47;
    public static final int DEEPSLATE = 48;
    public static final int POLISHED_DEEPSLATE = 49;
    public static final int DEEPSLATE_BRICKS = 50;
    public static final int QUARTZ_BLOCK = 51;
    public static final int QUARTZ_PILLAR_SIDE = 52;
    public static final int QUARTZ_PILLAR_TOP = 53;
    public static final int SMOOTH_QUARTZ = 54;
    public static final int OBSIDIAN = 55;
    public static final int NETHERRACK = 56;
    public static final int NETHER_BRICKS = 57;
    public static final int END_STONE = 58;
    public static final int GLOWSTONE = 59;
    public static final int SEA_LANTERN = 60;

    public static final int OAK_FENCE = 61;

    // Use separate upper and lower door tiles in both world geometry and the combined HUD icon.
    public static final int OAK_DOOR_UPPER = 62;
    public static final int OAK_TRAPDOOR = 63;
    public static final int LADDER = 64;
    public static final int TORCH = 65;
    public static final int CRAFTING_TABLE_SIDE = 66;
    public static final int CRAFTING_TABLE_TOP = 67;
    public static final int FURNACE_SIDE = 68;
    public static final int FURNACE_FRONT = 69;
    public static final int FURNACE_TOP = 70;
    public static final int CHEST_SIDE = 71;
    public static final int CHEST_TOP = 72;
    public static final int BOOKSHELF = 73;
    public static final int OAK_DOOR_LOWER = 74;

    // Reserve the last tile for white so solid HUD geometry can share the atlas shader.
    public static final int WHITE = 255;

    private AtlasTiles() {
    }
}

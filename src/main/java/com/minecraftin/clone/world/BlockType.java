package com.minecraftin.clone.world;

import com.minecraftin.clone.render.AtlasTiles;

import java.util.Locale;

public enum BlockType {
    AIR(0, false, false, 0, 0, 0),
    GRASS(1, true, true, AtlasTiles.GRASS_SIDE, AtlasTiles.GRASS_TOP, AtlasTiles.DIRT),
    DIRT(2, true, true, AtlasTiles.DIRT, AtlasTiles.DIRT, AtlasTiles.DIRT),
    STONE(3, true, true, AtlasTiles.STONE, AtlasTiles.STONE, AtlasTiles.STONE),
    SAND(4, true, true, AtlasTiles.SAND, AtlasTiles.SAND, AtlasTiles.SAND),
    WATER(5, false, false, AtlasTiles.WATER, AtlasTiles.WATER, AtlasTiles.WATER),
    LOG(6, true, true, AtlasTiles.LOG_SIDE, AtlasTiles.LOG_TOP, AtlasTiles.LOG_TOP),
    LEAVES(7, true, false, AtlasTiles.LEAVES, AtlasTiles.LEAVES, AtlasTiles.LEAVES),
    COBBLESTONE(8, true, true, AtlasTiles.COBBLE, AtlasTiles.COBBLE, AtlasTiles.COBBLE),
    PLANKS(9, true, true, AtlasTiles.PLANKS, AtlasTiles.PLANKS, AtlasTiles.PLANKS),
    GLASS(10, true, false, AtlasTiles.GLASS, AtlasTiles.GLASS, AtlasTiles.GLASS),
    BRICKS(11, true, true, AtlasTiles.BRICKS, AtlasTiles.BRICKS, AtlasTiles.BRICKS),
    BEDROCK(12, true, true, AtlasTiles.BEDROCK, AtlasTiles.BEDROCK, AtlasTiles.BEDROCK),
    SNOW(13, true, true, AtlasTiles.SNOW, AtlasTiles.SNOW, AtlasTiles.SNOW),
    RED_BLOCK(14, true, true, AtlasTiles.RED_BLOCK, AtlasTiles.RED_BLOCK, AtlasTiles.RED_BLOCK),
    ORANGE_BLOCK(15, true, true, AtlasTiles.ORANGE_BLOCK, AtlasTiles.ORANGE_BLOCK, AtlasTiles.ORANGE_BLOCK),
    YELLOW_BLOCK(16, true, true, AtlasTiles.YELLOW_BLOCK, AtlasTiles.YELLOW_BLOCK, AtlasTiles.YELLOW_BLOCK),
    GREEN_BLOCK(17, true, true, AtlasTiles.GREEN_BLOCK, AtlasTiles.GREEN_BLOCK, AtlasTiles.GREEN_BLOCK),
    BLUE_BLOCK(18, true, true, AtlasTiles.BLUE_BLOCK, AtlasTiles.BLUE_BLOCK, AtlasTiles.BLUE_BLOCK),
    PURPLE_BLOCK(19, true, true, AtlasTiles.PURPLE_BLOCK, AtlasTiles.PURPLE_BLOCK, AtlasTiles.PURPLE_BLOCK);

    private static final BlockType[] BY_ID;

    static {
        int maxId = 0;
        for (BlockType type : values()) {
            maxId = Math.max(maxId, type.id);
        }
        BY_ID = new BlockType[maxId + 1];
        for (BlockType type : values()) {
            BY_ID[type.id] = type;
        }
    }

    private final int id;
    private final boolean solid;
    private final boolean opaque;
    private final int sideTile;
    private final int topTile;
    private final int bottomTile;

    BlockType(int id, boolean solid, boolean opaque, int sideTile, int topTile, int bottomTile) {
        this.id = id;
        this.solid = solid;
        this.opaque = opaque;
        this.sideTile = sideTile;
        this.topTile = topTile;
        this.bottomTile = bottomTile;
    }

    public int id() {
        return id;
    }

    public boolean isSolid() {
        return solid;
    }

    public boolean isOpaque() {
        return opaque;
    }

    public boolean isTransparent() {
        return !opaque;
    }

    public String displayName() {
        return switch (this) {
            case RED_BLOCK -> "Red";
            case ORANGE_BLOCK -> "Orange";
            case YELLOW_BLOCK -> "Yellow";
            case GREEN_BLOCK -> "Green";
            case BLUE_BLOCK -> "Blue";
            case PURPLE_BLOCK -> "Purple";
            default -> titleCaseFromEnum(name());
        };
    }

    public int tileForFace(Face face) {
        return switch (face) {
            case UP -> topTile;
            case DOWN -> bottomTile;
            default -> sideTile;
        };
    }

    public static BlockType byId(int id) {
        if (id < 0 || id >= BY_ID.length) {
            return AIR;
        }
        BlockType type = BY_ID[id];
        return type != null ? type : AIR;
    }

    private static String titleCaseFromEnum(String enumName) {
        String[] words = enumName.toLowerCase(Locale.ROOT).split("_");
        StringBuilder out = new StringBuilder(enumName.length() + 4);
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                out.append(word, 1, word.length());
            }
        }
        return out.isEmpty() ? enumName : out.toString();
    }
}

package com.minecraftin.clone.world;

import com.minecraftin.clone.render.AtlasTiles;
import java.util.Locale;

// 定義遊戲中的方塊種類，以及每種方塊的碰撞、透明度與貼圖資訊。
// id 會直接寫入世界存檔；新增或調整方塊時不要重排既有 id，否則舊存檔會讀成錯誤方塊。
public enum BlockType {

    // 空氣方塊，不可碰撞，也不遮擋視線。
    AIR(0, false, false, 0, 0, 0),

    // 草地方塊，側面、上面、下面使用不同貼圖。
    GRASS(1, true, true, AtlasTiles.GRASS_SIDE, AtlasTiles.GRASS_TOP, AtlasTiles.DIRT),

    // 泥土方塊，三個面都使用相同貼圖。
    DIRT(2, true, true, AtlasTiles.DIRT, AtlasTiles.DIRT, AtlasTiles.DIRT),

    // 石頭方塊。
    STONE(3, true, true, AtlasTiles.STONE, AtlasTiles.STONE, AtlasTiles.STONE),

    // 沙子方塊。
    SAND(4, true, true, AtlasTiles.SAND, AtlasTiles.SAND, AtlasTiles.SAND),

    // 水方塊，不是實心，也不遮擋視線。
    WATER(5, false, false, AtlasTiles.WATER, AtlasTiles.WATER, AtlasTiles.WATER),

    // 原木方塊，側面與上下表面貼圖不同。
    LOG(6, true, true, AtlasTiles.LOG_SIDE, AtlasTiles.LOG_TOP, AtlasTiles.LOG_TOP),

    // 樹葉方塊，可碰撞，但不完全遮擋視線。
    LEAVES(7, true, false, AtlasTiles.LEAVES, AtlasTiles.LEAVES, AtlasTiles.LEAVES),

    // 鵝卵石方塊。
    COBBLESTONE(8, true, true, AtlasTiles.COBBLE, AtlasTiles.COBBLE, AtlasTiles.COBBLE),

    // 木板方塊。
    PLANKS(9, true, true, AtlasTiles.PLANKS, AtlasTiles.PLANKS, AtlasTiles.PLANKS),

    // 玻璃方塊，可碰撞，但可透視。
    GLASS(10, true, false, AtlasTiles.GLASS, AtlasTiles.GLASS, AtlasTiles.GLASS),

    // 磚塊方塊。
    BRICKS(11, true, true, AtlasTiles.BRICKS, AtlasTiles.BRICKS, AtlasTiles.BRICKS),

    // 基岩方塊。
    BEDROCK(12, true, true, AtlasTiles.BEDROCK, AtlasTiles.BEDROCK, AtlasTiles.BEDROCK),

    // 雪地方塊。
    SNOW(13, true, true, AtlasTiles.SNOW, AtlasTiles.SNOW, AtlasTiles.SNOW),

    // 紅色方塊。
    RED_BLOCK(14, true, true, AtlasTiles.RED_BLOCK, AtlasTiles.RED_BLOCK, AtlasTiles.RED_BLOCK),

    // 橘色方塊。
    ORANGE_BLOCK(15, true, true, AtlasTiles.ORANGE_BLOCK, AtlasTiles.ORANGE_BLOCK, AtlasTiles.ORANGE_BLOCK),

    // 黃色方塊。
    YELLOW_BLOCK(16, true, true, AtlasTiles.YELLOW_BLOCK, AtlasTiles.YELLOW_BLOCK, AtlasTiles.YELLOW_BLOCK),

    // 綠色方塊。
    GREEN_BLOCK(17, true, true, AtlasTiles.GREEN_BLOCK, AtlasTiles.GREEN_BLOCK, AtlasTiles.GREEN_BLOCK),

    // 藍色方塊。
    BLUE_BLOCK(18, true, true, AtlasTiles.BLUE_BLOCK, AtlasTiles.BLUE_BLOCK, AtlasTiles.BLUE_BLOCK),

    // 紫色方塊。
    PURPLE_BLOCK(19, true, true, AtlasTiles.PURPLE_BLOCK, AtlasTiles.PURPLE_BLOCK, AtlasTiles.PURPLE_BLOCK),

    // 羊毛系列，提供創造模式常用的柔和彩色建材。
    WHITE_WOOL(20, true, true, AtlasTiles.WHITE_WOOL, AtlasTiles.WHITE_WOOL, AtlasTiles.WHITE_WOOL),
    LIGHT_GRAY_WOOL(21, true, true, AtlasTiles.LIGHT_GRAY_WOOL, AtlasTiles.LIGHT_GRAY_WOOL, AtlasTiles.LIGHT_GRAY_WOOL),
    GRAY_WOOL(22, true, true, AtlasTiles.GRAY_WOOL, AtlasTiles.GRAY_WOOL, AtlasTiles.GRAY_WOOL),
    BLACK_WOOL(23, true, true, AtlasTiles.BLACK_WOOL, AtlasTiles.BLACK_WOOL, AtlasTiles.BLACK_WOOL),
    BROWN_WOOL(24, true, true, AtlasTiles.BROWN_WOOL, AtlasTiles.BROWN_WOOL, AtlasTiles.BROWN_WOOL),
    RED_WOOL(25, true, true, AtlasTiles.RED_WOOL, AtlasTiles.RED_WOOL, AtlasTiles.RED_WOOL),
    ORANGE_WOOL(26, true, true, AtlasTiles.ORANGE_WOOL, AtlasTiles.ORANGE_WOOL, AtlasTiles.ORANGE_WOOL),
    YELLOW_WOOL(27, true, true, AtlasTiles.YELLOW_WOOL, AtlasTiles.YELLOW_WOOL, AtlasTiles.YELLOW_WOOL),
    LIME_WOOL(28, true, true, AtlasTiles.LIME_WOOL, AtlasTiles.LIME_WOOL, AtlasTiles.LIME_WOOL),
    GREEN_WOOL(29, true, true, AtlasTiles.GREEN_WOOL, AtlasTiles.GREEN_WOOL, AtlasTiles.GREEN_WOOL),
    CYAN_WOOL(30, true, true, AtlasTiles.CYAN_WOOL, AtlasTiles.CYAN_WOOL, AtlasTiles.CYAN_WOOL),
    BLUE_WOOL(31, true, true, AtlasTiles.BLUE_WOOL, AtlasTiles.BLUE_WOOL, AtlasTiles.BLUE_WOOL),
    PURPLE_WOOL(32, true, true, AtlasTiles.PURPLE_WOOL, AtlasTiles.PURPLE_WOOL, AtlasTiles.PURPLE_WOOL),
    MAGENTA_WOOL(33, true, true, AtlasTiles.MAGENTA_WOOL, AtlasTiles.MAGENTA_WOOL, AtlasTiles.MAGENTA_WOOL),
    PINK_WOOL(34, true, true, AtlasTiles.PINK_WOOL, AtlasTiles.PINK_WOOL, AtlasTiles.PINK_WOOL),

    // 木材系列。
    BIRCH_PLANKS(35, true, true, AtlasTiles.BIRCH_PLANKS, AtlasTiles.BIRCH_PLANKS, AtlasTiles.BIRCH_PLANKS),
    SPRUCE_PLANKS(36, true, true, AtlasTiles.SPRUCE_PLANKS, AtlasTiles.SPRUCE_PLANKS, AtlasTiles.SPRUCE_PLANKS),
    DARK_OAK_PLANKS(37, true, true, AtlasTiles.DARK_OAK_PLANKS, AtlasTiles.DARK_OAK_PLANKS, AtlasTiles.DARK_OAK_PLANKS),

    // 石材與建築材料。
    STONE_BRICKS(38, true, true, AtlasTiles.STONE_BRICKS, AtlasTiles.STONE_BRICKS, AtlasTiles.STONE_BRICKS),
    CHISELED_STONE_BRICKS(39, true, true, AtlasTiles.CHISELED_STONE_BRICKS, AtlasTiles.CHISELED_STONE_BRICKS,
            AtlasTiles.CHISELED_STONE_BRICKS),
    MOSSY_STONE_BRICKS(40, true, true, AtlasTiles.MOSSY_STONE_BRICKS, AtlasTiles.MOSSY_STONE_BRICKS,
            AtlasTiles.MOSSY_STONE_BRICKS),
    GRANITE(41, true, true, AtlasTiles.GRANITE, AtlasTiles.GRANITE, AtlasTiles.GRANITE),
    POLISHED_GRANITE(42, true, true, AtlasTiles.POLISHED_GRANITE, AtlasTiles.POLISHED_GRANITE,
            AtlasTiles.POLISHED_GRANITE),
    DIORITE(43, true, true, AtlasTiles.DIORITE, AtlasTiles.DIORITE, AtlasTiles.DIORITE),
    POLISHED_DIORITE(44, true, true, AtlasTiles.POLISHED_DIORITE, AtlasTiles.POLISHED_DIORITE,
            AtlasTiles.POLISHED_DIORITE),
    ANDESITE(45, true, true, AtlasTiles.ANDESITE, AtlasTiles.ANDESITE, AtlasTiles.ANDESITE),
    POLISHED_ANDESITE(46, true, true, AtlasTiles.POLISHED_ANDESITE, AtlasTiles.POLISHED_ANDESITE,
            AtlasTiles.POLISHED_ANDESITE),
    DEEPSLATE(47, true, true, AtlasTiles.DEEPSLATE, AtlasTiles.DEEPSLATE, AtlasTiles.DEEPSLATE),
    POLISHED_DEEPSLATE(48, true, true, AtlasTiles.POLISHED_DEEPSLATE, AtlasTiles.POLISHED_DEEPSLATE,
            AtlasTiles.POLISHED_DEEPSLATE),
    DEEPSLATE_BRICKS(49, true, true, AtlasTiles.DEEPSLATE_BRICKS, AtlasTiles.DEEPSLATE_BRICKS,
            AtlasTiles.DEEPSLATE_BRICKS),
    QUARTZ_BLOCK(50, true, true, AtlasTiles.QUARTZ_BLOCK, AtlasTiles.QUARTZ_BLOCK, AtlasTiles.QUARTZ_BLOCK),
    QUARTZ_PILLAR(51, true, true, AtlasTiles.QUARTZ_PILLAR_SIDE, AtlasTiles.QUARTZ_PILLAR_TOP,
            AtlasTiles.QUARTZ_PILLAR_TOP),
    SMOOTH_QUARTZ(52, true, true, AtlasTiles.SMOOTH_QUARTZ, AtlasTiles.SMOOTH_QUARTZ, AtlasTiles.SMOOTH_QUARTZ),
    OBSIDIAN(53, true, true, AtlasTiles.OBSIDIAN, AtlasTiles.OBSIDIAN, AtlasTiles.OBSIDIAN),
    NETHERRACK(54, true, true, AtlasTiles.NETHERRACK, AtlasTiles.NETHERRACK, AtlasTiles.NETHERRACK),
    NETHER_BRICKS(55, true, true, AtlasTiles.NETHER_BRICKS, AtlasTiles.NETHER_BRICKS, AtlasTiles.NETHER_BRICKS),
    END_STONE(56, true, true, AtlasTiles.END_STONE, AtlasTiles.END_STONE, AtlasTiles.END_STONE),
    GLOWSTONE(57, true, true, AtlasTiles.GLOWSTONE, AtlasTiles.GLOWSTONE, AtlasTiles.GLOWSTONE),
    SEA_LANTERN(58, true, true, AtlasTiles.SEA_LANTERN, AtlasTiles.SEA_LANTERN, AtlasTiles.SEA_LANTERN);

    // 依照 id 快速查詢方塊種類的陣列，用在存檔與 Chunk 原始資料轉回 enum。
    private static final BlockType[] BY_ID;

    private static final BlockType[] CREATIVE_PALETTE = {
            GRASS, DIRT, STONE, COBBLESTONE, BEDROCK,
            SAND, SNOW, WATER, GLASS,
            LOG, PLANKS, BIRCH_PLANKS, SPRUCE_PLANKS, DARK_OAK_PLANKS, LEAVES,
            BRICKS, STONE_BRICKS, CHISELED_STONE_BRICKS, MOSSY_STONE_BRICKS,
            GRANITE, POLISHED_GRANITE, DIORITE, POLISHED_DIORITE, ANDESITE, POLISHED_ANDESITE,
            DEEPSLATE, POLISHED_DEEPSLATE, DEEPSLATE_BRICKS,
            QUARTZ_BLOCK, QUARTZ_PILLAR, SMOOTH_QUARTZ, OBSIDIAN,
            NETHERRACK, NETHER_BRICKS, END_STONE, GLOWSTONE, SEA_LANTERN,
            WHITE_WOOL, LIGHT_GRAY_WOOL, GRAY_WOOL, BLACK_WOOL, BROWN_WOOL,
            RED_WOOL, ORANGE_WOOL, YELLOW_WOOL, LIME_WOOL, GREEN_WOOL, CYAN_WOOL,
            BLUE_WOOL, PURPLE_WOOL, MAGENTA_WOOL, PINK_WOOL,
            RED_BLOCK, ORANGE_BLOCK, YELLOW_BLOCK, GREEN_BLOCK, BLUE_BLOCK, PURPLE_BLOCK
    };

    static {
        int maxId = 0;

        // 找出目前所有方塊中最大的 id。
        for (BlockType type : values()) {
            maxId = Math.max(maxId, type.id);
        }

        // 建立可用 id 直接索引的查表陣列。
        BY_ID = new BlockType[maxId + 1];

        // 將每個方塊放到對應 id 的位置。
        for (BlockType type : values()) {
            BY_ID[type.id] = type;
        }
    }

    // 方塊的數字編號。
    private final int id;

    // 是否為實心方塊，會影響碰撞判定。
    private final boolean solid;

    // 是否為不透明方塊，會影響遮擋與渲染判定。
    private final boolean opaque;

    // 側面的貼圖編號。
    private final int sideTile;

    // 上面的貼圖編號。
    private final int topTile;

    // 下面的貼圖編號。
    private final int bottomTile;

    // 建立一種方塊時，設定它的基本屬性與貼圖。
    BlockType(int id, boolean solid, boolean opaque, int sideTile, int topTile, int bottomTile) {
        this.id = id;
        this.solid = solid;
        this.opaque = opaque;
        this.sideTile = sideTile;
        this.topTile = topTile;
        this.bottomTile = bottomTile;
    }

    // 回傳方塊的 id。
    public int id() {
        return id;
    }

    // 回傳方塊是否為實心。
    public boolean isSolid() {
        return solid;
    }

    // 回傳方塊是否為不透明。
    public boolean isOpaque() {
        return opaque;
    }

    // 回傳方塊是否為透明。
    public boolean isTransparent() {
        return !opaque;
    }

    // 回傳顯示給玩家看的名稱。
    public String displayName() {
        return switch (this) {
            case RED_BLOCK -> "Red";
            case ORANGE_BLOCK -> "Orange";
            case YELLOW_BLOCK -> "Yellow";
            case GREEN_BLOCK -> "Green";
            case BLUE_BLOCK -> "Blue";
            case PURPLE_BLOCK -> "Purple";

            // 其他方塊名稱會由列舉名稱自動轉成較好讀的格式。
            default -> titleCaseFromEnum(name());
        };
    }

    // 根據方塊的面向，回傳該面要使用的貼圖編號。
    public int tileForFace(Face face) {
        return switch (face) {
            case UP -> topTile;
            case DOWN -> bottomTile;
            default -> sideTile;
        };
    }

    // 依照 id 取得方塊種類。
    // 如果 id 超出範圍或找不到對應方塊，預設回傳 AIR。
    public static BlockType byId(int id) {
        if (id < 0 || id >= BY_ID.length) {
            return AIR;
        }

        BlockType type = BY_ID[id];
        return type != null ? type : AIR;
    }

    // 回傳創造模式背包中可選用的方塊。回傳副本避免呼叫端改到全域順序。
    public static BlockType[] creativePalette() {
        return CREATIVE_PALETTE.clone();
    }

    // 將列舉名稱轉成較易讀的文字。
    // 例如 RED_BLOCK 會變成 Red Block。
    private static String titleCaseFromEnum(String enumName) {
        String[] words = enumName.toLowerCase(Locale.ROOT).split("_");
        StringBuilder out = new StringBuilder(enumName.length() + 4);

        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }

            // 如果前面已經有文字，先補一個空白。
            if (!out.isEmpty()) {
                out.append(' ');
            }

            // 把每個單字的第一個字母改成大寫。
            out.append(Character.toUpperCase(word.charAt(0)));

            // 如果單字長度大於 1，接上剩下的字元。
            if (word.length() > 1) {
                out.append(word, 1, word.length());
            }
        }

        // 如果轉換後沒有內容，就回傳原本的列舉名稱。
        return out.isEmpty() ? enumName : out.toString();
    }
}

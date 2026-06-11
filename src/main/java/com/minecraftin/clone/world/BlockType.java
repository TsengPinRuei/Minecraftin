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
    SEA_LANTERN(58, true, true, AtlasTiles.SEA_LANTERN, AtlasTiles.SEA_LANTERN, AtlasTiles.SEA_LANTERN),

    // 創造模式常用基本物品與非完整方塊。Base 版本出現在背包；方向版本由放置流程寫入世界。
    OAK_STAIRS(59, true, true, AtlasTiles.PLANKS, AtlasTiles.PLANKS, AtlasTiles.PLANKS),
    OAK_STAIRS_NORTH(60, true, true, AtlasTiles.PLANKS, AtlasTiles.PLANKS, AtlasTiles.PLANKS),
    OAK_STAIRS_EAST(61, true, true, AtlasTiles.PLANKS, AtlasTiles.PLANKS, AtlasTiles.PLANKS),
    OAK_STAIRS_SOUTH(62, true, true, AtlasTiles.PLANKS, AtlasTiles.PLANKS, AtlasTiles.PLANKS),
    OAK_STAIRS_WEST(63, true, true, AtlasTiles.PLANKS, AtlasTiles.PLANKS, AtlasTiles.PLANKS),
    OAK_SLAB(64, true, true, AtlasTiles.PLANKS, AtlasTiles.PLANKS, AtlasTiles.PLANKS),
    STONE_SLAB(65, true, true, AtlasTiles.STONE, AtlasTiles.STONE, AtlasTiles.STONE),
    OAK_FENCE(66, true, true, AtlasTiles.OAK_FENCE, AtlasTiles.OAK_FENCE, AtlasTiles.OAK_FENCE),
    OAK_DOOR(67, true, false, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR),
    OAK_DOOR_NORTH_BOTTOM(68, true, false, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR),
    OAK_DOOR_NORTH_TOP(69, true, false, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR),
    OAK_DOOR_EAST_BOTTOM(70, true, false, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR),
    OAK_DOOR_EAST_TOP(71, true, false, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR),
    OAK_DOOR_SOUTH_BOTTOM(72, true, false, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR),
    OAK_DOOR_SOUTH_TOP(73, true, false, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR),
    OAK_DOOR_WEST_BOTTOM(74, true, false, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR),
    OAK_DOOR_WEST_TOP(75, true, false, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR),
    OAK_TRAPDOOR(76, true, false, AtlasTiles.OAK_TRAPDOOR, AtlasTiles.OAK_TRAPDOOR, AtlasTiles.OAK_TRAPDOOR),
    LADDER(77, false, false, AtlasTiles.LADDER, AtlasTiles.LADDER, AtlasTiles.LADDER),
    LADDER_NORTH(78, false, false, AtlasTiles.LADDER, AtlasTiles.LADDER, AtlasTiles.LADDER),
    LADDER_EAST(79, false, false, AtlasTiles.LADDER, AtlasTiles.LADDER, AtlasTiles.LADDER),
    LADDER_SOUTH(80, false, false, AtlasTiles.LADDER, AtlasTiles.LADDER, AtlasTiles.LADDER),
    LADDER_WEST(81, false, false, AtlasTiles.LADDER, AtlasTiles.LADDER, AtlasTiles.LADDER),
    TORCH(82, false, false, AtlasTiles.TORCH, AtlasTiles.TORCH, AtlasTiles.TORCH),
    CRAFTING_TABLE(83, true, true, AtlasTiles.CRAFTING_TABLE_SIDE, AtlasTiles.CRAFTING_TABLE_TOP,
            AtlasTiles.PLANKS),
    FURNACE(84, true, true, AtlasTiles.FURNACE_SIDE, AtlasTiles.FURNACE_TOP, AtlasTiles.FURNACE_TOP),
    CHEST(85, true, true, AtlasTiles.CHEST_SIDE, AtlasTiles.CHEST_TOP, AtlasTiles.CHEST_TOP),
    BOOKSHELF(86, true, true, AtlasTiles.BOOKSHELF, AtlasTiles.PLANKS, AtlasTiles.PLANKS),

    // 開啟狀態追加在既有 id 後方，避免舊存檔中的方塊編號被重排。
    OAK_DOOR_NORTH_OPEN_BOTTOM(87, true, false, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR),
    OAK_DOOR_NORTH_OPEN_TOP(88, true, false, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR),
    OAK_DOOR_EAST_OPEN_BOTTOM(89, true, false, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR),
    OAK_DOOR_EAST_OPEN_TOP(90, true, false, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR),
    OAK_DOOR_SOUTH_OPEN_BOTTOM(91, true, false, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR),
    OAK_DOOR_SOUTH_OPEN_TOP(92, true, false, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR),
    OAK_DOOR_WEST_OPEN_BOTTOM(93, true, false, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR),
    OAK_DOOR_WEST_OPEN_TOP(94, true, false, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR, AtlasTiles.OAK_DOOR),
    OAK_TRAPDOOR_OPEN(95, true, false, AtlasTiles.OAK_TRAPDOOR, AtlasTiles.OAK_TRAPDOOR, AtlasTiles.OAK_TRAPDOOR),

    // 流動水，強度 7（緊鄰水源）遞減到 1（最遠端）。只由水流模擬寫入世界，不出現在背包；
    // WATER 本身代表水源，失去支撐的流動水會逐步退去。
    WATER_FLOW_7(96, false, false, AtlasTiles.WATER, AtlasTiles.WATER, AtlasTiles.WATER),
    WATER_FLOW_6(97, false, false, AtlasTiles.WATER, AtlasTiles.WATER, AtlasTiles.WATER),
    WATER_FLOW_5(98, false, false, AtlasTiles.WATER, AtlasTiles.WATER, AtlasTiles.WATER),
    WATER_FLOW_4(99, false, false, AtlasTiles.WATER, AtlasTiles.WATER, AtlasTiles.WATER),
    WATER_FLOW_3(100, false, false, AtlasTiles.WATER, AtlasTiles.WATER, AtlasTiles.WATER),
    WATER_FLOW_2(101, false, false, AtlasTiles.WATER, AtlasTiles.WATER, AtlasTiles.WATER),
    WATER_FLOW_1(102, false, false, AtlasTiles.WATER, AtlasTiles.WATER, AtlasTiles.WATER);

    // 依照 id 快速查詢方塊種類的陣列，用在存檔與 Chunk 原始資料轉回 enum。
    private static final BlockType[] BY_ID;

    // 每種方塊顯示名稱的快取，避免 HUD 或視窗標題更新時重複拆字串與轉大小寫。
    private static final String[] DISPLAY_NAMES;

    // 每種方塊是否為完整方塊的快取；ChunkMesher 會在熱路徑中頻繁查詢。
    private static final boolean[] FULL_CUBE_FLAGS;

    // 互動與碰撞常用分類快取，避免反覆跑大型 enum switch。
    private static final boolean[] DOOR_FLAGS;
    private static final boolean[] DOOR_TOP_FLAGS;
    private static final boolean[] DOOR_OPEN_FLAGS;
    private static final boolean[] TRAPDOOR_FLAGS;
    private static final boolean[] LADDER_FLAGS;

    // 每種方塊的發光強度與額外光衰減快取；光照 BFS 在熱路徑中頻繁查詢。
    private static final int[] LIGHT_EMISSION;
    private static final int[] LIGHT_OPACITY;

    // 碰撞與渲染盒快取；玩家碰撞和 ChunkMesher 都會高頻查詢。
    private static final BlockBounds[][] COLLISION_BOXES;
    private static final BlockBounds[][] RENDER_BOXES;

    // 以下碰撞/渲染盒都使用方塊局部座標，讓 Player、ChunkMesher 與互動判定共用同一份形狀定義。
    private static final BlockBounds[] EMPTY_BOUNDS = new BlockBounds[0];
    private static final BlockBounds[] FULL_BOUNDS = { new BlockBounds(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f) };
    private static final BlockBounds[] NORTH_THIN = { new BlockBounds(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.1875f) };
    private static final BlockBounds[] SOUTH_THIN = { new BlockBounds(0.0f, 0.0f, 0.8125f, 1.0f, 1.0f, 1.0f) };
    private static final BlockBounds[] WEST_THIN = { new BlockBounds(0.0f, 0.0f, 0.0f, 0.1875f, 1.0f, 1.0f) };
    private static final BlockBounds[] EAST_THIN = { new BlockBounds(0.8125f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f) };
    private static final BlockBounds[] NORTH_LADDER = { new BlockBounds(0.0625f, 0.0f, 0.0f, 0.9375f, 1.0f, 0.0625f) };
    private static final BlockBounds[] SOUTH_LADDER = { new BlockBounds(0.0625f, 0.0f, 0.9375f, 0.9375f, 1.0f, 1.0f) };
    private static final BlockBounds[] WEST_LADDER = { new BlockBounds(0.0f, 0.0f, 0.0625f, 0.0625f, 1.0f, 0.9375f) };
    private static final BlockBounds[] EAST_LADDER = { new BlockBounds(0.9375f, 0.0f, 0.0625f, 1.0f, 1.0f, 0.9375f) };
    private static final BlockBounds[] OAK_FENCE_BOUNDS = {
            new BlockBounds(0.375f, 0.0f, 0.375f, 0.625f, 1.5f, 0.625f),
            new BlockBounds(0.0f, 0.5625f, 0.4375f, 1.0f, 0.9375f, 0.5625f),
            new BlockBounds(0.4375f, 0.5625f, 0.0f, 0.5625f, 0.9375f, 1.0f)
    };
    private static final BlockBounds[] OAK_SLAB_BOUNDS = { new BlockBounds(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f) };
    private static final BlockBounds[] OAK_TRAPDOOR_BOUNDS = { new BlockBounds(0.0f, 0.0f, 0.0f, 1.0f, 0.1875f, 1.0f) };
    private static final BlockBounds[] OAK_TRAPDOOR_OPEN_BOUNDS = { new BlockBounds(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.1875f) };
    private static final BlockBounds[] TORCH_BOUNDS = { new BlockBounds(0.40625f, 0.0f, 0.40625f, 0.59375f, 0.625f, 0.59375f) };
    private static final BlockBounds[] STAIRS_NORTH_BOUNDS = {
            new BlockBounds(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f),
            new BlockBounds(0.0f, 0.5f, 0.0f, 1.0f, 1.0f, 0.5f)
    };
    private static final BlockBounds[] STAIRS_SOUTH_BOUNDS = {
            new BlockBounds(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f),
            new BlockBounds(0.0f, 0.5f, 0.5f, 1.0f, 1.0f, 1.0f)
    };
    private static final BlockBounds[] STAIRS_WEST_BOUNDS = {
            new BlockBounds(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f),
            new BlockBounds(0.0f, 0.5f, 0.0f, 0.5f, 1.0f, 1.0f)
    };
    private static final BlockBounds[] STAIRS_EAST_BOUNDS = {
            new BlockBounds(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f),
            new BlockBounds(0.5f, 0.5f, 0.0f, 1.0f, 1.0f, 1.0f)
    };

    // 創造模式背包只放「玩家可直接選取」的基礎方塊；方向變體由放置流程依視角或命中面轉換。
    private static final BlockType[] CREATIVE_PALETTE = {
            GRASS, DIRT, STONE, COBBLESTONE, SAND, SNOW, WATER, GLASS, BEDROCK,
            LOG, PLANKS, BIRCH_PLANKS, SPRUCE_PLANKS, DARK_OAK_PLANKS, LEAVES, OAK_STAIRS, OAK_SLAB, STONE_SLAB,
            OAK_FENCE, OAK_DOOR, OAK_TRAPDOOR, LADDER, TORCH, CRAFTING_TABLE, FURNACE, CHEST, BOOKSHELF,
            BRICKS, STONE_BRICKS, CHISELED_STONE_BRICKS, MOSSY_STONE_BRICKS,
            GRANITE, POLISHED_GRANITE, DIORITE, POLISHED_DIORITE, ANDESITE, POLISHED_ANDESITE,
            DEEPSLATE, POLISHED_DEEPSLATE, DEEPSLATE_BRICKS,
            QUARTZ_BLOCK, QUARTZ_PILLAR, SMOOTH_QUARTZ, OBSIDIAN, GLOWSTONE,
            SEA_LANTERN, NETHERRACK, NETHER_BRICKS, END_STONE,
            WHITE_WOOL, LIGHT_GRAY_WOOL, GRAY_WOOL, BLACK_WOOL, BROWN_WOOL,
            RED_WOOL, ORANGE_WOOL, YELLOW_WOOL, LIME_WOOL, GREEN_WOOL, CYAN_WOOL,
            BLUE_WOOL, PURPLE_WOOL, MAGENTA_WOOL, PINK_WOOL,
            RED_BLOCK, ORANGE_BLOCK, YELLOW_BLOCK, GREEN_BLOCK, BLUE_BLOCK, PURPLE_BLOCK
    };

    static {
        int maxId = 0;
        BlockType[] types = values();

        // 找出目前所有方塊中最大的 id。
        for (BlockType type : types) {
            maxId = Math.max(maxId, type.id);
        }

        // 建立可用 id 直接索引的查表陣列。
        BY_ID = new BlockType[maxId + 1];

        // 將每個方塊放到對應 id 的位置。
        for (BlockType type : types) {
            BY_ID[type.id] = type;
        }

        DISPLAY_NAMES = new String[types.length];
        for (BlockType type : types) {
            DISPLAY_NAMES[type.ordinal()] = type.computeDisplayName();
        }

        FULL_CUBE_FLAGS = new boolean[types.length];
        for (BlockType type : types) {
            FULL_CUBE_FLAGS[type.ordinal()] = type.computeIsFullCube();
        }

        DOOR_FLAGS = new boolean[types.length];
        DOOR_TOP_FLAGS = new boolean[types.length];
        DOOR_OPEN_FLAGS = new boolean[types.length];
        TRAPDOOR_FLAGS = new boolean[types.length];
        LADDER_FLAGS = new boolean[types.length];
        for (BlockType type : types) {
            DOOR_FLAGS[type.ordinal()] = type.computeIsDoorBlock();
            DOOR_TOP_FLAGS[type.ordinal()] = type.computeIsDoorTop();
            DOOR_OPEN_FLAGS[type.ordinal()] = type.computeIsDoorOpen();
            TRAPDOOR_FLAGS[type.ordinal()] = type.computeIsTrapdoorBlock();
            LADDER_FLAGS[type.ordinal()] = type.computeIsLadderBlock();
        }

        LIGHT_EMISSION = new int[types.length];
        LIGHT_OPACITY = new int[types.length];
        for (BlockType type : types) {
            LIGHT_EMISSION[type.ordinal()] = type.computeLightEmission();
            LIGHT_OPACITY[type.ordinal()] = type.computeLightOpacity();
        }

        RENDER_BOXES = new BlockBounds[types.length][];
        COLLISION_BOXES = new BlockBounds[types.length][];
        for (BlockType type : types) {
            RENDER_BOXES[type.ordinal()] = type.computeRenderBoxes();
            COLLISION_BOXES[type.ordinal()] = type.computeCollisionBoxes();
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

    // 需要以半透明 pass 繪製的方塊；其他非不透明方塊多半是形狀或 cutout，而不是玻璃/水那種混色透明。
    public boolean isTranslucent() {
        return isWaterBlock() || this == GLASS;
    }

    // 是否為水（水源或任一強度的流動水）。
    public boolean isWaterBlock() {
        return this == WATER || isFlowingWater();
    }

    // 是否為流動水。
    public boolean isFlowingWater() {
        return switch (this) {
            case WATER_FLOW_7, WATER_FLOW_6, WATER_FLOW_5, WATER_FLOW_4,
                    WATER_FLOW_3, WATER_FLOW_2, WATER_FLOW_1 -> true;
            default -> false;
        };
    }

    // 水的流動強度：水源 8、流動水 7 到 1、非水 0。水流模擬以此決定衰減與覆蓋。
    public int waterStrength() {
        return switch (this) {
            case WATER -> 8;
            case WATER_FLOW_7 -> 7;
            case WATER_FLOW_6 -> 6;
            case WATER_FLOW_5 -> 5;
            case WATER_FLOW_4 -> 4;
            case WATER_FLOW_3 -> 3;
            case WATER_FLOW_2 -> 2;
            case WATER_FLOW_1 -> 1;
            default -> 0;
        };
    }

    // 依強度取得對應的流動水方塊；8 以上回傳水源。
    public static BlockType flowingWaterOfStrength(int strength) {
        return switch (strength) {
            case 7 -> WATER_FLOW_7;
            case 6 -> WATER_FLOW_6;
            case 5 -> WATER_FLOW_5;
            case 4 -> WATER_FLOW_4;
            case 3 -> WATER_FLOW_3;
            case 2 -> WATER_FLOW_2;
            case 1 -> WATER_FLOW_1;
            default -> WATER;
        };
    }

    // 回傳顯示給玩家看的名稱。
    public String displayName() {
        return DISPLAY_NAMES[ordinal()];
    }

    // 建立顯示名稱；只在 enum 靜態初始化時呼叫一次，名稱盡量對齊 Minecraft 的英文方塊名。
    private String computeDisplayName() {
        return switch (this) {
            case GRASS -> "Grass Block";
            case LOG -> "Oak Log";
            case PLANKS -> "Oak Planks";
            case LEAVES -> "Oak Leaves";
            case SNOW -> "Snow Block";
            case RED_BLOCK -> "Red Concrete";
            case ORANGE_BLOCK -> "Orange Concrete";
            case YELLOW_BLOCK -> "Yellow Concrete";
            case GREEN_BLOCK -> "Green Concrete";
            case BLUE_BLOCK -> "Blue Concrete";
            case PURPLE_BLOCK -> "Purple Concrete";
            case OAK_STAIRS, OAK_STAIRS_NORTH, OAK_STAIRS_EAST, OAK_STAIRS_SOUTH, OAK_STAIRS_WEST -> "Oak Stairs";
            case OAK_DOOR, OAK_DOOR_NORTH_BOTTOM, OAK_DOOR_NORTH_TOP, OAK_DOOR_EAST_BOTTOM, OAK_DOOR_EAST_TOP,
                    OAK_DOOR_SOUTH_BOTTOM, OAK_DOOR_SOUTH_TOP, OAK_DOOR_WEST_BOTTOM, OAK_DOOR_WEST_TOP,
                    OAK_DOOR_NORTH_OPEN_BOTTOM, OAK_DOOR_NORTH_OPEN_TOP, OAK_DOOR_EAST_OPEN_BOTTOM,
                    OAK_DOOR_EAST_OPEN_TOP, OAK_DOOR_SOUTH_OPEN_BOTTOM, OAK_DOOR_SOUTH_OPEN_TOP,
                    OAK_DOOR_WEST_OPEN_BOTTOM, OAK_DOOR_WEST_OPEN_TOP -> "Oak Door";
            case OAK_TRAPDOOR, OAK_TRAPDOOR_OPEN -> "Oak Trapdoor";
            case LADDER, LADDER_NORTH, LADDER_EAST, LADDER_SOUTH, LADDER_WEST -> "Ladder";
            case WATER_FLOW_7, WATER_FLOW_6, WATER_FLOW_5, WATER_FLOW_4,
                    WATER_FLOW_3, WATER_FLOW_2, WATER_FLOW_1 -> "Water";

            // 其他方塊名稱會由列舉名稱自動轉成較好讀的格式。
            default -> titleCaseFromEnum(name());
        };
    }

    // 根據方塊的面向，回傳該面要使用的貼圖編號。
    public int tileForFace(Face face) {
        if (this == FURNACE && face == Face.NORTH) {
            return AtlasTiles.FURNACE_FRONT;
        }
        if (this == CHEST && face == Face.NORTH) {
            return AtlasTiles.CHEST_SIDE;
        }
        if (this == CRAFTING_TABLE && face == Face.UP) {
            return AtlasTiles.CRAFTING_TABLE_TOP;
        }
        if (this == BOOKSHELF && (face == Face.UP || face == Face.DOWN)) {
            return AtlasTiles.PLANKS;
        }

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

    // 是否可視為完整 1x1x1 方塊。這會影響 meshing 的鄰面裁切與玩家碰撞盒。
    public boolean isFullCube() {
        return FULL_CUBE_FLAGS[ordinal()];
    }

    // 方塊本身的發光強度（0 到 15）。
    public int lightEmission() {
        return LIGHT_EMISSION[ordinal()];
    }

    // 光線穿過這個方塊時的額外衰減；15 代表完全阻擋（每跨一格本來就會衰減 1）。
    public int lightOpacity() {
        return LIGHT_OPACITY[ordinal()];
    }

    // 建立發光強度；只在 enum 靜態初始化時呼叫一次，數值對齊 Minecraft。
    private int computeLightEmission() {
        return switch (this) {
            case TORCH -> 14;
            case GLOWSTONE, SEA_LANTERN -> 15;
            default -> 0;
        };
    }

    // 建立額外光衰減；只在 enum 靜態初始化時呼叫一次。
    private int computeLightOpacity() {
        if (isWaterBlock()) {
            return 2;
        }
        if (this == LEAVES) {
            return 1;
        }
        return isOpaque() && isFullCube() ? 15 : 0;
    }

    // 建立完整方塊旗標；只在 enum 靜態初始化時呼叫一次。
    private boolean computeIsFullCube() {
        return switch (this) {
            case AIR, OAK_STAIRS, OAK_STAIRS_NORTH, OAK_STAIRS_EAST, OAK_STAIRS_SOUTH, OAK_STAIRS_WEST,
                    OAK_SLAB, STONE_SLAB, OAK_FENCE, OAK_DOOR, OAK_DOOR_NORTH_BOTTOM, OAK_DOOR_NORTH_TOP,
                    OAK_DOOR_EAST_BOTTOM, OAK_DOOR_EAST_TOP, OAK_DOOR_SOUTH_BOTTOM, OAK_DOOR_SOUTH_TOP,
                    OAK_DOOR_WEST_BOTTOM, OAK_DOOR_WEST_TOP, OAK_DOOR_NORTH_OPEN_BOTTOM, OAK_DOOR_NORTH_OPEN_TOP,
                    OAK_DOOR_EAST_OPEN_BOTTOM, OAK_DOOR_EAST_OPEN_TOP, OAK_DOOR_SOUTH_OPEN_BOTTOM,
                    OAK_DOOR_SOUTH_OPEN_TOP, OAK_DOOR_WEST_OPEN_BOTTOM, OAK_DOOR_WEST_OPEN_TOP,
                    OAK_TRAPDOOR, OAK_TRAPDOOR_OPEN, LADDER, LADDER_NORTH, LADDER_EAST, LADDER_SOUTH,
                    LADDER_WEST, TORCH -> false;
            default -> true;
        };
    }

    // 門有上下半格與開關狀態多個 enum 變體，互動與破壞時都需要把它們視為同一類方塊。
    public boolean isDoorBlock() {
        return DOOR_FLAGS[ordinal()];
    }

    private boolean computeIsDoorBlock() {
        return switch (this) {
            case OAK_DOOR, OAK_DOOR_NORTH_BOTTOM, OAK_DOOR_NORTH_TOP, OAK_DOOR_EAST_BOTTOM, OAK_DOOR_EAST_TOP,
                    OAK_DOOR_SOUTH_BOTTOM, OAK_DOOR_SOUTH_TOP, OAK_DOOR_WEST_BOTTOM, OAK_DOOR_WEST_TOP,
                    OAK_DOOR_NORTH_OPEN_BOTTOM, OAK_DOOR_NORTH_OPEN_TOP, OAK_DOOR_EAST_OPEN_BOTTOM,
                    OAK_DOOR_EAST_OPEN_TOP, OAK_DOOR_SOUTH_OPEN_BOTTOM, OAK_DOOR_SOUTH_OPEN_TOP,
                    OAK_DOOR_WEST_OPEN_BOTTOM, OAK_DOOR_WEST_OPEN_TOP -> true;
            default -> false;
        };
    }

    // 判斷目前門變體是否為上半部，用來定位同一扇門的底部座標。
    public boolean isDoorTop() {
        return DOOR_TOP_FLAGS[ordinal()];
    }

    private boolean computeIsDoorTop() {
        return switch (this) {
            case OAK_DOOR_NORTH_TOP, OAK_DOOR_EAST_TOP, OAK_DOOR_SOUTH_TOP, OAK_DOOR_WEST_TOP,
                    OAK_DOOR_NORTH_OPEN_TOP, OAK_DOOR_EAST_OPEN_TOP, OAK_DOOR_SOUTH_OPEN_TOP,
                    OAK_DOOR_WEST_OPEN_TOP -> true;
            default -> false;
        };
    }

    // 判斷門是否為開啟狀態；開關門時會在開/關變體之間切換，但保留朝向與上下半部。
    public boolean isDoorOpen() {
        return DOOR_OPEN_FLAGS[ordinal()];
    }

    private boolean computeIsDoorOpen() {
        return switch (this) {
            case OAK_DOOR_NORTH_OPEN_BOTTOM, OAK_DOOR_NORTH_OPEN_TOP, OAK_DOOR_EAST_OPEN_BOTTOM,
                    OAK_DOOR_EAST_OPEN_TOP, OAK_DOOR_SOUTH_OPEN_BOTTOM, OAK_DOOR_SOUTH_OPEN_TOP,
                    OAK_DOOR_WEST_OPEN_BOTTOM, OAK_DOOR_WEST_OPEN_TOP -> true;
            default -> false;
        };
    }

    // 活板門目前只有關閉與固定開啟方向兩種狀態。
    public boolean isTrapdoorBlock() {
        return TRAPDOOR_FLAGS[ordinal()];
    }

    private boolean computeIsTrapdoorBlock() {
        return this == OAK_TRAPDOOR || this == OAK_TRAPDOOR_OPEN;
    }

    // 梯子沒有碰撞盒，但玩家爬梯與渲染仍需要辨識它的方向變體。
    public boolean isLadderBlock() {
        return LADDER_FLAGS[ordinal()];
    }

    private boolean computeIsLadderBlock() {
        return this == LADDER || this == LADDER_NORTH || this == LADDER_EAST || this == LADDER_SOUTH
                || this == LADDER_WEST;
    }

    // 回傳活板門切換後的變體；呼叫端會先檢查新碰撞盒是否卡到玩家。
    public BlockType toggledTrapdoorVariant() {
        return this == OAK_TRAPDOOR ? OAK_TRAPDOOR_OPEN : OAK_TRAPDOOR;
    }

    // 回傳同朝向、同上下半部的開關相反門變體。
    public BlockType toggledDoorVariant() {
        return isDoorOpen() ? closedDoorVariant() : openedDoorVariant();
    }

    // 關門變體轉開門變體；非關門變體原樣回傳，讓呼叫端可安全地對任意方塊呼叫。
    private BlockType openedDoorVariant() {
        return switch (this) {
            case OAK_DOOR_NORTH_BOTTOM -> OAK_DOOR_NORTH_OPEN_BOTTOM;
            case OAK_DOOR_NORTH_TOP -> OAK_DOOR_NORTH_OPEN_TOP;
            case OAK_DOOR_EAST_BOTTOM -> OAK_DOOR_EAST_OPEN_BOTTOM;
            case OAK_DOOR_EAST_TOP -> OAK_DOOR_EAST_OPEN_TOP;
            case OAK_DOOR_SOUTH_BOTTOM -> OAK_DOOR_SOUTH_OPEN_BOTTOM;
            case OAK_DOOR_SOUTH_TOP -> OAK_DOOR_SOUTH_OPEN_TOP;
            case OAK_DOOR_WEST_BOTTOM -> OAK_DOOR_WEST_OPEN_BOTTOM;
            case OAK_DOOR_WEST_TOP -> OAK_DOOR_WEST_OPEN_TOP;
            default -> this;
        };
    }

    // 開門變體轉關門變體；非開門變體原樣回傳。
    private BlockType closedDoorVariant() {
        return switch (this) {
            case OAK_DOOR_NORTH_OPEN_BOTTOM -> OAK_DOOR_NORTH_BOTTOM;
            case OAK_DOOR_NORTH_OPEN_TOP -> OAK_DOOR_NORTH_TOP;
            case OAK_DOOR_EAST_OPEN_BOTTOM -> OAK_DOOR_EAST_BOTTOM;
            case OAK_DOOR_EAST_OPEN_TOP -> OAK_DOOR_EAST_TOP;
            case OAK_DOOR_SOUTH_OPEN_BOTTOM -> OAK_DOOR_SOUTH_BOTTOM;
            case OAK_DOOR_SOUTH_OPEN_TOP -> OAK_DOOR_SOUTH_TOP;
            case OAK_DOOR_WEST_OPEN_BOTTOM -> OAK_DOOR_WEST_BOTTOM;
            case OAK_DOOR_WEST_OPEN_TOP -> OAK_DOOR_WEST_TOP;
            default -> this;
        };
    }

    // 背包中的 OAK_DOOR 是放置用代表項，真正寫入世界時會拆成上下兩格方向變體。
    public boolean placesAsDoor() {
        return this == OAK_DOOR;
    }

    // 將背包中的基礎方塊轉成實際寫入世界的朝向變體；facing 使用 Game 的 0=N、1=E、2=S、3=W 約定。
    public BlockType placedVariantForFacing(int facing) {
        return switch (this) {
            case OAK_STAIRS -> switch (facing) {
                case 1 -> OAK_STAIRS_EAST;
                case 2 -> OAK_STAIRS_SOUTH;
                case 3 -> OAK_STAIRS_WEST;
                default -> OAK_STAIRS_NORTH;
            };
            case OAK_DOOR -> doorBottomForFacing(facing);
            case LADDER -> ladderForFacing(facing);
            default -> this;
        };
    }

    // 由門底部變體取得對應上半部；開關狀態與朝向必須一致。
    public BlockType doorTopVariant() {
        return switch (this) {
            case OAK_DOOR_NORTH_BOTTOM -> OAK_DOOR_NORTH_TOP;
            case OAK_DOOR_EAST_BOTTOM -> OAK_DOOR_EAST_TOP;
            case OAK_DOOR_SOUTH_BOTTOM -> OAK_DOOR_SOUTH_TOP;
            case OAK_DOOR_WEST_BOTTOM -> OAK_DOOR_WEST_TOP;
            case OAK_DOOR_NORTH_OPEN_BOTTOM -> OAK_DOOR_NORTH_OPEN_TOP;
            case OAK_DOOR_EAST_OPEN_BOTTOM -> OAK_DOOR_EAST_OPEN_TOP;
            case OAK_DOOR_SOUTH_OPEN_BOTTOM -> OAK_DOOR_SOUTH_OPEN_TOP;
            case OAK_DOOR_WEST_OPEN_BOTTOM -> OAK_DOOR_WEST_OPEN_TOP;
            default -> this;
        };
    }

    // 找出同一扇門的另一半；破壞或開關任一半時用來同步處理上下格。
    public BlockType matchingDoorHalf() {
        return switch (this) {
            case OAK_DOOR_NORTH_BOTTOM -> OAK_DOOR_NORTH_TOP;
            case OAK_DOOR_NORTH_TOP -> OAK_DOOR_NORTH_BOTTOM;
            case OAK_DOOR_EAST_BOTTOM -> OAK_DOOR_EAST_TOP;
            case OAK_DOOR_EAST_TOP -> OAK_DOOR_EAST_BOTTOM;
            case OAK_DOOR_SOUTH_BOTTOM -> OAK_DOOR_SOUTH_TOP;
            case OAK_DOOR_SOUTH_TOP -> OAK_DOOR_SOUTH_BOTTOM;
            case OAK_DOOR_WEST_BOTTOM -> OAK_DOOR_WEST_TOP;
            case OAK_DOOR_WEST_TOP -> OAK_DOOR_WEST_BOTTOM;
            case OAK_DOOR_NORTH_OPEN_BOTTOM -> OAK_DOOR_NORTH_OPEN_TOP;
            case OAK_DOOR_NORTH_OPEN_TOP -> OAK_DOOR_NORTH_OPEN_BOTTOM;
            case OAK_DOOR_EAST_OPEN_BOTTOM -> OAK_DOOR_EAST_OPEN_TOP;
            case OAK_DOOR_EAST_OPEN_TOP -> OAK_DOOR_EAST_OPEN_BOTTOM;
            case OAK_DOOR_SOUTH_OPEN_BOTTOM -> OAK_DOOR_SOUTH_OPEN_TOP;
            case OAK_DOOR_SOUTH_OPEN_TOP -> OAK_DOOR_SOUTH_OPEN_BOTTOM;
            case OAK_DOOR_WEST_OPEN_BOTTOM -> OAK_DOOR_WEST_OPEN_TOP;
            case OAK_DOOR_WEST_OPEN_TOP -> OAK_DOOR_WEST_OPEN_BOTTOM;
            default -> this;
        };
    }

    // 回傳實際會阻擋玩家的碰撞盒。梯子與火把可見但不阻擋，水也不阻擋。
    public BlockBounds[] collisionBoxes() {
        return COLLISION_BOXES[ordinal()];
    }

    private BlockBounds[] computeCollisionBoxes() {
        if (this == AIR || isWaterBlock() || computeIsLadderBlock() || this == TORCH) {
            return EMPTY_BOUNDS;
        }
        return computeRenderBoxes();
    }

    // 回傳渲染用幾何盒；非完整方塊會在 ChunkMesher 中依這些盒子建立簡化模型。
    public BlockBounds[] renderBoxes() {
        return RENDER_BOXES[ordinal()];
    }

    private BlockBounds[] computeRenderBoxes() {
        return switch (this) {
            case AIR -> EMPTY_BOUNDS;
            case OAK_STAIRS, OAK_STAIRS_NORTH -> STAIRS_NORTH_BOUNDS;
            case OAK_STAIRS_EAST -> STAIRS_EAST_BOUNDS;
            case OAK_STAIRS_SOUTH -> STAIRS_SOUTH_BOUNDS;
            case OAK_STAIRS_WEST -> STAIRS_WEST_BOUNDS;
            case OAK_SLAB, STONE_SLAB -> OAK_SLAB_BOUNDS;
            case OAK_FENCE -> OAK_FENCE_BOUNDS;
            case OAK_DOOR, OAK_DOOR_NORTH_BOTTOM, OAK_DOOR_NORTH_TOP -> NORTH_THIN;
            case OAK_DOOR_EAST_BOTTOM, OAK_DOOR_EAST_TOP -> EAST_THIN;
            case OAK_DOOR_SOUTH_BOTTOM, OAK_DOOR_SOUTH_TOP -> SOUTH_THIN;
            case OAK_DOOR_WEST_BOTTOM, OAK_DOOR_WEST_TOP -> WEST_THIN;
            case OAK_DOOR_NORTH_OPEN_BOTTOM, OAK_DOOR_NORTH_OPEN_TOP -> WEST_THIN;
            case OAK_DOOR_EAST_OPEN_BOTTOM, OAK_DOOR_EAST_OPEN_TOP -> NORTH_THIN;
            case OAK_DOOR_SOUTH_OPEN_BOTTOM, OAK_DOOR_SOUTH_OPEN_TOP -> EAST_THIN;
            case OAK_DOOR_WEST_OPEN_BOTTOM, OAK_DOOR_WEST_OPEN_TOP -> SOUTH_THIN;
            case OAK_TRAPDOOR -> OAK_TRAPDOOR_BOUNDS;
            case OAK_TRAPDOOR_OPEN -> OAK_TRAPDOOR_OPEN_BOUNDS;
            case LADDER, LADDER_NORTH -> NORTH_LADDER;
            case LADDER_EAST -> EAST_LADDER;
            case LADDER_SOUTH -> SOUTH_LADDER;
            case LADDER_WEST -> WEST_LADDER;
            case TORCH -> TORCH_BOUNDS;
            default -> FULL_BOUNDS;
        };
    }

    // 依玩家面向取得門底部變體，供放置流程建立下半部後再推得上半部。
    public static BlockType doorBottomForFacing(int facing) {
        return switch (facing) {
            case 1 -> OAK_DOOR_EAST_BOTTOM;
            case 2 -> OAK_DOOR_SOUTH_BOTTOM;
            case 3 -> OAK_DOOR_WEST_BOTTOM;
            default -> OAK_DOOR_NORTH_BOTTOM;
        };
    }

    // 依附著面方向取得梯子變體；方向代表梯子所在的薄面朝向。
    public static BlockType ladderForFacing(int facing) {
        return switch (facing) {
            case 1 -> LADDER_EAST;
            case 2 -> LADDER_SOUTH;
            case 3 -> LADDER_WEST;
            default -> LADDER_NORTH;
        };
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

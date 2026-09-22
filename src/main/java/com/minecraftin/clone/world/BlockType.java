package com.minecraftin.clone.world;

import com.minecraftin.clone.render.AtlasTiles;
import java.util.Locale;

// Persist explicit block IDs, never enum ordinals; do not renumber or reuse IDs without a save
// migration.
public enum BlockType {

    AIR(0, false, false, 0, 0, 0),

    GRASS(1, true, true, AtlasTiles.GRASS_SIDE, AtlasTiles.GRASS_TOP, AtlasTiles.DIRT),

    DIRT(2, true, true, AtlasTiles.DIRT, AtlasTiles.DIRT, AtlasTiles.DIRT),

    STONE(3, true, true, AtlasTiles.STONE, AtlasTiles.STONE, AtlasTiles.STONE),

    SAND(4, true, true, AtlasTiles.SAND, AtlasTiles.SAND, AtlasTiles.SAND),

    // Source water does not block movement or targeting.
    WATER(5, false, false, AtlasTiles.WATER, AtlasTiles.WATER, AtlasTiles.WATER),

    LOG(6, true, true, AtlasTiles.LOG_SIDE, AtlasTiles.LOG_TOP, AtlasTiles.LOG_TOP),

    // Leaves block movement but allow attenuated light; their current texture pixels are fully
    // opaque.
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

    PURPLE_BLOCK(19, true, true, AtlasTiles.PURPLE_BLOCK, AtlasTiles.PURPLE_BLOCK, AtlasTiles.PURPLE_BLOCK),

    // Selectable wool colors share full-cube behavior.
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

    BIRCH_PLANKS(35, true, true, AtlasTiles.BIRCH_PLANKS, AtlasTiles.BIRCH_PLANKS, AtlasTiles.BIRCH_PLANKS),
    SPRUCE_PLANKS(36, true, true, AtlasTiles.SPRUCE_PLANKS, AtlasTiles.SPRUCE_PLANKS, AtlasTiles.SPRUCE_PLANKS),
    DARK_OAK_PLANKS(37, true, true, AtlasTiles.DARK_OAK_PLANKS, AtlasTiles.DARK_OAK_PLANKS, AtlasTiles.DARK_OAK_PLANKS),

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

    // The creative palette exposes base entries; placement converts directional blocks to world
    // variants.
    OAK_STAIRS(59, true, true, AtlasTiles.PLANKS, AtlasTiles.PLANKS, AtlasTiles.PLANKS),
    OAK_STAIRS_NORTH(60, true, true, AtlasTiles.PLANKS, AtlasTiles.PLANKS, AtlasTiles.PLANKS),
    OAK_STAIRS_EAST(61, true, true, AtlasTiles.PLANKS, AtlasTiles.PLANKS, AtlasTiles.PLANKS),
    OAK_STAIRS_SOUTH(62, true, true, AtlasTiles.PLANKS, AtlasTiles.PLANKS, AtlasTiles.PLANKS),
    OAK_STAIRS_WEST(63, true, true, AtlasTiles.PLANKS, AtlasTiles.PLANKS, AtlasTiles.PLANKS),
    OAK_SLAB(64, true, true, AtlasTiles.PLANKS, AtlasTiles.PLANKS, AtlasTiles.PLANKS),
    STONE_SLAB(65, true, true, AtlasTiles.STONE, AtlasTiles.STONE, AtlasTiles.STONE),
    OAK_FENCE(66, true, true, AtlasTiles.OAK_FENCE, AtlasTiles.OAK_FENCE, AtlasTiles.OAK_FENCE),
    OAK_DOOR(67, true, false, AtlasTiles.OAK_DOOR_LOWER, AtlasTiles.OAK_DOOR_LOWER, AtlasTiles.OAK_DOOR_LOWER),
    OAK_DOOR_NORTH_BOTTOM(68, true, false, AtlasTiles.OAK_DOOR_LOWER, AtlasTiles.OAK_DOOR_LOWER, AtlasTiles.OAK_DOOR_LOWER),
    OAK_DOOR_NORTH_TOP(69, true, false, AtlasTiles.OAK_DOOR_UPPER, AtlasTiles.OAK_DOOR_UPPER, AtlasTiles.OAK_DOOR_UPPER),
    OAK_DOOR_EAST_BOTTOM(70, true, false, AtlasTiles.OAK_DOOR_LOWER, AtlasTiles.OAK_DOOR_LOWER, AtlasTiles.OAK_DOOR_LOWER),
    OAK_DOOR_EAST_TOP(71, true, false, AtlasTiles.OAK_DOOR_UPPER, AtlasTiles.OAK_DOOR_UPPER, AtlasTiles.OAK_DOOR_UPPER),
    OAK_DOOR_SOUTH_BOTTOM(72, true, false, AtlasTiles.OAK_DOOR_LOWER, AtlasTiles.OAK_DOOR_LOWER, AtlasTiles.OAK_DOOR_LOWER),
    OAK_DOOR_SOUTH_TOP(73, true, false, AtlasTiles.OAK_DOOR_UPPER, AtlasTiles.OAK_DOOR_UPPER, AtlasTiles.OAK_DOOR_UPPER),
    OAK_DOOR_WEST_BOTTOM(74, true, false, AtlasTiles.OAK_DOOR_LOWER, AtlasTiles.OAK_DOOR_LOWER, AtlasTiles.OAK_DOOR_LOWER),
    OAK_DOOR_WEST_TOP(75, true, false, AtlasTiles.OAK_DOOR_UPPER, AtlasTiles.OAK_DOOR_UPPER, AtlasTiles.OAK_DOOR_UPPER),
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

    // Append open-state IDs without changing IDs already stored in existing saves.
    OAK_DOOR_NORTH_OPEN_BOTTOM(87, true, false, AtlasTiles.OAK_DOOR_LOWER, AtlasTiles.OAK_DOOR_LOWER, AtlasTiles.OAK_DOOR_LOWER),
    OAK_DOOR_NORTH_OPEN_TOP(88, true, false, AtlasTiles.OAK_DOOR_UPPER, AtlasTiles.OAK_DOOR_UPPER, AtlasTiles.OAK_DOOR_UPPER),
    OAK_DOOR_EAST_OPEN_BOTTOM(89, true, false, AtlasTiles.OAK_DOOR_LOWER, AtlasTiles.OAK_DOOR_LOWER, AtlasTiles.OAK_DOOR_LOWER),
    OAK_DOOR_EAST_OPEN_TOP(90, true, false, AtlasTiles.OAK_DOOR_UPPER, AtlasTiles.OAK_DOOR_UPPER, AtlasTiles.OAK_DOOR_UPPER),
    OAK_DOOR_SOUTH_OPEN_BOTTOM(91, true, false, AtlasTiles.OAK_DOOR_LOWER, AtlasTiles.OAK_DOOR_LOWER, AtlasTiles.OAK_DOOR_LOWER),
    OAK_DOOR_SOUTH_OPEN_TOP(92, true, false, AtlasTiles.OAK_DOOR_UPPER, AtlasTiles.OAK_DOOR_UPPER, AtlasTiles.OAK_DOOR_UPPER),
    OAK_DOOR_WEST_OPEN_BOTTOM(93, true, false, AtlasTiles.OAK_DOOR_LOWER, AtlasTiles.OAK_DOOR_LOWER, AtlasTiles.OAK_DOOR_LOWER),
    OAK_DOOR_WEST_OPEN_TOP(94, true, false, AtlasTiles.OAK_DOOR_UPPER, AtlasTiles.OAK_DOOR_UPPER, AtlasTiles.OAK_DOOR_UPPER),
    OAK_TRAPDOOR_OPEN(95, true, false, AtlasTiles.OAK_TRAPDOOR, AtlasTiles.OAK_TRAPDOOR, AtlasTiles.OAK_TRAPDOOR),

    // Sources use WATER; simulation writes flowing states with strength 7 down to 1.
    // Strength controls propagation, not visible surface height; all water states currently use
    // full-cube geometry.
    WATER_FLOW_7(96, false, false, AtlasTiles.WATER, AtlasTiles.WATER, AtlasTiles.WATER),
    WATER_FLOW_6(97, false, false, AtlasTiles.WATER, AtlasTiles.WATER, AtlasTiles.WATER),
    WATER_FLOW_5(98, false, false, AtlasTiles.WATER, AtlasTiles.WATER, AtlasTiles.WATER),
    WATER_FLOW_4(99, false, false, AtlasTiles.WATER, AtlasTiles.WATER, AtlasTiles.WATER),
    WATER_FLOW_3(100, false, false, AtlasTiles.WATER, AtlasTiles.WATER, AtlasTiles.WATER),
    WATER_FLOW_2(101, false, false, AtlasTiles.WATER, AtlasTiles.WATER, AtlasTiles.WATER),
    WATER_FLOW_1(102, false, false, AtlasTiles.WATER, AtlasTiles.WATER, AtlasTiles.WATER);

    // Index persisted IDs separately from ordinal-based runtime caches.
    private static final BlockType[] BY_ID;

    // Cache display names so HUD updates do not repeatedly split and capitalize enum names.
    private static final String[] DISPLAY_NAMES;

    // Cache full-cube classification for frequent mesher queries.
    private static final boolean[] FULL_CUBE_FLAGS;

    // Cache block categories used frequently by interaction and collision code.
    private static final boolean[] DOOR_FLAGS;
    private static final boolean[] DOOR_TOP_FLAGS;
    private static final boolean[] DOOR_OPEN_FLAGS;
    private static final boolean[] TRAPDOOR_FLAGS;
    private static final boolean[] LADDER_FLAGS;

    // Cache emission and attenuation for lighting propagation.
    private static final int[] LIGHT_EMISSION;
    private static final int[] LIGHT_OPACITY;

    // Cache shared shape arrays for collision and mesh generation.
    private static final BlockBounds[][] COLLISION_BOXES;
    private static final BlockBounds[][] RENDER_BOXES;

    // Use block-local boxes so collision, rendering, and targeting share shape definitions.
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

    // Expose placeable base entries in the palette; placement chooses directional variants.
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

    // Initialize FULL_CUBE_FLAGS before LIGHT_OPACITY because computeLightOpacity() calls
    // isFullCube().
    static {
        int maxId = 0;
        BlockType[] types = values();

        for (BlockType type : types) {
            maxId = Math.max(maxId, type.id);
        }

        BY_ID = new BlockType[maxId + 1];

        for (BlockType type : types) {
            BY_ID[type.id] = type;
        }

        // Use stable IDs for saves and ordinals only for runtime caches; these indices are not
        // interchangeable.
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

    private final int id;

    // Whether this block can provide solid support; actual player collisions use collisionBoxes().
    private final boolean solid;

    // Opacity classification affects face culling; light attenuation also depends on full-cube
    // shape.
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

    // Only water and glass use alpha blending; isTransparent() alone does not select this pass.
    public boolean isTranslucent() {
        return isWaterBlock() || this == GLASS;
    }

    public boolean isWaterBlock() {
        return this == WATER || isFlowingWater();
    }

    public boolean isFlowingWater() {
        return switch (this) {
            case WATER_FLOW_7, WATER_FLOW_6, WATER_FLOW_5, WATER_FLOW_4,
                    WATER_FLOW_3, WATER_FLOW_2, WATER_FLOW_1 -> true;
            default -> false;
        };
    }

    // Simulation strength is 8 for sources, 7-1 for flowing water, and 0 for other blocks.
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

    // Map nonpositive strength to AIR, strengths 1-7 to flow states, and higher values to a source.
    public static BlockType flowingWaterOfStrength(int strength) {
        if (strength <= 0) {
            return AIR;
        }
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

    public String displayName() {
        return DISPLAY_NAMES[ordinal()];
    }

    // Compute each display name once; internal directional and state variants share the base block
    // name.
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

            default -> titleCaseFromEnum(name());
        };
    }

    // Choose the material by face; furnaces currently keep their front on NORTH.
    public int tileForFace(Face face) {
        if (this == FURNACE && face == Face.NORTH) {
            return AtlasTiles.FURNACE_FRONT;
        }
        return switch (face) {
            case UP -> topTile;
            case DOWN -> bottomTile;
            default -> sideTile;
        };
    }

    // Unknown or out-of-range IDs return AIR; the save loader must validate IDs before accepting
    // them.
    public static BlockType byId(int id) {
        if (id < 0 || id >= BY_ID.length) {
            return AIR;
        }

        BlockType type = BY_ID[id];
        return type != null ? type : AIR;
    }

    // Return a copy so callers cannot reorder the global creative palette.
    public static BlockType[] creativePalette() {
        return CREATIVE_PALETTE.clone();
    }

    public boolean isFullCube() {
        return FULL_CUBE_FLAGS[ordinal()];
    }

    // Emitted block light level, from 0 to 15.
    public int lightEmission() {
        return LIGHT_EMISSION[ordinal()];
    }

    // Light attenuation through this block; 15 stops incoming light.
    // Propagation still loses at least one level except for downward full-strength sky light.
    public int lightOpacity() {
        return LIGHT_OPACITY[ordinal()];
    }

    private int computeLightEmission() {
        return switch (this) {
            case TORCH -> 14;
            case GLOWSTONE, SEA_LANTERN -> 15;
            default -> 0;
        };
    }

    private int computeLightOpacity() {
        if (isWaterBlock()) {
            return 2;
        }
        if (this == LEAVES) {
            return 1;
        }
        return isOpaque() && isFullCube() ? 15 : 0;
    }

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

    // Treat all door orientations and open/closed halves as one interaction category.
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

    // Identify top halves so interactions can locate the door's lower cell.
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

    // Report whether a door half is open, regardless of its orientation or top/bottom position.
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

    // Trapdoors have only closed and fixed-direction open variants.
    public boolean isTrapdoorBlock() {
        return TRAPDOOR_FLAGS[ordinal()];
    }

    private boolean computeIsTrapdoorBlock() {
        return this == OAK_TRAPDOOR || this == OAK_TRAPDOOR_OPEN;
    }

    // Ladders have no collision boxes, but climbing and rendering still use their directional
    // variants.
    public boolean isLadderBlock() {
        return LADDER_FLAGS[ordinal()];
    }

    private boolean computeIsLadderBlock() {
        return this == LADDER || this == LADDER_NORTH || this == LADDER_EAST || this == LADDER_SOUTH
                || this == LADDER_WEST;
    }

    // Call only for a trapdoor; the caller must reject a new shape that would overlap the player.
    public BlockType toggledTrapdoorVariant() {
        return this == OAK_TRAPDOOR ? OAK_TRAPDOOR_OPEN : OAK_TRAPDOOR;
    }

    public BlockType toggledDoorVariant() {
        return isDoorOpen() ? closedDoorVariant() : openedDoorVariant();
    }

    // Convert directional closed-door states to open states; leave unrelated values unchanged.
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

    // Convert open-door states to closed states; leave unrelated values unchanged.
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

    // OAK_DOOR is a palette entry; placement writes two directional halves.
    public boolean placesAsDoor() {
        return this == OAK_DOOR;
    }

    // Facing indices match Game: 0 = north, 1 = east, 2 = south, 3 = west.
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

    // Find the matching top variant while preserving orientation and open/closed state.
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

    // Find the paired half with the same orientation and state for synchronized interaction or
    // removal.
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

    // Return shared collision boxes; air, water, ladders, and torches do not block the player.
    // Callers must not replace entries in the cached array.
    public BlockBounds[] collisionBoxes() {
        return COLLISION_BOXES[ordinal()];
    }

    private BlockBounds[] computeCollisionBoxes() {
        if (this == AIR || isWaterBlock() || computeIsLadderBlock() || this == TORCH) {
            return EMPTY_BOUNDS;
        }
        return computeRenderBoxes();
    }

    // Return shared render geometry, also used by targeting; callers must not replace array
    // entries.
    public BlockBounds[] renderBoxes() {
        return RENDER_BOXES[ordinal()];
    }

    private BlockBounds[] computeRenderBoxes() {
        // Open doors rotate to an adjacent thin face; keep the orientation convention aligned with
        // Game.
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

    public static BlockType doorBottomForFacing(int facing) {
        return switch (facing) {
            case 1 -> OAK_DOOR_EAST_BOTTOM;
            case 2 -> OAK_DOOR_SOUTH_BOTTOM;
            case 3 -> OAK_DOOR_WEST_BOTTOM;
            default -> OAK_DOOR_NORTH_BOTTOM;
        };
    }

    // Facing identifies the cell side holding the ladder, not the outward normal of the block that
    // was clicked.
    public static BlockType ladderForFacing(int facing) {
        return switch (facing) {
            case 1 -> LADDER_EAST;
            case 2 -> LADDER_SOUTH;
            case 3 -> LADDER_WEST;
            default -> LADDER_NORTH;
        };
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

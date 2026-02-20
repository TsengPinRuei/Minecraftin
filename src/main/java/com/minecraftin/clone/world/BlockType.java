// 說明：宣告此檔案所屬的套件。
package com.minecraftin.clone.world;

// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.render.AtlasTiles;

// 說明：匯入後續會使用到的型別或函式。
import java.util.Locale;

// 說明：定義主要型別與其結構。
public enum BlockType {
    // 說明：下一行程式碼負責執行目前步驟。
    AIR(0, false, false, 0, 0, 0),
    // 說明：下一行程式碼負責執行目前步驟。
    GRASS(1, true, true, AtlasTiles.GRASS_SIDE, AtlasTiles.GRASS_TOP, AtlasTiles.DIRT),
    // 說明：下一行程式碼負責執行目前步驟。
    DIRT(2, true, true, AtlasTiles.DIRT, AtlasTiles.DIRT, AtlasTiles.DIRT),
    // 說明：下一行程式碼負責執行目前步驟。
    STONE(3, true, true, AtlasTiles.STONE, AtlasTiles.STONE, AtlasTiles.STONE),
    // 說明：下一行程式碼負責執行目前步驟。
    SAND(4, true, true, AtlasTiles.SAND, AtlasTiles.SAND, AtlasTiles.SAND),
    // 說明：下一行程式碼負責執行目前步驟。
    WATER(5, false, false, AtlasTiles.WATER, AtlasTiles.WATER, AtlasTiles.WATER),
    // 說明：下一行程式碼負責執行目前步驟。
    LOG(6, true, true, AtlasTiles.LOG_SIDE, AtlasTiles.LOG_TOP, AtlasTiles.LOG_TOP),
    // 說明：下一行程式碼負責執行目前步驟。
    LEAVES(7, true, false, AtlasTiles.LEAVES, AtlasTiles.LEAVES, AtlasTiles.LEAVES),
    // 說明：下一行程式碼負責執行目前步驟。
    COBBLESTONE(8, true, true, AtlasTiles.COBBLE, AtlasTiles.COBBLE, AtlasTiles.COBBLE),
    // 說明：下一行程式碼負責執行目前步驟。
    PLANKS(9, true, true, AtlasTiles.PLANKS, AtlasTiles.PLANKS, AtlasTiles.PLANKS),
    // 說明：下一行程式碼負責執行目前步驟。
    GLASS(10, true, false, AtlasTiles.GLASS, AtlasTiles.GLASS, AtlasTiles.GLASS),
    // 說明：下一行程式碼負責執行目前步驟。
    BRICKS(11, true, true, AtlasTiles.BRICKS, AtlasTiles.BRICKS, AtlasTiles.BRICKS),
    // 說明：下一行程式碼負責執行目前步驟。
    BEDROCK(12, true, true, AtlasTiles.BEDROCK, AtlasTiles.BEDROCK, AtlasTiles.BEDROCK),
    // 說明：下一行程式碼負責執行目前步驟。
    SNOW(13, true, true, AtlasTiles.SNOW, AtlasTiles.SNOW, AtlasTiles.SNOW),
    // 說明：下一行程式碼負責執行目前步驟。
    RED_BLOCK(14, true, true, AtlasTiles.RED_BLOCK, AtlasTiles.RED_BLOCK, AtlasTiles.RED_BLOCK),
    // 說明：下一行程式碼負責執行目前步驟。
    ORANGE_BLOCK(15, true, true, AtlasTiles.ORANGE_BLOCK, AtlasTiles.ORANGE_BLOCK, AtlasTiles.ORANGE_BLOCK),
    // 說明：下一行程式碼負責執行目前步驟。
    YELLOW_BLOCK(16, true, true, AtlasTiles.YELLOW_BLOCK, AtlasTiles.YELLOW_BLOCK, AtlasTiles.YELLOW_BLOCK),
    // 說明：下一行程式碼負責執行目前步驟。
    GREEN_BLOCK(17, true, true, AtlasTiles.GREEN_BLOCK, AtlasTiles.GREEN_BLOCK, AtlasTiles.GREEN_BLOCK),
    // 說明：下一行程式碼負責執行目前步驟。
    BLUE_BLOCK(18, true, true, AtlasTiles.BLUE_BLOCK, AtlasTiles.BLUE_BLOCK, AtlasTiles.BLUE_BLOCK),
    // 說明：呼叫方法執行對應功能。
    PURPLE_BLOCK(19, true, true, AtlasTiles.PURPLE_BLOCK, AtlasTiles.PURPLE_BLOCK, AtlasTiles.PURPLE_BLOCK);

    // 說明：下一行程式碼負責執行目前步驟。
    private static final BlockType[] BY_ID;

    // 說明：下一行程式碼負責執行目前步驟。
    static {
        // 說明：宣告並初始化變數。
        int maxId = 0;
        // 說明：使用迴圈逐一處理每個元素或區間。
        for (BlockType type : values()) {
            // 說明：設定或更新變數的值。
            maxId = Math.max(maxId, type.id);
        }
        // 說明：設定或更新變數的值。
        BY_ID = new BlockType[maxId + 1];
        // 說明：使用迴圈逐一處理每個元素或區間。
        for (BlockType type : values()) {
            // 說明：設定或更新變數的值。
            BY_ID[type.id] = type;
        }
    }

    // 說明：下一行程式碼負責執行目前步驟。
    private final int id;
    // 說明：下一行程式碼負責執行目前步驟。
    private final boolean solid;
    // 說明：下一行程式碼負責執行目前步驟。
    private final boolean opaque;
    // 說明：下一行程式碼負責執行目前步驟。
    private final int sideTile;
    // 說明：下一行程式碼負責執行目前步驟。
    private final int topTile;
    // 說明：下一行程式碼負責執行目前步驟。
    private final int bottomTile;

    // 說明：下一行程式碼負責執行目前步驟。
    BlockType(int id, boolean solid, boolean opaque, int sideTile, int topTile, int bottomTile) {
        // 說明：設定或更新變數的值。
        this.id = id;
        // 說明：設定或更新變數的值。
        this.solid = solid;
        // 說明：設定或更新變數的值。
        this.opaque = opaque;
        // 說明：設定或更新變數的值。
        this.sideTile = sideTile;
        // 說明：設定或更新變數的值。
        this.topTile = topTile;
        // 說明：設定或更新變數的值。
        this.bottomTile = bottomTile;
    }

    // 說明：定義對外可呼叫的方法。
    public int id() {
        // 說明：下一行程式碼負責執行目前步驟。
        return id;
    }

    // 說明：定義對外可呼叫的方法。
    public boolean isSolid() {
        // 說明：下一行程式碼負責執行目前步驟。
        return solid;
    }

    // 說明：定義對外可呼叫的方法。
    public boolean isOpaque() {
        // 說明：下一行程式碼負責執行目前步驟。
        return opaque;
    }

    // 說明：定義對外可呼叫的方法。
    public boolean isTransparent() {
        // 說明：下一行程式碼負責執行目前步驟。
        return !opaque;
    }

    // 說明：定義對外可呼叫的方法。
    public String displayName() {
        // 說明：下一行程式碼負責執行目前步驟。
        return switch (this) {
            // 說明：宣告 switch 的其中一個分支。
            case RED_BLOCK -> "Red";
            // 說明：宣告 switch 的其中一個分支。
            case ORANGE_BLOCK -> "Orange";
            // 說明：宣告 switch 的其中一個分支。
            case YELLOW_BLOCK -> "Yellow";
            // 說明：宣告 switch 的其中一個分支。
            case GREEN_BLOCK -> "Green";
            // 說明：宣告 switch 的其中一個分支。
            case BLUE_BLOCK -> "Blue";
            // 說明：宣告 switch 的其中一個分支。
            case PURPLE_BLOCK -> "Purple";
            // 說明：呼叫方法執行對應功能。
            default -> titleCaseFromEnum(name());
        };
    }

    // 說明：定義對外可呼叫的方法。
    public int tileForFace(Face face) {
        // 說明：下一行程式碼負責執行目前步驟。
        return switch (face) {
            // 說明：宣告 switch 的其中一個分支。
            case UP -> topTile;
            // 說明：宣告 switch 的其中一個分支。
            case DOWN -> bottomTile;
            // 說明：下一行程式碼負責執行目前步驟。
            default -> sideTile;
        };
    }

    // 說明：定義對外可呼叫的方法。
    public static BlockType byId(int id) {
        // 說明：根據條件決定是否進入此邏輯分支。
        if (id < 0 || id >= BY_ID.length) {
            // 說明：下一行程式碼負責執行目前步驟。
            return AIR;
        }
        // 說明：宣告並初始化變數。
        BlockType type = BY_ID[id];
        // 說明：設定或更新變數的值。
        return type != null ? type : AIR;
    }

    // 說明：定義類別內部使用的方法。
    private static String titleCaseFromEnum(String enumName) {
        // 說明：宣告並初始化變數。
        String[] words = enumName.toLowerCase(Locale.ROOT).split("_");
        // 說明：宣告並初始化變數。
        StringBuilder out = new StringBuilder(enumName.length() + 4);
        // 說明：使用迴圈逐一處理每個元素或區間。
        for (String word : words) {
            // 說明：根據條件決定是否進入此邏輯分支。
            if (word.isEmpty()) {
                // 說明：跳過本次迴圈剩餘邏輯，直接進入下一次迭代。
                continue;
            }
            // 說明：根據條件決定是否進入此邏輯分支。
            if (!out.isEmpty()) {
                // 說明：呼叫方法執行對應功能。
                out.append(' ');
            }
            // 說明：呼叫方法執行對應功能。
            out.append(Character.toUpperCase(word.charAt(0)));
            // 說明：根據條件決定是否進入此邏輯分支。
            if (word.length() > 1) {
                // 說明：呼叫方法執行對應功能。
                out.append(word, 1, word.length());
            }
        }
        // 說明：呼叫方法執行對應功能。
        return out.isEmpty() ? enumName : out.toString();
    }
}

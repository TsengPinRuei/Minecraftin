package com.minecraftin.clone.render;

// 集中管理材質圖集中各個貼圖的編號。
// 這些索引對應 TextureAtlas.fillAtlas() 寫入的位置，新增貼圖時兩邊要同步。
public final class AtlasTiles {

    // 草地方塊側面的貼圖編號。
    public static final int GRASS_SIDE = 0;

    // 草地方塊上表面的貼圖編號。
    public static final int GRASS_TOP = 1;

    // 泥土貼圖編號。
    public static final int DIRT = 2;

    // 石頭貼圖編號。
    public static final int STONE = 3;

    // 沙子貼圖編號。
    public static final int SAND = 4;

    // 木板貼圖編號。
    public static final int PLANKS = 5;

    // 原木側面的貼圖編號。
    public static final int LOG_SIDE = 6;

    // 原木上下表面的貼圖編號。
    public static final int LOG_TOP = 7;

    // 樹葉貼圖編號。
    public static final int LEAVES = 8;

    // 鵝卵石貼圖編號。
    public static final int COBBLE = 9;

    // 水貼圖編號。
    public static final int WATER = 10;

    // 玻璃貼圖編號。
    public static final int GLASS = 11;

    // 磚塊貼圖編號。
    public static final int BRICKS = 12;

    // 基岩貼圖編號。
    public static final int BEDROCK = 13;

    // 雪貼圖編號。
    public static final int SNOW = 14;

    // 紅色方塊貼圖編號。
    public static final int RED_BLOCK = 15;

    // 橘色方塊貼圖編號。
    public static final int ORANGE_BLOCK = 16;

    // 黃色方塊貼圖編號。
    public static final int YELLOW_BLOCK = 17;

    // 綠色方塊貼圖編號。
    public static final int GREEN_BLOCK = 18;

    // 藍色方塊貼圖編號。
    public static final int BLUE_BLOCK = 19;

    // 紫色方塊貼圖編號。
    public static final int PURPLE_BLOCK = 20;

    // 這個類別只提供常數，不需要建立物件。
    private AtlasTiles() {
    }
}

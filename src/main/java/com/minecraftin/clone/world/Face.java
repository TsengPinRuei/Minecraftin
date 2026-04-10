package com.minecraftin.clone.world;

// 表示方塊的六個面，並記錄每個面的方向與亮度。
public enum Face {

    // 北面，Z 軸往負方向。
    NORTH(0, 0, -1, 0.80f),

    // 南面，Z 軸往正方向。
    SOUTH(0, 0, 1, 0.80f),

    // 西面，X 軸往負方向。
    WEST(-1, 0, 0, 0.72f),

    // 東面，X 軸往正方向。
    EAST(1, 0, 0, 0.72f),

    // 上面，Y 軸往正方向。
    UP(0, 1, 0, 1.0f),

    // 下面，Y 軸往負方向。
    DOWN(0, -1, 0, 0.55f);

    // 此面在 X 軸上的方向位移。
    private final int dx;

    // 此面在 Y 軸上的方向位移。
    private final int dy;

    // 此面在 Z 軸上的方向位移。
    private final int dz;

    // 此面的亮度值，用來做簡單光影效果。
    private final float light;

    // 建立每個面時，同時設定方向與亮度。
    Face(int dx, int dy, int dz, float light) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.light = light;
    }

    // 回傳此面在 X 軸上的位移。
    public int dx() {
        return dx;
    }

    // 回傳此面在 Y 軸上的位移。
    public int dy() {
        return dy;
    }

    // 回傳此面在 Z 軸上的位移。
    public int dz() {
        return dz;
    }

    // 回傳此面的亮度值。
    public float light() {
        return light;
    }
}
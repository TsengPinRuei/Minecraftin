package com.minecraftin.clone.world;

// 表示方塊的六個面，並記錄每個面的鄰格方向與簡易亮度。
// 亮度會直接寫入 mesh 頂點，shader 只負責套用，不再重新計算法線光照。
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

    private final int dx;

    private final int dy;

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

    public int dx() {
        return dx;
    }

    public int dy() {
        return dy;
    }

    public int dz() {
        return dz;
    }

    public float light() {
        return light;
    }
}

// 說明：宣告此檔案所屬的套件。
package com.minecraftin.clone.world;

// 說明：定義主要型別與其結構。
public enum Face {
    // 說明：下一行程式碼負責執行目前步驟。
    NORTH(0, 0, -1, 0.80f),
    // 說明：下一行程式碼負責執行目前步驟。
    SOUTH(0, 0, 1, 0.80f),
    // 說明：下一行程式碼負責執行目前步驟。
    WEST(-1, 0, 0, 0.72f),
    // 說明：下一行程式碼負責執行目前步驟。
    EAST(1, 0, 0, 0.72f),
    // 說明：下一行程式碼負責執行目前步驟。
    UP(0, 1, 0, 1.0f),
    // 說明：呼叫方法執行對應功能。
    DOWN(0, -1, 0, 0.55f);

    // 說明：下一行程式碼負責執行目前步驟。
    private final int dx;
    // 說明：下一行程式碼負責執行目前步驟。
    private final int dy;
    // 說明：下一行程式碼負責執行目前步驟。
    private final int dz;
    // 說明：下一行程式碼負責執行目前步驟。
    private final float light;

    // 說明：下一行程式碼負責執行目前步驟。
    Face(int dx, int dy, int dz, float light) {
        // 說明：設定或更新變數的值。
        this.dx = dx;
        // 說明：設定或更新變數的值。
        this.dy = dy;
        // 說明：設定或更新變數的值。
        this.dz = dz;
        // 說明：設定或更新變數的值。
        this.light = light;
    }

    // 說明：定義對外可呼叫的方法。
    public int dx() {
        // 說明：下一行程式碼負責執行目前步驟。
        return dx;
    }

    // 說明：定義對外可呼叫的方法。
    public int dy() {
        // 說明：下一行程式碼負責執行目前步驟。
        return dy;
    }

    // 說明：定義對外可呼叫的方法。
    public int dz() {
        // 說明：下一行程式碼負責執行目前步驟。
        return dz;
    }

    // 說明：定義對外可呼叫的方法。
    public float light() {
        // 說明：下一行程式碼負責執行目前步驟。
        return light;
    }
}

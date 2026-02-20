// 說明：宣告此檔案所屬的套件。
package com.minecraftin.clone.config;

// 說明：定義主要型別與其結構。
public final class GameConfig {
    // 說明：設定或更新變數的值。
    public static final int WINDOW_WIDTH = 1600;
    // 說明：設定或更新變數的值。
    public static final int WINDOW_HEIGHT = 900;
    // 說明：設定或更新變數的值。
    public static final String WINDOW_TITLE = "Minecraftin Java Clone";

    // 說明：設定或更新變數的值。
    public static final float FOV_DEGREES = 75.0f;
    // 說明：設定或更新變數的值。
    public static final float NEAR_PLANE = 0.05f;
    // 說明：設定或更新變數的值。
    public static final float FAR_PLANE = 1200.0f;

    // 說明：設定或更新變數的值。
    public static final int CHUNK_SIZE = 16;
    // 說明：設定或更新變數的值。
    public static final int CHUNK_HEIGHT = 128;
    // 說明：設定或更新變數的值。
    public static final int RENDER_DISTANCE_CHUNKS = 10;

    // 說明：設定或更新變數的值。
    public static final boolean CREATIVE_MODE_ONLY = true;

    // 說明：設定或更新變數的值。
    public static final float MOUSE_SENSITIVITY = 0.12f;
    // 說明：設定或更新變數的值。
    public static final float WALK_SPEED = 4.5f;
    // 說明：設定或更新變數的值。
    public static final float FLY_SPEED = WALK_SPEED * 3.0f;
    // 說明：設定或更新變數的值。
    public static final float SPRINT_MULTIPLIER = 1.65f;
    // 說明：設定或更新變數的值。
    public static final float JUMP_VELOCITY = 6.8f;
    // 說明：設定或更新變數的值。
    public static final float GRAVITY = 22.0f;

    // 說明：設定或更新變數的值。
    public static final float PLAYER_WIDTH = 0.6f;
    // 說明：設定或更新變數的值。
    public static final float PLAYER_HEIGHT = 1.8f;
    // 說明：設定或更新變數的值。
    public static final float PLAYER_EYE_HEIGHT = 1.62f;

    // 說明：設定或更新變數的值。
    public static final float BLOCK_REACH = 6.0f;
    // 說明：設定或更新變數的值。
    public static final float BREAK_COOLDOWN_SECONDS = 0.05f;
    // 說明：設定或更新變數的值。
    public static final float PLACE_COOLDOWN_SECONDS = 0.05f;

    // 說明：設定或更新變數的值。
    public static final String WORLD_FILE = "saves/world.dat";

    // 說明：設定或更新變數的值。
    public static final long DEFAULT_WORLD_SEED = 20260219L;

    // 說明：定義類別內部使用的方法。
    private GameConfig() {
    }
}

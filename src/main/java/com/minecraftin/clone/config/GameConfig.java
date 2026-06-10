package com.minecraftin.clone.config;

// 集中管理遊戲中會用到的固定設定值。
// 這些值會同時影響地形、碰撞、渲染距離與存檔位置，調整前要確認相關系統的假設。
public final class GameConfig {

    // 視窗預設寬度。
    public static final int WINDOW_WIDTH = 1600;

    // 視窗預設高度。
    public static final int WINDOW_HEIGHT = 900;

    // 視窗標題文字。
    public static final String WINDOW_TITLE = "Minecraftin Java Clone";

    // 相機視野角度；對齊 Minecraft 預設 FOV 70。
    public static final float FOV_DEGREES = 70.0f;

    // 相機可看到的最近距離。
    public static final float NEAR_PLANE = 0.05f;

    // 相機可看到的最遠距離。
    public static final float FAR_PLANE = 1200.0f;

    // 每個 Chunk 在 X、Z 方向的邊長；World 與 Chunk 的座標換算都依賴這個值。
    public static final int CHUNK_SIZE = 16;

    // 每個 Chunk 的高度。
    public static final int CHUNK_HEIGHT = 128;

    // 世界渲染距離，以 Chunk 為單位。
    public static final int RENDER_DISTANCE_CHUNKS = 10;

    // 是否只啟用創造模式玩法；目前玩家移動與 HUD 標示都假設這是全域開關。
    public static final boolean CREATIVE_MODE_ONLY = true;

    // 滑鼠靈敏度。
    public static final float MOUSE_SENSITIVITY = 0.12f;

    // 一般行走速度；Minecraft 的步行速度約為 4.317 m/s。
    public static final float WALK_SPEED = 4.317f;

    // 飛行速度；Minecraft 創造飛行約為 10.89 m/s，約為步行速度的 2.5 倍。
    public static final float FLY_SPEED = WALK_SPEED * 2.5f;

    // 按住 Left Ctrl 時的加速倍率，套用於地面移動與創造模式飛行。
    public static final float SPRINT_MULTIPLIER = 3.0f;

    // 跳躍時向上的初速度；搭配 GRAVITY 可跳約 1.25 格，與 Minecraft 一致。
    public static final float JUMP_VELOCITY = 9.0f;

    // 重力大小；Minecraft 的有效重力約為 32 m/s^2。
    public static final float GRAVITY = 32.0f;

    // 玩家碰撞箱寬度。
    public static final float PLAYER_WIDTH = 0.6f;

    // 玩家碰撞箱高度。
    public static final float PLAYER_HEIGHT = 1.8f;

    // 玩家視角高度，也就是眼睛位置高度。
    public static final float PLAYER_EYE_HEIGHT = 1.62f;

    // 玩家可互動方塊的最遠距離；Minecraft 創造模式為 5 格。
    public static final float BLOCK_REACH = 5.0f;

    // 按住左鍵連續破壞方塊的間隔；接近 Minecraft 創造模式的連續挖掘節奏。
    public static final float BREAK_COOLDOWN_SECONDS = 0.25f;

    // 按住右鍵連續放置方塊的間隔；Minecraft 為 4 tick（0.2 秒）。
    public static final float PLACE_COOLDOWN_SECONDS = 0.2f;

    // 世界存檔檔案位置；World 會自動建立父目錄，但不會遷移舊格式以外的檔案。
    public static final String WORLD_FILE = "saves/world.dat";

    // 預設世界種子。
    public static final long DEFAULT_WORLD_SEED = 20260219L;

    // 一天的長度（秒）；Minecraft 為 20 分鐘。
    public static final float DAY_LENGTH_SECONDS = 1200.0f;

    // 這個類別只提供設定值，不需要建立物件。
    private GameConfig() {
    }
}

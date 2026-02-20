package com.minecraftin.clone.config;

public final class GameConfig {
    public static final int WINDOW_WIDTH = 1600;
    public static final int WINDOW_HEIGHT = 900;
    public static final String WINDOW_TITLE = "Minecraftin Java Clone";

    public static final float FOV_DEGREES = 75.0f;
    public static final float NEAR_PLANE = 0.05f;
    public static final float FAR_PLANE = 1200.0f;

    public static final int CHUNK_SIZE = 16;
    public static final int CHUNK_HEIGHT = 128;
    public static final int RENDER_DISTANCE_CHUNKS = 10;

    public static final boolean CREATIVE_MODE_ONLY = true;

    public static final float MOUSE_SENSITIVITY = 0.12f;
    public static final float WALK_SPEED = 4.5f;
    public static final float FLY_SPEED = WALK_SPEED * 3.0f;
    public static final float SPRINT_MULTIPLIER = 1.65f;
    public static final float JUMP_VELOCITY = 6.8f;
    public static final float GRAVITY = 22.0f;

    public static final float PLAYER_WIDTH = 0.6f;
    public static final float PLAYER_HEIGHT = 1.8f;
    public static final float PLAYER_EYE_HEIGHT = 1.62f;

    public static final float BLOCK_REACH = 6.0f;
    public static final float BREAK_COOLDOWN_SECONDS = 0.05f;
    public static final float PLACE_COOLDOWN_SECONDS = 0.05f;

    public static final String WORLD_FILE = "saves/world.dat";

    public static final long DEFAULT_WORLD_SEED = 20260219L;

    private GameConfig() {
    }
}

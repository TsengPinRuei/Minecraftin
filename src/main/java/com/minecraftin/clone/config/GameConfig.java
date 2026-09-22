package com.minecraftin.clone.config;

// These constants are shared by generation, physics, rendering, and persistence.
public final class GameConfig {

    public static final int WINDOW_WIDTH = 1600;

    public static final int WINDOW_HEIGHT = 900;

    public static final String WINDOW_TITLE = "Minecraftin Java Clone";

    // Vertical field of view in degrees.
    public static final float FOV_DEGREES = 70.0f;

    // Near clipping distance in world units.
    public static final float NEAR_PLANE = 0.05f;

    // Far clipping distance in world units; chunk visibility is also limited by the render radius.
    public static final float FAR_PLANE = 1200.0f;

    // Horizontal chunk size; changing it also changes saved block-array layout.
    public static final int CHUNK_SIZE = 16;

    // Chunk height must remain below 256 because LightEngine packs Y and neighbor sentinels into
    // eight bits.
    // Changing chunk dimensions also changes the save layout.
    public static final int CHUNK_HEIGHT = 128;

    // Horizontal render radius in chunks.
    public static final int RENDER_DISTANCE_CHUNKS = 10;

    // Controls creative flight and the mode label; disabling it does not implement survival
    // gameplay.
    public static final boolean CREATIVE_MODE_ONLY = true;

    // Camera rotation in degrees per GLFW cursor-coordinate unit.
    public static final float MOUSE_SENSITIVITY = 0.12f;

    // Walking target speed in blocks per second.
    public static final float WALK_SPEED = 4.317f;

    // Flight target speed is 2.5 times WALK_SPEED.
    public static final float FLY_SPEED = WALK_SPEED * 2.5f;

    // Applied while Left Ctrl and W are held; crouching on land disables this boost.
    public static final float SPRINT_MULTIPLIER = 3.0f;

    // Upward jump speed in blocks per second; actual jump height depends on gravity and the
    // simulation time step.
    public static final float JUMP_VELOCITY = 9.0f;

    // Downward acceleration in blocks per second squared.
    public static final float GRAVITY = 32.0f;

    // Player collision width in blocks.
    public static final float PLAYER_WIDTH = 0.6f;

    // Standing collision height; crouching uses a separate height in Player.
    public static final float PLAYER_HEIGHT = 1.8f;

    // Standing eye height above the player's feet; crouching uses a separate height in Player.
    public static final float PLAYER_EYE_HEIGHT = 1.62f;

    // Maximum targeting distance in blocks, measured from the camera.
    public static final float BLOCK_REACH = 5.0f;

    // Repeat interval while the break button is held; a new press bypasses the remaining cooldown.
    public static final float BREAK_COOLDOWN_SECONDS = 0.25f;

    // Repeat interval while the place button is held; a new press bypasses the remaining cooldown.
    public static final float PLACE_COOLDOWN_SECONDS = 0.2f;

    // Save path relative to the process working directory; World creates missing parent
    // directories.
    public static final String WORLD_FILE = "saves/world.dat";

    public static final long DEFAULT_WORLD_SEED = 20260219L;

    // Duration of one full day/night cycle in simulation seconds.
    public static final float DAY_LENGTH_SECONDS = 1200.0f;

    private GameConfig() {
    }
}

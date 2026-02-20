// 說明：宣告此檔案所屬的套件。
package com.minecraftin.clone.game;

// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.config.GameConfig;
// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.engine.Camera;
// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.engine.InputState;
// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.engine.Window;
// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.gameplay.Player;
// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.render.HudRenderer;
// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.render.WorldRenderer;
// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.world.BlockType;
// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.world.RaycastHit;
// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.world.World;
// 說明：匯入後續會使用到的型別或函式。
import org.joml.Vector3f;

// 說明：匯入後續會使用到的型別或函式。
import java.nio.file.Paths;

// 說明：匯入後續會使用到的型別或函式。
import static org.lwjgl.glfw.GLFW.*;
// 說明：匯入後續會使用到的型別或函式。
import static org.lwjgl.opengl.GL33C.*;

// 說明：定義主要型別與其結構。
public final class Game {
    // 說明：設定或更新變數的值。
    private static final String MODE_LABEL = GameConfig.CREATIVE_MODE_ONLY ? "CREATIVE" : "SURVIVAL";
    // 說明：設定或更新變數的值。
    private static final float WALK_BOB_VERTICAL_BASE = 0.020f;
    // 說明：設定或更新變數的值。
    private static final float WALK_BOB_VERTICAL_SCALE = 0.024f;
    // 說明：設定或更新變數的值。
    private static final float WALK_BOB_HORIZONTAL_FACTOR = 0.60f;
    // 說明：設定或更新變數的值。
    private static final float WALK_BOB_RESET_SPEED = 8.0f;

    // 說明：下一行程式碼負責執行目前步驟。
    private final BlockType[] hotbar = {
            // 說明：下一行程式碼負責執行目前步驟。
            BlockType.RED_BLOCK,
            // 說明：下一行程式碼負責執行目前步驟。
            BlockType.ORANGE_BLOCK,
            // 說明：下一行程式碼負責執行目前步驟。
            BlockType.YELLOW_BLOCK,
            // 說明：下一行程式碼負責執行目前步驟。
            BlockType.GREEN_BLOCK,
            // 說明：下一行程式碼負責執行目前步驟。
            BlockType.BLUE_BLOCK,
            // 說明：下一行程式碼負責執行目前步驟。
            BlockType.PURPLE_BLOCK,
            // 說明：下一行程式碼負責執行目前步驟。
            BlockType.DIRT,
            // 說明：下一行程式碼負責執行目前步驟。
            BlockType.STONE,
            // 說明：下一行程式碼負責執行目前步驟。
            BlockType.GLASS
    };

    // 說明：設定或更新變數的值。
    private final Window window = new Window();
    // 說明：設定或更新變數的值。
    private final InputState input = new InputState();
    // 說明：設定或更新變數的值。
    private final Camera camera = new Camera();
    // 說明：設定或更新變數的值。
    private final Player player = new Player();
    // 說明：設定或更新變數的值。
    private final World world = new World(Paths.get(GameConfig.WORLD_FILE), GameConfig.DEFAULT_WORLD_SEED);
    // 說明：設定或更新變數的值。
    private final Vector3f tmpCameraRight = new Vector3f();
    // 說明：設定或更新變數的值。
    private final Vector3f tmpRayOrigin = new Vector3f();
    // 說明：設定或更新變數的值。
    private final Vector3f tmpRayDirection = new Vector3f();

    // 說明：下一行程式碼負責執行目前步驟。
    private WorldRenderer worldRenderer;
    // 說明：下一行程式碼負責執行目前步驟。
    private HudRenderer hudRenderer;

    // 說明：設定或更新變數的值。
    private boolean cursorCaptured = false;
    // 說明：下一行程式碼負責執行目前步驟。
    private int hotbarIndex;

    // 說明：下一行程式碼負責執行目前步驟。
    private float breakCooldown;
    // 說明：下一行程式碼負責執行目前步驟。
    private float placeCooldown;
    // 說明：下一行程式碼負責執行目前步驟。
    private float autosaveTimer;

    // 說明：下一行程式碼負責執行目前步驟。
    private RaycastHit targetedBlock;

    // 說明：下一行程式碼負責執行目前步驟。
    private double fpsTimer;
    // 說明：下一行程式碼負責執行目前步驟。
    private int fpsFrames;
    // 說明：下一行程式碼負責執行目前步驟。
    private boolean worldInitialized;
    // 說明：下一行程式碼負責執行目前步驟。
    private float walkBobPhase;
    // 說明：下一行程式碼負責執行目前步驟。
    private float walkBobVertical;
    // 說明：下一行程式碼負責執行目前步驟。
    private float walkBobHorizontal;

    // 說明：定義對外可呼叫的方法。
    public void run() {
        // 說明：下一行程式碼負責執行目前步驟。
        try {
            // 說明：呼叫方法執行對應功能。
            window.create();
            // 說明：呼叫方法執行對應功能。
            input.attach(window.handle());

            // 說明：呼叫方法執行對應功能。
            initGraphicsState();

            // 說明：呼叫方法執行對應功能。
            world.initialize();
            // 說明：設定或更新變數的值。
            worldInitialized = true;
            // 說明：設定或更新變數的值。
            worldRenderer = new WorldRenderer();
            // 說明：設定或更新變數的值。
            hudRenderer = new HudRenderer();

            // 說明：宣告並初始化變數。
            Vector3f spawn = world.defaultSpawn(new Vector3f());
            // 說明：呼叫方法執行對應功能。
            player.setPosition(spawn.x, spawn.y, spawn.z);
            // 說明：呼叫方法執行對應功能。
            camera.setRotation(-90.0f, -15.0f);
            // 說明：呼叫方法執行對應功能。
            updateCameraFromPlayer(0.0f);

            // 說明：呼叫方法執行對應功能。
            window.captureCursor(false);
            // 說明：呼叫方法執行對應功能。
            input.resetMouseTracking();

            // 說明：呼叫方法執行對應功能。
            loop();
        // 說明：下一行程式碼負責執行目前步驟。
        } finally {
            // 說明：根據條件決定是否進入此邏輯分支。
            if (worldInitialized && world.hasModifiedChunks()) {
                // 說明：呼叫方法執行對應功能。
                safeSaveWorld();
            }
            // 說明：根據條件決定是否進入此邏輯分支。
            if (hudRenderer != null) {
                // 說明：呼叫方法執行對應功能。
                hudRenderer.close();
            }
            // 說明：根據條件決定是否進入此邏輯分支。
            if (worldRenderer != null) {
                // 說明：呼叫方法執行對應功能。
                worldRenderer.close();
            }
            // 說明：呼叫方法執行對應功能。
            window.close();
        }
    }

    // 說明：定義類別內部使用的方法。
    private void initGraphicsState() {
        // 說明：呼叫方法執行對應功能。
        glEnable(GL_DEPTH_TEST);
        // 說明：呼叫方法執行對應功能。
        glDepthFunc(GL_LEQUAL);

        // 說明：呼叫方法執行對應功能。
        glEnable(GL_BLEND);
        // 說明：呼叫方法執行對應功能。
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // 說明：呼叫方法執行對應功能。
        glDisable(GL_CULL_FACE);
        // 說明：呼叫方法執行對應功能。
        glEnable(GL_MULTISAMPLE);
    }

    // 說明：定義類別內部使用的方法。
    private void loop() {
        // 說明：宣告並初始化變數。
        double lastTime = glfwGetTime();

        // 說明：在條件成立時重複執行此區塊。
        while (!window.shouldClose()) {
            // 說明：宣告並初始化變數。
            double now = glfwGetTime();
            // 說明：宣告並初始化變數。
            float delta = (float) Math.min(0.05, now - lastTime);
            // 說明：設定或更新變數的值。
            lastTime = now;

            // 說明：呼叫方法執行對應功能。
            window.pollEvents();
            // 說明：呼叫方法執行對應功能。
            handleInputState();

            // 說明：宣告並初始化變數。
            int playerChunkX = Math.floorDiv((int) Math.floor(player.position().x), GameConfig.CHUNK_SIZE);
            // 說明：宣告並初始化變數。
            int playerChunkZ = Math.floorDiv((int) Math.floor(player.position().z), GameConfig.CHUNK_SIZE);
            // Load one extra ring so rendered chunks always have neighbor data at the edges.
            // 說明：呼叫方法執行對應功能。
            world.ensureChunksAround(playerChunkX, playerChunkZ, GameConfig.RENDER_DISTANCE_CHUNKS + 1);

            // 說明：根據條件決定是否進入此邏輯分支。
            if (cursorCaptured) {
                // 說明：呼叫方法執行對應功能。
                applyLookFromMouse();
                // 說明：呼叫方法執行對應功能。
                player.update(input, camera, world, delta);
            }

            // 說明：呼叫方法執行對應功能。
            updateCameraFromPlayer(delta);
            // 說明：呼叫方法執行對應功能。
            updateTargetBlock();
            // 說明：根據條件決定是否進入此邏輯分支。
            if (cursorCaptured) {
                // 說明：呼叫方法執行對應功能。
                updateBlockInteraction(delta);
            }

            // 說明：呼叫方法執行對應功能。
            worldRenderer.render(world, camera, window.width(), window.height(), targetedBlock);
            // 說明：呼叫方法執行對應功能。
            hudRenderer.render(hotbar, hotbarIndex);

            // 說明：呼叫方法執行對應功能。
            updateDebugTitle(now);

            // 說明：呼叫方法執行對應功能。
            window.swapBuffers();
            // 說明：呼叫方法執行對應功能。
            input.endFrame();

            // 說明：設定或更新變數的值。
            autosaveTimer += delta;
            // 說明：根據條件決定是否進入此邏輯分支。
            if (autosaveTimer >= 20.0f) {
                // 說明：根據條件決定是否進入此邏輯分支。
                if (world.hasModifiedChunks()) {
                    // 說明：呼叫方法執行對應功能。
                    safeSaveWorld();
                }
                // 說明：設定或更新變數的值。
                autosaveTimer = 0.0f;
            }
        }
    }

    // 說明：定義類別內部使用的方法。
    private void handleInputState() {
        // 說明：根據條件決定是否進入此邏輯分支。
        if (input.wasKeyPressed(GLFW_KEY_ESCAPE) && cursorCaptured) {
            // 說明：設定或更新變數的值。
            cursorCaptured = false;
            // 說明：呼叫方法執行對應功能。
            window.captureCursor(false);
            // 說明：呼叫方法執行對應功能。
            input.resetMouseTracking();
        }

        // 說明：根據條件決定是否進入此邏輯分支。
        if (!cursorCaptured && input.wasMousePressed(GLFW_MOUSE_BUTTON_LEFT)) {
            // 說明：設定或更新變數的值。
            cursorCaptured = true;
            // 說明：呼叫方法執行對應功能。
            window.captureCursor(true);
            // 說明：呼叫方法執行對應功能。
            input.resetMouseTracking();
        }

        // 說明：根據條件決定是否進入此邏輯分支。
        if (input.wasKeyPressed(GLFW_KEY_Q)) {
            // 說明：呼叫方法執行對應功能。
            window.requestClose();
        }

        // 說明：使用迴圈逐一處理每個元素或區間。
        for (int i = 0; i < hotbar.length && i < 9; i++) {
            // 說明：宣告並初始化變數。
            int key = GLFW_KEY_1 + i;
            // 說明：根據條件決定是否進入此邏輯分支。
            if (input.wasKeyPressed(key)) {
                // 說明：設定或更新變數的值。
                hotbarIndex = i;
            }
        }

        // 說明：宣告並初始化變數。
        double scroll = input.consumeScrollDeltaY();
        // 說明：根據條件決定是否進入此邏輯分支。
        if (scroll != 0.0) {
            // 說明：宣告並初始化變數。
            int direction = scroll > 0.0 ? -1 : 1;
            // 說明：設定或更新變數的值。
            hotbarIndex = Math.floorMod(hotbarIndex + direction, hotbar.length);
        }
    }

    // 說明：定義類別內部使用的方法。
    private void applyLookFromMouse() {
        // 說明：宣告並初始化變數。
        float yawDelta = (float) input.mouseDeltaX() * GameConfig.MOUSE_SENSITIVITY;
        // 說明：宣告並初始化變數。
        float pitchDelta = (float) -input.mouseDeltaY() * GameConfig.MOUSE_SENSITIVITY;
        // 說明：呼叫方法執行對應功能。
        camera.rotate(yawDelta, pitchDelta);
    }

    // 說明：定義類別內部使用的方法。
    private void updateCameraFromPlayer(float deltaSeconds) {
        // 說明：呼叫方法執行對應功能。
        updateWalkBob(deltaSeconds);

        // 說明：宣告並初始化變數。
        Vector3f right = camera.right(tmpCameraRight);
        // 說明：下一行程式碼負責執行目前步驟。
        camera.setPosition(
                // 說明：下一行程式碼負責執行目前步驟。
                player.position().x + right.x * walkBobHorizontal,
                // 說明：下一行程式碼負責執行目前步驟。
                player.position().y + GameConfig.PLAYER_EYE_HEIGHT + walkBobVertical,
                // 說明：下一行程式碼負責執行目前步驟。
                player.position().z + right.z * walkBobHorizontal
        // 說明：下一行程式碼負責執行目前步驟。
        );
    }

    // 說明：定義類別內部使用的方法。
    private void updateWalkBob(float deltaSeconds) {
        // 說明：根據條件決定是否進入此邏輯分支。
        if (deltaSeconds <= 0.0f) {
            // 說明：設定或更新變數的值。
            walkBobVertical = 0.0f;
            // 說明：設定或更新變數的值。
            walkBobHorizontal = 0.0f;
            // 說明：下一行程式碼負責執行目前步驟。
            return;
        }

        // 說明：根據條件決定是否進入此邏輯分支。
        if (!cursorCaptured || player.isFlying() || !player.isOnGround()) {
            // 說明：設定或更新變數的值。
            walkBobVertical = approach(walkBobVertical, 0.0f, WALK_BOB_RESET_SPEED * deltaSeconds);
            // 說明：設定或更新變數的值。
            walkBobHorizontal = approach(walkBobHorizontal, 0.0f, WALK_BOB_RESET_SPEED * deltaSeconds);
            // 說明：下一行程式碼負責執行目前步驟。
            return;
        }

        // 說明：宣告並初始化變數。
        float speedRatio = Math.min(player.horizontalSpeed() / Math.max(GameConfig.WALK_SPEED, 0.001f), 1.6f);
        // 說明：根據條件決定是否進入此邏輯分支。
        if (speedRatio < 0.06f) {
            // 說明：設定或更新變數的值。
            walkBobVertical = approach(walkBobVertical, 0.0f, WALK_BOB_RESET_SPEED * deltaSeconds);
            // 說明：設定或更新變數的值。
            walkBobHorizontal = approach(walkBobHorizontal, 0.0f, WALK_BOB_RESET_SPEED * deltaSeconds);
            // 說明：下一行程式碼負責執行目前步驟。
            return;
        }

        // 說明：設定或更新變數的值。
        walkBobPhase += deltaSeconds * (8.0f + 4.0f * speedRatio);
        // 說明：根據條件決定是否進入此邏輯分支。
        if (walkBobPhase > (float) (Math.PI * 2.0)) {
            // 說明：設定或更新變數的值。
            walkBobPhase -= (float) (Math.PI * 2.0);
        }

        // 說明：宣告並初始化變數。
        float amplitude = WALK_BOB_VERTICAL_BASE + WALK_BOB_VERTICAL_SCALE * speedRatio;
        // 說明：設定或更新變數的值。
        walkBobVertical = (float) Math.sin(walkBobPhase * 2.0f) * amplitude;
        // 說明：設定或更新變數的值。
        walkBobHorizontal = (float) Math.cos(walkBobPhase) * amplitude * WALK_BOB_HORIZONTAL_FACTOR;
    }

    // 說明：定義類別內部使用的方法。
    private void updateTargetBlock() {
        // 說明：呼叫方法執行對應功能。
        tmpRayOrigin.set(camera.position());
        // 說明：呼叫方法執行對應功能。
        camera.forward(tmpRayDirection);
        // 說明：設定或更新變數的值。
        targetedBlock = world.raycast(tmpRayOrigin, tmpRayDirection, GameConfig.BLOCK_REACH);
    }

    // 說明：定義類別內部使用的方法。
    private void updateBlockInteraction(float deltaSeconds) {
        // 說明：設定或更新變數的值。
        breakCooldown -= deltaSeconds;
        // 說明：設定或更新變數的值。
        placeCooldown -= deltaSeconds;

        // 說明：根據條件決定是否進入此邏輯分支。
        if (targetedBlock != null && input.wasMousePressed(GLFW_MOUSE_BUTTON_LEFT) && breakCooldown <= 0.0f) {
            // 說明：根據條件決定是否進入此邏輯分支。
            if (targetedBlock.block() != BlockType.BEDROCK) {
                // 說明：呼叫方法執行對應功能。
                world.setBlock(targetedBlock.x(), targetedBlock.y(), targetedBlock.z(), BlockType.AIR);
            }
            // 說明：設定或更新變數的值。
            breakCooldown = GameConfig.BREAK_COOLDOWN_SECONDS;
        }

        // 說明：根據條件決定是否進入此邏輯分支。
        if (targetedBlock != null && input.wasMousePressed(GLFW_MOUSE_BUTTON_RIGHT) && placeCooldown <= 0.0f) {
            // 說明：宣告並初始化變數。
            int px = targetedBlock.x() + targetedBlock.normalX();
            // 說明：宣告並初始化變數。
            int py = targetedBlock.y() + targetedBlock.normalY();
            // 說明：宣告並初始化變數。
            int pz = targetedBlock.z() + targetedBlock.normalZ();

            // 說明：宣告並初始化變數。
            BlockType current = world.getBlock(px, py, pz);
            // 說明：根據條件決定是否進入此邏輯分支。
            if ((current == BlockType.AIR || current == BlockType.WATER) && !player.intersectsBlock(px, py, pz)) {
                // 說明：呼叫方法執行對應功能。
                world.setBlock(px, py, pz, hotbar[hotbarIndex]);
            }
            // 說明：設定或更新變數的值。
            placeCooldown = GameConfig.PLACE_COOLDOWN_SECONDS;
        }
    }

    // 說明：定義類別內部使用的方法。
    private void updateDebugTitle(double now) {
        // 說明：下一行程式碼負責執行目前步驟。
        fpsFrames++;
        // 說明：根據條件決定是否進入此邏輯分支。
        if (fpsTimer == 0.0) {
            // 說明：設定或更新變數的值。
            fpsTimer = now;
        }

        // 說明：宣告並初始化變數。
        double elapsed = now - fpsTimer;
        // 說明：根據條件決定是否進入此邏輯分支。
        if (elapsed >= 1.0) {
            // 說明：宣告並初始化變數。
            int fps = (int) Math.round(fpsFrames / elapsed);
            // 說明：宣告並初始化變數。
            BlockType selected = hotbar[hotbarIndex];
            // 說明：宣告並初始化變數。
            String title = GameConfig.WINDOW_TITLE
                    // 說明：下一行程式碼負責執行目前步驟。
                    + " | " + MODE_LABEL
                    // 說明：下一行程式碼負責執行目前步驟。
                    + " | Flight: " + (player.isFlying() ? "ON" : "OFF")
                    // 說明：下一行程式碼負責執行目前步驟。
                    + " | FPS: " + fps
                    // 說明：下一行程式碼負責執行目前步驟。
                    + " | Chunks: " + world.chunkCount()
                    // 說明：呼叫方法執行對應功能。
                    + " | Block: " + selected.displayName();
            // 說明：呼叫方法執行對應功能。
            glfwSetWindowTitle(window.handle(), title);

            // 說明：設定或更新變數的值。
            fpsFrames = 0;
            // 說明：設定或更新變數的值。
            fpsTimer = now;
        }
    }

    // 說明：定義類別內部使用的方法。
    private void safeSaveWorld() {
        // 說明：下一行程式碼負責執行目前步驟。
        try {
            // 說明：呼叫方法執行對應功能。
            world.save();
        // 說明：下一行程式碼負責執行目前步驟。
        } catch (Exception e) {
            // 說明：呼叫方法執行對應功能。
            System.err.println("World save failed: " + e.getMessage());
        }
    }

    // 說明：定義類別內部使用的方法。
    private float approach(float current, float target, float delta) {
        // 說明：根據條件決定是否進入此邏輯分支。
        if (current < target) {
            // 說明：呼叫方法執行對應功能。
            return Math.min(current + delta, target);
        }
        // 說明：呼叫方法執行對應功能。
        return Math.max(current - delta, target);
    }
}

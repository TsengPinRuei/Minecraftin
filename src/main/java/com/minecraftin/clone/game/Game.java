// 宣告此檔案所屬的套件。
package com.minecraftin.clone.game;

// 匯入後續會使用到的型別或函式。
import com.minecraftin.clone.config.GameConfig;
// 匯入後續會使用到的型別或函式。
import com.minecraftin.clone.engine.Camera;
// 匯入後續會使用到的型別或函式。
import com.minecraftin.clone.engine.InputState;
// 匯入後續會使用到的型別或函式。
import com.minecraftin.clone.engine.Window;
// 匯入後續會使用到的型別或函式。
import com.minecraftin.clone.gameplay.Player;
// 匯入後續會使用到的型別或函式。
import com.minecraftin.clone.render.HudRenderer;
// 匯入後續會使用到的型別或函式。
import com.minecraftin.clone.render.WorldRenderer;
// 匯入後續會使用到的型別或函式。
import com.minecraftin.clone.world.BlockType;
// 匯入後續會使用到的型別或函式。
import com.minecraftin.clone.world.RaycastHit;
// 匯入後續會使用到的型別或函式。
import com.minecraftin.clone.world.World;
// 匯入後續會使用到的型別或函式。
import org.joml.Vector3f;

// 匯入後續會使用到的型別或函式。
import java.nio.file.Paths;

// 匯入後續會使用到的型別或函式。
import static org.lwjgl.glfw.GLFW.*;
// 匯入後續會使用到的型別或函式。
import static org.lwjgl.opengl.GL33C.*;

// 定義主要型別與其結構。
public final class Game {
    // 設定或更新變數的值。
    private static final String MODE_LABEL = GameConfig.CREATIVE_MODE_ONLY ? "CREATIVE" : "SURVIVAL";
    // 設定或更新變數的值。
    private static final float WALK_BOB_VERTICAL_BASE = 0.020f;
    // 設定或更新變數的值。
    private static final float WALK_BOB_VERTICAL_SCALE = 0.024f;
    // 設定或更新變數的值。
    private static final float WALK_BOB_HORIZONTAL_FACTOR = 0.60f;
    // 設定或更新變數的值。
    private static final float WALK_BOB_RESET_SPEED = 8.0f;

    // 下一行程式碼負責執行目前步驟。
    private final BlockType[] hotbar = {
            // 下一行程式碼負責執行目前步驟。
            BlockType.RED_BLOCK,
            // 下一行程式碼負責執行目前步驟。
            BlockType.ORANGE_BLOCK,
            // 下一行程式碼負責執行目前步驟。
            BlockType.YELLOW_BLOCK,
            // 下一行程式碼負責執行目前步驟。
            BlockType.GREEN_BLOCK,
            // 下一行程式碼負責執行目前步驟。
            BlockType.BLUE_BLOCK,
            // 下一行程式碼負責執行目前步驟。
            BlockType.PURPLE_BLOCK,
            // 下一行程式碼負責執行目前步驟。
            BlockType.DIRT,
            // 下一行程式碼負責執行目前步驟。
            BlockType.STONE,
            // 下一行程式碼負責執行目前步驟。
            BlockType.GLASS
    };

    // 設定或更新變數的值。
    private final Window window = new Window();
    // 設定或更新變數的值。
    private final InputState input = new InputState();
    // 設定或更新變數的值。
    private final Camera camera = new Camera();
    // 設定或更新變數的值。
    private final Player player = new Player();
    // 設定或更新變數的值。
    private final World world = new World(Paths.get(GameConfig.WORLD_FILE), GameConfig.DEFAULT_WORLD_SEED);
    // 設定或更新變數的值。
    private final Vector3f tmpCameraRight = new Vector3f();
    // 設定或更新變數的值。
    private final Vector3f tmpRayOrigin = new Vector3f();
    // 設定或更新變數的值。
    private final Vector3f tmpRayDirection = new Vector3f();

    // 下一行程式碼負責執行目前步驟。
    private WorldRenderer worldRenderer;
    // 下一行程式碼負責執行目前步驟。
    private HudRenderer hudRenderer;

    // 設定或更新變數的值。
    private boolean cursorCaptured = false;
    // 下一行程式碼負責執行目前步驟。
    private int hotbarIndex;

    // 下一行程式碼負責執行目前步驟。
    private float breakCooldown;
    // 下一行程式碼負責執行目前步驟。
    private float placeCooldown;
    // 下一行程式碼負責執行目前步驟。
    private float autosaveTimer;

    // 下一行程式碼負責執行目前步驟。
    private RaycastHit targetedBlock;

    // 下一行程式碼負責執行目前步驟。
    private double fpsTimer;
    // 下一行程式碼負責執行目前步驟。
    private int fpsFrames;
    // 下一行程式碼負責執行目前步驟。
    private boolean worldInitialized;
    // 下一行程式碼負責執行目前步驟。
    private boolean playerSpawnInitialized;
    // 下一行程式碼負責執行目前步驟。
    private float walkBobPhase;
    // 下一行程式碼負責執行目前步驟。
    private float walkBobVertical;
    // 下一行程式碼負責執行目前步驟。
    private float walkBobHorizontal;

    // 定義對外可呼叫的方法。
    public void run() {
        // 下一行程式碼負責執行目前步驟。
        try {
            // 呼叫方法執行對應功能。
            window.create();
            // 呼叫方法執行對應功能。
            input.attach(window.handle());

            // 呼叫方法執行對應功能。
            initGraphicsState();

            // 呼叫方法執行對應功能。
            world.initialize();
            // 設定或更新變數的值。
            worldInitialized = true;
            // 設定或更新變數的值。
            worldRenderer = new WorldRenderer();
            // 設定或更新變數的值。
            hudRenderer = new HudRenderer();

            // 宣告並初始化變數。
            // 優先使用關閉遊戲時存下的重生點，舊存檔或新世界則退回地形出生點搜尋。
            Vector3f spawn = new Vector3f();
            // 根據條件決定是否進入此邏輯分支。
            if (!world.tryGetSavedRespawnPosition(spawn)) {
                // 呼叫方法執行對應功能。
                world.defaultSpawn(spawn);
            }
            // 呼叫方法執行對應功能。
            player.setPosition(spawn.x, spawn.y, spawn.z);
            // 設定或更新變數的值。
            playerSpawnInitialized = true;
            // 呼叫方法執行對應功能。
            camera.setRotation(-90.0f, -15.0f);
            // 呼叫方法執行對應功能。
            updateCameraFromPlayer(0.0f);

            // 呼叫方法執行對應功能。
            window.captureCursor(false);
            // 呼叫方法執行對應功能。
            input.resetMouseTracking();

            // 呼叫方法執行對應功能。
            loop();
        // 下一行程式碼負責執行目前步驟。
        } finally {
            // 根據條件決定是否進入此邏輯分支。
            if (worldInitialized && playerSpawnInitialized) {
                // 將關閉遊戲當下玩家位置存成下次開啟時的重生點。
                world.setRespawnPosition(player.position().x, player.position().y, player.position().z);
            }
            // 根據條件決定是否進入此邏輯分支。
            if (worldInitialized && world.hasPendingSave()) {
                // 呼叫方法執行對應功能。
                safeSaveWorld();
            }
            // 根據條件決定是否進入此邏輯分支。
            if (hudRenderer != null) {
                // 呼叫方法執行對應功能。
                hudRenderer.close();
            }
            // 根據條件決定是否進入此邏輯分支。
            if (worldRenderer != null) {
                // 呼叫方法執行對應功能。
                worldRenderer.close();
            }
            // 呼叫方法執行對應功能。
            window.close();
        }
    }

    // 定義類別內部使用的方法。
    private void initGraphicsState() {
        // 呼叫方法執行對應功能。
        glEnable(GL_DEPTH_TEST);
        // 呼叫方法執行對應功能。
        glDepthFunc(GL_LEQUAL);

        // 呼叫方法執行對應功能。
        glEnable(GL_BLEND);
        // 呼叫方法執行對應功能。
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // 呼叫方法執行對應功能。
        glDisable(GL_CULL_FACE);
        // 呼叫方法執行對應功能。
        glEnable(GL_MULTISAMPLE);
    }

    // 定義類別內部使用的方法。
    private void loop() {
        // 宣告並初始化變數。
        double lastTime = glfwGetTime();

        // 在條件成立時重複執行此區塊。
        while (!window.shouldClose()) {
            // 宣告並初始化變數。
            double now = glfwGetTime();
            // 宣告並初始化變數。
            float delta = (float) Math.min(0.05, now - lastTime);
            // 設定或更新變數的值。
            lastTime = now;

            // 呼叫方法執行對應功能。
            window.pollEvents();
            // 呼叫方法執行對應功能。
            handleInputState();

            // 宣告並初始化變數。
            int playerChunkX = Math.floorDiv((int) Math.floor(player.position().x), GameConfig.CHUNK_SIZE);
            // 宣告並初始化變數。
            int playerChunkZ = Math.floorDiv((int) Math.floor(player.position().z), GameConfig.CHUNK_SIZE);
            // Load one extra ring so rendered chunks always have neighbor data at the edges.
            // 呼叫方法執行對應功能。
            world.ensureChunksAround(playerChunkX, playerChunkZ, GameConfig.RENDER_DISTANCE_CHUNKS + 1);

            // 根據條件決定是否進入此邏輯分支。
            if (cursorCaptured) {
                // 呼叫方法執行對應功能。
                applyLookFromMouse();
                // 呼叫方法執行對應功能。
                player.update(input, camera, world, delta);
            }

            // 呼叫方法執行對應功能。
            updateCameraFromPlayer(delta);
            // 呼叫方法執行對應功能。
            updateTargetBlock();
            // 根據條件決定是否進入此邏輯分支。
            if (cursorCaptured) {
                // 呼叫方法執行對應功能。
                updateBlockInteraction(delta);
            }

            // 呼叫方法執行對應功能。
            worldRenderer.render(world, camera, window.width(), window.height(), targetedBlock);
            // 呼叫方法執行對應功能。
            hudRenderer.render(hotbar, hotbarIndex);

            // 呼叫方法執行對應功能。
            updateDebugTitle(now);

            // 呼叫方法執行對應功能。
            window.swapBuffers();
            // 呼叫方法執行對應功能。
            input.endFrame();

            // 設定或更新變數的值。
            autosaveTimer += delta;
            // 根據條件決定是否進入此邏輯分支。
            if (autosaveTimer >= 20.0f) {
                // 根據條件決定是否進入此邏輯分支。
                if (world.hasModifiedChunks()) {
                    // 呼叫方法執行對應功能。
                    safeSaveWorld();
                }
                // 設定或更新變數的值。
                autosaveTimer = 0.0f;
            }
        }
    }

    // 定義類別內部使用的方法。
    private void handleInputState() {
        // 根據條件決定是否進入此邏輯分支。
        if (input.wasKeyPressed(GLFW_KEY_ESCAPE) && cursorCaptured) {
            // 設定或更新變數的值。
            cursorCaptured = false;
            // 呼叫方法執行對應功能。
            window.captureCursor(false);
            // 呼叫方法執行對應功能。
            input.resetMouseTracking();
        }

        // 根據條件決定是否進入此邏輯分支。
        if (!cursorCaptured && input.wasMousePressed(GLFW_MOUSE_BUTTON_LEFT)) {
            // 設定或更新變數的值。
            cursorCaptured = true;
            // 呼叫方法執行對應功能。
            window.captureCursor(true);
            // 呼叫方法執行對應功能。
            input.resetMouseTracking();
        }

        // 根據條件決定是否進入此邏輯分支。
        if (input.wasKeyPressed(GLFW_KEY_Q)) {
            // 呼叫方法執行對應功能。
            window.requestClose();
        }

        // 使用迴圈逐一處理每個元素或區間。
        for (int i = 0; i < hotbar.length && i < 9; i++) {
            // 宣告並初始化變數。
            int key = GLFW_KEY_1 + i;
            // 根據條件決定是否進入此邏輯分支。
            if (input.wasKeyPressed(key)) {
                // 設定或更新變數的值。
                hotbarIndex = i;
            }
        }

        // 宣告並初始化變數。
        double scroll = input.consumeScrollDeltaY();
        // 根據條件決定是否進入此邏輯分支。
        if (scroll != 0.0) {
            // 宣告並初始化變數。
            int direction = scroll > 0.0 ? -1 : 1;
            // 設定或更新變數的值。
            hotbarIndex = Math.floorMod(hotbarIndex + direction, hotbar.length);
        }
    }

    // 定義類別內部使用的方法。
    private void applyLookFromMouse() {
        // 宣告並初始化變數。
        float yawDelta = (float) input.mouseDeltaX() * GameConfig.MOUSE_SENSITIVITY;
        // 宣告並初始化變數。
        float pitchDelta = (float) -input.mouseDeltaY() * GameConfig.MOUSE_SENSITIVITY;
        // 呼叫方法執行對應功能。
        camera.rotate(yawDelta, pitchDelta);
    }

    // 定義類別內部使用的方法。
    private void updateCameraFromPlayer(float deltaSeconds) {
        // 呼叫方法執行對應功能。
        updateWalkBob(deltaSeconds);

        // 宣告並初始化變數。
        Vector3f right = camera.right(tmpCameraRight);
        // 下一行程式碼負責執行目前步驟。
        camera.setPosition(
                // 下一行程式碼負責執行目前步驟。
                player.position().x + right.x * walkBobHorizontal,
                // 下一行程式碼負責執行目前步驟。
                player.position().y + GameConfig.PLAYER_EYE_HEIGHT + walkBobVertical,
                // 下一行程式碼負責執行目前步驟。
                player.position().z + right.z * walkBobHorizontal
        // 下一行程式碼負責執行目前步驟。
        );
    }

    // 定義類別內部使用的方法。
    private void updateWalkBob(float deltaSeconds) {
        // 根據條件決定是否進入此邏輯分支。
        if (deltaSeconds <= 0.0f) {
            // 設定或更新變數的值。
            walkBobVertical = 0.0f;
            // 設定或更新變數的值。
            walkBobHorizontal = 0.0f;
            // 下一行程式碼負責執行目前步驟。
            return;
        }

        // 根據條件決定是否進入此邏輯分支。
        if (!cursorCaptured || player.isFlying() || !player.isOnGround()) {
            // 設定或更新變數的值。
            walkBobVertical = approach(walkBobVertical, 0.0f, WALK_BOB_RESET_SPEED * deltaSeconds);
            // 設定或更新變數的值。
            walkBobHorizontal = approach(walkBobHorizontal, 0.0f, WALK_BOB_RESET_SPEED * deltaSeconds);
            // 下一行程式碼負責執行目前步驟。
            return;
        }

        // 宣告並初始化變數。
        float speedRatio = Math.min(player.horizontalSpeed() / Math.max(GameConfig.WALK_SPEED, 0.001f), 1.6f);
        // 根據條件決定是否進入此邏輯分支。
        if (speedRatio < 0.06f) {
            // 設定或更新變數的值。
            walkBobVertical = approach(walkBobVertical, 0.0f, WALK_BOB_RESET_SPEED * deltaSeconds);
            // 設定或更新變數的值。
            walkBobHorizontal = approach(walkBobHorizontal, 0.0f, WALK_BOB_RESET_SPEED * deltaSeconds);
            // 下一行程式碼負責執行目前步驟。
            return;
        }

        // 設定或更新變數的值。
        walkBobPhase += deltaSeconds * (8.0f + 4.0f * speedRatio);
        // 根據條件決定是否進入此邏輯分支。
        if (walkBobPhase > (float) (Math.PI * 2.0)) {
            // 設定或更新變數的值。
            walkBobPhase -= (float) (Math.PI * 2.0);
        }

        // 宣告並初始化變數。
        float amplitude = WALK_BOB_VERTICAL_BASE + WALK_BOB_VERTICAL_SCALE * speedRatio;
        // 設定或更新變數的值。
        walkBobVertical = (float) Math.sin(walkBobPhase * 2.0f) * amplitude;
        // 設定或更新變數的值。
        walkBobHorizontal = (float) Math.cos(walkBobPhase) * amplitude * WALK_BOB_HORIZONTAL_FACTOR;
    }

    // 定義類別內部使用的方法。
    private void updateTargetBlock() {
        // 呼叫方法執行對應功能。
        tmpRayOrigin.set(camera.position());
        // 呼叫方法執行對應功能。
        camera.forward(tmpRayDirection);
        // 設定或更新變數的值。
        targetedBlock = world.raycast(tmpRayOrigin, tmpRayDirection, GameConfig.BLOCK_REACH);
    }

    // 定義類別內部使用的方法。
    private void updateBlockInteraction(float deltaSeconds) {
        // 設定或更新變數的值。
        breakCooldown -= deltaSeconds;
        // 設定或更新變數的值。
        placeCooldown -= deltaSeconds;

        // 根據條件決定是否進入此邏輯分支。
        if (targetedBlock != null && input.wasMousePressed(GLFW_MOUSE_BUTTON_LEFT) && breakCooldown <= 0.0f) {
            // 根據條件決定是否進入此邏輯分支。
            if (targetedBlock.block() != BlockType.BEDROCK) {
                // 呼叫方法執行對應功能。
                world.setBlock(targetedBlock.x(), targetedBlock.y(), targetedBlock.z(), BlockType.AIR);
            }
            // 設定或更新變數的值。
            breakCooldown = GameConfig.BREAK_COOLDOWN_SECONDS;
        }

        // 根據條件決定是否進入此邏輯分支。
        if (targetedBlock != null && input.wasMousePressed(GLFW_MOUSE_BUTTON_RIGHT) && placeCooldown <= 0.0f) {
            // 宣告並初始化變數。
            int px = targetedBlock.x() + targetedBlock.normalX();
            // 宣告並初始化變數。
            int py = targetedBlock.y() + targetedBlock.normalY();
            // 宣告並初始化變數。
            int pz = targetedBlock.z() + targetedBlock.normalZ();

            // 宣告並初始化變數。
            BlockType current = world.getBlock(px, py, pz);
            // 根據條件決定是否進入此邏輯分支。
            if ((current == BlockType.AIR || current == BlockType.WATER) && !player.intersectsBlock(px, py, pz)) {
                // 呼叫方法執行對應功能。
                world.setBlock(px, py, pz, hotbar[hotbarIndex]);
            }
            // 設定或更新變數的值。
            placeCooldown = GameConfig.PLACE_COOLDOWN_SECONDS;
        }
    }

    // 定義類別內部使用的方法。
    private void updateDebugTitle(double now) {
        // 下一行程式碼負責執行目前步驟。
        fpsFrames++;
        // 根據條件決定是否進入此邏輯分支。
        if (fpsTimer == 0.0) {
            // 設定或更新變數的值。
            fpsTimer = now;
        }

        // 宣告並初始化變數。
        double elapsed = now - fpsTimer;
        // 根據條件決定是否進入此邏輯分支。
        if (elapsed >= 1.0) {
            // 宣告並初始化變數。
            int fps = (int) Math.round(fpsFrames / elapsed);
            // 宣告並初始化變數。
            BlockType selected = hotbar[hotbarIndex];
            // 宣告並初始化變數。
            String title = GameConfig.WINDOW_TITLE
                    // 下一行程式碼負責執行目前步驟。
                    + " | " + MODE_LABEL
                    // 下一行程式碼負責執行目前步驟。
                    + " | Flight: " + (player.isFlying() ? "ON" : "OFF")
                    // 下一行程式碼負責執行目前步驟。
                    + " | FPS: " + fps
                    // 下一行程式碼負責執行目前步驟。
                    + " | Chunks: " + world.chunkCount()
                    // 呼叫方法執行對應功能。
                    + " | Block: " + selected.displayName();
            // 呼叫方法執行對應功能。
            glfwSetWindowTitle(window.handle(), title);

            // 設定或更新變數的值。
            fpsFrames = 0;
            // 設定或更新變數的值。
            fpsTimer = now;
        }
    }

    // 定義類別內部使用的方法。
    private void safeSaveWorld() {
        // 下一行程式碼負責執行目前步驟。
        try {
            // 呼叫方法執行對應功能。
            world.save();
        // 下一行程式碼負責執行目前步驟。
        } catch (Exception e) {
            // 呼叫方法執行對應功能。
            System.err.println("World save failed: " + e.getMessage());
        }
    }

    // 定義類別內部使用的方法。
    private float approach(float current, float target, float delta) {
        // 根據條件決定是否進入此邏輯分支。
        if (current < target) {
            // 呼叫方法執行對應功能。
            return Math.min(current + delta, target);
        }
        // 呼叫方法執行對應功能。
        return Math.max(current - delta, target);
    }
}

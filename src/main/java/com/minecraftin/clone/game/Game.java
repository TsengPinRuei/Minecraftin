package com.minecraftin.clone.game;

import com.minecraftin.clone.config.GameConfig;
import com.minecraftin.clone.engine.Camera;
import com.minecraftin.clone.engine.InputState;
import com.minecraftin.clone.engine.Window;
import com.minecraftin.clone.gameplay.Player;
import com.minecraftin.clone.render.HudRenderer;
import com.minecraftin.clone.render.WorldRenderer;
import com.minecraftin.clone.world.BlockType;
import com.minecraftin.clone.world.RaycastHit;
import com.minecraftin.clone.world.World;
import org.joml.Vector3f;

import java.nio.file.Paths;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;

public final class Game {
    // 顯示目前遊戲模式的文字
    private static final String MODE_LABEL = GameConfig.CREATIVE_MODE_ONLY ? "CREATIVE" : "SURVIVAL";

    // 角色走路時，鏡頭上下左右晃動用的參數
    private static final float WALK_BOB_VERTICAL_BASE = 0.020f;
    private static final float WALK_BOB_VERTICAL_SCALE = 0.024f;
    private static final float WALK_BOB_HORIZONTAL_FACTOR = 0.60f;
    private static final float WALK_BOB_RESET_SPEED = 8.0f;

    // 快捷欄中可選擇的方塊
    private final BlockType[] hotbar = {
            BlockType.RED_BLOCK,
            BlockType.ORANGE_BLOCK,
            BlockType.YELLOW_BLOCK,
            BlockType.GREEN_BLOCK,
            BlockType.BLUE_BLOCK,
            BlockType.PURPLE_BLOCK,
            BlockType.DIRT,
            BlockType.STONE,
            BlockType.GLASS
    };

    // 遊戲會用到的核心物件
    private final Window window = new Window();
    private final InputState input = new InputState();
    private final Camera camera = new Camera();
    private final Player player = new Player();
    private final World world = new World(Paths.get(GameConfig.WORLD_FILE), GameConfig.DEFAULT_WORLD_SEED);

    // 暫存向量，避免重複建立物件
    private final Vector3f tmpCameraRight = new Vector3f();
    private final Vector3f tmpRayOrigin = new Vector3f();
    private final Vector3f tmpRayDirection = new Vector3f();

    // 負責世界與 HUD 的渲染器
    private WorldRenderer worldRenderer;
    private HudRenderer hudRenderer;

    // 是否已鎖定滑鼠到遊戲視窗內
    private boolean cursorCaptured = false;

    // 目前快捷欄選到的方塊索引
    private int hotbarIndex;

    // 破壞、放置方塊與自動存檔的冷卻或計時
    private float breakCooldown;
    private float placeCooldown;
    private float autosaveTimer;

    // 目前準星指向的方塊資訊
    private RaycastHit targetedBlock;

    // FPS 計算用
    private double fpsTimer;
    private int fpsFrames;

    // 用來確認世界與玩家出生點是否已完成初始化
    private boolean worldInitialized;
    private boolean playerSpawnInitialized;

    // 走路時鏡頭晃動的狀態
    private float walkBobPhase;
    private float walkBobVertical;
    private float walkBobHorizontal;

    public void run() {
        try {
            // 建立視窗並綁定輸入控制
            window.create();
            input.attach(window.handle());

            // 初始化 OpenGL 狀態
            initGraphicsState();

            // 載入或建立世界
            world.initialize();
            worldInitialized = true;

            // 建立渲染器
            worldRenderer = new WorldRenderer();
            hudRenderer = new HudRenderer();

            // 先嘗試讀取上次離開時的重生點，失敗則使用預設出生點
            Vector3f spawn = new Vector3f();
            if (!world.tryGetSavedRespawnPosition(spawn)) {
                world.defaultSpawn(spawn);
            }

            // 設定玩家初始位置與鏡頭角度
            player.setPosition(spawn.x, spawn.y, spawn.z);
            playerSpawnInitialized = true;
            camera.setRotation(-90.0f, -15.0f);
            updateCameraFromPlayer(0.0f);

            // 一開始不鎖定滑鼠
            window.captureCursor(false);
            input.resetMouseTracking();

            // 進入遊戲主迴圈
            loop();
        } finally {
            // 結束前記錄玩家目前位置，作為下次重生點
            if (worldInitialized && playerSpawnInitialized) {
                world.setRespawnPosition(player.position().x, player.position().y, player.position().z);
            }

            // 若世界有尚未存檔的變更，離開前先存檔
            if (worldInitialized && world.hasPendingSave()) {
                safeSaveWorld();
            }

            // 依序釋放資源
            if (hudRenderer != null) {
                hudRenderer.close();
            }
            if (worldRenderer != null) {
                worldRenderer.close();
            }
            window.close();
        }
    }

    private void initGraphicsState() {
        // 開啟深度測試，讓前後物體能正確遮擋
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        // 開啟透明混合
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // 關閉背面剔除，並開啟多重採樣抗鋸齒
        glDisable(GL_CULL_FACE);
        glEnable(GL_MULTISAMPLE);
    }

    private void loop() {
        // 記錄上一幀時間，用來計算 delta time
        double lastTime = glfwGetTime();

        while (!window.shouldClose()) {
            double now = glfwGetTime();

            // 計算這一幀經過的秒數，並限制最大值避免卡頓時數值過大
            float delta = (float) Math.min(0.05, now - lastTime);
            lastTime = now;

            // 處理視窗事件與輸入
            window.pollEvents();
            handleInputState();

            // 根據玩家所在區塊，確保附近的地圖區塊都有載入
            int playerChunkX = Math.floorDiv((int) Math.floor(player.position().x), GameConfig.CHUNK_SIZE);
            int playerChunkZ = Math.floorDiv((int) Math.floor(player.position().z), GameConfig.CHUNK_SIZE);
            world.ensureChunksAround(playerChunkX, playerChunkZ, GameConfig.RENDER_DISTANCE_CHUNKS + 1);

            // 只有在滑鼠被鎖定時，才允許玩家控制鏡頭與移動
            if (cursorCaptured) {
                applyLookFromMouse();
                player.update(input, camera, world, delta);
            }

            // 更新鏡頭位置、目標方塊與互動
            updateCameraFromPlayer(delta);
            updateTargetBlock();
            if (cursorCaptured) {
                updateBlockInteraction(delta);
            }

            // 渲染世界與快捷欄
            worldRenderer.render(world, camera, window.width(), window.height(), targetedBlock);
            hudRenderer.render(hotbar, hotbarIndex);

            // 更新視窗標題中的偵錯資訊
            updateDebugTitle(now);

            // 顯示畫面並結束本幀輸入狀態
            window.swapBuffers();
            input.endFrame();

            // 每隔一段時間自動存檔一次
            autosaveTimer += delta;
            if (autosaveTimer >= 20.0f) {
                if (world.hasModifiedChunks()) {
                    safeSaveWorld();
                }
                autosaveTimer = 0.0f;
            }
        }
    }

    private void handleInputState() {
        // 按 ESC 時解除滑鼠鎖定
        if (input.wasKeyPressed(GLFW_KEY_ESCAPE) && cursorCaptured) {
            cursorCaptured = false;
            window.captureCursor(false);
            input.resetMouseTracking();
        }

        // 尚未鎖定滑鼠時，點左鍵可進入遊戲控制模式
        if (!cursorCaptured && input.wasMousePressed(GLFW_MOUSE_BUTTON_LEFT)) {
            cursorCaptured = true;
            window.captureCursor(true);
            input.resetMouseTracking();
        }

        // 按 Q 關閉遊戲
        if (input.wasKeyPressed(GLFW_KEY_Q)) {
            window.requestClose();
        }

        // 按數字鍵 1 到 9 切換快捷欄
        for (int i = 0; i < hotbar.length && i < 9; i++) {
            int key = GLFW_KEY_1 + i;
            if (input.wasKeyPressed(key)) {
                hotbarIndex = i;
            }
        }

        // 滑鼠滾輪可切換快捷欄
        double scroll = input.consumeScrollDeltaY();
        if (scroll != 0.0) {
            int direction = scroll > 0.0 ? -1 : 1;
            hotbarIndex = Math.floorMod(hotbarIndex + direction, hotbar.length);
        }
    }

    private void applyLookFromMouse() {
        // 根據滑鼠移動距離調整鏡頭角度
        float yawDelta = (float) input.mouseDeltaX() * GameConfig.MOUSE_SENSITIVITY;
        float pitchDelta = (float) -input.mouseDeltaY() * GameConfig.MOUSE_SENSITIVITY;
        camera.rotate(yawDelta, pitchDelta);
    }

    private void updateCameraFromPlayer(float deltaSeconds) {
        // 先更新走路晃動效果
        updateWalkBob(deltaSeconds);

        // 鏡頭位置跟著玩家移動，並套用上下左右晃動
        Vector3f right = camera.right(tmpCameraRight);
        camera.setPosition(
                player.position().x + right.x * walkBobHorizontal,
                player.position().y + GameConfig.PLAYER_EYE_HEIGHT + walkBobVertical,
                player.position().z + right.z * walkBobHorizontal);
    }

    private void updateWalkBob(float deltaSeconds) {
        // 沒有時間差時，不需要更新晃動
        if (deltaSeconds <= 0.0f) {
            walkBobVertical = 0.0f;
            walkBobHorizontal = 0.0f;
            return;
        }

        // 未進入控制模式、正在飛行或不在地面時，晃動逐漸回到 0
        if (!cursorCaptured || player.isFlying() || !player.isOnGround()) {
            walkBobVertical = approach(walkBobVertical, 0.0f, WALK_BOB_RESET_SPEED * deltaSeconds);
            walkBobHorizontal = approach(walkBobHorizontal, 0.0f, WALK_BOB_RESET_SPEED * deltaSeconds);
            return;
        }

        // 用玩家水平移動速度決定晃動強度
        float speedRatio = Math.min(player.horizontalSpeed() / Math.max(GameConfig.WALK_SPEED, 0.001f), 1.6f);

        // 幾乎沒在移動時，晃動逐漸回到 0
        if (speedRatio < 0.06f) {
            walkBobVertical = approach(walkBobVertical, 0.0f, WALK_BOB_RESET_SPEED * deltaSeconds);
            walkBobHorizontal = approach(walkBobHorizontal, 0.0f, WALK_BOB_RESET_SPEED * deltaSeconds);
            return;
        }

        // 更新晃動週期
        walkBobPhase += deltaSeconds * (8.0f + 4.0f * speedRatio);
        if (walkBobPhase > (float) (Math.PI * 2.0)) {
            walkBobPhase -= (float) (Math.PI * 2.0);
        }

        // 根據週期計算上下與左右的晃動量
        float amplitude = WALK_BOB_VERTICAL_BASE + WALK_BOB_VERTICAL_SCALE * speedRatio;
        walkBobVertical = (float) Math.sin(walkBobPhase * 2.0f) * amplitude;
        walkBobHorizontal = (float) Math.cos(walkBobPhase) * amplitude * WALK_BOB_HORIZONTAL_FACTOR;
    }

    private void updateTargetBlock() {
        // 從鏡頭位置往前發射射線，找出玩家目前指到的方塊
        tmpRayOrigin.set(camera.position());
        camera.forward(tmpRayDirection);
        targetedBlock = world.raycast(tmpRayOrigin, tmpRayDirection, GameConfig.BLOCK_REACH);
    }

    private void updateBlockInteraction(float deltaSeconds) {
        // 更新破壞與放置方塊的冷卻時間
        breakCooldown -= deltaSeconds;
        placeCooldown -= deltaSeconds;

        // 左鍵破壞方塊，但不能破壞基岩
        if (targetedBlock != null && input.wasMousePressed(GLFW_MOUSE_BUTTON_LEFT) && breakCooldown <= 0.0f) {
            if (targetedBlock.block() != BlockType.BEDROCK) {
                world.setBlock(targetedBlock.x(), targetedBlock.y(), targetedBlock.z(), BlockType.AIR);
            }
            breakCooldown = GameConfig.BREAK_COOLDOWN_SECONDS;
        }

        // 右鍵在目標方塊旁邊放置新方塊
        if (targetedBlock != null && input.wasMousePressed(GLFW_MOUSE_BUTTON_RIGHT) && placeCooldown <= 0.0f) {
            int px = targetedBlock.x() + targetedBlock.normalX();
            int py = targetedBlock.y() + targetedBlock.normalY();
            int pz = targetedBlock.z() + targetedBlock.normalZ();

            BlockType current = world.getBlock(px, py, pz);

            // 只能放在空氣或水的位置，且不能和玩家身體重疊
            if ((current == BlockType.AIR || current == BlockType.WATER) && !player.intersectsBlock(px, py, pz)) {
                world.setBlock(px, py, pz, hotbar[hotbarIndex]);
            }

            placeCooldown = GameConfig.PLACE_COOLDOWN_SECONDS;
        }
    }

    private void updateDebugTitle(double now) {
        // 每幀都累計一次，用來計算 FPS
        fpsFrames++;

        if (fpsTimer == 0.0) {
            fpsTimer = now;
        }

        double elapsed = now - fpsTimer;

        // 每秒更新一次視窗標題
        if (elapsed >= 1.0) {
            int fps = (int) Math.round(fpsFrames / elapsed);
            BlockType selected = hotbar[hotbarIndex];

            String title = GameConfig.WINDOW_TITLE
                    + " | " + MODE_LABEL
                    + " | Flight: " + (player.isFlying() ? "ON" : "OFF")
                    + " | FPS: " + fps
                    + " | Chunks: " + world.chunkCount()
                    + " | Block: " + selected.displayName();

            glfwSetWindowTitle(window.handle(), title);

            // 重設 FPS 計算
            fpsFrames = 0;
            fpsTimer = now;
        }
    }

    private void safeSaveWorld() {
        // 儲存世界時若發生錯誤，避免讓遊戲直接崩潰
        try {
            world.save();
        } catch (Exception e) {
            System.err.println("World save failed: " + e.getMessage());
        }
    }

    private float approach(float current, float target, float delta) {
        // 讓 current 逐步接近 target，但每次最多只改變 delta
        if (current < target) {
            return Math.min(current + delta, target);
        }
        return Math.max(current - delta, target);
    }
}
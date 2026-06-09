package com.minecraftin.clone.gameplay;

import com.minecraftin.clone.config.GameConfig;
import com.minecraftin.clone.engine.Camera;
import com.minecraftin.clone.engine.InputState;
import com.minecraftin.clone.world.BlockBounds;
import com.minecraftin.clone.world.BlockType;
import com.minecraftin.clone.world.World;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

// 維護玩家位置、速度與碰撞箱，並把輸入轉成地面移動或創造模式飛行。
public final class Player {
    // 每次位移切成更小的步驟，避免高速度或低 FPS 時直接穿過一格方塊。
    private static final float COLLISION_STEP = 0.04f;

    // 碰撞箱取樣時的微小邊界，避免剛好貼齊方塊邊界時因浮點誤差卡牆。
    private static final float EPSILON = 0.001f;

    // 玩家水平移動時可自動踏上的高度；讓樓梯與半磚有接近 Minecraft 的走上去手感。
    private static final float STEP_HEIGHT = 0.58f;

    // 將踏階高度分段測試，避免一次抬太高穿過較薄的碰撞盒。
    private static final int STEP_ATTEMPTS = 8;

    // 梯子沒有碰撞盒，因此用接觸範圍判斷玩家是否正在梯子上。
    private static final float LADDER_TOUCH_MARGIN = 0.20f;
    private static final float LADDER_CLIMB_SPEED = 3.2f;

    // 蹲下會降低碰撞箱與視角，並放慢水平移動。
    private static final float CROUCH_HEIGHT = 1.45f;
    private static final float CROUCH_EYE_HEIGHT = 1.25f;
    private static final float CROUCH_SPEED_MULTIPLIER = 0.42f;

    // 水中移動不套用一般重力，而是用較慢的游泳速度與輕微上浮。
    private static final float WATER_SWIM_SPEED = 2.45f;
    private static final float WATER_VERTICAL_SPEED = 2.8f;
    private static final float WATER_FLOAT_SPEED = 0.55f;
    private static final float WATER_ACCEL = 12.0f;
    private static final float WATER_DRAG = 8.0f;

    // 兩次按空白鍵的最大間隔，超過就不算雙擊
    private static final float DOUBLE_TAP_SECONDS = 0.28f;

    // 角色目前位置
    private final Vector3f position = new Vector3f();

    // 角色目前速度
    private final Vector3f velocity = new Vector3f();

    // 暫存玩家想移動的方向，避免一直建立新物件
    private final Vector3f tmpWish = new Vector3f();

    // 暫存相機前方方向
    private final Vector3f tmpForward = new Vector3f();

    // 暫存相機右方方向
    private final Vector3f tmpRight = new Vector3f();

    // 是否啟用創造模式
    private final boolean creativeMode = GameConfig.CREATIVE_MODE_ONLY;

    // 是否站在地面上
    private boolean onGround;

    // 是否正在飛行
    private boolean flying;

    // 是否正在蹲下
    private boolean crouching;

    // 距離上次按下空白鍵已經過了多久
    private float timeSinceLastSpaceTap = Float.POSITIVE_INFINITY;

    // 回傳角色目前位置
    public Vector3f position() {
        return position;
    }

    // 回傳角色是否正在飛行
    public boolean isFlying() {
        return flying;
    }

    // 回傳角色是否站在地面
    public boolean isOnGround() {
        return onGround;
    }

    public boolean isCrouching() {
        return crouching;
    }

    public float eyeHeight() {
        return crouching ? CROUCH_EYE_HEIGHT : GameConfig.PLAYER_EYE_HEIGHT;
    }

    // 計算水平移動速度，只看 x 和 z，不看上下速度
    public float horizontalSpeed() {
        return (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
    }

    // 直接設定角色位置，並重置移動狀態
    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        velocity.zero();
        onGround = false;
        flying = false;
        crouching = false;
        timeSinceLastSpaceTap = Float.POSITIVE_INFINITY;
    }

    // 取得角色眼睛位置，通常用於相機或視角起點
    public Vector3f eyePosition(Vector3f out) {
        out.set(position.x, position.y + eyeHeight(), position.z);
        return out;
    }

    // 每一幀更新角色狀態；呼叫前 Game 已經確保附近 Chunk 載入，碰撞查詢才不會邊移動邊生成遠處地形。
    public void update(InputState input, Camera camera, World world, float deltaSeconds) {
        // 先嘗試把角色從方塊內推出來，避免出生或移動後卡進牆裡
        resolveIntersections(world);

        // 累加距離上次按空白鍵的時間
        timeSinceLastSpaceTap += deltaSeconds;

        boolean spacePressed = input.wasKeyPressed(GLFW_KEY_SPACE);
        boolean swimmingBeforeFlightToggle = !flying && isInWater(world);

        // 創造模式下，雙擊空白鍵可切換飛行
        if (creativeMode && spacePressed && !swimmingBeforeFlightToggle) {
            if (timeSinceLastSpaceTap <= DOUBLE_TAP_SECONDS) {
                flying = !flying;
                timeSinceLastSpaceTap = Float.POSITIVE_INFINITY;

                // 進入飛行時，取消落地狀態並清除上下速度
                if (flying) {
                    onGround = false;
                    velocity.y = 0.0f;
                }
            } else {
                // 第一次按空白鍵，開始計時等待第二次點擊
                timeSinceLastSpaceTap = 0.0f;
            }
        }

        // 飛行模式和一般地面模式分開處理，因為飛行可使用相機完整 3D 方向，地面移動只取水平分量。
        if (creativeMode && flying) {
            updateCrouchState(input, world, false);
            updateCreative(input, camera, world, deltaSeconds);
            return;
        }

        updateGrounded(input, camera, world, deltaSeconds, spacePressed);
    }

    // 如果角色一開始就卡進方塊，往上嘗試移動直到脫離碰撞；常見於載入舊存檔或地形重新生成後。
    private void resolveIntersections(World world) {
        if (!collides(world, position.x, position.y, position.z)) {
            return;
        }

        float originalY = position.y;

        // 最多往上嘗試 24 次，每次抬高 0.125
        for (int i = 1; i <= 24; i++) {
            float candidateY = originalY + i * 0.125f;

            if (!collides(world, position.x, candidateY, position.z)) {
                position.y = candidateY;
                velocity.y = 0.0f;
                onGround = false;
                return;
            }
        }
    }

    // 飛行模式下的移動更新
    private void updateCreative(InputState input, Camera camera, World world, float deltaSeconds) {
        Vector3f wish = tmpWish.zero();
        Vector3f forward = camera.forward(tmpForward);
        Vector3f right = camera.right(tmpRight);

        // 根據按鍵累加想移動的方向
        if (input.isKeyDown(GLFW_KEY_W)) {
            wish.add(forward);
        }
        if (input.isKeyDown(GLFW_KEY_S)) {
            wish.sub(forward);
        }
        if (input.isKeyDown(GLFW_KEY_D)) {
            wish.add(right);
        }
        if (input.isKeyDown(GLFW_KEY_A)) {
            wish.sub(right);
        }
        if (input.isKeyDown(GLFW_KEY_SPACE)) {
            wish.add(0.0f, 1.0f, 0.0f);
        }
        if (input.isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
            wish.add(0.0f, -1.0f, 0.0f);
        }

        // 預設飛行速度，按住 Ctrl 可加速
        float targetSpeed = GameConfig.FLY_SPEED;
        if (input.isKeyDown(GLFW_KEY_LEFT_CONTROL)) {
            targetSpeed *= GameConfig.SPRINT_MULTIPLIER;
        }

        // 將方向向量正規化後乘上目標速度
        if (wish.lengthSquared() > 0.0001f) {
            wish.normalize(targetSpeed);
        } else {
            wish.zero();
        }

        // 讓目前速度逐漸接近目標速度，避免瞬間改變太生硬
        float accel = 28.0f;
        velocity.x = approach(velocity.x, wish.x, accel * deltaSeconds);
        velocity.y = approach(velocity.y, wish.y, accel * deltaSeconds);
        velocity.z = approach(velocity.z, wish.z, accel * deltaSeconds);

        // 分三個軸移動，讓碰撞後只清掉撞到方向的速度，而不影響其他方向的滑動。
        moveOnAxis(world, velocity.x * deltaSeconds, 0.0f, 0.0f);
        moveOnAxis(world, 0.0f, velocity.y * deltaSeconds, 0.0f);
        moveOnAxis(world, 0.0f, 0.0f, velocity.z * deltaSeconds);

        // 飛行時不算站在地面
        onGround = false;
    }

    // 一般走路、跳躍、重力模式下的移動更新
    private void updateGrounded(InputState input, Camera camera, World world, float deltaSeconds,
            boolean spacePressed) {
        Vector3f wish = tmpWish.zero();

        // 取得相機前方方向，但只保留水平分量
        Vector3f forward = camera.forward(tmpForward);
        forward.y = 0.0f;
        if (forward.lengthSquared() > 0.0001f) {
            forward.normalize();
        }

        // 取得相機右方方向，但只保留水平分量
        Vector3f right = camera.right(tmpRight);
        right.y = 0.0f;
        if (right.lengthSquared() > 0.0001f) {
            right.normalize();
        }

        boolean touchingLadder = isTouchingLadder(world);
        boolean inWater = !touchingLadder && isInWater(world);
        updateCrouchState(input, world, !touchingLadder && !inWater);

        // 根據按鍵決定角色想前後左右移動的方向
        if (input.isKeyDown(GLFW_KEY_W)) {
            wish.add(forward);
        }
        if (input.isKeyDown(GLFW_KEY_S)) {
            wish.sub(forward);
        }
        if (input.isKeyDown(GLFW_KEY_D)) {
            wish.add(right);
        }
        if (input.isKeyDown(GLFW_KEY_A)) {
            wish.sub(right);
        }

        // 預設走路速度，按住 Ctrl 可加速
        float targetSpeed = inWater ? WATER_SWIM_SPEED : GameConfig.WALK_SPEED;
        if (!inWater && crouching) {
            targetSpeed *= CROUCH_SPEED_MULTIPLIER;
        } else if (input.isKeyDown(GLFW_KEY_LEFT_CONTROL)) {
            targetSpeed *= GameConfig.SPRINT_MULTIPLIER;
        }

        if (wish.lengthSquared() > 0.0001f) {
            // 有移動輸入時，把方向轉成固定速度
            wish.normalize(targetSpeed);

            // 在地上加速比較快，空中加速比較慢
            float accel = inWater ? WATER_ACCEL : (onGround ? 34.0f : 10.0f);
            velocity.x = approach(velocity.x, wish.x, accel * deltaSeconds);
            velocity.z = approach(velocity.z, wish.z, accel * deltaSeconds);
        } else if (inWater) {
            float drag = WATER_DRAG * deltaSeconds;
            velocity.x = approach(velocity.x, 0.0f, drag);
            velocity.z = approach(velocity.z, 0.0f, drag);
        } else if (onGround) {
            // 沒有輸入而且在地上時，套用摩擦力讓角色慢慢停下來
            float friction = 20.0f * deltaSeconds;
            velocity.x = approach(velocity.x, 0.0f, friction);
            velocity.z = approach(velocity.z, 0.0f, friction);
        }

        if (touchingLadder) {
            // 梯子不使用實心碰撞盒，因此用接觸判定進入爬梯狀態，再用 W/Space 與 S/Shift 控制上下。
            float climbInput = 0.0f;
            if (input.isKeyDown(GLFW_KEY_W) || input.isKeyDown(GLFW_KEY_SPACE)) {
                climbInput += 1.0f;
            }
            if (input.isKeyDown(GLFW_KEY_S) || input.isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
                climbInput -= 1.0f;
            }

            velocity.y = climbInput * LADDER_CLIMB_SPEED;
            moveOnAxis(world, velocity.x * deltaSeconds, 0.0f, 0.0f);
            onGround = false;
            moveOnAxis(world, 0.0f, velocity.y * deltaSeconds, 0.0f);
            moveOnAxis(world, 0.0f, 0.0f, velocity.z * deltaSeconds);
            return;
        }

        if (inWater) {
            updateSwimming(input, world, deltaSeconds);
            return;
        }

        // 在地面按空白鍵時跳躍
        if (onGround && spacePressed) {
            velocity.y = GameConfig.JUMP_VELOCITY;
            onGround = false;
        }

        // 套用重力
        velocity.y -= GameConfig.GRAVITY * deltaSeconds;

        // 限制最大下落速度，避免掉太快
        if (velocity.y < -65.0f) {
            velocity.y = -65.0f;
        }

        // 依序處理各軸，讓碰撞行為接近 AABB 滑牆：某一軸被擋住時，其他軸仍可繼續前進。
        moveOnAxis(world, velocity.x * deltaSeconds, 0.0f, 0.0f);

        // 每幀先重設落地狀態，只有向下移動時真的撞到方塊才重新標記為在地面。
        onGround = false;

        moveOnAxis(world, 0.0f, velocity.y * deltaSeconds, 0.0f);
        moveOnAxis(world, 0.0f, 0.0f, velocity.z * deltaSeconds);

        // 防止角色掉到地圖底部以下
        if (position.y < 1.1f) {
            position.y = 1.1f;
            velocity.y = Math.max(0.0f, velocity.y);
            onGround = true;
        }
    }

    // 判斷角色碰撞箱是否和指定方塊相交
    public boolean intersectsBlock(int x, int y, int z) {
        return intersectsBlock(x, y, z, BlockType.STONE);
    }

    // 判斷角色碰撞箱是否和指定方塊種類的碰撞盒相交。
    public boolean intersectsBlock(int x, int y, int z, BlockType type) {
        float half = GameConfig.PLAYER_WIDTH * 0.5f;
        float minX = position.x - half;
        float maxX = position.x + half;
        float minY = position.y;
        float maxY = position.y + playerHeight();
        float minZ = position.z - half;
        float maxZ = position.z + half;

        for (BlockBounds bounds : type.collisionBoxes()) {
            if (bounds.intersectsWorldBox(x, y, z, minX, minY, minZ, maxX, maxY, maxZ)) {
                return true;
            }
        }
        return false;
    }

    // 檢查玩家加上一點外擴範圍後是否碰到梯子的 render box；比只看玩家中心點更容易抓到貼邊爬梯。
    private boolean isTouchingLadder(World world) {
        float half = GameConfig.PLAYER_WIDTH * 0.5f;
        float minX = position.x - half - LADDER_TOUCH_MARGIN;
        float maxX = position.x + half + LADDER_TOUCH_MARGIN;
        float minY = position.y + EPSILON;
        float maxY = position.y + playerHeight() - EPSILON;
        float minZ = position.z - half - LADDER_TOUCH_MARGIN;
        float maxZ = position.z + half + LADDER_TOUCH_MARGIN;

        int blockMinX = fastFloor(minX);
        int blockMaxX = fastFloor(maxX);
        int blockMinY = fastFloor(minY);
        int blockMaxY = fastFloor(maxY);
        int blockMinZ = fastFloor(minZ);
        int blockMaxZ = fastFloor(maxZ);

        for (int by = blockMinY; by <= blockMaxY; by++) {
            for (int bz = blockMinZ; bz <= blockMaxZ; bz++) {
                for (int bx = blockMinX; bx <= blockMaxX; bx++) {
                    BlockType block = world.getBlock(bx, by, bz);
                    if (!block.isLadderBlock()) {
                        continue;
                    }

                    for (BlockBounds bounds : block.renderBoxes()) {
                        if (bounds.intersectsWorldBox(bx, by, bz, minX, minY, minZ, maxX, maxY, maxZ)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private void updateCrouchState(InputState input, World world, boolean canCrouch) {
        if (canCrouch && input.isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
            crouching = true;
            return;
        }

        if (crouching && !collidesWithHeight(world, position.x, position.y, position.z, GameConfig.PLAYER_HEIGHT)) {
            crouching = false;
        }
    }

    private void updateSwimming(InputState input, World world, float deltaSeconds) {
        float verticalInput = 0.0f;
        if (input.isKeyDown(GLFW_KEY_SPACE)) {
            verticalInput += 1.0f;
        }
        if (input.isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
            verticalInput -= 1.0f;
        }

        float targetVertical = verticalInput != 0.0f ? verticalInput * WATER_VERTICAL_SPEED : WATER_FLOAT_SPEED;
        velocity.y = approach(velocity.y, targetVertical, WATER_ACCEL * deltaSeconds);

        moveOnAxis(world, velocity.x * deltaSeconds, 0.0f, 0.0f);
        onGround = false;
        moveOnAxis(world, 0.0f, velocity.y * deltaSeconds, 0.0f);
        moveOnAxis(world, 0.0f, 0.0f, velocity.z * deltaSeconds);
    }

    private boolean isInWater(World world) {
        float half = GameConfig.PLAYER_WIDTH * 0.5f;
        float minWorldX = position.x - half + EPSILON;
        float minWorldY = position.y + 0.08f;
        float minWorldZ = position.z - half + EPSILON;
        float maxWorldX = position.x + half - EPSILON;
        float maxWorldY = position.y + playerHeight() - EPSILON;
        float maxWorldZ = position.z + half - EPSILON;

        int minX = fastFloor(minWorldX);
        int maxX = fastFloor(maxWorldX);
        int minY = fastFloor(minWorldY);
        int maxY = fastFloor(maxWorldY);
        int minZ = fastFloor(minWorldZ);
        int maxZ = fastFloor(maxWorldZ);

        for (int by = minY; by <= maxY; by++) {
            for (int bz = minZ; bz <= maxZ; bz++) {
                for (int bx = minX; bx <= maxX; bx++) {
                    if (world.getBlock(bx, by, bz) == BlockType.WATER) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // 沿著單一軸移動，並在過程中逐步檢查碰撞；呼叫端保證同一時間只傳入一個非零軸。
    private void moveOnAxis(World world, float dx, float dy, float dz) {
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        // 位移太小就直接略過
        if (distance < 1e-6f) {
            return;
        }

        // 把整段位移切成多小步，減少穿牆問題
        int steps = Math.max(1, (int) Math.ceil(distance / COLLISION_STEP));
        float stepX = dx / steps;
        float stepY = dy / steps;
        float stepZ = dz / steps;

        for (int i = 0; i < steps; i++) {
            float targetX = position.x + stepX;
            float targetY = position.y + stepY;
            float targetZ = position.z + stepZ;

            // 沒撞到就走到下一小步
            if (!collides(world, targetX, targetY, targetZ)) {
                position.set(targetX, targetY, targetZ);
                continue;
            }

            // 水平移動撞到非完整方塊時嘗試踏階；垂直移動或飛行不做，避免干擾跳躍/飛行控制。
            if (!flying && dy == 0.0f && tryStepUp(world, targetX, targetZ)) {
                continue;
            }

            // 撞到牆或方塊時，把對應方向速度清掉
            if (stepX != 0.0f) {
                velocity.x = 0.0f;
            }
            if (stepY != 0.0f) {
                // 往下撞到地面時，標記為站在地上
                if (stepY < 0.0f) {
                    onGround = true;
                }
                velocity.y = 0.0f;
            }
            if (stepZ != 0.0f) {
                velocity.z = 0.0f;
            }

            // 這個軸一旦撞到就停止移動
            return;
        }
    }

    // 水平移動被半磚或樓梯擋住時，嘗試小幅抬高玩家，形成自然踏階。
    private boolean tryStepUp(World world, float targetX, float targetZ) {
        for (int i = 1; i <= STEP_ATTEMPTS; i++) {
            float candidateY = position.y + (STEP_HEIGHT * i / STEP_ATTEMPTS);
            if (!collides(world, targetX, candidateY, targetZ)) {
                position.set(targetX, candidateY, targetZ);
                onGround = false;
                return true;
            }
        }
        return false;
    }

    // 判斷角色碰撞箱在指定位置時，是否會碰到任何方塊碰撞盒。
    private boolean collides(World world, float x, float y, float z) {
        return collidesWithHeight(world, x, y, z, playerHeight());
    }

    private boolean collidesWithHeight(World world, float x, float y, float z, float height) {
        float half = GameConfig.PLAYER_WIDTH * 0.5f;
        float minWorldX = x - half;
        float minWorldY = y;
        float minWorldZ = z - half;
        float maxWorldX = x + half;
        float maxWorldY = y + height;
        float maxWorldZ = z + half;

        // 算出角色碰撞箱涵蓋到哪些方塊座標；EPSILON 讓「剛好貼邊」不被當成進入鄰格。
        int minX = fastFloor(minWorldX + EPSILON);
        int maxX = fastFloor(maxWorldX - EPSILON);
        // 往下多檢查一格，讓半磚、樓梯這種低於玩家腳底的盒子仍可被偵測到。
        int minY = fastFloor(minWorldY + EPSILON) - 1;
        int maxY = fastFloor(maxWorldY - EPSILON);
        int minZ = fastFloor(minWorldZ + EPSILON);
        int maxZ = fastFloor(maxWorldZ - EPSILON);

        // 逐一檢查碰撞箱範圍內的所有方塊，只要有實心方塊就算碰撞
        for (int by = minY; by <= maxY; by++) {
            for (int bz = minZ; bz <= maxZ; bz++) {
                for (int bx = minX; bx <= maxX; bx++) {
                    BlockType block = world.getBlock(bx, by, bz);
                    for (BlockBounds bounds : block.collisionBoxes()) {
                        if (bounds.intersectsWorldBox(bx, by, bz,
                                minWorldX, minWorldY, minWorldZ,
                                maxWorldX, maxWorldY, maxWorldZ)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private float playerHeight() {
        return crouching ? CROUCH_HEIGHT : GameConfig.PLAYER_HEIGHT;
    }

    // 讓 current 以固定步長慢慢接近 target
    private float approach(float current, float target, float delta) {
        if (current < target) {
            return Math.min(current + delta, target);
        }
        return Math.max(current - delta, target);
    }

    // 比 Math.floor 更快的版本，專門把浮點數轉成較小的整數格子座標
    private int fastFloor(float value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }
}

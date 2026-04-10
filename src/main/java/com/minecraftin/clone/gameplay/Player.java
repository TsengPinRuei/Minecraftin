package com.minecraftin.clone.gameplay;

import com.minecraftin.clone.config.GameConfig;
import com.minecraftin.clone.engine.Camera;
import com.minecraftin.clone.engine.InputState;
import com.minecraftin.clone.world.World;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public final class Player {
    // 每次位移切成更小的步驟，讓碰撞判定更穩定
    private static final float COLLISION_STEP = 0.04f;

    // 避免浮點數誤差造成角色卡牆或誤判碰撞
    private static final float EPSILON = 0.001f;

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
        timeSinceLastSpaceTap = Float.POSITIVE_INFINITY;
    }

    // 取得角色眼睛位置，通常用於相機或視角起點
    public Vector3f eyePosition(Vector3f out) {
        out.set(position.x, position.y + GameConfig.PLAYER_EYE_HEIGHT, position.z);
        return out;
    }

    // 每一幀更新角色狀態
    public void update(InputState input, Camera camera, World world, float deltaSeconds) {
        // 先嘗試把角色從方塊內推出來，避免出生或移動後卡進牆裡
        resolveIntersections(world);

        // 累加距離上次按空白鍵的時間
        timeSinceLastSpaceTap += deltaSeconds;

        boolean spacePressed = input.wasKeyPressed(GLFW_KEY_SPACE);

        // 創造模式下，雙擊空白鍵可切換飛行
        if (creativeMode && spacePressed) {
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

        // 飛行模式和一般地面模式分開處理
        if (creativeMode && flying) {
            updateCreative(input, camera, world, deltaSeconds);
            return;
        }

        updateGrounded(input, camera, world, deltaSeconds, spacePressed);
    }

    // 如果角色一開始就卡進方塊，往上嘗試移動直到脫離碰撞
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

        // 分三個軸移動，方便逐軸做碰撞處理
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
        float targetSpeed = GameConfig.WALK_SPEED;
        if (input.isKeyDown(GLFW_KEY_LEFT_CONTROL)) {
            targetSpeed *= GameConfig.SPRINT_MULTIPLIER;
        }

        if (wish.lengthSquared() > 0.0001f) {
            // 有移動輸入時，把方向轉成固定速度
            wish.normalize(targetSpeed);

            // 在地上加速比較快，空中加速比較慢
            float accel = onGround ? 34.0f : 10.0f;
            velocity.x = approach(velocity.x, wish.x, accel * deltaSeconds);
            velocity.z = approach(velocity.z, wish.z, accel * deltaSeconds);
        } else if (onGround) {
            // 沒有輸入而且在地上時，套用摩擦力讓角色慢慢停下來
            float friction = 20.0f * deltaSeconds;
            velocity.x = approach(velocity.x, 0.0f, friction);
            velocity.z = approach(velocity.z, 0.0f, friction);
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

        // 先處理 x 軸移動
        moveOnAxis(world, velocity.x * deltaSeconds, 0.0f, 0.0f);

        // 先重設為不在地面，之後由 y 軸碰撞重新判定
        onGround = false;

        // 再處理 y 軸移動
        moveOnAxis(world, 0.0f, velocity.y * deltaSeconds, 0.0f);

        // 最後處理 z 軸移動
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
        float half = GameConfig.PLAYER_WIDTH * 0.5f;
        float minX = position.x - half;
        float maxX = position.x + half;
        float minY = position.y;
        float maxY = position.y + GameConfig.PLAYER_HEIGHT;
        float minZ = position.z - half;
        float maxZ = position.z + half;

        return maxX > x && minX < x + 1
                && maxY > y && minY < y + 1
                && maxZ > z && minZ < z + 1;
    }

    // 沿著單一軸移動，並在過程中逐步檢查碰撞
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

    // 判斷角色碰撞箱在指定位置時，是否會碰到實心方塊
    private boolean collides(World world, float x, float y, float z) {
        float half = GameConfig.PLAYER_WIDTH * 0.5f;

        // 算出角色碰撞箱涵蓋到哪些方塊座標
        int minX = fastFloor(x - half + EPSILON);
        int maxX = fastFloor(x + half - EPSILON);
        int minY = fastFloor(y + EPSILON);
        int maxY = fastFloor(y + GameConfig.PLAYER_HEIGHT - EPSILON);
        int minZ = fastFloor(z - half + EPSILON);
        int maxZ = fastFloor(z + half - EPSILON);

        // 逐一檢查碰撞箱範圍內的所有方塊，只要有實心方塊就算碰撞
        for (int by = minY; by <= maxY; by++) {
            for (int bz = minZ; bz <= maxZ; bz++) {
                for (int bx = minX; bx <= maxX; bx++) {
                    if (world.getBlock(bx, by, bz).isSolid()) {
                        return true;
                    }
                }
            }
        }

        return false;
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
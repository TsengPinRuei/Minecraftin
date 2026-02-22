// 宣告此檔案所屬的套件。
package com.minecraftin.clone.gameplay;

// 匯入後續會使用到的型別或函式。
import com.minecraftin.clone.config.GameConfig;
// 匯入後續會使用到的型別或函式。
import com.minecraftin.clone.engine.Camera;
// 匯入後續會使用到的型別或函式。
import com.minecraftin.clone.engine.InputState;
// 匯入後續會使用到的型別或函式。
import com.minecraftin.clone.world.World;
// 匯入後續會使用到的型別或函式。
import org.joml.Vector3f;

// 匯入後續會使用到的型別或函式。
import static org.lwjgl.glfw.GLFW.*;

// 定義主要型別與其結構。
public final class Player {
    // 設定或更新變數的值。
    private static final float COLLISION_STEP = 0.04f;
    // 設定或更新變數的值。
    private static final float EPSILON = 0.001f;
    // 設定或更新變數的值。
    private static final float DOUBLE_TAP_SECONDS = 0.28f;

    // 設定或更新變數的值。
    private final Vector3f position = new Vector3f();
    // 設定或更新變數的值。
    private final Vector3f velocity = new Vector3f();
    // 設定或更新變數的值。
    private final Vector3f tmpWish = new Vector3f();
    // 設定或更新變數的值。
    private final Vector3f tmpForward = new Vector3f();
    // 設定或更新變數的值。
    private final Vector3f tmpRight = new Vector3f();

    // 設定或更新變數的值。
    private final boolean creativeMode = GameConfig.CREATIVE_MODE_ONLY;

    // 下一行程式碼負責執行目前步驟。
    private boolean onGround;
    // 下一行程式碼負責執行目前步驟。
    private boolean flying;
    // 設定或更新變數的值。
    private float timeSinceLastSpaceTap = Float.POSITIVE_INFINITY;

    // 定義對外可呼叫的方法。
    public Vector3f position() {
        // 下一行程式碼負責執行目前步驟。
        return position;
    }

    // 定義對外可呼叫的方法。
    public boolean isFlying() {
        // 下一行程式碼負責執行目前步驟。
        return flying;
    }

    // 定義對外可呼叫的方法。
    public boolean isOnGround() {
        // 下一行程式碼負責執行目前步驟。
        return onGround;
    }

    // 定義對外可呼叫的方法。
    public float horizontalSpeed() {
        // 呼叫方法執行對應功能。
        return (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
    }

    // 定義對外可呼叫的方法。
    public void setPosition(float x, float y, float z) {
        // 呼叫方法執行對應功能。
        position.set(x, y, z);
        // 呼叫方法執行對應功能。
        velocity.zero();
        // 設定或更新變數的值。
        onGround = false;
        // 設定或更新變數的值。
        flying = false;
        // 設定或更新變數的值。
        timeSinceLastSpaceTap = Float.POSITIVE_INFINITY;
    }

    // 定義對外可呼叫的方法。
    public Vector3f eyePosition(Vector3f out) {
        // 呼叫方法執行對應功能。
        out.set(position.x, position.y + GameConfig.PLAYER_EYE_HEIGHT, position.z);
        // 下一行程式碼負責執行目前步驟。
        return out;
    }

    // 定義對外可呼叫的方法。
    public void update(InputState input, Camera camera, World world, float deltaSeconds) {
        // 呼叫方法執行對應功能。
        resolveIntersections(world);
        // 設定或更新變數的值。
        timeSinceLastSpaceTap += deltaSeconds;

        // 宣告並初始化變數。
        boolean spacePressed = input.wasKeyPressed(GLFW_KEY_SPACE);
        // 根據條件決定是否進入此邏輯分支。
        if (creativeMode && spacePressed) {
            // 根據條件決定是否進入此邏輯分支。
            if (timeSinceLastSpaceTap <= DOUBLE_TAP_SECONDS) {
                // 設定或更新變數的值。
                flying = !flying;
                // 設定或更新變數的值。
                timeSinceLastSpaceTap = Float.POSITIVE_INFINITY;
                // 根據條件決定是否進入此邏輯分支。
                if (flying) {
                    // 設定或更新變數的值。
                    onGround = false;
                    // 設定或更新變數的值。
                    velocity.y = 0.0f;
                }
            // 下一行程式碼負責執行目前步驟。
            } else {
                // 設定或更新變數的值。
                timeSinceLastSpaceTap = 0.0f;
            }
        }

        // 根據條件決定是否進入此邏輯分支。
        if (creativeMode && flying) {
            // 呼叫方法執行對應功能。
            updateCreative(input, camera, world, deltaSeconds);
            // 下一行程式碼負責執行目前步驟。
            return;
        }

        // 呼叫方法執行對應功能。
        updateGrounded(input, camera, world, deltaSeconds, spacePressed);
    }

    // 定義類別內部使用的方法。
    private void resolveIntersections(World world) {
        // 根據條件決定是否進入此邏輯分支。
        if (!collides(world, position.x, position.y, position.z)) {
            // 下一行程式碼負責執行目前步驟。
            return;
        }

        // 宣告並初始化變數。
        float originalY = position.y;
        // 使用迴圈逐一處理每個元素或區間。
        for (int i = 1; i <= 24; i++) {
            // 宣告並初始化變數。
            float candidateY = originalY + i * 0.125f;
            // 根據條件決定是否進入此邏輯分支。
            if (!collides(world, position.x, candidateY, position.z)) {
                // 設定或更新變數的值。
                position.y = candidateY;
                // 設定或更新變數的值。
                velocity.y = 0.0f;
                // 設定或更新變數的值。
                onGround = false;
                // 下一行程式碼負責執行目前步驟。
                return;
            }
        }
    }

    // 定義類別內部使用的方法。
    private void updateCreative(InputState input, Camera camera, World world, float deltaSeconds) {
        // 宣告並初始化變數。
        Vector3f wish = tmpWish.zero();
        // 宣告並初始化變數。
        Vector3f forward = camera.forward(tmpForward);
        // 宣告並初始化變數。
        Vector3f right = camera.right(tmpRight);

        // 根據條件決定是否進入此邏輯分支。
        if (input.isKeyDown(GLFW_KEY_W)) {
            // 呼叫方法執行對應功能。
            wish.add(forward);
        }
        // 根據條件決定是否進入此邏輯分支。
        if (input.isKeyDown(GLFW_KEY_S)) {
            // 呼叫方法執行對應功能。
            wish.sub(forward);
        }
        // 根據條件決定是否進入此邏輯分支。
        if (input.isKeyDown(GLFW_KEY_D)) {
            // 呼叫方法執行對應功能。
            wish.add(right);
        }
        // 根據條件決定是否進入此邏輯分支。
        if (input.isKeyDown(GLFW_KEY_A)) {
            // 呼叫方法執行對應功能。
            wish.sub(right);
        }
        // 根據條件決定是否進入此邏輯分支。
        if (input.isKeyDown(GLFW_KEY_SPACE)) {
            // 呼叫方法執行對應功能。
            wish.add(0.0f, 1.0f, 0.0f);
        }
        // 根據條件決定是否進入此邏輯分支。
        if (input.isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
            // 呼叫方法執行對應功能。
            wish.add(0.0f, -1.0f, 0.0f);
        }

        // 宣告並初始化變數。
        float targetSpeed = GameConfig.FLY_SPEED;
        // 根據條件決定是否進入此邏輯分支。
        if (input.isKeyDown(GLFW_KEY_LEFT_CONTROL)) {
            // 設定或更新變數的值。
            targetSpeed *= GameConfig.SPRINT_MULTIPLIER;
        }

        // 根據條件決定是否進入此邏輯分支。
        if (wish.lengthSquared() > 0.0001f) {
            // 呼叫方法執行對應功能。
            wish.normalize(targetSpeed);
        // 下一行程式碼負責執行目前步驟。
        } else {
            // 呼叫方法執行對應功能。
            wish.zero();
        }

        // 宣告並初始化變數。
        float accel = 28.0f;
        // 設定或更新變數的值。
        velocity.x = approach(velocity.x, wish.x, accel * deltaSeconds);
        // 設定或更新變數的值。
        velocity.y = approach(velocity.y, wish.y, accel * deltaSeconds);
        // 設定或更新變數的值。
        velocity.z = approach(velocity.z, wish.z, accel * deltaSeconds);

        // 呼叫方法執行對應功能。
        moveOnAxis(world, velocity.x * deltaSeconds, 0.0f, 0.0f);
        // 呼叫方法執行對應功能。
        moveOnAxis(world, 0.0f, velocity.y * deltaSeconds, 0.0f);
        // 呼叫方法執行對應功能。
        moveOnAxis(world, 0.0f, 0.0f, velocity.z * deltaSeconds);
        // 設定或更新變數的值。
        onGround = false;
    }

    // 定義類別內部使用的方法。
    private void updateGrounded(InputState input, Camera camera, World world, float deltaSeconds, boolean spacePressed) {
        // 宣告並初始化變數。
        Vector3f wish = tmpWish.zero();

        // 宣告並初始化變數。
        Vector3f forward = camera.forward(tmpForward);
        // 設定或更新變數的值。
        forward.y = 0.0f;
        // 根據條件決定是否進入此邏輯分支。
        if (forward.lengthSquared() > 0.0001f) {
            // 呼叫方法執行對應功能。
            forward.normalize();
        }

        // 宣告並初始化變數。
        Vector3f right = camera.right(tmpRight);
        // 設定或更新變數的值。
        right.y = 0.0f;
        // 根據條件決定是否進入此邏輯分支。
        if (right.lengthSquared() > 0.0001f) {
            // 呼叫方法執行對應功能。
            right.normalize();
        }

        // 根據條件決定是否進入此邏輯分支。
        if (input.isKeyDown(GLFW_KEY_W)) {
            // 呼叫方法執行對應功能。
            wish.add(forward);
        }
        // 根據條件決定是否進入此邏輯分支。
        if (input.isKeyDown(GLFW_KEY_S)) {
            // 呼叫方法執行對應功能。
            wish.sub(forward);
        }
        // 根據條件決定是否進入此邏輯分支。
        if (input.isKeyDown(GLFW_KEY_D)) {
            // 呼叫方法執行對應功能。
            wish.add(right);
        }
        // 根據條件決定是否進入此邏輯分支。
        if (input.isKeyDown(GLFW_KEY_A)) {
            // 呼叫方法執行對應功能。
            wish.sub(right);
        }

        // 宣告並初始化變數。
        float targetSpeed = GameConfig.WALK_SPEED;
        // 根據條件決定是否進入此邏輯分支。
        if (input.isKeyDown(GLFW_KEY_LEFT_CONTROL)) {
            // 設定或更新變數的值。
            targetSpeed *= GameConfig.SPRINT_MULTIPLIER;
        }

        // 根據條件決定是否進入此邏輯分支。
        if (wish.lengthSquared() > 0.0001f) {
            // 呼叫方法執行對應功能。
            wish.normalize(targetSpeed);
            // 宣告並初始化變數。
            float accel = onGround ? 34.0f : 10.0f;
            // 設定或更新變數的值。
            velocity.x = approach(velocity.x, wish.x, accel * deltaSeconds);
            // 設定或更新變數的值。
            velocity.z = approach(velocity.z, wish.z, accel * deltaSeconds);
        // 下一行程式碼負責執行目前步驟。
        } else if (onGround) {
            // 宣告並初始化變數。
            float friction = 20.0f * deltaSeconds;
            // 設定或更新變數的值。
            velocity.x = approach(velocity.x, 0.0f, friction);
            // 設定或更新變數的值。
            velocity.z = approach(velocity.z, 0.0f, friction);
        }

        // 根據條件決定是否進入此邏輯分支。
        if (onGround && spacePressed) {
            // 設定或更新變數的值。
            velocity.y = GameConfig.JUMP_VELOCITY;
            // 設定或更新變數的值。
            onGround = false;
        }

        // 設定或更新變數的值。
        velocity.y -= GameConfig.GRAVITY * deltaSeconds;
        // 根據條件決定是否進入此邏輯分支。
        if (velocity.y < -65.0f) {
            // 設定或更新變數的值。
            velocity.y = -65.0f;
        }

        // 呼叫方法執行對應功能。
        moveOnAxis(world, velocity.x * deltaSeconds, 0.0f, 0.0f);

        // 設定或更新變數的值。
        onGround = false;
        // 呼叫方法執行對應功能。
        moveOnAxis(world, 0.0f, velocity.y * deltaSeconds, 0.0f);

        // 呼叫方法執行對應功能。
        moveOnAxis(world, 0.0f, 0.0f, velocity.z * deltaSeconds);

        // 根據條件決定是否進入此邏輯分支。
        if (position.y < 1.1f) {
            // 設定或更新變數的值。
            position.y = 1.1f;
            // 設定或更新變數的值。
            velocity.y = Math.max(0.0f, velocity.y);
            // 設定或更新變數的值。
            onGround = true;
        }
    }

    // 定義對外可呼叫的方法。
    public boolean intersectsBlock(int x, int y, int z) {
        // 宣告並初始化變數。
        float half = GameConfig.PLAYER_WIDTH * 0.5f;
        // 宣告並初始化變數。
        float minX = position.x - half;
        // 宣告並初始化變數。
        float maxX = position.x + half;
        // 宣告並初始化變數。
        float minY = position.y;
        // 宣告並初始化變數。
        float maxY = position.y + GameConfig.PLAYER_HEIGHT;
        // 宣告並初始化變數。
        float minZ = position.z - half;
        // 宣告並初始化變數。
        float maxZ = position.z + half;

        // 下一行程式碼負責執行目前步驟。
        return maxX > x && minX < x + 1
                // 下一行程式碼負責執行目前步驟。
                && maxY > y && minY < y + 1
                // 下一行程式碼負責執行目前步驟。
                && maxZ > z && minZ < z + 1;
    }

    // 定義類別內部使用的方法。
    private void moveOnAxis(World world, float dx, float dy, float dz) {
        // 宣告並初始化變數。
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        // 根據條件決定是否進入此邏輯分支。
        if (distance < 1e-6f) {
            // 下一行程式碼負責執行目前步驟。
            return;
        }

        // 宣告並初始化變數。
        int steps = Math.max(1, (int) Math.ceil(distance / COLLISION_STEP));
        // 宣告並初始化變數。
        float stepX = dx / steps;
        // 宣告並初始化變數。
        float stepY = dy / steps;
        // 宣告並初始化變數。
        float stepZ = dz / steps;

        // 使用迴圈逐一處理每個元素或區間。
        for (int i = 0; i < steps; i++) {
            // 宣告並初始化變數。
            float targetX = position.x + stepX;
            // 宣告並初始化變數。
            float targetY = position.y + stepY;
            // 宣告並初始化變數。
            float targetZ = position.z + stepZ;

            // 根據條件決定是否進入此邏輯分支。
            if (!collides(world, targetX, targetY, targetZ)) {
                // 呼叫方法執行對應功能。
                position.set(targetX, targetY, targetZ);
                // 跳過本次迴圈剩餘邏輯，直接進入下一次迭代。
                continue;
            }

            // 根據條件決定是否進入此邏輯分支。
            if (stepX != 0.0f) {
                // 設定或更新變數的值。
                velocity.x = 0.0f;
            }
            // 根據條件決定是否進入此邏輯分支。
            if (stepY != 0.0f) {
                // 根據條件決定是否進入此邏輯分支。
                if (stepY < 0.0f) {
                    // 設定或更新變數的值。
                    onGround = true;
                }
                // 設定或更新變數的值。
                velocity.y = 0.0f;
            }
            // 根據條件決定是否進入此邏輯分支。
            if (stepZ != 0.0f) {
                // 設定或更新變數的值。
                velocity.z = 0.0f;
            }
            // 下一行程式碼負責執行目前步驟。
            return;
        }
    }

    // 定義類別內部使用的方法。
    private boolean collides(World world, float x, float y, float z) {
        // 宣告並初始化變數。
        float half = GameConfig.PLAYER_WIDTH * 0.5f;

        // 宣告並初始化變數。
        int minX = fastFloor(x - half + EPSILON);
        // 宣告並初始化變數。
        int maxX = fastFloor(x + half - EPSILON);
        // 宣告並初始化變數。
        int minY = fastFloor(y + EPSILON);
        // 宣告並初始化變數。
        int maxY = fastFloor(y + GameConfig.PLAYER_HEIGHT - EPSILON);
        // 宣告並初始化變數。
        int minZ = fastFloor(z - half + EPSILON);
        // 宣告並初始化變數。
        int maxZ = fastFloor(z + half - EPSILON);

        // 使用迴圈逐一處理每個元素或區間。
        for (int by = minY; by <= maxY; by++) {
            // 使用迴圈逐一處理每個元素或區間。
            for (int bz = minZ; bz <= maxZ; bz++) {
                // 使用迴圈逐一處理每個元素或區間。
                for (int bx = minX; bx <= maxX; bx++) {
                    // 根據條件決定是否進入此邏輯分支。
                    if (world.getBlock(bx, by, bz).isSolid()) {
                        // 下一行程式碼負責執行目前步驟。
                        return true;
                    }
                }
            }
        }
        // 下一行程式碼負責執行目前步驟。
        return false;
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

    // 定義類別內部使用的方法。
    private int fastFloor(float value) {
        // 宣告並初始化變數。
        int i = (int) value;
        // 下一行程式碼負責執行目前步驟。
        return value < i ? i - 1 : i;
    }
}

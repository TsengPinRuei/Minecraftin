// 說明：宣告此檔案所屬的套件。
package com.minecraftin.clone.gameplay;

// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.config.GameConfig;
// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.engine.Camera;
// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.engine.InputState;
// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.world.World;
// 說明：匯入後續會使用到的型別或函式。
import org.joml.Vector3f;

// 說明：匯入後續會使用到的型別或函式。
import static org.lwjgl.glfw.GLFW.*;

// 說明：定義主要型別與其結構。
public final class Player {
    // 說明：設定或更新變數的值。
    private static final float COLLISION_STEP = 0.04f;
    // 說明：設定或更新變數的值。
    private static final float EPSILON = 0.001f;
    // 說明：設定或更新變數的值。
    private static final float DOUBLE_TAP_SECONDS = 0.28f;

    // 說明：設定或更新變數的值。
    private final Vector3f position = new Vector3f();
    // 說明：設定或更新變數的值。
    private final Vector3f velocity = new Vector3f();
    // 說明：設定或更新變數的值。
    private final Vector3f tmpWish = new Vector3f();
    // 說明：設定或更新變數的值。
    private final Vector3f tmpForward = new Vector3f();
    // 說明：設定或更新變數的值。
    private final Vector3f tmpRight = new Vector3f();

    // 說明：設定或更新變數的值。
    private final boolean creativeMode = GameConfig.CREATIVE_MODE_ONLY;

    // 說明：下一行程式碼負責執行目前步驟。
    private boolean onGround;
    // 說明：下一行程式碼負責執行目前步驟。
    private boolean flying;
    // 說明：設定或更新變數的值。
    private float timeSinceLastSpaceTap = Float.POSITIVE_INFINITY;

    // 說明：定義對外可呼叫的方法。
    public Vector3f position() {
        // 說明：下一行程式碼負責執行目前步驟。
        return position;
    }

    // 說明：定義對外可呼叫的方法。
    public boolean isFlying() {
        // 說明：下一行程式碼負責執行目前步驟。
        return flying;
    }

    // 說明：定義對外可呼叫的方法。
    public boolean isOnGround() {
        // 說明：下一行程式碼負責執行目前步驟。
        return onGround;
    }

    // 說明：定義對外可呼叫的方法。
    public float horizontalSpeed() {
        // 說明：呼叫方法執行對應功能。
        return (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
    }

    // 說明：定義對外可呼叫的方法。
    public void setPosition(float x, float y, float z) {
        // 說明：呼叫方法執行對應功能。
        position.set(x, y, z);
        // 說明：呼叫方法執行對應功能。
        velocity.zero();
        // 說明：設定或更新變數的值。
        onGround = false;
        // 說明：設定或更新變數的值。
        flying = false;
        // 說明：設定或更新變數的值。
        timeSinceLastSpaceTap = Float.POSITIVE_INFINITY;
    }

    // 說明：定義對外可呼叫的方法。
    public Vector3f eyePosition(Vector3f out) {
        // 說明：呼叫方法執行對應功能。
        out.set(position.x, position.y + GameConfig.PLAYER_EYE_HEIGHT, position.z);
        // 說明：下一行程式碼負責執行目前步驟。
        return out;
    }

    // 說明：定義對外可呼叫的方法。
    public void update(InputState input, Camera camera, World world, float deltaSeconds) {
        // 說明：呼叫方法執行對應功能。
        resolveIntersections(world);
        // 說明：設定或更新變數的值。
        timeSinceLastSpaceTap += deltaSeconds;

        // 說明：宣告並初始化變數。
        boolean spacePressed = input.wasKeyPressed(GLFW_KEY_SPACE);
        // 說明：根據條件決定是否進入此邏輯分支。
        if (creativeMode && spacePressed) {
            // 說明：根據條件決定是否進入此邏輯分支。
            if (timeSinceLastSpaceTap <= DOUBLE_TAP_SECONDS) {
                // 說明：設定或更新變數的值。
                flying = !flying;
                // 說明：設定或更新變數的值。
                timeSinceLastSpaceTap = Float.POSITIVE_INFINITY;
                // 說明：根據條件決定是否進入此邏輯分支。
                if (flying) {
                    // 說明：設定或更新變數的值。
                    onGround = false;
                    // 說明：設定或更新變數的值。
                    velocity.y = 0.0f;
                }
            // 說明：下一行程式碼負責執行目前步驟。
            } else {
                // 說明：設定或更新變數的值。
                timeSinceLastSpaceTap = 0.0f;
            }
        }

        // 說明：根據條件決定是否進入此邏輯分支。
        if (creativeMode && flying) {
            // 說明：呼叫方法執行對應功能。
            updateCreative(input, camera, world, deltaSeconds);
            // 說明：下一行程式碼負責執行目前步驟。
            return;
        }

        // 說明：呼叫方法執行對應功能。
        updateGrounded(input, camera, world, deltaSeconds, spacePressed);
    }

    // 說明：定義類別內部使用的方法。
    private void resolveIntersections(World world) {
        // 說明：根據條件決定是否進入此邏輯分支。
        if (!collides(world, position.x, position.y, position.z)) {
            // 說明：下一行程式碼負責執行目前步驟。
            return;
        }

        // 說明：宣告並初始化變數。
        float originalY = position.y;
        // 說明：使用迴圈逐一處理每個元素或區間。
        for (int i = 1; i <= 24; i++) {
            // 說明：宣告並初始化變數。
            float candidateY = originalY + i * 0.125f;
            // 說明：根據條件決定是否進入此邏輯分支。
            if (!collides(world, position.x, candidateY, position.z)) {
                // 說明：設定或更新變數的值。
                position.y = candidateY;
                // 說明：設定或更新變數的值。
                velocity.y = 0.0f;
                // 說明：設定或更新變數的值。
                onGround = false;
                // 說明：下一行程式碼負責執行目前步驟。
                return;
            }
        }
    }

    // 說明：定義類別內部使用的方法。
    private void updateCreative(InputState input, Camera camera, World world, float deltaSeconds) {
        // 說明：宣告並初始化變數。
        Vector3f wish = tmpWish.zero();
        // 說明：宣告並初始化變數。
        Vector3f forward = camera.forward(tmpForward);
        // 說明：宣告並初始化變數。
        Vector3f right = camera.right(tmpRight);

        // 說明：根據條件決定是否進入此邏輯分支。
        if (input.isKeyDown(GLFW_KEY_W)) {
            // 說明：呼叫方法執行對應功能。
            wish.add(forward);
        }
        // 說明：根據條件決定是否進入此邏輯分支。
        if (input.isKeyDown(GLFW_KEY_S)) {
            // 說明：呼叫方法執行對應功能。
            wish.sub(forward);
        }
        // 說明：根據條件決定是否進入此邏輯分支。
        if (input.isKeyDown(GLFW_KEY_D)) {
            // 說明：呼叫方法執行對應功能。
            wish.add(right);
        }
        // 說明：根據條件決定是否進入此邏輯分支。
        if (input.isKeyDown(GLFW_KEY_A)) {
            // 說明：呼叫方法執行對應功能。
            wish.sub(right);
        }
        // 說明：根據條件決定是否進入此邏輯分支。
        if (input.isKeyDown(GLFW_KEY_SPACE)) {
            // 說明：呼叫方法執行對應功能。
            wish.add(0.0f, 1.0f, 0.0f);
        }
        // 說明：根據條件決定是否進入此邏輯分支。
        if (input.isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
            // 說明：呼叫方法執行對應功能。
            wish.add(0.0f, -1.0f, 0.0f);
        }

        // 說明：宣告並初始化變數。
        float targetSpeed = GameConfig.FLY_SPEED;
        // 說明：根據條件決定是否進入此邏輯分支。
        if (input.isKeyDown(GLFW_KEY_LEFT_CONTROL)) {
            // 說明：設定或更新變數的值。
            targetSpeed *= GameConfig.SPRINT_MULTIPLIER;
        }

        // 說明：根據條件決定是否進入此邏輯分支。
        if (wish.lengthSquared() > 0.0001f) {
            // 說明：呼叫方法執行對應功能。
            wish.normalize(targetSpeed);
        // 說明：下一行程式碼負責執行目前步驟。
        } else {
            // 說明：呼叫方法執行對應功能。
            wish.zero();
        }

        // 說明：宣告並初始化變數。
        float accel = 28.0f;
        // 說明：設定或更新變數的值。
        velocity.x = approach(velocity.x, wish.x, accel * deltaSeconds);
        // 說明：設定或更新變數的值。
        velocity.y = approach(velocity.y, wish.y, accel * deltaSeconds);
        // 說明：設定或更新變數的值。
        velocity.z = approach(velocity.z, wish.z, accel * deltaSeconds);

        // 說明：呼叫方法執行對應功能。
        moveOnAxis(world, velocity.x * deltaSeconds, 0.0f, 0.0f);
        // 說明：呼叫方法執行對應功能。
        moveOnAxis(world, 0.0f, velocity.y * deltaSeconds, 0.0f);
        // 說明：呼叫方法執行對應功能。
        moveOnAxis(world, 0.0f, 0.0f, velocity.z * deltaSeconds);
        // 說明：設定或更新變數的值。
        onGround = false;
    }

    // 說明：定義類別內部使用的方法。
    private void updateGrounded(InputState input, Camera camera, World world, float deltaSeconds, boolean spacePressed) {
        // 說明：宣告並初始化變數。
        Vector3f wish = tmpWish.zero();

        // 說明：宣告並初始化變數。
        Vector3f forward = camera.forward(tmpForward);
        // 說明：設定或更新變數的值。
        forward.y = 0.0f;
        // 說明：根據條件決定是否進入此邏輯分支。
        if (forward.lengthSquared() > 0.0001f) {
            // 說明：呼叫方法執行對應功能。
            forward.normalize();
        }

        // 說明：宣告並初始化變數。
        Vector3f right = camera.right(tmpRight);
        // 說明：設定或更新變數的值。
        right.y = 0.0f;
        // 說明：根據條件決定是否進入此邏輯分支。
        if (right.lengthSquared() > 0.0001f) {
            // 說明：呼叫方法執行對應功能。
            right.normalize();
        }

        // 說明：根據條件決定是否進入此邏輯分支。
        if (input.isKeyDown(GLFW_KEY_W)) {
            // 說明：呼叫方法執行對應功能。
            wish.add(forward);
        }
        // 說明：根據條件決定是否進入此邏輯分支。
        if (input.isKeyDown(GLFW_KEY_S)) {
            // 說明：呼叫方法執行對應功能。
            wish.sub(forward);
        }
        // 說明：根據條件決定是否進入此邏輯分支。
        if (input.isKeyDown(GLFW_KEY_D)) {
            // 說明：呼叫方法執行對應功能。
            wish.add(right);
        }
        // 說明：根據條件決定是否進入此邏輯分支。
        if (input.isKeyDown(GLFW_KEY_A)) {
            // 說明：呼叫方法執行對應功能。
            wish.sub(right);
        }

        // 說明：宣告並初始化變數。
        float targetSpeed = GameConfig.WALK_SPEED;
        // 說明：根據條件決定是否進入此邏輯分支。
        if (input.isKeyDown(GLFW_KEY_LEFT_CONTROL)) {
            // 說明：設定或更新變數的值。
            targetSpeed *= GameConfig.SPRINT_MULTIPLIER;
        }

        // 說明：根據條件決定是否進入此邏輯分支。
        if (wish.lengthSquared() > 0.0001f) {
            // 說明：呼叫方法執行對應功能。
            wish.normalize(targetSpeed);
            // 說明：宣告並初始化變數。
            float accel = onGround ? 34.0f : 10.0f;
            // 說明：設定或更新變數的值。
            velocity.x = approach(velocity.x, wish.x, accel * deltaSeconds);
            // 說明：設定或更新變數的值。
            velocity.z = approach(velocity.z, wish.z, accel * deltaSeconds);
        // 說明：下一行程式碼負責執行目前步驟。
        } else if (onGround) {
            // 說明：宣告並初始化變數。
            float friction = 20.0f * deltaSeconds;
            // 說明：設定或更新變數的值。
            velocity.x = approach(velocity.x, 0.0f, friction);
            // 說明：設定或更新變數的值。
            velocity.z = approach(velocity.z, 0.0f, friction);
        }

        // 說明：根據條件決定是否進入此邏輯分支。
        if (onGround && spacePressed) {
            // 說明：設定或更新變數的值。
            velocity.y = GameConfig.JUMP_VELOCITY;
            // 說明：設定或更新變數的值。
            onGround = false;
        }

        // 說明：設定或更新變數的值。
        velocity.y -= GameConfig.GRAVITY * deltaSeconds;
        // 說明：根據條件決定是否進入此邏輯分支。
        if (velocity.y < -65.0f) {
            // 說明：設定或更新變數的值。
            velocity.y = -65.0f;
        }

        // 說明：呼叫方法執行對應功能。
        moveOnAxis(world, velocity.x * deltaSeconds, 0.0f, 0.0f);

        // 說明：設定或更新變數的值。
        onGround = false;
        // 說明：呼叫方法執行對應功能。
        moveOnAxis(world, 0.0f, velocity.y * deltaSeconds, 0.0f);

        // 說明：呼叫方法執行對應功能。
        moveOnAxis(world, 0.0f, 0.0f, velocity.z * deltaSeconds);

        // 說明：根據條件決定是否進入此邏輯分支。
        if (position.y < 1.1f) {
            // 說明：設定或更新變數的值。
            position.y = 1.1f;
            // 說明：設定或更新變數的值。
            velocity.y = Math.max(0.0f, velocity.y);
            // 說明：設定或更新變數的值。
            onGround = true;
        }
    }

    // 說明：定義對外可呼叫的方法。
    public boolean intersectsBlock(int x, int y, int z) {
        // 說明：宣告並初始化變數。
        float half = GameConfig.PLAYER_WIDTH * 0.5f;
        // 說明：宣告並初始化變數。
        float minX = position.x - half;
        // 說明：宣告並初始化變數。
        float maxX = position.x + half;
        // 說明：宣告並初始化變數。
        float minY = position.y;
        // 說明：宣告並初始化變數。
        float maxY = position.y + GameConfig.PLAYER_HEIGHT;
        // 說明：宣告並初始化變數。
        float minZ = position.z - half;
        // 說明：宣告並初始化變數。
        float maxZ = position.z + half;

        // 說明：下一行程式碼負責執行目前步驟。
        return maxX > x && minX < x + 1
                // 說明：下一行程式碼負責執行目前步驟。
                && maxY > y && minY < y + 1
                // 說明：下一行程式碼負責執行目前步驟。
                && maxZ > z && minZ < z + 1;
    }

    // 說明：定義類別內部使用的方法。
    private void moveOnAxis(World world, float dx, float dy, float dz) {
        // 說明：宣告並初始化變數。
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        // 說明：根據條件決定是否進入此邏輯分支。
        if (distance < 1e-6f) {
            // 說明：下一行程式碼負責執行目前步驟。
            return;
        }

        // 說明：宣告並初始化變數。
        int steps = Math.max(1, (int) Math.ceil(distance / COLLISION_STEP));
        // 說明：宣告並初始化變數。
        float stepX = dx / steps;
        // 說明：宣告並初始化變數。
        float stepY = dy / steps;
        // 說明：宣告並初始化變數。
        float stepZ = dz / steps;

        // 說明：使用迴圈逐一處理每個元素或區間。
        for (int i = 0; i < steps; i++) {
            // 說明：宣告並初始化變數。
            float targetX = position.x + stepX;
            // 說明：宣告並初始化變數。
            float targetY = position.y + stepY;
            // 說明：宣告並初始化變數。
            float targetZ = position.z + stepZ;

            // 說明：根據條件決定是否進入此邏輯分支。
            if (!collides(world, targetX, targetY, targetZ)) {
                // 說明：呼叫方法執行對應功能。
                position.set(targetX, targetY, targetZ);
                // 說明：跳過本次迴圈剩餘邏輯，直接進入下一次迭代。
                continue;
            }

            // 說明：根據條件決定是否進入此邏輯分支。
            if (stepX != 0.0f) {
                // 說明：設定或更新變數的值。
                velocity.x = 0.0f;
            }
            // 說明：根據條件決定是否進入此邏輯分支。
            if (stepY != 0.0f) {
                // 說明：根據條件決定是否進入此邏輯分支。
                if (stepY < 0.0f) {
                    // 說明：設定或更新變數的值。
                    onGround = true;
                }
                // 說明：設定或更新變數的值。
                velocity.y = 0.0f;
            }
            // 說明：根據條件決定是否進入此邏輯分支。
            if (stepZ != 0.0f) {
                // 說明：設定或更新變數的值。
                velocity.z = 0.0f;
            }
            // 說明：下一行程式碼負責執行目前步驟。
            return;
        }
    }

    // 說明：定義類別內部使用的方法。
    private boolean collides(World world, float x, float y, float z) {
        // 說明：宣告並初始化變數。
        float half = GameConfig.PLAYER_WIDTH * 0.5f;

        // 說明：宣告並初始化變數。
        int minX = fastFloor(x - half + EPSILON);
        // 說明：宣告並初始化變數。
        int maxX = fastFloor(x + half - EPSILON);
        // 說明：宣告並初始化變數。
        int minY = fastFloor(y + EPSILON);
        // 說明：宣告並初始化變數。
        int maxY = fastFloor(y + GameConfig.PLAYER_HEIGHT - EPSILON);
        // 說明：宣告並初始化變數。
        int minZ = fastFloor(z - half + EPSILON);
        // 說明：宣告並初始化變數。
        int maxZ = fastFloor(z + half - EPSILON);

        // 說明：使用迴圈逐一處理每個元素或區間。
        for (int by = minY; by <= maxY; by++) {
            // 說明：使用迴圈逐一處理每個元素或區間。
            for (int bz = minZ; bz <= maxZ; bz++) {
                // 說明：使用迴圈逐一處理每個元素或區間。
                for (int bx = minX; bx <= maxX; bx++) {
                    // 說明：根據條件決定是否進入此邏輯分支。
                    if (world.getBlock(bx, by, bz).isSolid()) {
                        // 說明：下一行程式碼負責執行目前步驟。
                        return true;
                    }
                }
            }
        }
        // 說明：下一行程式碼負責執行目前步驟。
        return false;
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

    // 說明：定義類別內部使用的方法。
    private int fastFloor(float value) {
        // 說明：宣告並初始化變數。
        int i = (int) value;
        // 說明：下一行程式碼負責執行目前步驟。
        return value < i ? i - 1 : i;
    }
}

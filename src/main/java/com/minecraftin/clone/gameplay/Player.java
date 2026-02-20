package com.minecraftin.clone.gameplay;

import com.minecraftin.clone.config.GameConfig;
import com.minecraftin.clone.engine.Camera;
import com.minecraftin.clone.engine.InputState;
import com.minecraftin.clone.world.World;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public final class Player {
    private static final float COLLISION_STEP = 0.04f;
    private static final float EPSILON = 0.001f;
    private static final float DOUBLE_TAP_SECONDS = 0.28f;

    private final Vector3f position = new Vector3f();
    private final Vector3f velocity = new Vector3f();
    private final Vector3f tmpWish = new Vector3f();
    private final Vector3f tmpForward = new Vector3f();
    private final Vector3f tmpRight = new Vector3f();

    private final boolean creativeMode = GameConfig.CREATIVE_MODE_ONLY;

    private boolean onGround;
    private boolean flying;
    private float timeSinceLastSpaceTap = Float.POSITIVE_INFINITY;

    public Vector3f position() {
        return position;
    }

    public boolean isFlying() {
        return flying;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public float horizontalSpeed() {
        return (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
    }

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        velocity.zero();
        onGround = false;
        flying = false;
        timeSinceLastSpaceTap = Float.POSITIVE_INFINITY;
    }

    public Vector3f eyePosition(Vector3f out) {
        out.set(position.x, position.y + GameConfig.PLAYER_EYE_HEIGHT, position.z);
        return out;
    }

    public void update(InputState input, Camera camera, World world, float deltaSeconds) {
        resolveIntersections(world);
        timeSinceLastSpaceTap += deltaSeconds;

        boolean spacePressed = input.wasKeyPressed(GLFW_KEY_SPACE);
        if (creativeMode && spacePressed) {
            if (timeSinceLastSpaceTap <= DOUBLE_TAP_SECONDS) {
                flying = !flying;
                timeSinceLastSpaceTap = Float.POSITIVE_INFINITY;
                if (flying) {
                    onGround = false;
                    velocity.y = 0.0f;
                }
            } else {
                timeSinceLastSpaceTap = 0.0f;
            }
        }

        if (creativeMode && flying) {
            updateCreative(input, camera, world, deltaSeconds);
            return;
        }

        updateGrounded(input, camera, world, deltaSeconds, spacePressed);
    }

    private void resolveIntersections(World world) {
        if (!collides(world, position.x, position.y, position.z)) {
            return;
        }

        float originalY = position.y;
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

    private void updateCreative(InputState input, Camera camera, World world, float deltaSeconds) {
        Vector3f wish = tmpWish.zero();
        Vector3f forward = camera.forward(tmpForward);
        Vector3f right = camera.right(tmpRight);

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

        float targetSpeed = GameConfig.FLY_SPEED;
        if (input.isKeyDown(GLFW_KEY_LEFT_CONTROL)) {
            targetSpeed *= GameConfig.SPRINT_MULTIPLIER;
        }

        if (wish.lengthSquared() > 0.0001f) {
            wish.normalize(targetSpeed);
        } else {
            wish.zero();
        }

        float accel = 28.0f;
        velocity.x = approach(velocity.x, wish.x, accel * deltaSeconds);
        velocity.y = approach(velocity.y, wish.y, accel * deltaSeconds);
        velocity.z = approach(velocity.z, wish.z, accel * deltaSeconds);

        moveOnAxis(world, velocity.x * deltaSeconds, 0.0f, 0.0f);
        moveOnAxis(world, 0.0f, velocity.y * deltaSeconds, 0.0f);
        moveOnAxis(world, 0.0f, 0.0f, velocity.z * deltaSeconds);
        onGround = false;
    }

    private void updateGrounded(InputState input, Camera camera, World world, float deltaSeconds, boolean spacePressed) {
        Vector3f wish = tmpWish.zero();

        Vector3f forward = camera.forward(tmpForward);
        forward.y = 0.0f;
        if (forward.lengthSquared() > 0.0001f) {
            forward.normalize();
        }

        Vector3f right = camera.right(tmpRight);
        right.y = 0.0f;
        if (right.lengthSquared() > 0.0001f) {
            right.normalize();
        }

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

        float targetSpeed = GameConfig.WALK_SPEED;
        if (input.isKeyDown(GLFW_KEY_LEFT_CONTROL)) {
            targetSpeed *= GameConfig.SPRINT_MULTIPLIER;
        }

        if (wish.lengthSquared() > 0.0001f) {
            wish.normalize(targetSpeed);
            float accel = onGround ? 34.0f : 10.0f;
            velocity.x = approach(velocity.x, wish.x, accel * deltaSeconds);
            velocity.z = approach(velocity.z, wish.z, accel * deltaSeconds);
        } else if (onGround) {
            float friction = 20.0f * deltaSeconds;
            velocity.x = approach(velocity.x, 0.0f, friction);
            velocity.z = approach(velocity.z, 0.0f, friction);
        }

        if (onGround && spacePressed) {
            velocity.y = GameConfig.JUMP_VELOCITY;
            onGround = false;
        }

        velocity.y -= GameConfig.GRAVITY * deltaSeconds;
        if (velocity.y < -65.0f) {
            velocity.y = -65.0f;
        }

        moveOnAxis(world, velocity.x * deltaSeconds, 0.0f, 0.0f);

        onGround = false;
        moveOnAxis(world, 0.0f, velocity.y * deltaSeconds, 0.0f);

        moveOnAxis(world, 0.0f, 0.0f, velocity.z * deltaSeconds);

        if (position.y < 1.1f) {
            position.y = 1.1f;
            velocity.y = Math.max(0.0f, velocity.y);
            onGround = true;
        }
    }

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

    private void moveOnAxis(World world, float dx, float dy, float dz) {
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 1e-6f) {
            return;
        }

        int steps = Math.max(1, (int) Math.ceil(distance / COLLISION_STEP));
        float stepX = dx / steps;
        float stepY = dy / steps;
        float stepZ = dz / steps;

        for (int i = 0; i < steps; i++) {
            float targetX = position.x + stepX;
            float targetY = position.y + stepY;
            float targetZ = position.z + stepZ;

            if (!collides(world, targetX, targetY, targetZ)) {
                position.set(targetX, targetY, targetZ);
                continue;
            }

            if (stepX != 0.0f) {
                velocity.x = 0.0f;
            }
            if (stepY != 0.0f) {
                if (stepY < 0.0f) {
                    onGround = true;
                }
                velocity.y = 0.0f;
            }
            if (stepZ != 0.0f) {
                velocity.z = 0.0f;
            }
            return;
        }
    }

    private boolean collides(World world, float x, float y, float z) {
        float half = GameConfig.PLAYER_WIDTH * 0.5f;

        int minX = fastFloor(x - half + EPSILON);
        int maxX = fastFloor(x + half - EPSILON);
        int minY = fastFloor(y + EPSILON);
        int maxY = fastFloor(y + GameConfig.PLAYER_HEIGHT - EPSILON);
        int minZ = fastFloor(z - half + EPSILON);
        int maxZ = fastFloor(z + half - EPSILON);

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

    private float approach(float current, float target, float delta) {
        if (current < target) {
            return Math.min(current + delta, target);
        }
        return Math.max(current - delta, target);
    }

    private int fastFloor(float value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }
}

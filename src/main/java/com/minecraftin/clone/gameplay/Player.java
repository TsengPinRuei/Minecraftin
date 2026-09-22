package com.minecraftin.clone.gameplay;

import com.minecraftin.clone.config.GameConfig;
import com.minecraftin.clone.engine.Camera;
import com.minecraftin.clone.engine.InputState;
import com.minecraftin.clone.world.BlockBounds;
import com.minecraftin.clone.world.BlockType;
import com.minecraftin.clone.world.World;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

// Keep position at the center of the player's feet; eye height is applied only to the camera.
public final class Player {
    // Split movement into small collision probes to reduce tunneling at high speed.
    private static final float COLLISION_STEP = 0.04f;

    // Exclude exact cell boundaries from candidate sampling to avoid floating-point edge artifacts.
    private static final float EPSILON = 0.001f;

    // Maximum automatic step height, allowing slabs and stairs to be climbed without jumping.
    private static final float STEP_HEIGHT = 0.58f;

    // Probe the upward path in increments so stepping is blocked by overhead geometry.
    private static final int STEP_ATTEMPTS = 8;

    // Ladders have no collision boxes; use a wider contact region to enter climbing mode.
    private static final float LADDER_TOUCH_MARGIN = 0.20f;
    private static final float LADDER_CLIMB_SPEED = 2.35f;

    // Crouching immediately reduces body height and horizontal target speed; the camera height
    // eases separately.
    private static final float CROUCH_HEIGHT = 1.45f;
    private static final float CROUCH_EYE_HEIGHT = 1.25f;
    private static final float CROUCH_SPEED_MULTIPLIER = 0.3f;

    // Camera height transition speed in blocks per second.
    private static final float EYE_HEIGHT_TRANSITION_SPEED = 3.0f;

    // Decay the visual step offset in blocks per second without changing the collision position.
    private static final float STEP_SMOOTHING_SPEED = 6.0f;

    // Swimming uses separate movement and vertical drag; idle swimmers slowly sink.
    private static final float WATER_SWIM_SPEED = 2.45f;
    private static final float WATER_VERTICAL_SPEED = 2.8f;
    private static final float WATER_SINK_SPEED = -0.55f;
    private static final float WATER_ACCEL = 12.0f;
    private static final float WATER_DRAG = 8.0f;

    // Maximum interval between Space presses to toggle flight.
    private static final float DOUBLE_TAP_SECONDS = 0.28f;

    private final Vector3f position = new Vector3f();

    private final Vector3f velocity = new Vector3f();

    // Reuse movement vectors across frames.
    private final Vector3f tmpWish = new Vector3f();

    private final Vector3f tmpForward = new Vector3f();

    private final Vector3f tmpRight = new Vector3f();

    private final boolean creativeMode = GameConfig.CREATIVE_MODE_ONLY;

    private boolean onGround;

    private boolean flying;

    private boolean crouching;

    private float currentEyeHeight = GameConfig.PLAYER_EYE_HEIGHT;

    // A negative camera offset smooths an immediate upward step of the collision body.
    private float stepSmoothingOffset;

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

    public float eyeHeight() {
        return currentEyeHeight;
    }

    // Game adds this nonpositive offset to eye height to smooth steps without affecting collisions.
    public float stepSmoothingOffset() {
        return stepSmoothingOffset;
    }

    public float horizontalSpeed() {
        return (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
    }

    // Teleport the feet position and reset velocity, flight, crouching, and camera smoothing.
    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        velocity.zero();
        onGround = false;
        flying = false;
        crouching = false;
        currentEyeHeight = GameConfig.PLAYER_EYE_HEIGHT;
        stepSmoothingOffset = 0.0f;
        timeSinceLastSpaceTap = Float.POSITIVE_INFINITY;
    }

    // Game preloads nearby chunks before movement; collision reads can otherwise generate missing
    // chunks.
    public void update(InputState input, Camera camera, World world, float deltaSeconds) {
        // Try to recover from a spawn or loaded position that overlaps solid geometry.
        resolveIntersections(world);

        float targetEyeHeight = crouching ? CROUCH_EYE_HEIGHT : GameConfig.PLAYER_EYE_HEIGHT;
        currentEyeHeight = approach(currentEyeHeight, targetEyeHeight,
                EYE_HEIGHT_TRANSITION_SPEED * deltaSeconds);

        stepSmoothingOffset = approach(stepSmoothingOffset, 0.0f, STEP_SMOOTHING_SPEED * deltaSeconds);

        timeSinceLastSpaceTap += deltaSeconds;

        boolean spacePressed = input.wasKeyPressed(GLFW_KEY_SPACE);

        // Double-tap Space to toggle creative flight, including while swimming.
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

        // Flight uses the full camera direction; ordinary movement uses a normalized horizontal
        // direction.
        if (creativeMode && flying) {
            updateCrouchState(input, world, false);
            updateCreative(input, camera, world, deltaSeconds);
            return;
        }

        updateGrounded(input, camera, world, deltaSeconds, spacePressed);
    }

    // Try upward offsets up to three blocks to escape an overlap.
    // Leave the position unchanged if no tested offset is clear.
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

        // Apply the flight speed boost only while Left Ctrl and W are held.
        float targetSpeed = GameConfig.FLY_SPEED;
        if (input.isKeyDown(GLFW_KEY_LEFT_CONTROL) && input.isKeyDown(GLFW_KEY_W)) {
            targetSpeed *= GameConfig.SPRINT_MULTIPLIER;
        }

        // Normalize combined input so diagonal flight does not increase target speed.
        if (wish.lengthSquared() > 0.0001f) {
            wish.normalize(targetSpeed);
        } else {
            wish.zero();
        }

        // Approach the target velocity gradually for smoother flight control.
        float accel = 28.0f;
        velocity.x = approach(velocity.x, wish.x, accel * deltaSeconds);
        velocity.y = approach(velocity.y, wish.y, accel * deltaSeconds);
        velocity.z = approach(velocity.z, wish.z, accel * deltaSeconds);

        // Resolve one axis at a time so a blocked axis does not prevent sliding along the others.
        moveOnAxis(world, velocity.x * deltaSeconds, 0.0f, 0.0f);
        moveOnAxis(world, 0.0f, velocity.y * deltaSeconds, 0.0f);
        moveOnAxis(world, 0.0f, 0.0f, velocity.z * deltaSeconds);

        onGround = false;
    }

    private void updateGrounded(InputState input, Camera camera, World world, float deltaSeconds,
            boolean spacePressed) {
        Vector3f wish = tmpWish.zero();

        Vector3f forward = camera.forward(tmpForward);
        forward.y = 0.0f;
        // Camera pitch stays below vertical, so normalize the horizontal projection even when it is
        // very small.
        forward.normalize();

        Vector3f right = camera.right(tmpRight);
        right.y = 0.0f;
        if (right.lengthSquared() > 0.0001f) {
            right.normalize();
        }

        boolean touchingLadder = isTouchingLadder(world);
        boolean inWater = !touchingLadder && isInWater(world);
        updateCrouchState(input, world, !touchingLadder && !inWater);

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

        // Choose the walking or swimming speed, then apply crouch or forward-sprint modifiers.
        float targetSpeed = inWater ? WATER_SWIM_SPEED : GameConfig.WALK_SPEED;
        if (!inWater && crouching) {
            targetSpeed *= CROUCH_SPEED_MULTIPLIER;
        } else if (input.isKeyDown(GLFW_KEY_LEFT_CONTROL) && input.isKeyDown(GLFW_KEY_W)) {
            targetSpeed *= GameConfig.SPRINT_MULTIPLIER;
        }

        if (wish.lengthSquared() > 0.0001f) {
            wish.normalize(targetSpeed);

            // Use separate acceleration for water, supported movement, and airborne movement.
            float accel = inWater ? WATER_ACCEL : (onGround ? 34.0f : 10.0f);
            velocity.x = approach(velocity.x, wish.x, accel * deltaSeconds);
            velocity.z = approach(velocity.z, wish.z, accel * deltaSeconds);
        } else if (inWater) {
            float drag = WATER_DRAG * deltaSeconds;
            velocity.x = approach(velocity.x, 0.0f, drag);
            velocity.z = approach(velocity.z, 0.0f, drag);
        } else if (onGround) {
            // Apply ground friction only when movement input is released; airborne motion retains
            // momentum.
            float friction = 20.0f * deltaSeconds;
            velocity.x = approach(velocity.x, 0.0f, friction);
            velocity.z = approach(velocity.z, 0.0f, friction);
        }

        if (touchingLadder) {
            // Use ladder contact instead of solid collision, with W/Space to climb and S/Shift to
            // descend.
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

        if (onGround && spacePressed) {
            velocity.y = GameConfig.JUMP_VELOCITY;
            onGround = false;
        }

        velocity.y -= GameConfig.GRAVITY * deltaSeconds;

        // Cap downward speed at 78.4 blocks per second.
        if (velocity.y < -78.4f) {
            velocity.y = -78.4f;
        }

        // Resolve axes separately so blocked motion can slide along nearby surfaces.
        moveOnAxis(world, velocity.x * deltaSeconds, 0.0f, 0.0f);

        // Clear the previous ground state before vertical movement; a downward collision restores
        // it.
        onGround = false;

        moveOnAxis(world, 0.0f, velocity.y * deltaSeconds, 0.0f);
        moveOnAxis(world, 0.0f, 0.0f, velocity.z * deltaSeconds);

        // Keep the walking player above the bottom bedrock layer.
        if (position.y < 1.1f) {
            position.y = 1.1f;
            velocity.y = Math.max(0.0f, velocity.y);
            onGround = true;
        }
    }

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

    // Expand the player's horizontal bounds when testing ladder render boxes to make edge contact
    // usable.
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

    // New crouch input is allowed in ordinary movement, including midair, but not on ladders or in
    // water.
    // Keep an existing crouch until standing height fits, even when crouch input is no longer
    // allowed.
    private void updateCrouchState(InputState input, World world, boolean canCrouch) {
        if (canCrouch && input.isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
            crouching = true;
            return;
        }

        if (crouching && !collidesWithHeight(world, position.x, position.y, position.z, GameConfig.PLAYER_HEIGHT)) {
            crouching = false;
        }
    }

    // Swimming uses vertical input and drag instead of jumping and normal gravity.
    // With no net vertical input, approach a slow sinking speed.
    private void updateSwimming(InputState input, World world, float deltaSeconds) {
        float verticalInput = 0.0f;
        if (input.isKeyDown(GLFW_KEY_SPACE)) {
            verticalInput += 1.0f;
        }
        if (input.isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
            verticalInput -= 1.0f;
        }

        float targetVertical = verticalInput != 0.0f ? verticalInput * WATER_VERTICAL_SPEED : WATER_SINK_SPEED;
        velocity.y = approach(velocity.y, targetVertical, WATER_ACCEL * deltaSeconds);

        moveOnAxis(world, velocity.x * deltaSeconds, 0.0f, 0.0f);
        onGround = false;
        moveOnAxis(world, 0.0f, velocity.y * deltaSeconds, 0.0f);
        moveOnAxis(world, 0.0f, 0.0f, velocity.z * deltaSeconds);
    }

    // Detect water by occupied cells overlapping the body, with the foot sample raised slightly.
    // This does not test a separate liquid surface height.
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
                    if (world.getBlock(bx, by, bz).isWaterBlock()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // Move along one axis only; callers must not pass multiple nonzero components.
    private void moveOnAxis(World world, float dx, float dy, float dz) {
        float distance = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);

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

            // Try stepping after a horizontal collision; flight skips this path and tryStepUp
            // checks support.
            if (!flying && dy == 0.0f && tryStepUp(world, targetX, targetZ)) {
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

    // Step only when not rising and a downward probe finds support.
    // Test upward clearance before moving sideways so the player cannot step upward in midair.
    private boolean tryStepUp(World world, float targetX, float targetZ) {
        if (velocity.y > 0.0f || !collides(world, position.x, position.y - COLLISION_STEP, position.z)) {
            return false;
        }
        for (int i = 1; i <= STEP_ATTEMPTS; i++) {
            float candidateY = position.y + (STEP_HEIGHT * i / STEP_ATTEMPTS);
            if (collides(world, position.x, candidateY, position.z)) {
                return false;
            }
            if (!collides(world, targetX, candidateY, targetZ)) {
                // Move the body immediately but retain a negative camera offset for a smooth visual
                // rise.
                stepSmoothingOffset = Math.max(-1.0f, stepSmoothingOffset - (candidateY - position.y));
                position.set(targetX, candidateY, targetZ);
                onGround = false;
                return true;
            }
        }
        return false;
    }

    private boolean collides(World world, float x, float y, float z) {
        return collidesWithHeight(world, x, y, z, playerHeight());
    }

    // Use the requested height for both normal collisions and standing-up clearance checks.
    private boolean collidesWithHeight(World world, float x, float y, float z, float height) {
        float half = GameConfig.PLAYER_WIDTH * 0.5f;
        float minWorldX = x - half;
        float minWorldY = y;
        float minWorldZ = z - half;
        float maxWorldX = x + half;
        float maxWorldY = y + height;
        float maxWorldZ = z + half;

        // Use a small inward offset when selecting candidate cells so touching a cell edge is not
        // treated as entering it.
        int minX = fastFloor(minWorldX + EPSILON);
        int maxX = fastFloor(maxWorldX - EPSILON);
        // Include the cell below the feet because fence posts extend above their own cell.
        int minY = fastFloor(minWorldY + EPSILON) - 1;
        int maxY = fastFloor(maxWorldY - EPSILON);
        int minZ = fastFloor(minWorldZ + EPSILON);
        int maxZ = fastFloor(maxWorldZ - EPSILON);

        // Candidate cells only narrow the search; collision requires overlap with an actual block
        // collision box.
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

    private float approach(float current, float target, float delta) {
        if (current < target) {
            return Math.min(current + delta, target);
        }
        return Math.max(current - delta, target);
    }

    // Correct integer truncation for negative coordinates to match mathematical floor.
    private int fastFloor(float value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }
}

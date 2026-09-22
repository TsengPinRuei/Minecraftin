package com.minecraftin.clone.engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;

// First-person camera using a Y-up world and yaw/pitch angles in degrees.
public final class Camera {
    private static final Vector3f WORLD_UP = new Vector3f(0.0f, 1.0f, 0.0f);

    private final Vector3f position = new Vector3f();

    // Recompute direction vectors only after rotation changes.
    private final Vector3f cachedForward = new Vector3f();
    private final Vector3f cachedRight = new Vector3f();

    // Reuse scratch vectors to avoid allocations while building the view matrix.
    private final Vector3f tmpForward = new Vector3f();

    private final Vector3f tmpCenter = new Vector3f();

    // Start facing negative Z.
    private float yaw = -90.0f;

    // Zero pitch looks horizontally.
    private float pitch = 0.0f;

    private boolean directionDirty = true;

    public Vector3f position() {
        return position;
    }

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
    }

    // Clamp pitch below vertical so the forward/up cross product remains nonzero.
    public void setRotation(float yawDegrees, float pitchDegrees) {
        yaw = yawDegrees;
        pitch = clampPitch(pitchDegrees);
        directionDirty = true;
    }

    public void rotate(float yawDelta, float pitchDelta) {
        yaw += yawDelta;
        pitch = clampPitch(pitch + pitchDelta);
        directionDirty = true;
    }

    // Write the unit forward vector into out and return that same object.
    public Vector3f forward(Vector3f out) {
        updateDirectionCache();
        return out.set(cachedForward);
    }

    // Write the unit right vector into out; the pitch limit keeps its cross product well-defined.
    public Vector3f right(Vector3f out) {
        updateDirectionCache();
        return out.set(cachedRight);
    }

    public Matrix4f viewMatrix(Matrix4f out) {
        forward(tmpForward);

        tmpCenter.set(position).add(tmpForward);

        return out.identity().lookAt(position, tmpCenter, WORLD_UP);
    }

    private void updateDirectionCache() {
        if (!directionDirty) {
            return;
        }

        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        cachedForward.x = (float) (Math.cos(yawRad) * Math.cos(pitchRad));
        cachedForward.y = (float) Math.sin(pitchRad);
        cachedForward.z = (float) (Math.sin(yawRad) * Math.cos(pitchRad));

        cachedForward.normalize();
        cachedRight.set(cachedForward).cross(WORLD_UP).normalize();
        directionDirty = false;
    }

    // Keep forward away from WORLD_UP so the view basis does not become degenerate.
    private float clampPitch(float angle) {
        return Math.max(-89.9f, Math.min(89.9f, angle));
    }
}

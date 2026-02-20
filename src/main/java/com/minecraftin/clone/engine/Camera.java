package com.minecraftin.clone.engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class Camera {
    private static final Vector3f WORLD_UP = new Vector3f(0.0f, 1.0f, 0.0f);

    private final Vector3f position = new Vector3f();
    private final Vector3f tmpForward = new Vector3f();
    private final Vector3f tmpCenter = new Vector3f();
    private float yaw = -90.0f;
    private float pitch = 0.0f;

    public Vector3f position() {
        return position;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
    }

    public void setRotation(float yawDegrees, float pitchDegrees) {
        yaw = yawDegrees;
        pitch = clampPitch(pitchDegrees);
    }

    public void rotate(float yawDelta, float pitchDelta) {
        yaw += yawDelta;
        pitch = clampPitch(pitch + pitchDelta);
    }

    public Vector3f forward(Vector3f out) {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        out.x = (float) (Math.cos(yawRad) * Math.cos(pitchRad));
        out.y = (float) Math.sin(pitchRad);
        out.z = (float) (Math.sin(yawRad) * Math.cos(pitchRad));
        return out.normalize();
    }

    public Vector3f right(Vector3f out) {
        forward(out);
        out.cross(WORLD_UP).normalize();
        return out;
    }

    public Matrix4f viewMatrix(Matrix4f out) {
        forward(tmpForward);
        tmpCenter.set(position).add(tmpForward);
        return out.identity().lookAt(position, tmpCenter, WORLD_UP);
    }

    private float clampPitch(float angle) {
        return Math.max(-89.9f, Math.min(89.9f, angle));
    }
}

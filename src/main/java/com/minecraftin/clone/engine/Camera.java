// 說明：宣告此檔案所屬的套件。
package com.minecraftin.clone.engine;

// 說明：匯入後續會使用到的型別或函式。
import org.joml.Matrix4f;
// 說明：匯入後續會使用到的型別或函式。
import org.joml.Vector3f;

// 說明：定義主要型別與其結構。
public final class Camera {
    // 說明：設定或更新變數的值。
    private static final Vector3f WORLD_UP = new Vector3f(0.0f, 1.0f, 0.0f);

    // 說明：設定或更新變數的值。
    private final Vector3f position = new Vector3f();
    // 說明：設定或更新變數的值。
    private final Vector3f tmpForward = new Vector3f();
    // 說明：設定或更新變數的值。
    private final Vector3f tmpCenter = new Vector3f();
    // 說明：設定或更新變數的值。
    private float yaw = -90.0f;
    // 說明：設定或更新變數的值。
    private float pitch = 0.0f;

    // 說明：定義對外可呼叫的方法。
    public Vector3f position() {
        // 說明：下一行程式碼負責執行目前步驟。
        return position;
    }

    // 說明：定義對外可呼叫的方法。
    public float yaw() {
        // 說明：下一行程式碼負責執行目前步驟。
        return yaw;
    }

    // 說明：定義對外可呼叫的方法。
    public float pitch() {
        // 說明：下一行程式碼負責執行目前步驟。
        return pitch;
    }

    // 說明：定義對外可呼叫的方法。
    public void setPosition(float x, float y, float z) {
        // 說明：呼叫方法執行對應功能。
        position.set(x, y, z);
    }

    // 說明：定義對外可呼叫的方法。
    public void setRotation(float yawDegrees, float pitchDegrees) {
        // 說明：設定或更新變數的值。
        yaw = yawDegrees;
        // 說明：設定或更新變數的值。
        pitch = clampPitch(pitchDegrees);
    }

    // 說明：定義對外可呼叫的方法。
    public void rotate(float yawDelta, float pitchDelta) {
        // 說明：設定或更新變數的值。
        yaw += yawDelta;
        // 說明：設定或更新變數的值。
        pitch = clampPitch(pitch + pitchDelta);
    }

    // 說明：定義對外可呼叫的方法。
    public Vector3f forward(Vector3f out) {
        // 說明：宣告並初始化變數。
        float yawRad = (float) Math.toRadians(yaw);
        // 說明：宣告並初始化變數。
        float pitchRad = (float) Math.toRadians(pitch);

        // 說明：設定或更新變數的值。
        out.x = (float) (Math.cos(yawRad) * Math.cos(pitchRad));
        // 說明：設定或更新變數的值。
        out.y = (float) Math.sin(pitchRad);
        // 說明：設定或更新變數的值。
        out.z = (float) (Math.sin(yawRad) * Math.cos(pitchRad));
        // 說明：呼叫方法執行對應功能。
        return out.normalize();
    }

    // 說明：定義對外可呼叫的方法。
    public Vector3f right(Vector3f out) {
        // 說明：呼叫方法執行對應功能。
        forward(out);
        // 說明：呼叫方法執行對應功能。
        out.cross(WORLD_UP).normalize();
        // 說明：下一行程式碼負責執行目前步驟。
        return out;
    }

    // 說明：定義對外可呼叫的方法。
    public Matrix4f viewMatrix(Matrix4f out) {
        // 說明：呼叫方法執行對應功能。
        forward(tmpForward);
        // 說明：呼叫方法執行對應功能。
        tmpCenter.set(position).add(tmpForward);
        // 說明：呼叫方法執行對應功能。
        return out.identity().lookAt(position, tmpCenter, WORLD_UP);
    }

    // 說明：定義類別內部使用的方法。
    private float clampPitch(float angle) {
        // 說明：呼叫方法執行對應功能。
        return Math.max(-89.9f, Math.min(89.9f, angle));
    }
}

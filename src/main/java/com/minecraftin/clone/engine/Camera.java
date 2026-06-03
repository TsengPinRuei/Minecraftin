package com.minecraftin.clone.engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;

// 保存第一人稱相機的位置與 yaw/pitch，並提供方向向量與 view matrix 給玩家控制及渲染使用。
public final class Camera {
    // 世界座標中的「上方」方向，通常是 Y 軸正方向。
    private static final Vector3f WORLD_UP = new Vector3f(0.0f, 1.0f, 0.0f);

    // 攝影機目前所在的位置。
    private final Vector3f position = new Vector3f();

    // 暫存前方方向，避免每次都建立新物件。
    private final Vector3f tmpForward = new Vector3f();

    // 暫存攝影機注視的目標點，避免重複建立物件。
    private final Vector3f tmpCenter = new Vector3f();

    // 左右轉動角度。預設 -90 度，讓攝影機一開始朝向 Z 軸負方向。
    private float yaw = -90.0f;

    // 上下轉動角度。0 度代表水平看出去。
    private float pitch = 0.0f;

    // 取得攝影機目前的位置。
    public Vector3f position() {
        return position;
    }

    // 取得目前的左右轉動角度。
    public float yaw() {
        return yaw;
    }

    // 取得目前的上下轉動角度。
    public float pitch() {
        return pitch;
    }

    // 直接設定攝影機的位置。
    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
    }

    // 直接設定攝影機的旋轉角度。
    // pitch 會被限制在合理範圍內，避免視角翻轉。
    public void setRotation(float yawDegrees, float pitchDegrees) {
        yaw = yawDegrees;
        pitch = clampPitch(pitchDegrees);
    }

    // 在目前角度基礎上繼續旋轉。
    // yawDelta 控制左右轉，pitchDelta 控制上下轉。
    public void rotate(float yawDelta, float pitchDelta) {
        yaw += yawDelta;
        pitch = clampPitch(pitch + pitchDelta);
    }

    // 計算攝影機目前「往前看」的方向。
    // 結果會寫入傳入的 out，並回傳同一個物件。
    public Vector3f forward(Vector3f out) {
        // 將角度轉成弧度，因為三角函數使用的是弧度。
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        // 根據 yaw 和 pitch 計算 3D 方向向量。
        out.x = (float) (Math.cos(yawRad) * Math.cos(pitchRad));
        out.y = (float) Math.sin(pitchRad);
        out.z = (float) (Math.sin(yawRad) * Math.cos(pitchRad));

        // 正規化後回傳，讓向量長度固定為 1。
        return out.normalize();
    }

    // 計算攝影機右手邊的方向。
    // 做法是用前方方向與世界上方向做叉積，因此 pitch 接近垂直時需要被 clamp。
    public Vector3f right(Vector3f out) {
        forward(out);
        out.cross(WORLD_UP).normalize();
        return out;
    }

    // 建立攝影機的視角矩陣，用來決定畫面是從哪裡往哪裡看。
    public Matrix4f viewMatrix(Matrix4f out) {
        // 先算出前方方向。
        forward(tmpForward);

        // 目標點 = 目前位置 + 前方方向。
        tmpCenter.set(position).add(tmpForward);

        // 產生 LookAt 視角矩陣。
        return out.identity().lookAt(position, tmpCenter, WORLD_UP);
    }

    // 限制 pitch 的範圍，避免接近 90 度時造成視角翻轉或數學問題。
    private float clampPitch(float angle) {
        return Math.max(-89.9f, Math.min(89.9f, angle));
    }
}

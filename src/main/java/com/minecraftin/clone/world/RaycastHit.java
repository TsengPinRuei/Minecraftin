package com.minecraftin.clone.world;

// 用來表示射線檢測命中的結果。
public record RaycastHit(
                // 命中的方塊 X 座標。
                int x,

                // 命中的方塊 Y 座標。
                int y,

                // 命中的方塊 Z 座標。
                int z,

                // 命中表面的法線在 X 軸上的方向。
                int normalX,

                // 命中表面的法線在 Y 軸上的方向。
                int normalY,

                // 命中表面的法線在 Z 軸上的方向。
                int normalZ,

                // 從起點到命中位置的距離。
                float distance,

                // 命中的方塊種類。
                BlockType block) {
}
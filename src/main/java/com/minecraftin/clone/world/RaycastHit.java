package com.minecraftin.clone.world;

// 用來表示射線檢測命中的結果；normal 指向被命中方塊外側，放置方塊時會用它找相鄰格。
public record RaycastHit(
                int x,

                int y,

                int z,

                int normalX,

                int normalY,

                int normalZ,

                // 從起點到命中位置的距離。
                float distance,

                BlockType block) {
}

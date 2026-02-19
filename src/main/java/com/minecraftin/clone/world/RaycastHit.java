package com.minecraftin.clone.world;

public record RaycastHit(
        int x,
        int y,
        int z,
        int normalX,
        int normalY,
        int normalZ,
        float distance,
        BlockType block
) {
}

package com.minecraftin.clone.world;

// 表示一個方塊內的局部 AABB，座標範圍通常是 0..1，但像柵欄可略高於 1。
public record BlockBounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {

    public boolean intersectsWorldBox(int blockX, int blockY, int blockZ,
            float minWorldX, float minWorldY, float minWorldZ,
            float maxWorldX, float maxWorldY, float maxWorldZ) {
        float worldMinX = blockX + minX;
        float worldMinY = blockY + minY;
        float worldMinZ = blockZ + minZ;
        float worldMaxX = blockX + maxX;
        float worldMaxY = blockY + maxY;
        float worldMaxZ = blockZ + maxZ;

        return maxWorldX > worldMinX && minWorldX < worldMaxX
                && maxWorldY > worldMinY && minWorldY < worldMaxY
                && maxWorldZ > worldMinZ && minWorldZ < worldMaxZ;
    }
}

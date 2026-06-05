package com.minecraftin.clone.world;

// 表示一個方塊內的局部 AABB，座標範圍通常是 0..1，但像柵欄可略高於 1。
// 同一種方塊可由多個 BlockBounds 組成，例如樓梯、柵欄或薄片狀門板。
public record BlockBounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {

    // 將局部碰撞盒平移到世界座標後，檢查是否與玩家或其他世界座標 AABB 相交。
    // 邊界使用半開區間判斷，剛好貼齊時不算相交，避免貼牆或站在方塊上方被誤判卡住。
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

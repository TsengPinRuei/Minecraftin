package com.minecraftin.clone.world;

// Block-local axis-aligned bounds, usually within [0, 1]; fence posts extend to Y = 1.5.
// A block can contain several boxes, as with stairs and fences.
public record BlockBounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {

    // Translate to world coordinates and require overlap on all three axes.
    // Touching faces alone do not count as a collision.
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

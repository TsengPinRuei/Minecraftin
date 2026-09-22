package com.minecraftin.clone.world;

// A hit on block render geometry; the normal points outward for an entry from outside.
// A ray starting inside a box can return distance zero with a zero normal.
public record RaycastHit(
                int x,

                int y,

                int z,

                int normalX,

                int normalY,

                int normalZ,

                // Distance in world units; World.raycast normalizes the supplied direction.
                float distance,

                BlockType block) {
}

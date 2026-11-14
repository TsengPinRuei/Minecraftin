package com.minecraftin.clone.render;

import com.minecraftin.clone.config.GameConfig;
import com.minecraftin.clone.engine.Camera;
import com.minecraftin.clone.world.BlockType;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorldRenderBoundsTest {
    @Test
    void chunkBoundsContainEverySupportedShapeAtItsOuterCells() {
        var chunk = WorldRenderer.CHUNK_RENDER_BOUNDS;
        for (BlockType block : BlockType.values()) {
            for (var bounds : block.renderBoxes()) {
                assertTrue(chunk.minX() <= bounds.minX(), block.name());
                assertTrue(chunk.minY() <= bounds.minY(), block.name());
                assertTrue(chunk.minZ() <= bounds.minZ(), block.name());
                assertTrue(chunk.maxX() >= GameConfig.CHUNK_SIZE - 1 + bounds.maxX(), block.name());
                assertTrue(chunk.maxY() >= GameConfig.CHUNK_HEIGHT - 1 + bounds.maxY(), block.name());
                assertTrue(chunk.maxZ() >= GameConfig.CHUNK_SIZE - 1 + bounds.maxZ(), block.name());
            }
        }
    }

    @Test
    void fenceAboveBuildHeightRemainsVisibleAndDistantChunkIsCulled() {
        Camera camera = new Camera();
        camera.setPosition(8.5f, 128.1f, 16.1f);
        camera.setRotation(-90, 45);
        var projection = new Matrix4f().perspective((float) Math.toRadians(70), 1600f / 900,
                GameConfig.NEAR_PLANE, GameConfig.FAR_PLANE);
        var frustum = new FrustumIntersection(projection.mul(camera.viewMatrix(new Matrix4f())));
        assertFalse(frustum.testAab(0, 0, 0, 16, 128, 16), "Original box misses the visible fence top");
        assertTrue(WorldRenderer.chunkVisible(frustum, 0, 0));
        assertFalse(WorldRenderer.chunkVisible(frustum, 10_000, 10_000));
    }

    @Test
    void cloudOrderingChangesWhenTheCameraCrossesThePlane() {
        assertTrue(WorldRenderer.cloudsBehindWorld(149.999f));
        assertTrue(WorldRenderer.cloudsBehindWorld(150));
        assertFalse(WorldRenderer.cloudsBehindWorld(150.001f));
    }
}

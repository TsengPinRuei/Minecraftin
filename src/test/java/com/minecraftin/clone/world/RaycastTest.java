package com.minecraftin.clone.world;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class RaycastTest {
    @TempDir Path tempDir;

    @Test
    void rayPassesThroughEmptyHalfOfSlab() {
        World world = new World(tempDir.resolve("world.dat"), 123);
        world.setBlock(8, 120, 8, BlockType.OAK_SLAB);
        world.setBlock(8, 120, 7, BlockType.STONE);
        RaycastHit hit = world.raycast(new Vector3f(8.5f, 120.75f, 9.5f), new Vector3f(0, 0, -1), 5);
        assertNotNull(hit);
        assertEquals(7, hit.z());
        assertEquals(1.5f, hit.distance());
    }

    @Test
    void slabTopUsesItsActualSurfaceAndDistance() {
        World world = new World(tempDir.resolve("world.dat"), 123);
        world.setBlock(8, 120, 8, BlockType.OAK_SLAB);
        RaycastHit hit = world.raycast(new Vector3f(8.5f, 122, 8.5f), new Vector3f(0, -2, 0), 5);
        assertNotNull(hit);
        assertEquals(1.5f, hit.distance());
        assertEquals(1, hit.normalY());
    }

    @Test
    void fenceCanBeSelectedAboveItsCell() {
        World world = new World(tempDir.resolve("world.dat"), 123);
        world.setBlock(8, 120, 8, BlockType.OAK_FENCE);
        RaycastHit hit = world.raycast(new Vector3f(8.5f, 121.25f, 9.5f), new Vector3f(0, 0, -1), 5);
        assertNotNull(hit);
        assertEquals(120, hit.y());
        assertEquals(0.875f, hit.distance());
    }

    @Test
    void invalidRayDoesNotGenerateChunks() {
        World world = new World(tempDir.resolve("world.dat"), 123);
        assertNull(world.raycast(new Vector3f(), new Vector3f(), Float.POSITIVE_INFINITY));
        assertNull(world.raycast(new Vector3f(Float.NaN, 120, 0), new Vector3f(1, 0, 0), 5));
        assertEquals(0, world.chunkCount());
    }
}

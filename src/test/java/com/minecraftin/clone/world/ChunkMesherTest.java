package com.minecraftin.clone.world;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

final class ChunkMesherTest {
    @TempDir Path tempDir;

    @Test
    void snapshotAndMeshRemainUnchangedAfterWorldMutation() {
        World world = new World(tempDir.resolve("world.dat"), 123);
        Chunk center = world.getOrCreateChunk(0, 0);
        world.setBlock(15, 120, 15, BlockType.STONE);
        world.setBlock(16, 120, 16, BlockType.TORCH);
        ChunkSnapshot snapshot = ChunkSnapshot.capture(center, world);
        ChunkMesher.MeshData first = ChunkMesher.build(snapshot);
        int light = snapshot.block(16, 120, 16);
        int sky = snapshot.sky(15, 121, 15);
        world.setBlock(15, 120, 15, BlockType.AIR);
        world.setBlock(16, 120, 16, BlockType.AIR);
        world.setBlock(15, 121, 15, BlockType.STONE);

        assertEquals(BlockType.STONE, snapshot.get(15, 120, 15));
        assertEquals(BlockType.TORCH, snapshot.get(16, 120, 16));
        assertEquals(light, snapshot.block(16, 120, 16));
        assertEquals(sky, snapshot.sky(15, 121, 15));
        ChunkMesher.MeshData second = ChunkMesher.build(snapshot);
        assertArrayEquals(first.opaqueVertices(), second.opaqueVertices());
        assertArrayEquals(first.translucentVertices(), second.translucentVertices());
    }

    @Test
    void waterDoesNotDrawFacesAgainstOpaqueBlocks() {
        World world = new World(tempDir.resolve("world.dat"), 123);
        Chunk chunk = world.getOrCreateChunk(0, 0);
        Arrays.fill(chunk.rawBlocks(), (short) 0);
        chunk.setRaw(8, 100, 8, (short) BlockType.WATER.id());
        chunk.setRaw(9, 100, 8, (short) BlockType.STONE.id());
        ChunkMesher.MeshData mesh = ChunkMesher.build(ChunkSnapshot.capture(chunk, world));
        assertEquals(5 * 6 * ChunkMesher.STRIDE_FLOATS, mesh.translucentVertices().length);
        assertEquals(6 * 6 * ChunkMesher.STRIDE_FLOATS, mesh.opaqueVertices().length);
    }

    @Test
    void missingNeighborsHaveStableAirAndSkyWithoutGeneratingTerrain() {
        World world = new World(tempDir.resolve("world.dat"), 123);
        Chunk chunk = world.getOrCreateChunk(-1, -1);
        ChunkSnapshot snapshot = ChunkSnapshot.capture(chunk, world);
        assertEquals(1, world.chunkCount());
        assertEquals(BlockType.AIR, snapshot.get(-1, 120, -1));
        assertEquals(15, snapshot.sky(-1, 120, -1));
        assertEquals(BlockType.BEDROCK, snapshot.get(0, -1, 0));
    }
}

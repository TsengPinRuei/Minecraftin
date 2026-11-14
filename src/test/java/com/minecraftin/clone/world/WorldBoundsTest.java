package com.minecraftin.clone.world;

import com.minecraftin.clone.config.GameConfig;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(20)
final class WorldBoundsTest {
    @TempDir
    Path temporary;

    @Test
    void sharedLimitsPreserveFloatPrecisionAndPlayerClearance() {
        assertTrue(Math.ulp(WorldBounds.MAX_POSITION) < 0.001f);
        assertTrue(WorldBounds.containsBlock(WorldBounds.MIN_BLOCK, WorldBounds.MAX_BLOCK));
        assertFalse(WorldBounds.containsBlock(WorldBounds.MIN_BLOCK - 1, 0));
        assertFalse(WorldBounds.containsBlock(0, WorldBounds.MAX_BLOCK + 1));
        assertTrue(WorldBounds.containsPlayerPosition(WorldBounds.MIN_PLAYER_XZ, 120, WorldBounds.MAX_PLAYER_XZ));
        assertFalse(WorldBounds.containsPlayerPosition(WorldBounds.MIN_BLOCK, 120, 0));
        assertFalse(WorldBounds.containsPlayerPosition(0, WorldBounds.MAX_Y, 0));
    }

    @Test
    void invalidSpatialInputsReturnWithoutGeneratingTerrain() {
        try (World world = new World(temporary.resolve("world.dat"), 123)) {
            for (int value : new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE, WorldBounds.MIN_BLOCK - 1,
                    WorldBounds.MAX_BLOCK + 1, 8388616, -8388616}) {
                assertFalse(world.setBlock(value, 120, 0, BlockType.TORCH));
                assertFalse(world.setBlock(0, 120, value, BlockType.TORCH));
                assertThrows(IllegalArgumentException.class, () -> world.getBlock(value, 120, 0));
                assertThrows(IllegalArgumentException.class, () -> world.topSolidY(0, value));
                assertEquals(BlockType.AIR, world.peekBlock(value, 120, 0));
            }
            assertThrows(IllegalArgumentException.class, () -> new Chunk(Integer.MAX_VALUE, 0));
            assertThrows(IllegalArgumentException.class, () -> world.getOrCreateChunk(Integer.MAX_VALUE, 0));
            LightEngine light = new LightEngine(world);
            assertThrows(IllegalArgumentException.class,
                    () -> light.onBlockChanged(8388616, 120, 0, BlockType.AIR, BlockType.TORCH));
            assertThrows(IllegalArgumentException.class, () -> world.ensureChunksAround(0, 0, Integer.MAX_VALUE));
            assertEquals(0, world.chunkCount());
        }
    }

    @Test
    void extremeRaysAndRespawnPositionsAreRejectedWithinTimeout() {
        try (World world = new World(temporary.resolve("world.dat"), 123)) {
            assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
                for (float value : new float[]{Float.MAX_VALUE, -Float.MAX_VALUE, (float) Integer.MAX_VALUE,
                        (float) Integer.MIN_VALUE, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
                    for (int axis = 0; axis < 3; axis++) {
                        Vector3f position = new Vector3f(0, 120, 0);
                        position.setComponent(axis, value);
                        assertNull(world.raycast(position, new Vector3f(1, 0, 0), 5));
                        assertThrows(IllegalArgumentException.class,
                                () -> world.setRespawnPosition(position.x, position.y, position.z));
                    }
                }
                assertNull(world.raycast(new Vector3f(0, 200, 0), new Vector3f(1, 0, 0), Float.MAX_VALUE));
                // The longest supported ray above terrain traverses only bounded cells and stops at the border.
                assertNull(world.raycast(new Vector3f(0, 200, 0), new Vector3f(1, 1, 1), WorldBounds.MAX_RAY_DISTANCE));
            });
            assertEquals(0, world.chunkCount());
        }
    }

    @Test
    void chunkLoadingClipsItsNeighborhoodAtBothWorldCorners() {
        try (World world = new World(temporary.resolve("world.dat"), 123)) {
            world.ensureChunksAround(WorldBounds.MIN_CHUNK, WorldBounds.MIN_CHUNK, 1);
            world.ensureChunksAround(WorldBounds.MAX_CHUNK, WorldBounds.MAX_CHUNK, 1);
            assertEquals(6, world.chunkCount());
            assertNull(world.getChunkIfLoaded(WorldBounds.MIN_CHUNK - 1, WorldBounds.MIN_CHUNK));
            assertNull(world.getChunkIfLoaded(WorldBounds.MAX_CHUNK, WorldBounds.MAX_CHUNK + 1));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {-8192, 8191})
    void boundaryLightPropagatesRemovesAndSurvivesReloadWithoutCoordinateAliasing(int edge) {
        Path file = temporary.resolve("world.dat");
        int inward = edge < 0 ? 1 : -1;
        try (World world = new World(file, 123)) {
            world.initialize();
            for (boolean zAxis : new boolean[]{false, true}) {
                int x = zAxis ? 8 : edge;
                int z = zAxis ? edge : 8;
                world.setBlock(x, 120, z, BlockType.TORCH);
                assertEquals(13, world.blockLightAt(x + (zAxis ? 0 : inward), 120, z + (zAxis ? inward : 0)));
                assertEquals(0, world.blockLightAt(zAxis ? 8 : -edge, 120, zAxis ? -edge : 8));
            }
            world.save();
        }
        try (World loaded = new World(file, 456)) {
            loaded.initialize();
            for (boolean zAxis : new boolean[]{false, true}) {
                int x = zAxis ? 8 : edge;
                int z = zAxis ? edge : 8;
                assertEquals(13, loaded.blockLightAt(x + (zAxis ? 0 : inward), 120, z + (zAxis ? inward : 0)));
                loaded.setBlock(x, 120, z, BlockType.AIR);
                assertEquals(0, loaded.blockLightAt(x + (zAxis ? 0 : inward), 120, z + (zAxis ? inward : 0)));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {-8192, 8191})
    void boundaryWaterQueueRoundTripsAndFlowsOnlyInsideTheWorld(int edge) {
        Path file = temporary.resolve("world.dat");
        int inward = edge < 0 ? 1 : -1;
        try (World world = new World(file, 123)) {
            world.initialize();
            for (boolean zAxis : new boolean[]{false, true}) {
                int x = zAxis ? 8 : edge;
                int z = zAxis ? edge : 8;
                world.setBlock(x, 119, z, BlockType.STONE);
                world.setBlock(x + (zAxis ? 0 : inward), 119, z + (zAxis ? inward : 0), BlockType.STONE);
                world.setBlock(x, 120, z, BlockType.WATER);
            }
            world.save();
        }
        try (World loaded = new World(file, 456)) {
            loaded.initialize();
            loaded.update(0.25f);
            for (boolean zAxis : new boolean[]{false, true}) {
                int x = zAxis ? 8 : edge;
                int z = zAxis ? edge : 8;
                assertEquals(7, loaded.getBlock(x + (zAxis ? 0 : inward), 120, z + (zAxis ? inward : 0)).waterStrength());
                assertEquals(BlockType.AIR, loaded.peekBlock(x - (zAxis ? 0 : inward), 120, z - (zAxis ? inward : 0)));
            }
            assertEquals(2, loaded.chunkCount());
            loaded.save();
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {-8177, 8175})
    void lightCrossesChunkBoundaryOnBothAxesNearWorldEdges(int edge) {
        Path file = temporary.resolve("world.dat");
        try (World world = new World(file, 123)) {
            world.initialize();
            for (boolean zAxis : new boolean[]{false, true}) {
                int x = zAxis ? 8 : edge;
                int z = zAxis ? edge : 8;
                int nx = x + (zAxis ? 0 : 1);
                int nz = z + (zAxis ? 1 : 0);
                world.getOrCreateChunk(Math.floorDiv(x, GameConfig.CHUNK_SIZE), Math.floorDiv(z, GameConfig.CHUNK_SIZE));
                world.getOrCreateChunk(Math.floorDiv(nx, GameConfig.CHUNK_SIZE), Math.floorDiv(nz, GameConfig.CHUNK_SIZE));
                world.setBlock(x, 120, z, BlockType.TORCH);
                assertEquals(13, world.blockLightAt(nx, 120, nz));
            }
            world.save();
        }
        try (World loaded = new World(file, 456)) {
            loaded.initialize();
            for (boolean zAxis : new boolean[]{false, true}) {
                int x = zAxis ? 8 : edge;
                int z = zAxis ? edge : 8;
                int nx = x + (zAxis ? 0 : 1);
                int nz = z + (zAxis ? 1 : 0);
                assertEquals(13, loaded.blockLightAt(nx, 120, nz));
                loaded.setBlock(x, 120, z, BlockType.AIR);
                assertEquals(0, loaded.blockLightAt(nx, 120, nz));
            }
        }
    }

    @Test
    void raysTargetBothBoundaryCellsAndDoNotLeaveWorld() {
        try (World world = new World(temporary.resolve("world.dat"), 123)) {
            for (int edge : new int[]{WorldBounds.MIN_BLOCK, WorldBounds.MAX_BLOCK}) {
                int inward = edge < 0 ? 1 : -1;
                world.setBlock(edge, 120, 8, BlockType.GLASS);
                RaycastHit hit = world.raycast(new Vector3f(edge + 0.5f + 2 * inward, 120.5f, 8.5f),
                        new Vector3f(-inward, 0, 0), 5);
                assertNotNull(hit);
                assertEquals(edge, hit.x());
                world.setBlock(edge, 120, 8, BlockType.AIR);
                assertNull(world.raycast(new Vector3f(edge + 0.5f, 120.5f, 8.5f), new Vector3f(-inward, 0, 0), 5));
            }
            assertEquals(2, world.chunkCount());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    void invalidSavedRespawnChunkAndWaterCoordinatesPreserveOriginalFile(int kind) throws Exception {
        Path file = temporary.resolve("world.dat");
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(file))) {
            out.writeInt(0x4D434C4E);
            out.writeInt(4);
            out.writeLong(123);
            out.writeBoolean(kind < 2);
            if (kind < 2) {
                out.writeFloat(kind == 0 ? Float.MAX_VALUE : WorldBounds.MIN_BLOCK);
                out.writeFloat(120);
                out.writeFloat(8);
            }
            out.writeFloat(0.03f);
            out.writeInt(kind == 2 || kind == 3 ? 1 : 0);
            if (kind == 2 || kind == 3) {
                out.writeInt(kind == 2 ? WorldBounds.MIN_CHUNK - 1 : 0);
                out.writeInt(kind == 3 ? WorldBounds.MAX_CHUNK + 1 : 0);
                int cells = GameConfig.CHUNK_SIZE * GameConfig.CHUNK_SIZE * GameConfig.CHUNK_HEIGHT;
                out.writeInt(cells);
                for (int i = 0; i < cells; i++) out.writeShort(0);
            }
            out.writeFloat(0);
            out.writeInt(kind >= 4 ? 1 : 0);
            if (kind >= 4) {
                int x = kind == 4 ? WorldBounds.MIN_BLOCK - 1 : 0;
                int z = kind == 5 ? WorldBounds.MAX_BLOCK + 1 : 0;
                out.writeLong(((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | 120);
            }
        }
        byte[] before = Files.readAllBytes(file);
        try (World world = new World(file, 456)) {
            assertThrows(IllegalStateException.class, world::initialize);
            assertThrows(IllegalStateException.class, world::save);
            assertEquals(0, world.chunkCount());
        }
        assertArrayEquals(before, Files.readAllBytes(file));
    }
}

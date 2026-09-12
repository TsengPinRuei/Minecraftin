package com.minecraftin.clone.world;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class WorldPersistenceTest {
    @TempDir
    Path tempDir;

    @Test
    void roundTripPreservesBlocksSeedPositionAndTime() {
        Path file = tempDir.resolve("world.dat");
        World original = new World(file, 123);
        original.initialize();
        original.setBlock(-1, 120, -17, BlockType.GLASS);
        original.setRespawnPosition(-0.5f, 121, -16.5f);
        original.update(2);
        original.save();
        assertFalse(original.hasPendingSave());

        World loaded = new World(file, 456);
        loaded.initialize();
        assertEquals(123, loaded.seed());
        assertEquals(BlockType.GLASS, loaded.getBlock(-1, 120, -17));
        Vector3f position = new Vector3f();
        assertTrue(loaded.tryGetSavedRespawnPosition(position));
        assertEquals(new Vector3f(-0.5f, 121, -16.5f), position);
        assertEquals(original.timeOfDay(), loaded.timeOfDay());
        assertFalse(loaded.hasPendingSave());
    }

    @Test
    void corruptExistingSaveIsRejectedWithoutReplacingIt() throws Exception {
        Path file = tempDir.resolve("world.dat");
        byte[] corrupt = {1, 2, 3};
        Files.write(file, corrupt);
        World world = new World(file, 123);
        assertThrows(IllegalStateException.class, world::initialize);
        assertArrayEquals(corrupt, Files.readAllBytes(file));
        assertEquals(0, world.chunkCount());
    }

    @Test
    void negativeChunkCountIsRejected() throws Exception {
        Path file = tempDir.resolve("world.dat");
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(file))) {
            out.writeInt(0x4D434C4E);
            out.writeInt(1);
            out.writeLong(123);
            out.writeInt(-1);
        }
        assertThrows(IllegalStateException.class, () -> new World(file, 123).initialize());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void readsLegacySaveVersions(int version) throws Exception {
        Path file = tempDir.resolve("world.dat");
        Chunk chunk = new Chunk(-1, 0);
        chunk.setRaw(15, 120, 8, (short) BlockType.TORCH.id());
        chunk.setRaw(8, 120, 8, (short) BlockType.WATER.id());
        chunk.setRaw(8, 119, 8, (short) BlockType.STONE.id());
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(file))) {
            out.writeInt(0x4D434C4E);
            out.writeInt(version);
            out.writeLong(789);
            if (version >= 2) {
                out.writeBoolean(true);
                out.writeFloat(-0.5f);
                out.writeFloat(121);
                out.writeFloat(8.5f);
            }
            if (version >= 3) out.writeFloat(0.5f);
            out.writeInt(1);
            out.writeInt(-1);
            out.writeInt(0);
            out.writeInt(chunk.rawBlocks().length);
            for (short block : chunk.rawBlocks()) out.writeShort(block);
        }
        World loaded = new World(file, 123);
        loaded.initialize();
        assertEquals(789, loaded.seed());
        assertEquals(BlockType.TORCH, loaded.getBlock(-1, 120, 8));
        assertEquals(14, loaded.blockLightAt(-1, 120, 8));
        assertEquals(version >= 2, loaded.tryGetSavedRespawnPosition(new Vector3f()));
        assertEquals(version >= 3 ? 0.5f : 0.03f, loaded.timeOfDay());
        loaded.update(0.25f);
        assertEquals(7, loaded.getBlock(-7, 120, 8).waterStrength());
    }

    @Test
    void failedSaveKeepsChangesPendingAndCleansTemporaryFiles() throws Exception {
        Path file = tempDir.resolve("world.dat");
        World world = new World(file, 123);
        world.initialize();
        world.setBlock(8, 120, 8, BlockType.GLASS);
        Files.createDirectory(file);
        Files.writeString(file.resolve("keep.txt"), "existing data");
        assertThrows(IllegalStateException.class, world::save);
        assertTrue(world.hasPendingSave());
        assertEquals("existing data", Files.readString(file.resolve("keep.txt")));
        try (var files = Files.list(tempDir)) {
            assertEquals(1, files.count());
        }
    }
}

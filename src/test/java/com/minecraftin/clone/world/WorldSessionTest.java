package com.minecraftin.clone.world;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Timeout(30)
final class WorldSessionTest {
    @TempDir
    Path temporary;

    @Test
    void rejectsStaleSessionBeforeLoadAndPreservesSavedChanges() {
        Path file = temporary.resolve("world.dat");
        try (World first = new World(file, 123); World second = new World(file, 456)) {
            first.initialize();
            first.setBlock(8, 120, 8, BlockType.STONE);
            first.save();
            assertThrows(IllegalStateException.class, second::initialize);
            assertEquals(0, second.chunkCount());
            assertThrows(IllegalStateException.class, second::save);
            first.setBlock(8, 120, 8, BlockType.GLASS);
            first.save();
        }
        try (World loaded = new World(file, 456)) {
            loaded.initialize();
            assertEquals(BlockType.GLASS, loaded.getBlock(8, 120, 8));
        }
        assertTrue(Files.exists(temporary.resolve("world.dat.lock")), "A sidecar's existence is not an active lock");
    }

    @Test
    void secondJvmIsRejectedEvenAfterSameJvmOverlapAttemptAndAtomicSave() throws Exception {
        Path file = temporary.resolve("world.dat");
        try (World first = new World(file, 123); World second = new World(file, 456)) {
            first.initialize();
            assertThrows(IllegalStateException.class, second::initialize);
            first.save();
            assertTrue(runChild(file).startsWith("REJECTED: World is already open"));
            first.save();
            assertTrue(runChild(file).startsWith("REJECTED: World is already open"));
        }
        assertEquals("OPEN", runChild(file));
    }

    @Test
    void operatingSystemReleasesLockAfterAbruptProcessExit() throws Exception {
        Path file = temporary.resolve("world.dat");
        Process child = startChild(file, true);
        try {
            assertEquals("OPEN", awaitLine(child));
            try (World blocked = new World(file, 123)) {
                assertThrows(IllegalStateException.class, blocked::initialize);
            }
        } finally {
            child.destroyForcibly();
            assertTrue(child.waitFor(10, TimeUnit.SECONDS), "Child JVM did not exit");
        }
        try (World next = new World(file, 123)) {
            next.initialize();
            next.save();
        }
    }

    @Test
    void directoryAndFileSymlinksCannotBypassOwnership() throws Exception {
        Path directoryAlias = temporary.resolve("alias");
        Path realDirectory = Files.createDirectory(temporary.resolve("real"));
        Path file = realDirectory.resolve("world.dat");
        try {
            Files.createSymbolicLink(directoryAlias, realDirectory);
        } catch (UnsupportedOperationException | java.io.IOException | SecurityException e) {
            assumeTrue(false, "Symbolic links unavailable: " + e.getMessage());
        }
        try (World first = new World(file, 123)) {
            first.initialize();
            first.save();
            Path fileAlias = temporary.resolve("world-link.dat");
            Files.createSymbolicLink(fileAlias, file);
            for (Path alias : new Path[]{directoryAlias.resolve("world.dat"), fileAlias,
                    realDirectory.resolve(".").resolve("world.dat")}) {
                try (World second = new World(alias, 456)) {
                    assertThrows(IllegalStateException.class, second::initialize);
                }
                assertTrue(runChild(alias).startsWith("REJECTED: World is already open"));
            }
        }
        assertEquals("OPEN", runChild(directoryAlias.resolve("world.dat")));
    }

    @Test
    void caseAliasesOfAnUnsavedWorldCannotReleaseItsExistingLock() throws Exception {
        Path file = temporary.resolve("case-world.dat");
        try (World first = new World(file, 123)) {
            first.initialize();
            Path alias = temporary.resolve("CASE-WORLD.DAT");
            // On case-sensitive volumes these are distinct worlds, so there is no alias to test.
            if (Files.exists(temporary.resolve("CASE-WORLD.DAT.lock"))) {
                try (World second = new World(alias, 456)) {
                    assertThrows(IllegalStateException.class, second::initialize);
                }
                assertTrue(runChild(file).startsWith("REJECTED: World is already open"));
            }
        }
    }

    @Test
    void initializationFailureReleasesOwnershipAndNeverMakesCorruptSaveWritable() throws Exception {
        Path file = temporary.resolve("world.dat");
        byte[] bytes = {1, 2, 3};
        Files.write(file, bytes);
        try (World failed = new World(file, 123)) {
            assertThrows(IllegalStateException.class, failed::initialize);
            assertThrows(IllegalStateException.class, failed::save);
            assertArrayEquals(bytes, Files.readAllBytes(file));
        }
        Files.delete(file);
        assertEquals("OPEN", runChild(file));
    }

    @Test
    void saveRequiresLiveOwnershipAndCloseIsIdempotent() {
        World world = new World(temporary.resolve("world.dat"), 123);
        assertThrows(IllegalStateException.class, world::save);
        world.initialize();
        world.close();
        world.close();
        assertThrows(IllegalStateException.class, world::save);
        assertThrows(IllegalStateException.class, world::initialize);
    }

    private static Process startChild(Path file, boolean hold) throws Exception {
        String classpath = Stream.of(World.class, Vector3f.class, WorldSessionProcess.class)
                .map(type -> {
                    try {
                        return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                }).distinct().collect(Collectors.joining(java.io.File.pathSeparator));
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        return new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", executable).toString(),
                "-cp", classpath, WorldSessionProcess.class.getName(), file.toString(), hold ? "hold" : "open")
                .redirectErrorStream(true).start();
    }

    private static String awaitLine(Process child) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(child.getInputStream()));
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!reader.ready() && child.isAlive() && System.nanoTime() < deadline) Thread.sleep(10);
        assertTrue(reader.ready() || !child.isAlive(), "Child JVM did not report ownership");
        return reader.readLine();
    }

    private static String runChild(Path file) throws Exception {
        Process child = startChild(file, false);
        try {
            assertTrue(child.waitFor(10, TimeUnit.SECONDS), "Child JVM did not exit");
            assertEquals(0, child.exitValue());
            return new String(child.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).strip();
        } finally {
            if (child.isAlive()) child.destroyForcibly();
        }
    }
}

package com.minecraftin.clone.game;

import com.minecraftin.clone.world.World;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

final class GameSaveTest {
    @TempDir Path directory;

    @Test
    void actualWriteFailureIsVisibleWithoutF3AndRetryClearsIt() throws Exception {
        Path file = directory.resolve("world.dat");
        try (World world = new World(file, 123)) {
            world.initialize();
            Game game = new Game(world);
            Files.createDirectory(file);
            Path blocker = Files.writeString(file.resolve("blocker"), "preserve me");
            assertEquals(false, invoke(game, "safeSaveWorld", new Class<?>[] {boolean.class}, false));
            String[] lines = (String[]) invoke(game, "buildOverlayLines", new Class<?>[0]);
            assertTrue(lines[0].contains("SAVE FAILED"));
            assertTrue(Files.exists(blocker));
            Files.delete(blocker);
            Files.delete(file);
            assertEquals(true, invoke(game, "safeSaveWorld", new Class<?>[] {boolean.class}, false));
            assertNull(invoke(game, "buildOverlayLines", new Class<?>[0]));
            assertTrue(Files.isRegularFile(file));
        }
    }

    @Test
    void finalSaveFailureIsSuppressedUnderPrimaryCrash() throws Exception {
        Game game = new Game(new World(directory.resolve("unopened.dat"), 123));
        var field = Game.class.getDeclaredField("worldInitialized");
        field.setAccessible(true);
        field.setBoolean(game, true);
        RuntimeException primary = new RuntimeException("renderer failure");
        invoke(game, "saveOnUnexpectedExit", new Class<?>[] {Throwable.class}, primary);
        assertEquals("renderer failure", primary.getMessage());
        assertEquals(1, primary.getSuppressed().length);
        assertEquals("Final world save failed", primary.getSuppressed()[0].getMessage());
        InvocationTargetException onlySave = assertThrows(InvocationTargetException.class, () ->
                invoke(game, "saveOnUnexpectedExit", new Class<?>[] {Throwable.class}, (Object) null));
        assertEquals("Final world save failed", onlySave.getCause().getMessage());
    }

    private static Object invoke(Game game, String name, Class<?>[] types, Object... arguments) throws Exception {
        var method = Game.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(game, arguments);
    }
}

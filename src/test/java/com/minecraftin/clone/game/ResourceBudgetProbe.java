package com.minecraftin.clone.game;

import com.minecraftin.clone.world.BlockType;
import com.minecraftin.clone.world.World;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// A bounded exploration measurement, separate from unit tests and the player's real save.
public final class ResourceBudgetProbe {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("minecraftin-resource-probe-");
        Path file = directory.resolve("world.dat");
        List<String> rows = new ArrayList<>();
        rows.add("distance_blocks,chunks,array_payload_bytes,heap_used_bytes,save_bytes,save_ms");
        try {
            try (World world = new World(file, 123)) {
                world.initialize();
                for (int step = 0; step < 64; step++) {
                    int chunkX = step * 8;
                    world.ensureChunksAround(chunkX, 0, 1);
                    world.setBlock(chunkX * 16 + 8, 120, 8, BlockType.GLASS);
                    if ((step + 1) % 8 == 0) {
                        long started = System.nanoTime();
                        world.save();
                        double millis = (System.nanoTime() - started) / 1_000_000.0;
                        Runtime runtime = Runtime.getRuntime();
                        rows.add(String.format(Locale.ROOT, "%d,%d,%d,%d,%d,%.3f", step * 128,
                                world.chunkCount(), ResourceBudget.chunkPayloadBytes(world.chunkCount()),
                                runtime.totalMemory() - runtime.freeMemory(), Files.size(file), millis));
                    }
                }
            }
            try (World reloaded = new World(file, 123)) {
                reloaded.initialize();
                for (int step = 0; step < 64; step++) {
                    if (reloaded.getBlock(step * 128 + 8, 120, 8) != BlockType.GLASS) {
                        throw new AssertionError("Lost edit at route step " + step);
                    }
                }
            }
            Path report = Path.of("build/reports/resource-budget/exploration.csv");
            Files.createDirectories(report.getParent());
            Files.write(report, rows);
            rows.forEach(System.out::println);
            System.out.println("Resource probe passed: 64 saved edits restored; " + report.toAbsolutePath());
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private ResourceBudgetProbe() { }
}

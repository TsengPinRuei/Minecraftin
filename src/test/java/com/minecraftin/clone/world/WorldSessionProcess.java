package com.minecraftin.clone.world;

import java.nio.file.Path;

// A separate JVM exercises OS lock ownership rather than only Java's overlap detection.
public final class WorldSessionProcess {
    private WorldSessionProcess() {
    }

    public static void main(String[] args) throws Exception {
        try (World world = new World(Path.of(args[0]), 123)) {
            try {
                world.initialize();
            } catch (IllegalStateException e) {
                System.out.println("REJECTED: " + e.getMessage());
                return;
            }
            System.out.println("OPEN");
            System.out.flush();
            if (args.length > 1 && args[1].equals("hold")) System.in.read();
        }
    }
}

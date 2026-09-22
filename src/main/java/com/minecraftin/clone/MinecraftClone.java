package com.minecraftin.clone;

import com.minecraftin.clone.game.Game;

// Game.run() owns the application lifecycle and resource cleanup.
public final class MinecraftClone {

    private MinecraftClone() {
    }

    public static void main(String[] args) {
        new Game().run();
    }
}

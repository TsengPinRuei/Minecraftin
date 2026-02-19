package com.minecraftin.clone;

import com.minecraftin.clone.game.Game;

public final class MinecraftClone {
    private MinecraftClone() {
    }

    public static void main(String[] args) {
        new Game().run();
    }
}

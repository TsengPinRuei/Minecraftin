package com.minecraftin.clone;

import com.minecraftin.clone.game.Game;

// 啟動整個遊戲。
public final class MinecraftClone {

    // 私有建構子，避免這個類別被建立成物件。
    private MinecraftClone() {
    }

    // 程式開始執行的位置。
    public static void main(String[] args) {
        // 建立 Game 物件並啟動遊戲主流程。
        new Game().run();
    }
}
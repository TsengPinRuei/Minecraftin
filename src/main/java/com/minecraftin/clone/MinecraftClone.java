package com.minecraftin.clone;

import com.minecraftin.clone.game.Game;

// JVM 入口點，只負責把控制權交給 Game；實際生命週期與資源釋放都集中在 Game.run()。
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

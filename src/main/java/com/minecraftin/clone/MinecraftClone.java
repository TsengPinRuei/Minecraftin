// 宣告此檔案所屬的套件。
package com.minecraftin.clone;

// 匯入後續會使用到的型別或函式。
import com.minecraftin.clone.game.Game;

// 定義主要型別與其結構。
public final class MinecraftClone {
    // 定義類別內部使用的方法。
    private MinecraftClone() {
    }

    // 定義對外可呼叫的方法。
    public static void main(String[] args) {
        // 呼叫方法執行對應功能。
        new Game().run();
    }
}

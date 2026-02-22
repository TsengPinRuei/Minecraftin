// 宣告此檔案所屬的套件。
package com.minecraftin.clone.world;

// 定義主要型別與其結構。
public record RaycastHit(
        // 下一行程式碼負責執行目前步驟。
        int x,
        // 下一行程式碼負責執行目前步驟。
        int y,
        // 下一行程式碼負責執行目前步驟。
        int z,
        // 下一行程式碼負責執行目前步驟。
        int normalX,
        // 下一行程式碼負責執行目前步驟。
        int normalY,
        // 下一行程式碼負責執行目前步驟。
        int normalZ,
        // 下一行程式碼負責執行目前步驟。
        float distance,
        // 下一行程式碼負責執行目前步驟。
        BlockType block
// 下一行程式碼負責執行目前步驟。
) {
}

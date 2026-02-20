// 說明：宣告此檔案所屬的套件。
package com.minecraftin.clone.world;

// 說明：定義主要型別與其結構。
public record RaycastHit(
        // 說明：下一行程式碼負責執行目前步驟。
        int x,
        // 說明：下一行程式碼負責執行目前步驟。
        int y,
        // 說明：下一行程式碼負責執行目前步驟。
        int z,
        // 說明：下一行程式碼負責執行目前步驟。
        int normalX,
        // 說明：下一行程式碼負責執行目前步驟。
        int normalY,
        // 說明：下一行程式碼負責執行目前步驟。
        int normalZ,
        // 說明：下一行程式碼負責執行目前步驟。
        float distance,
        // 說明：下一行程式碼負責執行目前步驟。
        BlockType block
// 說明：下一行程式碼負責執行目前步驟。
) {
}

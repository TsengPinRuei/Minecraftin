# Minecraftin

這是一個以 Java + LWJGL 製作、受經典 Minecraft 啟發的體素沙盒專案。

目前重點是穩定且可擴充的**創造模式核心**：
- 程序化區塊地形
- 第一人稱移動與視角
- 破壞／放置方塊
- 世界存檔與讀檔

## 1) 這份文件適合誰

這份 README 用最容易理解的方式寫給：
- 沒跑過 Java 遊戲專案的新手
- 需要完整指令與排錯流程的使用者
- 想看架構與設定入口的開發者

如果你只想快速開啟遊戲，直接看 **2) 快速開始**。

## 2) 快速開始

### macOS / Linux

```bash
cd /你的/Minecraftin/路徑
./gradlew run
```

### Windows（PowerShell）

```powershell
cd C:\你的\Minecraftin\路徑
.\gradlew.bat run
```

第一次執行會下載 Gradle 依賴，可能需要幾分鐘。

## 3) 環境需求

### 最低需求

- 作業系統：macOS / Windows / Linux
- Java：JDK 17 以上
- 顯示卡：支援 OpenGL 3.3
- 記憶體：至少 4 GB（建議 8 GB）
- 磁碟空間：至少 1 GB（依賴與快取）

### 專案實際設定

- 原始碼以 Java `release 17` 編譯
- 已在 Java 21 執行測試
- macOS 透過 Gradle 執行時已內建 `-XstartOnFirstThread`

## 4) 先確認 Java（必要）

```bash
java -version
```

你應該看到 `17`、`21` 或更新版本。

### 常見情況

- `java: command not found`
  - 代表 Java 未安裝或 PATH 未設定。
- 版本過舊
  - 請安裝 JDK 17+，並讓終端機使用新版本。

## 5) 建置與執行指令

### 執行遊戲

- macOS/Linux：`./gradlew run`
- Windows：`gradlew.bat run`

### 建置（產生 jar / 發行檔）

- macOS/Linux：`./gradlew build -x test`
- Windows：`gradlew.bat build -x test`

### 清理建置檔

- macOS/Linux：`./gradlew clean`
- Windows：`gradlew.bat clean`

### 如果環境會擋 Gradle daemon socket

- macOS/Linux：
  ```bash
  GRADLE_USER_HOME=.gradle-home ./gradlew --no-daemon run
  ```
- Windows（PowerShell）：
  ```powershell
  $env:GRADLE_USER_HOME = ".gradle-home"
  .\gradlew.bat --no-daemon run
  ```

## 6) 遊玩方式

啟動後：
- 目前沒有主選單，會直接進入世界
- 一開始滑鼠是自由狀態
- 先在遊戲視窗按一下左鍵，才會鎖定滑鼠並開始控制角色

### 操作鍵位

- `W/A/S/D`：移動
- `Mouse`：視角
- `Left Ctrl`：衝刺 / 飛行加速
- `Space`（雙擊）：切換飛行模式
- `Space`（飛行中）：向上飛
- `Left Shift`（飛行中）：向下飛
- `Left Click`：破壞方塊
- `Right Click`：放置目前選取方塊
- `1-9` 或 `Mouse Wheel`：切換方塊欄
- `Esc`：釋放滑鼠
- `Left Click`（游標自由時）：重新鎖定滑鼠
- `Q`：離開遊戲

### 預設方塊欄（9 格）

1. 紅方塊
2. 橙方塊
3. 黃方塊
4. 綠方塊
5. 藍方塊
6. 紫方塊
7. 泥土
8. 石頭
9. 玻璃

### 全螢幕說明

- 遊戲預設以視窗模式啟動。
- 在 macOS 可用視窗左上綠色按鈕選單切換到原生全螢幕。

## 7) 存檔與重設世界

### 存檔位置

- `saves/world.dat`

### 自動存檔

- 每 20 秒
- 關閉遊戲時也會存

### 重設世界（重新開始）

```bash
rm -f saves/world.dat
```

Windows（PowerShell）：

```powershell
Remove-Item .\saves\world.dat -ErrorAction SilentlyContinue
```

刪除後重新執行遊戲即可生成新世界。

## 8) 常見錯誤與處理方式

### 錯誤：GLFW 必須在主執行緒（macOS）

常見訊息：

`GLFW may only be used on the main thread ... run the JVM with -XstartOnFirstThread`

處理方式：
- 直接用 `./gradlew run`（已自動帶參數）
- 若你手動啟動 JVM，請加上 `-XstartOnFirstThread`

### 錯誤：`./gradlew: Permission denied`

處理方式：

```bash
chmod +x gradlew
./gradlew run
```

### 錯誤：`java: command not found`

處理方式：
- 安裝 JDK 17+
- 重開終端機
- 再次確認 `java -version`

### 問題：黑畫面或程式立刻關閉

可能原因與處理：
- 顯示卡驅動 / OpenGL 不相容 -> 更新驅動
- 遠端桌面或虛擬機沒有 OpenGL 3.3 -> 改在本機執行

### 問題：人物無法走動

通常是滑鼠尚未鎖定。

處理方式：
- 在遊戲視窗點一次左鍵
- 若仍有問題，按 `Esc` 釋放後再左鍵重新鎖定

### 錯誤：世界存檔失敗

常見訊息：

`World save failed: Failed to save world to saves/world.dat`

處理方式：
- 確認專案資料夾可寫入
- 確認 `saves/` 可建立/可寫
- 避免在唯讀或受權限限制的位置執行

### 錯誤：依賴下載失敗

處理方式：
- 檢查網路連線
- 重新執行指令
- 受公司/校園網路限制時，設定 Gradle proxy

## 9) 目前完成內容與尚未完成內容

- [x] 多生態區塊地形生成
- [x] 洞穴、海平面、水體、樹木
- [x] 創造模式移動與飛行切換
- [x] 碰撞安全的破壞/放置
- [x] 世界持久化存檔
- [ ] 生存系統（血量、飢餓、合成、背包）
- [ ] 生物與 AI
- [ ] 日夜循環與天氣

## 10) 重要設定檔

主要設定檔：
- `src/main/java/com/minecraftin/clone/config/GameConfig.java`

常調整的項目：
- 視窗大小與標題
- 可視距離與區塊尺寸
- 移動/重力/跳躍參數
- 互動距離與冷卻
- 存檔路徑與預設種子

## 11) 專案結構

- `src/main/java/com/minecraftin/clone/MinecraftClone.java`（程式入口）
- `src/main/java/com/minecraftin/clone/game/Game.java`（主迴圈）
- `src/main/java/com/minecraftin/clone/engine/*`（視窗、輸入、相機、Shader、Mesh）
- `src/main/java/com/minecraftin/clone/world/*`（區塊、世界、地形、射線）
- `src/main/java/com/minecraftin/clone/gameplay/*`（玩家移動與物理）
- `src/main/java/com/minecraftin/clone/render/*`（世界與 HUD 渲染）
- `src/main/resources/shaders/*`（GLSL Shader）

## 12) 常見問答（FAQ）

### 為什麼預設不是全螢幕？

為了相容性與操作便利，預設視窗模式啟動。可再由系統視窗控制切換全螢幕。

### 這是生存模式嗎？

目前不是，現在只提供創造模式。

## 13) 協作與維護注意事項

- `saves/` 是本機資料，已在 `.gitignore` 忽略
- 建置快取資料夾也已忽略
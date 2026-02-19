# Minecraftin Java Clone（繁體中文）

English version: [README.md](README.md)

這是一個使用 Java + LWJGL 製作、受經典 Minecraft 啟發的體素沙盒專案。  
目前重點是完善的創造模式基礎：區塊地形生成、第一人稱移動、方塊互動，以及世界存檔/讀檔。

## 目前功能範圍

### 引擎與渲染

- OpenGL 3.3 Core 渲染流程（LWJGL 3）。
- GLFW 視窗與輸入迴圈，幀率不鎖定。
- 相機支援 yaw/pitch 滑鼠視角與透視投影。
- 區塊重建時可動態更新 VAO/VBO。
- 世界、選取框線、HUD 使用獨立 Shader。
- 程式內產生貼圖圖集（texture atlas）。
- 面向光照強度（上/側/下）與距離霧化。
- HUD 包含準心與 9 格方塊欄（含選取高亮）。
- 預設以視窗模式啟動，視窗尺寸會自動限制在螢幕範圍內。

### 世界與地形

- 區塊化世界（`16 x 16`，高度 `128`）。
- 依種子（seed）可重現的地形生成。
- 生態類型（Biome）：平原、森林、沙漠、雪地、山地、惡地。
- 海平面與水體填充。
- 使用 3D 雜訊雕刻洞穴。
- 依生態調整地表與地層。
- 程序化樹木生成。
- 出生點優先選在陸地上，避免出生在水下。

### 遊戲玩法

- 僅創造模式（`CREATIVE_MODE_ONLY = true`）。
- `Space` 雙擊可切換飛行開/關。
- 飛行模式仍會與實心方塊碰撞（不穿牆）。
- 關閉飛行時使用重力、跳躍與地面碰撞移動。
- `Left Ctrl` 可加速（地面與飛行皆可）。
- 使用 DDA 體素光線投射精準選取方塊與面法線。
- 破壞/放置皆有冷卻時間。
- 放置時會檢查玩家碰撞箱，避免把方塊放進自己身體。
- 方塊欄可用 `1-9` 或滑鼠滾輪切換。

### 存檔

- 二進位世界存檔路徑：`saves/world.dat`。
- 儲存世界種子與已載入區塊資料。
- 每 20 秒自動存檔，關閉遊戲時也會存檔。

## 預設方塊欄（9 格）

1. 紅方塊
2. 橙方塊
3. 黃方塊
4. 綠方塊
5. 藍方塊
6. 紫方塊
7. 泥土
8. 石頭
9. 玻璃

## 操作鍵位

- `W/A/S/D`：移動
- `Mouse`：視角
- `Left Ctrl`：衝刺 / 飛行加速
- `Space`（雙擊）：切換飛行模式
- `Space`（飛行中）：上升
- `Left Shift`（飛行中）：下降
- `Left Click`：破壞方塊
- `Right Click`：放置目前選取方塊
- `1-9` / `Mouse Wheel`：切換方塊欄
- `Esc`：釋放滑鼠游標
- `Left Click`（游標自由時）：重新鎖定游標
- `Q`：離開遊戲

## 建置與執行

### 需求

- JDK 17 以上（專案以 Java `release 17` 編譯，已在 Java 21 執行測試）
- 內建 Gradle Wrapper（`./gradlew`）

### 建置

```bash
./gradlew build -x test
```

### 執行

```bash
./gradlew run
```

若你的環境會阻擋 Gradle daemon socket：

```bash
GRADLE_USER_HOME=.gradle-home ./gradlew --no-daemon run
```

在 macOS 上，`build.gradle` 已為 Gradle 執行任務自動加入 `-XstartOnFirstThread`。

## 專案結構

- `src/main/java/com/minecraftin/clone/MinecraftClone.java`
- `src/main/java/com/minecraftin/clone/game/Game.java`
- `src/main/java/com/minecraftin/clone/engine/*`
- `src/main/java/com/minecraftin/clone/world/*`
- `src/main/java/com/minecraftin/clone/gameplay/*`
- `src/main/java/com/minecraftin/clone/render/*`
- `src/main/resources/shaders/*`

## 可調整設定

主要設定檔：

- `src/main/java/com/minecraftin/clone/config/GameConfig.java`

可調整項目包含：

- 視窗大小與標題
- 可視距離與區塊尺寸
- 滑鼠靈敏度與移動物理參數
- 互動距離與破壞/放置冷卻
- 存檔路徑與預設世界種子

## 與完整 Minecraft 的差距

1. 尚未實作生存系統（血量、飢餓、傷害、合成、背包）。
2. 尚未實作實體/生物 AI。
3. 尚未實作動態天光與方塊光照傳播。
4. 尚未實作日夜循環與天氣。
5. 尚未實作結構生成流程。
6. 尚未實作多人連線。
7. 尚未實作多執行緒區塊網格重建。
8. 目前尚無自動化測試（`test` 任務沒有測試來源）。

## 備註

- 專案架構偏向可擴充性，而非單檔示範。
- 存檔屬於本機資料，已由 Git 忽略（`saves/`）。
- macOS 原生全螢幕可透過視窗左上綠色按鈕選單進入。

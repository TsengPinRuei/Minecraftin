# Minecraftin

Minecraftin 是一個以 Java + LWJGL 製作，受經典 Minecraft 啟發的小型方塊沙盒遊戲。目前專案以創造模式為主：你可以探索程式生成的地形、用第一人稱移動、飛行、放置與破壞方塊、使用創造模式方塊清單，並把本機世界存檔。

## 主要功能

- 以 Chunk 為單位的程式地形生成，包含平原、森林、沙漠、雪地、山地與惡地。
- 洞穴、海平面水體、局部補水、玩家放置水的有限流動，以及樹木生成。
- 第一人稱移動，包含走路、衝刺、蹲下、跳躍、游泳、漂浮、爬梯子與創造模式飛行。
- 方塊破壞會產生碎屑粒子，方塊放置具備碰撞檢查。
- 多頁創造模式背包與 9 格快捷欄。
- 可用方塊包含自然地形方塊、羊毛色系、彩色方塊、木板、樓梯、半磚、柵欄、門、活板門、梯子、火把、工作台、熔爐、箱子、書櫃、石材變體、石英、地獄/終界風格方塊、玻璃、水、螢光石與海燈。
- 門與活板門可用右鍵開關。
- 箱子有空容器介面雛形。
- Minecraft 式光照引擎：天空光與方塊光各自 BFS 傳播，火把、螢光石與海燈會發光，水與樹葉會衰減光線。
- 平滑光照與環境光遮蔽（AO），角落與牆角有柔和陰影。
- 晝夜循環（一天 20 分鐘）：方形太陽與月亮、日出日落天色、夜晚月光，以及跟隨天色的霧。
- 漂移的平面雲層與視錐剔除。
- `F3` 偵錯畫面：FPS、座標、區塊、面向、光照強度與世界時間。
- 本機世界存讀檔，包含上次儲存的玩家重生位置與世界時間。
- 透過 LWJGL 使用 OpenGL 3.3 渲染，並使用 GLSL shader 與程式生成的像素風方塊貼圖集。

## 系統需求

- macOS、Windows 或 Linux。
- JDK 17 或更新版本。如果很新的 JDK 造成 Gradle 相容性問題，建議改用 JDK 21。
- 支援 OpenGL 3.3 的顯示卡與驅動程式。
- 第一次執行時需要網路，讓 Gradle 從 Maven Central 和 Gradle 官方來源下載依賴與 Gradle 發行檔。
- 至少 4 GB 記憶體，建議 8 GB。
- 約 1 GB 可用磁碟空間，用於 Gradle 快取、依賴與建置輸出。

Gradle wrapper 會下載 Gradle `8.10.2`。專案原始碼以 Java `release 17` 編譯。

## 安裝方式

1. 開啟終端機。
2. 進入專案資料夾。
3. 使用 Gradle wrapper 執行遊戲。

macOS / Linux：

```bash
cd /path/to/Minecraftin
./gradlew run
```

Windows PowerShell：

```powershell
cd C:\path\to\Minecraftin
.\gradlew.bat run
```

第一次執行可能需要幾分鐘，因為 Gradle 會下載自己的執行環境與專案依賴。

## 設定方式

這個專案不需要環境變數、API key、資料庫或外部服務。

主要遊戲設定是 `src/main/java/com/minecraftin/clone/config/GameConfig.java` 裡的 Java 常數：

- 視窗大小與標題。
- 視野角度與相機遠近裁切距離。
- Chunk 大小、Chunk 高度與渲染距離。
- 創造模式開關。
- 滑鼠靈敏度。
- 走路、飛行、衝刺、跳躍與重力數值。
- 玩家碰撞箱大小。
- 方塊互動距離與冷卻時間。
- 世界存檔路徑：`saves/world.dat`。
- 預設世界種子：`20260219`。

重要種子注意事項：存檔會記住世界種子。修改 `DEFAULT_WORLD_SEED` 只會影響新世界。如果要用新種子重新生成世界，請先刪除 `saves/world.dat`。

建置與依賴設定在 `build.gradle`：

- 主類別：`com.minecraftin.clone.MinecraftClone`。
- LWJGL 版本：`3.3.3`。
- JOML 版本：`1.10.5`。
- JUnit 版本：`5.10.2`。
- macOS 執行參數：`-XstartOnFirstThread`。
- 預設 JVM 記憶體參數：`-Xms1g` 與 `-Xmx2g`。

## 使用方式

執行遊戲：

```bash
./gradlew run
```

Windows：

```powershell
.\gradlew.bat run
```

遊戲視窗開啟後，對視窗按左鍵即可鎖定滑鼠。按 `Esc` 可釋放滑鼠。

### 操作鍵位

| 輸入 | 動作 |
| --- | --- |
| `W` / `A` / `S` / `D` | 移動 |
| 滑鼠 | 轉動視角 |
| `Left Ctrl` + `W` | 向前衝刺或飛行加速 |
| `Space` | 跳躍 |
| 在地面按 `Left Shift` | 蹲下 |
| 雙擊 `Space` | 切換創造模式飛行 |
| 飛行時按 `Space` | 向上飛 |
| 飛行時按 `Left Shift` | 向下飛 |
| 在水中按 `W` / `A` / `S` / `D` | 水平游泳 |
| 在水中按 `Space` | 上游或漂浮 |
| 在水中按 `Left Shift` | 下潛 |
| 在梯子上按 `W` 或 `Space` | 往上爬 |
| 在梯子上按 `S` 或 `Left Shift` | 往下爬 |
| `E` | 開啟或關閉創造模式背包 |
| `Left Click` | 鎖定滑鼠，或在滑鼠鎖定時破壞準星指向的方塊；按住可連續破壞 |
| `Right Click` | 放置目前選取方塊，或與門、活板門、箱子互動；按住可連續放置 |
| `1` 到 `9` | 選擇快捷欄格子 |
| 滑鼠滾輪 | 切換快捷欄格子 |
| 創造模式背包中使用滑鼠滾輪 | 切換背包頁面 |
| 創造模式背包中按左鍵 | 將方塊放入目前選取的快捷欄格子 |
| 創造模式背包中按右鍵 | 選取方塊並關閉背包 |
| `Esc` | 釋放滑鼠、關閉開啟中的 UI，或回到第一人稱控制 |
| `F3` | 開關偵錯畫面 |

### 初始快捷欄

1. 草地方塊
2. 泥土
3. 石頭
4. 鵝卵石
5. 橡木木板
6. 橡木原木
7. 玻璃
8. 火把
9. 工作台

### 存檔

世界資料會儲存在：

```text
saves/world.dat
```

遊戲每 20 秒會自動儲存有變更的 Chunk。正常關閉遊戲時，也會儲存尚未寫入的世界變更與玩家重生位置。

重設世界：

macOS / Linux：

```bash
rm -f saves/world.dat
```

Windows PowerShell：

```powershell
Remove-Item .\saves\world.dat -ErrorAction SilentlyContinue
```

刪除存檔後重新執行遊戲即可產生新世界。

## 常用指令

macOS / Linux：

```bash
./gradlew run
./gradlew test
./gradlew build
./gradlew clean
./gradlew installDist
```

Windows PowerShell：

```powershell
.\gradlew.bat run
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat clean
.\gradlew.bat installDist
```

指令說明：

- `run`：啟動遊戲。
- `test`：執行已設定的 JUnit 測試工作。repository 目前沒有測試原始碼檔案，但 Gradle 測試工作已設定。
- `build`：編譯專案、執行測試，並產生 Gradle 建置成果。
- `clean`：刪除建置輸出。
- `installDist`：在 `build/install/minecraftin-clone` 產生可執行的本機應用程式資料夾。

如果你的執行環境會擋 Gradle daemon socket 或共用 Gradle 快取位置，可以改用專案內的 Gradle 快取並關閉 daemon：

macOS / Linux：

```bash
GRADLE_USER_HOME=.gradle-home ./gradlew --no-daemon run
```

Windows PowerShell：

```powershell
$env:GRADLE_USER_HOME = ".gradle-home"
.\gradlew.bat --no-daemon run
```

## 專案結構

```text
Minecraftin/
├─ build.gradle
│  # Gradle 外掛、依賴、Java 版本、JVM 參數與主類別。
├─ settings.gradle
│  # Gradle 根專案名稱：minecraftin-clone。
├─ gradlew / gradlew.bat
│  # macOS/Linux 與 Windows 使用的 Gradle wrapper 啟動檔。
├─ gradle/wrapper/
│  # Gradle wrapper 設定與 wrapper JAR。
├─ src/main/java/com/minecraftin/clone/
│  ├─ MinecraftClone.java
│  │  # JVM 入口點。
│  ├─ config/GameConfig.java
│  │  # 集中管理遊戲常數。
│  ├─ game/Game.java
│  │  # 主迴圈、輸入流程、方塊互動、自動存檔與 UI 狀態。
│  ├─ engine/
│  │  # 視窗、輸入、相機、Shader、Mesh 與程式生成貼圖集。
│  ├─ gameplay/
│  │  # 玩家移動、碰撞、飛行、跳躍與梯子行為。
│  ├─ render/
│  │  # 世界渲染、HUD 渲染與貼圖集編號。
│  ├─ util/
│  │  # 噪音與 float 陣列輔助工具。
│  └─ world/
│     # Chunk、方塊種類、地形生成、射線檢測、水流、存讀檔。
├─ src/main/resources/shaders/
│  # 世界、HUD 與線框渲染使用的 GLSL shader。
├─ saves/
│  # 本機世界存檔，已由 Git 忽略。
├─ build/
│  # Gradle 建置輸出，已由 Git 忽略。
└─ .gradle/ 與 .gradle-home/
   # Gradle 快取資料夾，已由 Git 忽略。
```

## 疑難排解

### `java: command not found`

請安裝 JDK 17 或更新版本，重新開啟終端機後確認：

```bash
java -version
```

如果你安裝了多個 JDK，請確認 `JAVA_HOME` 指向相容的 JDK。

### `./gradlew: Permission denied`

macOS / Linux：

```bash
chmod +x gradlew
./gradlew run
```

### Gradle 無法下載依賴

第一次執行需要網路。請確認網路連線後重試。如果你在公司或學校 proxy 後面，請設定 Gradle proxy。

### Gradle 在受限制環境中失敗

有些沙盒會擋 daemon socket、本機快取存取或網路。可以嘗試：

```bash
GRADLE_USER_HOME=.gradle-home ./gradlew --no-daemon build
```

如果錯誤包含 `java.net.SocketException: Operation not permitted`，請先懷疑是執行環境限制，不要直接判定專案程式碼壞掉。

### macOS GLFW 第一執行緒錯誤

常見訊息：

```text
GLFW may only be used on the main thread ... run the JVM with -XstartOnFirstThread
```

請使用：

```bash
./gradlew run
```

macOS 的 Gradle `run` 工作已經自動加入 `-XstartOnFirstThread`。如果你手動啟動 Java 應用程式，請自行加上這個 JVM 參數。

### 黑畫面或程式立刻關閉

可能原因：

- 顯示卡或驅動程式不支援 OpenGL 3.3。
- 透過遠端桌面或虛擬機執行，環境沒有正確提供 OpenGL。
- 顯示卡驅動程式過舊。

請改在支援的本機硬體上執行，並更新顯示卡驅動程式。

### 世界存檔失敗

常見訊息：

```text
World save failed: Failed to save world to saves/world.dat
```

請確認專案資料夾可以寫入，且 `saves/` 目錄可以建立。避免從唯讀資料夾或被系統鎖定的外接硬碟執行專案。

### 修改預設種子後世界沒有變

請刪除 `saves/world.dat`。既有存檔會保留原本的種子。
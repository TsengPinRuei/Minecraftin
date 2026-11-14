# Minecraftin

[English](README.md) | [繁體中文](READMEzhTW.md)

Minecraftin 是以 Java、LWJGL 與 OpenGL 製作的獨立單人方塊沙盒遊戲。這類遊戲使用「體素（voxel）」，也就是一個個小方塊，組成 3D 世界。專案受經典 Minecraft 啟發，你可以探索程式生成的地形、飛行、使用無限方塊建造，並將世界儲存在自己的電腦上。

目前以**創造模式建築**為主，包含 70 種可選方塊、自行實作的渲染流程、光照與水流模擬，以及自動化測試。尚未完成的生存玩法、容器儲物等內容，請見[目前限制](#limitations)。

## 目錄

- [已實作功能](#features)
- [開發技術與作品亮點](#engineering)
- [從零開始啟動遊戲](#quick-start)
- [玩法與操作](#playing)
- [方塊與互動規則](#blocks)
- [存檔、備份與新世界](#saves)
- [調整設定](#configuration)
- [開發、測試與打包](#development)
- [專案檔案與資料](#project-structure)
- [目前限制](#limitations)
- [疑難排解](#troubleshooting)

<a id="features"></a>
## 已實作功能

### 世界與探索

- 使用**種子（seed）**生成地形。種子是一個數字；使用相同種子、生成程式與設定，就能重現相同地形。
- 世界分成多個**區塊（chunk）**，每區塊水平為 16 × 16 格、高度為 128 格；探索時會載入附近區塊。
- 六種地形生態區：平原、森林、沙漠、雪地、山地、惡地。生成內容包含分層地表、基岩、洞穴、海平面水體與樹木。
- 新世界的出生點會優先尋找大片森林，找不到時再嘗試其他適合站立的地面。

### 移動與建築

- 第一人稱走路、向前衝刺、跳躍、蹲下、游泳、爬梯子，以及雙擊空白鍵切換飛行。
- 移動加速度、重力、碰撞、半磚／樓梯自動踏階、蹲下與踏階時的平滑視角，以及走路時的鏡頭晃動。
- 半磚、樓梯、門、柵欄等方塊依實際形狀判定選取與碰撞；選取外框也會配合目標形狀。
- 即時破壞方塊並產生帶有方塊貼圖的碎屑；按住滑鼠可連續破壞或放置。
- 9 格快捷欄，以及**70 種可選方塊、共 2 頁**的創造背包，每頁最多 45 種；方塊數量無限。
- 橡木樓梯、門、梯子具備放置方向；門與活板門可開關。箱子可開啟 27 格的空介面。

### 水流、光照與畫面

- 水源向下流動、向旁邊擴散、填入缺口，水源被擋住後失去供水的水流會退去。符合下方支撐條件時，兩個相鄰水源可形成新水源。
- 水流可跨越已載入的區塊邊界；待處理的更新會存檔，重新開啟世界後繼續模擬。
- 天空光與方塊光分開傳播；火把、螢光石、海燈會發光，水與樹葉會減弱穿過的光線。
- 平滑光照與**環境光遮蔽（AO）**：讓方塊相接的角落變暗，呈現柔和陰影。
- 一天 20 分鐘的晝夜循環、方形太陽與月亮、日出日落天色、夜間月光、跟隨天色的霧，以及漂移的平面雲層。
- 玻璃與水的透明效果、由程式產生的像素貼圖、方塊縮圖，以及自製像素字型的 HUD。HUD 指疊在遊戲畫面上的準星、快捷欄與資訊。
- `F3` 顯示 FPS（每秒畫面數）、玩家座標、區塊座標與數量、面向、天空光／方塊光，以及世界時間。
- 視窗標題也會更新遊戲模式、`Flight: ON/OFF`、FPS、已載入區塊數量與目前選取的方塊名稱。

### 本機存讀檔

- 世界檔案保存種子、已載入區塊的方塊、世界時間、離開時記錄的位置，以及待處理的水流狀態。
- 每 20 秒檢查待存變更，正常離開時也會存檔。
- 先寫暫存檔，再以原子替換更新正式存檔，避免寫入失敗破壞上一次存檔。無效或不支援的存檔會阻止啟動，不會直接被新世界覆蓋。
- 目前存檔格式為第 4 版，可讀取第 1–3 版，並於下次成功存檔時升級。

<a id="engineering"></a>
## 開發技術與作品亮點

下表列出專案已經實作、可用於作品介紹的技術。原始碼連結可追查實作位置；[驗證指令](#development)則說明如何檢查。

| 項目 | 實作方式與用途 | 對應原始碼 |
| --- | --- | --- |
| 程式地形生成 | 使用多層 2D／3D 噪音（fBm）決定地形、氣候與洞穴；依世界座標取樣，讓負座標等位置也能重現。先完成地形，再放樹幹與樹葉。 | [TerrainGenerator.java](src/main/java/com/minecraftin/clone/world/TerrainGenerator.java)、[Noise.java](src/main/java/com/minecraftin/clone/util/Noise.java) |
| 方塊形狀與互動 | 3D DDA 沿準星射線逐格找候選方塊，再檢查實際幾何盒。AABB 是沿世界座標軸對齊的碰撞盒，用於移動、放置檢查與踏上半磚等行為。 | [World.java](src/main/java/com/minecraftin/clone/world/World.java)、[BlockType.java](src/main/java/com/minecraftin/clone/world/BlockType.java)、[Player.java](src/main/java/com/minecraftin/clone/gameplay/Player.java) |
| 光照系統 | 以廣度優先搜尋（BFS）向周圍傳播 0–15 級天空光／方塊光。頂點平滑光照與 AO 會取樣鄰格；區塊角落變更時，連對角區塊模型也會標記重建。 | [LightEngine.java](src/main/java/com/minecraftin/clone/world/LightEngine.java)、[ChunkMesher.java](src/main/java/com/minecraftin/clone/world/ChunkMesher.java)、[World.java](src/main/java/com/minecraftin/clone/world/World.java) |
| 水流模擬 | 排程佇列每 0.25 秒推進一次水流，並限制每次更新的工作量。水流查詢不會自行生成缺少的地形；相鄰區塊載入後會繼續處理邊界水流。 | [World.java](src/main/java/com/minecraftin/clone/world/World.java) |
| 背景建立區塊模型 | Mesh 是交給顯示卡的三角形資料。背景執行緒使用獨立的方塊／光照快照建立 mesh；優先處理近處區塊、限制待處理工作數量、丟棄過期結果。GPU 上傳留在主執行緒，每幀最多處理 16 筆結果。 | [ChunkSnapshot.java](src/main/java/com/minecraftin/clone/world/ChunkSnapshot.java)、[ChunkMesher.java](src/main/java/com/minecraftin/clone/world/ChunkMesher.java)、[WorldRenderer.java](src/main/java/com/minecraftin/clone/render/WorldRenderer.java) |
| 控制繪圖工作量 | 省略方塊內部不可見的面；視錐剔除會跳過相機看不到的區塊。不透明／cutout 模型先寫入深度，半透明面與碎片共用由遠至近排序，雲依相機所在平面側安排順序；超出渲染距離的 GPU 模型會釋放。 | [ChunkMesher.java](src/main/java/com/minecraftin/clone/world/ChunkMesher.java)、[WorldRenderer.java](src/main/java/com/minecraftin/clone/render/WorldRenderer.java) |
| 貼圖與操作介面 | 程式生成 16 × 16 像素的小貼圖，集中在同一張貼圖集，供世界與 HUD 縮圖共用。另實作像素字型、背包分頁、不同形狀縮圖，以及視窗／framebuffer 座標處理。 | [TextureAtlas.java](src/main/java/com/minecraftin/clone/engine/TextureAtlas.java)、[HudRenderer.java](src/main/java/com/minecraftin/clone/render/HudRenderer.java)、[InputState.java](src/main/java/com/minecraftin/clone/engine/InputState.java) |
| 存檔可靠性 | 自訂二進位格式與版本管理；讀取時先檢查檔頭、尺寸、方塊 ID、座標、時間與水流資料，通過後才套用。寫入失敗時保留舊檔與待存變更。 | [World.java](src/main/java/com/minecraftin/clone/world/World.java)、[WorldPersistenceTest.java](src/test/java/com/minecraftin/clone/world/WorldPersistenceTest.java) |
| 資源生命週期 | 依建立順序的反向釋放資源，初始化失敗時也會清理；清理過程的其他錯誤會一併保留。結束渲染時關閉模型工作執行緒與 GPU 資源。 | [ResourceScope.java](src/main/java/com/minecraftin/clone/engine/ResourceScope.java)、[WorldRenderer.java](src/main/java/com/minecraftin/clone/render/WorldRenderer.java)、[ResourceScopeTest.java](src/test/java/com/minecraftin/clone/engine/ResourceScopeTest.java) |
| 測試與打包 | 不需視窗的 JUnit 回歸測試，與桌面 OpenGL 冒煙測試分開執行。Gradle wrapper 固定建置工具版本，並依建置電腦的作業系統與 CPU 選用原生函式庫。 | [測試程式](src/test/java/com/minecraftin/clone)、[build.gradle](build.gradle)、[Gradle wrapper 設定](gradle/wrapper/gradle-wrapper.properties) |

可用於作品介紹的精簡描述：

> 以 Java／LWJGL 實作方塊沙盒遊戲，包含程式地形生成、70 種可選方塊、依形狀判定的互動、天空光與方塊光、水流模擬、背景區塊模型建構，以及具版本管理的本機存檔，並建立邏輯與 OpenGL 冒煙測試。

上述渲染技術已實作，但專案目前沒有可證明特定 FPS 或加速百分比的效能基準。履歷若要加入效能數字，應先在明確記錄的電腦與場景上量測。

<a id="quick-start"></a>
## 從零開始啟動遊戲

### 1. 準備電腦環境

- 使用 macOS、Windows 或 Linux 的本機桌面環境，顯示卡與驅動程式須支援 **OpenGL 3.3**。
- Linux 上的 LWJGL 原生函式庫要求：**x86_64 使用 GLIBC 2.28 以上**，**ARM 使用 GLIBC 2.35 以上**。詳見[上游平台需求](https://github.com/LWJGL/lwjgl3/releases/tag/3.4.0)。
- 安裝 **JDK 21（建議）**或 JDK 17。JDK 包含 Java 與編譯器；只有執行環境的 JRE 不足以建置這份原始碼。[Eclipse Temurin 安裝指南](https://adoptium.net/installation/)。
- 選擇符合系統與 CPU 的 JDK，例如 Apple Silicon 使用 ARM64／aarch64，Intel／AMD 電腦通常使用 x64。
- 第一次下載 Gradle 與依賴時需要網路。不需要安裝 Minecraft、登入帳號、申請 API key、設定資料庫或另裝 Gradle。
- 可先以 4 GB 系統記憶體、建議 8 GB，以及約 1 GB 工具所需磁碟空間作為準備方向。這不是實測的最低需求；快取與探索過的世界都可能增長。遊戲預設 JVM heap 起始為 1 GB、上限為 2 GB。

Wrapper 固定 Gradle `9.9.1` 與官方 distribution SHA-256；原始碼仍以 Java `release 17` 編譯。已驗證的專案流程請使用 **JDK 17 或 21**。[Gradle 9.9.1 相容性說明](https://docs.gradle.org/9.9.1/userguide/compatibility.html)。macOS ARM64 為主要桌面平台；其他平台尚未完成全面驗證。

### 2. 取得完整專案

開啟 [GitHub 專案頁面](https://github.com/TsengPinRuei/Minecraftin)，選擇 **Code → Download ZIP**，下載後解壓縮到可以寫入的資料夾。解壓後的資料夾名稱可能包含分支名稱。請保留所有檔案，包括 `gradle/wrapper/gradle-wrapper.jar`；只下載 README 或某一個 Java 檔案無法啟動遊戲。

如果已經會使用 Git，也可以在打算存放專案的位置執行：

```text
git clone https://github.com/TsengPinRuei/Minecraftin.git
cd Minecraftin
```

如果電腦上已經有這個專案，直接使用現有資料夾即可。

### 3. 在專案根目錄開啟終端機

macOS 開啟「終端機」，Linux 開啟終端機程式，Windows 開啟 PowerShell。**專案根目錄**就是同時看得到 `build.gradle`、`gradlew`、`gradlew.bat` 的資料夾。

請把下方範例路徑換成實際位置；路徑包含空白時要保留引號。接下來若沒有另外註明，所有指令都在這個資料夾執行。

macOS / Linux：

```bash
cd "/path/to/Minecraftin"
java -version
javac -version
./gradlew --version
./gradlew run
```

Windows PowerShell：

```powershell
cd "C:\path\to\Minecraftin"
java -version
javac -version
.\gradlew.bat --version
.\gradlew.bat run
```

請逐行執行。Java 指令應顯示所選的相容 JDK；wrapper 應顯示 Gradle `9.9.1` 與相容 JVM。若版本檢查失敗，先依[疑難排解](#troubleshooting)處理，再繼續。

### 4. 確認啟動成功

第一次執行可能需要幾分鐘下載依賴並準備地形。成功後會開啟標題為 `Minecraftin Java Clone` 的視窗，看到地形、準星與快捷欄。遊戲執行期間終端機會持續忙碌，這是正常現象。

若最後出現 `BUILD FAILED`，或沒有視窗且終端機印出錯誤，請保留錯誤訊息，依[疑難排解](#troubleshooting)檢查。

<a id="playing"></a>
## 玩法與操作

### 第一次遊玩

1. 在遊戲視窗內按一下左鍵，再放開。滑鼠會鎖定在遊戲中，用來控制視角；這一下不會破壞方塊。
2. 用 `W/A/S/D` 走路、滑鼠轉動視角，將準星對準附近地面。
3. 按 `5` 選取橡木木板，對準方塊表面按右鍵，就會在旁邊放置木板。若新方塊會與角色身體重疊，請先後退。
4. 按左鍵破壞目標方塊。互動距離為 5 格，基岩不能破壞。
5. 按 `E`，用 `1–9` 選擇快捷欄格子，再點選方塊圖示放入該格。滾動滑鼠可切換另一頁；右鍵點圖示可選取並關閉背包。
6. 連按兩次 `Space` 開始飛行；按住 `Space` 上升、`Left Shift` 下降，再連按兩次離開飛行。飛行仍會撞到方塊，而且 `W` 會沿視線前進，包含向上或向下。視窗標題顯示 `Flight: ON` 就表示已開啟飛行。
7. 結束時按 `Esc` 釋放滑鼠，再按視窗的關閉按鈕。若背包或箱子開著，先用 `E` 或 `Esc` 關閉面板。等待遊戲完成存檔，回到終端機可輸入指令的狀態。

**`Esc` 不是暫停或離開選單。** 釋放滑鼠或開啟面板時，玩家控制會停止，但水流、時間與自動存檔仍繼續。關閉面板後，若滑鼠仍可自由移動，點一下世界畫面即可恢復控制。請優先使用視窗關閉按鈕，避免強制結束程式或中斷終端機中的遊戲。

### 操作鍵位

| 輸入 | 動作 |
| --- | --- |
| `W / A / S / D` | 前進／左移／後退／右移 |
| 滑鼠 | 鎖定時轉動視角 |
| `Left Ctrl + W` | 向前衝刺或加速飛行 |
| `Space` | 跳躍；水中上游；飛行時上升 |
| `Left Shift` | 蹲下並減速；水中下潛；飛行時下降 |
| 連按兩次 `Space` | 切換創造模式飛行，水中也可使用 |
| 水中按 `W/A/S/D` | 水平游泳；沒有上下輸入時會緩慢下沉 |
| 接觸梯子時按 `W` 或 `Space` | 向上爬 |
| 接觸梯子時按 `S` 或 `Left Shift` | 向下爬 |
| `E` | 開啟／關閉創造背包，或關閉目前的箱子 |
| 左鍵 | 鎖定滑鼠；鎖定後破壞目標方塊，按住可連續破壞 |
| 右鍵 | 優先與門／活板門／箱子互動，其他情況放置方塊；按住可連續放置 |
| `1–9` | 選取快捷欄格子；創造背包開啟時也可使用 |
| 世界畫面中滾動滑鼠 | 切換快捷欄格子 |
| 創造背包中滾動滑鼠 | 切換頁面 |
| 左鍵點背包方塊圖示 | 放入所選快捷欄格子，背包保持開啟 |
| 右鍵點背包方塊圖示 | 放入所選快捷欄格子並關閉背包 |
| `Esc` | 關閉目前的面板，或釋放已鎖定的滑鼠 |
| `F3` | 開關偵錯資訊；部分鍵盤需要按 `Fn + F3` |
| 視窗關閉按鈕 | 離開遊戲並嘗試存檔 |

**初始快捷欄：** 1 草地方塊 · 2 泥土 · 3 石頭 · 4 鵝卵石 · 5 橡木木板 · 6 橡木原木 · 7 玻璃 · 8 火把 · 9 工作台。

### 簡短的功能展示流程

| 可以這樣試 | 展示的功能 |
| --- | --- |
| 建造一面有門、窗戶、樓梯、半磚的小牆，走過去並開關門。 | 方塊放置、透明玻璃、方向方塊、碰撞與踏階 |
| 在屋頂下放置火把或螢光石，再移除；用 `F3` 觀察光照數值。 | 自發光、陰影與光照更新 |
| 挖一個有實心底部的淺水池，在 `E` 選 Water，對準池底或池壁放水，再用實心方塊填住水源格。 | 水流擴散與退去；附近其他水源可能持續供水 |
| 捲動背包的兩頁，替換幾個快捷欄格子。 | 方塊目錄、分頁與介面選取 |
| 蓋一個容易辨識的標記，正常關閉視窗後重新啟動。 | 世界存讀檔；同時檢查終端機是否有存檔錯誤 |

要製作作品展示時，可以依這些步驟實際操作並錄影／截圖。下文自動圖形測試產生的截圖屬於測試場景。

<a id="blocks"></a>
## 方塊與互動規則

以下分類涵蓋 [BlockType.java](src/main/java/com/minecraftin/clone/world/BlockType.java) 的全部 **70 個創造背包項目**。內部使用的空氣、水流等級、門的上下半部、方向與開關狀態，不另外算成可選方塊。

| 分類 | 可用方塊 |
| --- | --- |
| 地形與水（8） | 草地方塊、泥土、石頭、鵝卵石、沙、雪塊、基岩、水 |
| 木材與樹葉（6） | 橡木原木，橡木／樺木／杉木／黑橡木木板，橡木樹葉 |
| 建築形狀（7） | 橡木樓梯、橡木半磚、石半磚、橡木柵欄、橡木門、橡木活板門、梯子 |
| 用途與裝飾（5） | 火把、工作台、熔爐、箱子、書櫃 |
| 磚與石材變體（13） | 紅磚；石磚、浮雕石磚、青苔石磚；花崗岩、閃長岩、安山岩及各自的拋光版本；深板岩、拋光深板岩、深板岩磚 |
| 石英、主題建材、玻璃與光源（10） | 石英方塊、石英柱、平滑石英、黑曜石、地獄石、地獄磚、終界石、玻璃、螢光石、海燈 |
| 羊毛（15） | 白、淺灰、灰、黑、棕、紅、橙、黃、淺綠、綠、青、藍、紫、洋紅、粉紅 |
| 彩色混凝土方塊（6） | 紅、橙、黃、綠、藍、紫 |

互動規則：

- 放置時必須瞄準距離內的表面，目標空間需為空氣或水；實心方塊不能穿過角色身體放置。
- 橡木樓梯與門依玩家視線決定方向，梯子依點擊的側面決定方向。門需要上下兩格空間；破壞任一半會一併移除另一半。
- 右鍵點門、活板門或箱子時，會先執行互動。沒有「蹲下略過互動」的操作；想在旁邊蓋東西時，請瞄準其他方塊表面。
- 準星不能直接選取或破壞水。要停止供水，可用實心方塊取代水源所在格。形成新無限水源時，空隙的水平方向至少要有兩個相鄰水源，下方需為實心方塊或水源。
- 基岩可以選取與放置，但目前**不能破壞**。
- 箱子的 27 格只是空介面，不能存放或轉移物品。工作台與熔爐可以放置，但沒有合成或冶煉邏輯。
- 地獄／終界風格方塊是建材，目前沒有地獄／終界維度或傳送門。

<a id="saves"></a>
## 存檔、備份與新世界

### 會儲存哪些資料

預設檔案為 `saves/world.dat`。使用 `gradlew run` 時，它位於專案根目錄下。直接執行打包後的啟動檔時，則相對於終端機目前的資料夾；請固定從相同位置啟動，才能找到同一個世界。

| 資料 | 是否儲存 |
| --- | --- |
| 種子、所有已載入區塊的方塊 ID，包含建築變更與門的狀態 | 是 |
| 世界時間、待處理水流更新與水流計時 | 是 |
| 玩家位置 | 正常離開時記錄；自動存檔不會持續更新玩家位置 |
| 視角方向、飛行狀態、快捷欄內容／選取格子、開啟中的面板 | 否；重新啟動會回到初始設定 |
| 貼圖、模型、光照陣列 | 依程式與世界資料重新建立 |

遊戲每累積 20 秒更新時間，就檢查是否有待儲存的方塊／位置變更。有需要時會寫入**所有已載入區塊**的快照；正常離開時也會儲存世界時間與當下玩家位置。強制結束可能失去上次成功存檔之後的變更。

讀取前先以 canonical 路徑的旁置鎖檔（`world.dat.lock`）取得整個工作階段的獨占所有權。第二個可寫實例會被拒絕，路徑別名也不能繞過。鎖檔可在退出後保留；檔案存在不代表仍被鎖定。遊戲執行中請勿刪除或替換鎖檔。

寫入時先使用暫存檔，再以原子替換更新舊檔。失敗時保留舊檔與待存變更，且即使 F3 關閉也會持續顯示 `SAVE FAILED`；按 **R** 可重試。關閉視窗時若存檔失敗，遊戲會保持開啟：**R** 重試存檔並退出、**Esc** 返回遊戲、**D** 後再按 **Y** 才會明確放棄未存變更並退出。放棄確認畫面按 **Esc** 可返回。退出提示開啟期間世界模擬暫停。沒有自動保留多份歷史備份；詳細錯誤仍記錄於終端機。

若存檔位置位於低天花板下且站姿放不下，重載會在原位恢復蹲姿；飛行仍重設。支援的方塊 X/Z 範圍為 **−8,192 至 8,191**，玩家中心依碰撞半徑向內保留距離。飛行腳部 Y 限制為 **−8,192 至 8,190.2**（方塊世界仍為 Y=0–127）。移動會停在邊界；無效或超界存檔會明確拒絕，不覆寫也不默默裁切。若舊檔超過範圍，請保留原檔並使用相符的舊版。

第 4 版格式可讀取第 1–3 版，並於下次成功存檔時升級。使用新版遊戲前請先備份；舊版讀取程式可能無法理解升級後的格式。這是 Minecraftin 自訂的二進位格式，不是官方 Minecraft 世界的匯入格式。

### 重設、還原或更新前先備份

**先關閉遊戲**，等它完成，再確認 `saves/world.dat` 存在。在專案根目錄執行符合系統的指令，建立帶有時間戳記的副本：

macOS / Linux：

```bash
cp saves/world.dat "saves/world-backup-$(date +%Y%m%d-%H%M%S).dat"
```

Windows PowerShell：

```powershell
$worldBackupPath = ".\saves\world-backup-$(Get-Date -Format 'yyyyMMdd-HHmmss').dat"
Copy-Item .\saves\world.dat $worldBackupPath
Write-Output $worldBackupPath
```

確認 `saves/` 中出現新副本。如果可能刪除或更換整個專案資料夾，也請在專案外保留一份。Git 會忽略整個 `saves/`，因此 commit 或 push 不會幫你備份世界。

### 產生新世界或還原備份

遊戲關閉後：

1. 先依上面步驟備份。
2. 若要重設，用檔案管理員把 `saves/world.dat` 改成尚未使用的名稱，例如 `world-before-reset.dat`。下次啟動時，只要找不到 `world.dat`，就會建立新世界。
3. 若要還原，先把目前世界另存名稱，再將選定的備份**複製**到 `saves/`，把這份副本命名為 `world.dat`。
4. 啟動遊戲，確認出現預期的世界。

相同種子與設定會生成相同的起始地形。若想換地形，請在沒有既有 `world.dat` 的情況下，先修改 `DEFAULT_WORLD_SEED` 再啟動。只改預設種子，不會改變既有存檔。如果還沒有任何存檔，可以跳過備份與改名步驟。

<a id="configuration"></a>
## 調整設定

目前沒有遊戲內設定畫面。請先關閉遊戲，用文字編輯器修改對應原始碼、存檔，再執行 Gradle 重新編譯。

主要設定在 [GameConfig.java](src/main/java/com/minecraftin/clone/config/GameConfig.java)：

| 設定 | 預設值 | 用途 |
| --- | --- | --- |
| `WINDOW_WIDTH` / `WINDOW_HEIGHT` | `1600` / `900` | 初始視窗大小 |
| `FOV_DEGREES` | `70.0f` | 視野角度，也就是相機看得多寬 |
| `MOUSE_SENSITIVITY` | `0.12f` | 調低後轉動視角較慢 |
| `RENDER_DISTANCE_CHUNKS` | `10` | 以區塊為單位的顯示距離；可試 `6` 降低繪圖工作量 |
| `CHUNK_SIZE` / `CHUNK_HEIGHT` | `16` / `128` | 世界儲存尺寸；修改後可能無法讀取既有存檔 |
| `BLOCK_REACH` | `5.0f` | 最遠方塊互動距離 |
| `BREAK_COOLDOWN_SECONDS` / `PLACE_COOLDOWN_SECONDS` | `0.25f` / `0.2f` | 按住滑鼠時的連續操作間隔 |
| `DAY_LENGTH_SECONDS` | `1200.0f` | 完整晝夜循環需要的秒數 |
| `DEFAULT_WORLD_SEED` | `20260219L` | 只有不存在世界檔案時才使用的種子 |
| `WORLD_FILE` | `"saves/world.dat"` | 相對於工作資料夾的世界路徑 |
| `CREATIVE_MODE_ONLY` | `true` | 啟用創造模式移動；改成 `false` 不會自動實作生存系統 |

同一個檔案也控制視窗標題、相機裁切距離、走路／飛行／衝刺／跳躍／重力，以及角色碰撞與眼睛高度。蹲下、游泳、梯子與踏階常數位於 [Player.java](src/main/java/com/minecraftin/clone/gameplay/Player.java)；水流更新上限位於 [World.java](src/main/java/com/minecraftin/clone/world/World.java)。

建置設定位於 [build.gradle](build.gradle)：Java 程式入口 `com.minecraftin.clone.MinecraftClone`、LWJGL `3.4.3`、JOML `1.10.9`、JUnit `6.1.3`，以及遊戲 JVM 記憶體參數 `-Xms1g -Xmx2g`。macOS 會自動加入 `-XstartOnFirstThread`。遊戲本身不要求專用環境變數或線上服務；`JAVA_HOME`、`GRADLE_USER_HOME` 是可選的 Java／建置工具設定。

<a id="development"></a>
## 開發、測試與打包

### 常用指令

依目的選擇需要的指令即可；單純遊玩不需要全部執行。

| 目的 | macOS / Linux | Windows PowerShell |
| --- | --- | --- |
| 啟動遊戲 | `./gradlew run` | `.\gradlew.bat run` |
| 執行不需視窗的邏輯測試 | `./gradlew test` | `.\gradlew.bat test` |
| 執行桌面圖形檢查 | `./gradlew graphicsSmokeTest` | `.\gradlew.bat graphicsSmokeTest` |
| 編譯、測試並建立發行壓縮檔 | `./gradlew build` | `.\gradlew.bat build` |
| 建立本機可執行資料夾 | `./gradlew installDist` | `.\gradlew.bat installDist` |
| 檢查 install／ZIP／TAR 授權告知 | `./gradlew verifyDistributionNotices` | `.\gradlew.bat verifyDistributionNotices` |
| 量測隔離世界探索資源 | `./gradlew resourceBudgetProbe` | `.\gradlew.bat resourceBudgetProbe` |
| 移除生成的建置成果 | `./gradlew clean` | `.\gradlew.bat clean` |

完成的建置工作應顯示 `BUILD SUCCESSFUL`；`UP-TO-DATE` 表示 Gradle 重用目前有效的成果。若要強制重新跑邏輯測試，可用 `./gradlew test --rerun-tasks` 或 `.\gradlew.bat test --rerun-tasks`。`clean` 會刪除 `build/`，包含本機發行資料夾、報告與截圖，但不會刪除根目錄下的 `saves/`。

### 檢查涵蓋什麼

| 檢查 | 驗證內容 |
| --- | --- |
| 光照與水流回歸測試 | 光線衰減／移除、跨區塊光照、對角模型重建、水流擴散／退去、新水源形成、區塊邊界接續，以及存讀檔後接續 |
| 世界存檔測試 | 方塊、種子、位置、時間的存讀檔往返；舊版相容性；拒絕損壞資料；寫入失敗保留舊檔與清除暫存檔 |
| 幾何與移動測試 | 非完整方塊射線、柵欄選取、無效射線、穩定的模型快照、隱藏水面、踏階，以及低頭移動 |
| 輸入與資源測試 | 失去視窗焦點時清除輸入、滾輪重設、無效模型格式、佇列行為，以及錯誤發生後反向釋放資源 |
| 桌面圖形冒煙測試 | Shader 編譯、實際背景模型上傳、渲染、方塊更新／碎屑、HUD／背包、視窗尺寸，以及 OpenGL 錯誤檢查 |

不需視窗的測試報告會產生在 `build/reports/tests/test/index.html`；供程式讀取的測試結果位於 `build/test-results/test/`。

`graphicsSmokeTest` 需要可用的桌面與 OpenGL 3.3 以上。它會開啟暫時測試場景、自動完成，成功時印出 `Graphics smoke passed: ...`。它不會讀寫 `saves/world.dat`，也不包含在 `test` 或 `build` 中。產生的截圖為：

- `build/reports/graphics-smoke/world.png`
- `build/reports/graphics-smoke/inventory.png`

「冒煙測試」是快速確認主要功能能否啟動的檢查，不代表完整人工遊玩測試或效能基準測試。建置設定支援某個作業系統／CPU 組合，也不等於已在每個平台實際驗證遊玩。

**驗證紀錄 — 2026-09-12：** 在 macOS ARM64、JDK `21.0.10` 上，以 `test graphicsSmokeTest --rerun-tasks` 重新執行，45 項 JUnit 測試全部通過，失敗、錯誤、略過皆為 0；OpenGL 冒煙測試也通過，且已檢視兩張截圖。`build installDist` 同樣成功。本次已核對 Windows／Linux 指令與建置設定，但未在這兩個系統實際執行；程式變更後應重新驗證。

2026-11-14 審查修正、決策、目前驗證與平台限制記錄於[審查處理紀錄](docs/review-fixes-20261114.md)。CI 會記錄 Linux／macOS、JDK 17／21 的來源 commit、環境與測試報告；設定完成不等於遠端已實際執行。桌面圖形仍獨立驗證。

**升級前桌面補驗 — 2026-11-15：** 已用 LWJGL `3.3.3`、JOML `1.10.5` 的 macOS ARM64 正式發行包與臨時世界，完成正常關閉／存檔／重開，以及存檔失敗時重試、取消與確認放棄的實際操作。三輪皆以退出碼0正常結束；區塊資料保持一致，放棄時保留前次成功存檔。詳情與截圖見[審查處理紀錄](docs/review-fixes-20261114.md)。

Gradle `9.9.1`、JUnit `6.1.3`、JOML `1.10.9` 與 LWJGL `3.4.3` 的整合更新另有[依賴更新驗證紀錄](docs/dependency-updates-20261115.md)，列出受測來源、指令、結果與尚未驗證的平台範圍。上方歷史檢查僅代表各自當時的版本。

### 資源預算

F3 會顯示取樣 heap 用量、常駐方塊／光照陣列容量、上次存檔大小與最近一次存檔嘗試耗時。原型的建議預算為 **2,048 個區塊**、**最大 heap 的 75%** 或**單次存檔 1,000 ms**；任一達到便提示，F3 關閉也可見。這些是保守操作目標，不是強制快取上限，也不能保證不會耗盡記憶體。看到提示時應停止探索、存檔退出，再備份世界後建立新世界。重開同一世界仍會載入原有區塊，不會減少資料量。

每個區塊僅三個資料陣列就占 128 KiB；2,048 個區塊為 256 MiB，尚未含物件、queue、snapshot 與模型。`resourceBudgetProbe` 在臨時世界量測固定 8,064 格路線，並確認 64 個放置方塊皆可重載；結果位於 `build/reports/resource-budget/exploration.csv`。這是有界量測，並非長時間壓力測試。

### 啟動打包後的版本

先執行表格中的 `installDist`。`build/install/minecraftin-clone/` 裡的 `bin/`、`lib/`、`LICENSE`、`THIRD-PARTY-NOTICES.md` 與 `licenses/` 必須一起保留，啟動檔需要裡面的函式庫；電腦仍須安裝相容的 Java。

若要沿用專案根目錄的存檔，請留在專案根目錄執行：

macOS / Linux：

```bash
./build/install/minecraftin-clone/bin/minecraftin-clone
```

Windows PowerShell：

```powershell
.\build\install\minecraftin-clone\bin\minecraftin-clone.bat
```

`build` 也會在 `build/distributions/` 產生 ZIP／TAR 發行壓縮檔。若放到另一台電腦，請完整解壓縮，並固定從同一個工作資料夾啟動；需要沿用世界時，另外複製存檔。

**發行包是依建置電腦的作業系統與 CPU 產生。** 即使資料夾裡同時有兩種啟動檔，LWJGL 原生函式庫與 JVM 參數仍是在建置時決定；請在目標平台重新建置，macOS 產生的套件不能直接當作 Windows 套件。`build/libs/` 中的專案 JAR 也不是包含所有依賴的獨立可執行檔。

### 開發新功能時從哪裡開始

- 新增可選方塊：修改 [BlockType.java](src/main/java/com/minecraftin/clone/world/BlockType.java)，包含 `CREATIVE_PALETTE`；在 [AtlasTiles.java](src/main/java/com/minecraftin/clone/render/AtlasTiles.java) 接上貼圖編號，並於 [TextureAtlas.java](src/main/java/com/minecraftin/clone/engine/TextureAtlas.java) 產生像素。特殊形狀也可能需要調整幾何盒、放置邏輯與 HUD 圖示。
- 新增移動或互動：從 [Player.java](src/main/java/com/minecraftin/clone/gameplay/Player.java) 與 [Game.java](src/main/java/com/minecraftin/clone/game/Game.java) 開始。
- 修改地形、水流、光照或存檔：從 [world/](src/main/java/com/minecraftin/clone/world) 中對應類別開始，再執行相關回歸測試。
- 保留既有方塊 ID：存檔使用 `id()`，不是 enum 的 `ordinal()`。修改 ID、區塊尺寸或二進位欄位，需要先決定相容／遷移方式並加入測試。
- 邏輯變更執行 `test`；渲染／UI 變更還要在桌面執行 `graphicsSmokeTest`。兩份 README 應同步反映已完成的行為。

<a id="project-structure"></a>
## 專案檔案與資料

### 儲存庫內附的檔案

| 路徑 | 用途 |
| --- | --- |
| [README.md](README.md)、[READMEzhTW.md](READMEzhTW.md) | 英文與繁體中文操作文件 |
| [build.gradle](build.gradle)、[settings.gradle](settings.gradle) | 依賴、Java 設定、工作指令，以及專案名稱 `minecraftin-clone` |
| [gradlew](gradlew)、[gradlew.bat](gradlew.bat)、[gradle/wrapper/](gradle/wrapper) | 建置工具啟動檔、固定的發行版本設定與 wrapper JAR |
| [MinecraftClone.java](src/main/java/com/minecraftin/clone/MinecraftClone.java) | 應用程式入口 |
| [config/](src/main/java/com/minecraftin/clone/config)、[game/](src/main/java/com/minecraftin/clone/game) | 遊戲常數，以及主迴圈／輸入／介面／存檔協調 |
| [engine/](src/main/java/com/minecraftin/clone/engine) | 視窗、輸入、相機、shader、GPU 模型、生成貼圖集與資源清理 |
| [gameplay/](src/main/java/com/minecraftin/clone/gameplay) | 玩家移動與碰撞 |
| [render/](src/main/java/com/minecraftin/clone/render) | 世界渲染、模型排程、天空／雲／碎屑、HUD 與貼圖編號 |
| [world/](src/main/java/com/minecraftin/clone/world) | 方塊／幾何盒、區塊／快照、地形、光照、水流、射線、模型建立與存讀檔 |
| [util/](src/main/java/com/minecraftin/clone/util) | 噪音函式、基本型別 long 佇列與 float 陣列建構器 |
| [src/main/resources/shaders/](src/main/resources/shaders) | `world.vert/.frag`、`hud.vert/.frag`、`line.vert/.frag` 三組 shader |
| [src/test/java/com/minecraftin/clone/](src/test/java/com/minecraftin/clone) | JUnit 測試與獨立的 `GraphicsSmokeTest` 入口 |
| [.gitignore](.gitignore) | 忽略本機存檔、建置成果、快取、記錄與編輯器檔案 |

### 執行後在本機產生的檔案

| 路徑 | 內容 |
| --- | --- |
| `saves/world.dat` | 你的本機世界，應保留或備份 |
| `build/classes/`、`build/resources/`、`build/libs/` | 編譯後的 Java、複製的 shader 與專案 JAR |
| `build/install/minecraftin-clone/` | 打包後的啟動檔與執行依賴 |
| `build/distributions/` | ZIP／TAR 發行壓縮檔 |
| `build/reports/`、`build/test-results/` | 測試報告、結果與圖形測試截圖 |
| `.gradle/` | 專案本機 Gradle 快取 |
| `.gradle-home/` | 只有指定為本機 Gradle user home 時才會建立；預設通常使用 `~/.gradle`，Windows 為 `%USERPROFILE%\.gradle` |

生成資料夾在對應指令尚未執行前可能不存在，而且不會納入 Git。專案不需要另下載地圖、圖片資料集、資料庫或外部材質包：地形、貼圖像素與 HUD 字型由程式產生，shader 原始碼則已內附。

<a id="limitations"></a>
## 目前限制

以下描述目前的實作邊界，不是承諾的開發時程。

- **以創造沙盒為主：** 沒有生命／飢餓、傷害／戰鬥、生物、物品掉落、配方、冶煉或生存進度。修改 `CREATIVE_MODE_ONLY` 不會新增這些系統。
- **介面雛形：** 箱子無儲物資料；創造背包是把無限方塊指定到快捷欄。沒有完整背包／堆疊系統、快捷欄存檔或遊戲內語言切換，UI 文字為英文。
- **單一本機世界路徑：** 沒有世界選擇畫面、遊戲內種子／設定／按鍵修改選單、暫停選單、多人連線、帳號或雲端存檔。
- **簡化的方塊行為：** 活板門開啟方向與柵欄形狀固定；沒有柵欄自動連接、上半磚／半磚合併或完整 Minecraft 方塊狀態規則。蹲下不提供懸崖邊緣防墜。
- **渲染與記憶體限制：** 半透明面依視角深度排序；相交的透明幾何仍可能有一般 alpha 排序瑕疵。縮小 cutout 材質採固定螢幕覆蓋點陣，刻意放大時可見此圖樣。GPU 模型會清理，但已載入方塊仍留在記憶體並一起存檔；長距離探索會增加記憶體與存檔大小。地形生成仍在主執行緒，背景執行緒負責的是模型建立。
- **其他尚無系統：** 沒有音效、天氣、紅石、傳送門／維度，或官方 Minecraft 世界匯入。主題建材不代表對應系統已存在。

<a id="troubleshooting"></a>
## 疑難排解

| 狀況 | 處理方式 |
| --- | --- |
| 找不到 `java`／`javac`，或 `JAVA_HOME` 無效 | 安裝完整 JDK 21 或 17，重新開啟終端機並再做版本檢查。若設定 `JAVA_HOME`，應指向 JDK 資料夾，不是 `bin` 或 Java 執行檔，也要確認 wrapper 顯示的 JVM。 |
| Java／class-file 版本不支援 | 使用啟動章節列出的相容 JDK 範圍。已驗證的專案流程請使用 JDK 17 或 21。 |
| 找不到 `gradlew`，或目前資料夾不是 Gradle 專案 | 進入解壓後包含 `build.gradle` 的資料夾。PowerShell 使用 `.\gradlew.bat`，開頭的 `.\` 不可省略。 |
| `./gradlew: Permission denied` | macOS／Linux 執行 `chmod +x gradlew`，再試 `./gradlew run`。 |
| 缺少 wrapper JAR | 重新完整解壓縮或 clone 專案，保留 `gradle/wrapper/gradle-wrapper.jar`。 |
| 無法下載依賴 | 確認網路後重試。公司／學校網路請使用其允許的 Gradle proxy 設定；沒有必要快取時，第一次執行無法離線完成。 |
| macOS 要求 `-XstartOnFirstThread` | 使用 `./gradlew run` 或在 macOS 建立的啟動檔，會自動加入此參數。自行設定 Java／IDE 啟動時需加入。 |
| `Unsupported platform`、原生函式庫錯誤或 `UnsatisfiedLinkError` | 確認 JDK 的 CPU 架構，並在目標系統／CPU 重新建置；不要沿用其他平台的發行包。 |
| 黑畫面或立即關閉 | 先讀終端機錯誤，再檢查本機 OpenGL 3.3 支援與驅動程式。遠端桌面或虛擬機可能沒有提供需要的圖形能力。 |
| 滑鼠或移動沒有反應 | 關閉面板，左鍵點一下視窗再放開。使用觸控板時，請在系統設定啟用輔助點按，才能操作右鍵。 |
| 不能放置／破壞方塊 | 保持在 5 格內、瞄準實體表面，並讓放置空間避開角色身體。基岩不能破壞，水無法直接選取；門需要上下兩格空間。 |
| FPS 低或地形顯示慢 | 調低 `RENDER_DISTANCE_CHUNKS`，例如改成 `6`，再重啟。新模型會隨背景工作完成逐步出現；已探索世界的方塊資料仍占用記憶體。 |
| 世界不見了，或改種子無效 | 檢查啟動時的工作資料夾與 `WORLD_FILE`。既有存檔保留自己的種子；請依備份／新世界步驟處理，避免直接刪除資料。 |
| `Failed to load world from saves/world.dat` | 保留檔案並閱讀底層錯誤；可能是損壞、無法讀取或版本不相容。還原可用備份或使用對應遊戲版本，不要覆蓋唯一副本。 |
| `World save failed: Failed to save world to saves/world.dat` | 確認資料夾可寫入且空間足夠；檔案系統需支援原子替換。失敗時會保留舊檔，但最近的變更不一定已保存。 |
| `Deprecated Gradle features were used in this build` | 只有警告不代表失敗，請看最後狀態。可用 `./gradlew --warning-mode all build` 或 `.\gradlew.bat --warning-mode all build` 查看詳細來源。 |

若是快取權限問題，可以嘗試改用專案內的 Gradle user home：

macOS / Linux：

```bash
GRADLE_USER_HOME=.gradle-home ./gradlew --no-daemon build
```

Windows PowerShell：

```powershell
$env:GRADLE_USER_HOME = "$PWD\.gradle-home"
.\gradlew.bat --no-daemon build
```

PowerShell 的設定會持續到該終端機工作階段結束；改用本機快取可能需要重新下載依賴。**`--no-daemon` 仍可能建立單次使用的 Gradle 程序並使用本機 socket**，無法繞過沙盒或網路限制。若錯誤包含 `java.net.SocketException: Operation not permitted`，請改用允許執行的本機終端機，或先排除環境限制。

## 授權

專案原創程式與程序生成素材採用 [MIT License](LICENSE)，著作權人為 2026 TsengPinRuei。第三方元件保留各自條款；每份發行包都包含 [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md)、[licenses/](licenses/) 與專案 LICENSE，請一起保留。

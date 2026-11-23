# Minecraftin

[English](README.md) · 繁體中文

Minecraftin 是一款在電腦上玩的第一人稱單人建造遊戲。你可以飛越森林、雪地等六種生態區，用 70 種方塊搭出自己的地標。建材可以無限取用，世界會存在你的電腦裡，下次開啟就能繼續蓋。遊戲以 Java 開發，可獨立於 Minecraft 執行，地形和像素材質都由程式產生。

[啟動遊戲](#quick-start) · [遊戲操作](#playing) · [世界與方塊](#features) · [存檔與備份](#saves) · [認識引擎](#engineering) · [建置與測試](#development) · [疑難排解](#troubleshooting)

<a id="quick-start"></a>

## 啟動遊戲

1. 安裝適合你電腦的 **Java 開發套件（JDK）21 或 17**。開啟新的終端機，輸入 `java -version` 和 `javac -version`，兩者都應顯示剛安裝的版本。遊玩需要桌面環境，以及支援 **OpenGL 3.3** 的顯示卡和驅動程式。[Eclipse Temurin 安裝說明](https://adoptium.net/installation/)
2. 在 GitHub 專案頁選 **Code → Download ZIP**，下載後解壓縮。熟悉 Git 的話，也可以執行 `git clone https://github.com/TsengPinRuei/Minecraftin.git`。[GitHub 專案頁](https://github.com/TsengPinRuei/Minecraftin)
3. 在含有 `build.gradle` 和 `gradlew` 的專案資料夾開啟終端機或 Windows PowerShell。若剛用 Git 下載，可輸入 `cd Minecraftin` 進入該資料夾，再執行下方對應的指令。

**macOS 或 Linux**

```sh
./gradlew run
```

**Windows PowerShell**

```powershell
.\gradlew.bat run
```

專案隨附的 Gradle 啟動程式會在第一次執行時下載建置工具與函式庫，請保持網路連線。當 **Minecraftin Java Clone** 視窗出現地形、準星，以及畫面底部的九格快捷欄，就可以開始遊玩。遊玩時請讓終端機保持開啟。

<details>
<summary>可建置的平台與 Linux 系統需求</summary>

Gradle 會依建置電腦的作業系統和處理器，選用對應的原生函式庫。專案已設定 macOS（Apple Silicon、Intel）、Windows（x64、x86、ARM64）和 Linux（x64、ARM64、ARM32）。Linux x64 需要 glibc 2.28 以上，Linux ARM 需要 2.35 以上。桌面實測範圍見[建置與測試](#development)。[LWJGL 平台需求](https://github.com/LWJGL/lwjgl3/releases/tag/3.4.0)

</details>

<a id="playing"></a>

## 蓋出你的第一座地標

1. 在遊戲視窗按一下滑鼠左鍵並放開，讓滑鼠控制視角。用 `W/A/S/D` 移動，移動滑鼠環顧四周，把準星對準前方幾步遠的地面。
2. 按 `5` 選擇橡木木板（**Oak Planks**）。對地面按右鍵放一塊木板，再對木板頂面按右鍵疊一塊。按 `8` 選火把（**Torch**），放在柱頂。放錯方塊時，對準它按左鍵就能拆除。
3. 快速按兩下 `Space` 開啟飛行，再按住 `Space` 升到地標上方。按住 `Left Shift` 下降，再按兩下 `Space` 結束飛行。飛行時視窗標題會顯示 `Flight: ON`。
4. 按 `Esc` 釋放滑鼠，點視窗的關閉鈕，等終端機回到可輸入指令的狀態。從同一個資料夾再次啟動，你就能從離開時的位置繼續遊玩，也能找到剛蓋好的地標。

### 常用操作

| 想做什麼 | 操作方式 |
| --- | --- |
| 走路、環顧與衝刺 | 用 `W/A/S/D` 移動、滑鼠轉動視角。按住 `Left Ctrl + W` 可向前衝刺或加速飛行 |
| 跳躍或蹲下 | `Space` 跳躍，`Left Shift` 蹲下 |
| 飛行或游泳 | 快速按兩下 `Space` 切換飛行。在空中飛行或水中游泳時，按住 `Space` 上升、`Left Shift` 下降 |
| 爬梯子 | 接觸梯子時，按 `W` 或 `Space` 向上，按 `S` 或 `Left Shift` 向下 |
| 拆除、放置或使用方塊 | 左鍵拆除。對門、活板門或箱子按右鍵可互動，對其他方塊表面按右鍵可放置建材。按住可連續拆除或放置 |
| 選擇快捷欄位置 | 按 `1–9`，或在世界中捲動滑鼠滾輪 |
| 更換建材 | 按 `E` 開關創造背包，用 `1–9` 選擇要替換的快捷欄格子，捲動滾輪可翻頁。左鍵點選方塊會保留背包畫面，右鍵點選則會同時關閉背包 |
| 釋放滑鼠或關閉面板 | `Esc` 可關閉目前面板或釋放滑鼠。左鍵點遊戲畫面即可重新控制視角。瀏覽背包時，時間和水流仍會繼續推進 |
| 查看世界資訊 | `F3`（部分鍵盤用 `Fn + F3`）顯示每秒畫面數、座標、區塊、光照、世界時間、記憶體與存檔數據 |

<a id="features"></a>

## 探索世界與建造

- **世界種子**是決定地形的數字。在相同的種子與生成設定下，會出現相同的平原、森林、沙漠、雪地、山地和惡地。沿途可以探索洞穴、樹林與海岸。新世界會優先選擇森林作為出生地點。
- 用準星瞄準**五格內的方塊**，就能放置或拆除方塊。角色能沿著樓梯或半磚的形狀踏上去，拆除方塊時會飛出帶有該方塊材質的碎屑。放門需要上下兩格空間，梯子則依點選的側面決定朝向。基岩一旦放下就會永久保留。
- **挖一條淺溝**，對溝底放水，就能看到水先往下流、再向兩側擴散。用實心方塊填住水源，失去供水的水流便會退去。若空格在水平方向緊鄰至少兩個水源，且下方有實心方塊或水源支撐，空格也能形成新水源。重新開啟世界後，待更新的水流會繼續流動。
- **在屋簷下放一支火把**，就能看見方塊發出的光如何照亮日光難以到達的位置。螢光石和海燈也會發光。完整的晝夜循環為 20 分鐘，抬頭可以看到太陽、月亮、飄移的雲和遠方的霧。

<a id="limitations"></a>

目前的玩法是單人創造建築。創造背包分為兩頁，每頁最多 45 種方塊，都能無限取用。箱子可以打開，裡面的 27 格目前僅供查看。工作台和熔爐可用來布置場景。遊戲介面使用英文，世界採用 Minecraftin 自己的存檔格式。

<a id="blocks"></a>

<details>
<summary>展開全部 70 種可選方塊</summary>

| 分類 | 建材 |
| --- | --- |
| 地表與水 | 草地方塊、泥土、石頭、鵝卵石、沙、雪塊、基岩、水 |
| 木材與樹葉 | 橡木原木、橡木樹葉，以及橡木、樺木、杉木和黑橡木四種木板 |
| 建築構件 | 橡木樓梯、橡木半磚、石半磚、橡木柵欄、橡木門、橡木活板門、梯子 |
| 設備與擺設 | 火把、工作台、熔爐、箱子、書櫃 |
| 磚與石材 | 紅磚、石磚、浮雕石磚、青苔石磚，花崗岩、閃長岩、安山岩及三者的拋光版本，深板岩、拋光深板岩、深板岩磚 |
| 石英、玻璃與特殊方塊 | 石英方塊、石英柱、平滑石英、黑曜石、地獄石、地獄磚、終界石、玻璃、螢光石、海燈 |
| 羊毛 | 白、淺灰、灰、黑、棕、紅、橙、黃、淺綠、綠、青、藍、紫、洋紅、粉紅 |
| 混凝土 | 紅、橙、黃、綠、藍、紫 |

</details>

<a id="saves"></a>

## 存檔、備份與更換世界

世界存放在啟動遊戲時的工作資料夾下，檔名是 **`saves/world.dat`**。透過 Gradle 執行時，工作資料夾就是專案資料夾。使用打包後的啟動程式時，每次都從同一個資料夾啟動，就能接續同一個世界。

| 想做什麼 | 做法 |
| --- | --- |
| 保存建築 | 每經過 20 秒模擬時間，遊戲會檢查是否有待存變更。存檔包含所有已載入區塊、種子、世界時間，以及待更新的水流。正常關閉視窗也會記錄角色當下的位置。下次啟動時，快捷欄、視角方向和飛行狀態會回到預設值。 |
| 備份 | 關閉遊戲並等存檔完成，將 `saves/world.dat` 複製到專案外的資料夾。Git 會忽略 `saves/`。 |
| 建立新世界 | 關閉遊戲後，把 `world.dat` 改為尚未使用的檔名。下次啟動會用預設種子產生世界。想換一種地形，可在啟動前修改 `DEFAULT_WORLD_SEED`。 |
| 還原備份 | 關閉遊戲，若已有 `world.dat`，先將它改名保留。把備份複製到 `saves/`，將副本命名為 `world.dat`，再啟動遊戲。 |

**如果畫面顯示 `SAVE FAILED`**，先確認磁碟空間和資料夾寫入權限，再按 `R` 重試。若是在關閉視窗時發生，重試成功後會存檔並退出。按 `Esc` 可回到遊戲。若要放棄未存變更並退出，按 `D` 再按 `Y` 確認。在確認畫面按 `Esc` 可以返回上一步。

同一時間只允許一個遊戲程序使用該世界，由 `world.dat.lock` 管理。遊戲關閉後，這個鎖定檔仍可能留在磁碟上。讀檔時會先檢查整份檔案，遇到損壞或不支援的版本會停止啟動，保留原檔供還原。存檔時先寫好暫存檔，再以原子操作一次替換舊檔。目前格式為 **v4**，可讀取 v1–v3，並在下一次成功存檔時升級。跨版本使用時請保留升級前的備份。

<a id="engineering"></a>

## 引擎如何運作

程式從 `MinecraftClone.main` 進入 `Game`。每一幀都會讀取輸入、推進模擬，再繪製畫面。LWJGL 3.4.3 提供 GLFW 視窗、輸入與 OpenGL 介面，JOML 1.10.9 負責向量與矩陣運算。[程式入口](src/main/java/com/minecraftin/clone/MinecraftClone.java) · [建置設定](build.gradle)

### 從按下右鍵到火把出現在畫面上

1. **`Game` 從攝影機沿準星方向送出射線**，找出玩家瞄準的位置。`World.raycast` 沿射線逐格前進（3D DDA），並檢查格內方塊的實際形狀，所以能穿過半磚上方的空間瞄準後方。放置前也會檢查新方塊是否與角色的碰撞範圍重疊。[Game](src/main/java/com/minecraftin/clone/game/Game.java) · [World](src/main/java/com/minecraftin/clone/world/World.java) · [Player](src/main/java/com/minecraftin/clone/gameplay/Player.java)
2. **`World` 將火把寫進對應的區塊（chunk）。** 每個區塊底面 16 × 16 格、高 128 格。這次放置會觸發光照傳播、排程附近的水流更新，並標記需要重繪的區塊。角落的陰影會參考周圍方塊，連斜對角區塊也可能需要重繪。[World](src/main/java/com/minecraftin/clone/world/World.java) · [LightEngine](src/main/java/com/minecraftin/clone/world/LightEngine.java)
3. **`WorldRenderer` 在主執行緒擷取快照**，複製整個區塊及其外圍一格的方塊和光照資料。背景執行緒讀取這份唯讀快照，將可見的方塊表面組成三角形網格（mesh）。主執行緒每幀最多處理 16 份結果，捨棄過期網格，將最新的網格上傳到 GPU。繪製時先畫不透明表面和鏤空材質，再由遠到近混合玻璃、水和半透明碎屑。[ChunkSnapshot](src/main/java/com/minecraftin/clone/world/ChunkSnapshot.java) · [ChunkMesher](src/main/java/com/minecraftin/clone/world/ChunkMesher.java) · [WorldRenderer](src/main/java/com/minecraftin/clone/render/WorldRenderer.java)

地形在遊戲主執行緒生成。繪圖系統會釋放遠處的網格。**已探索區塊的方塊資料會留在記憶體中**，每次存檔都會一併寫入。已載入 2,048 個區塊、用到 Java 堆積記憶體上限的 75%，或一次存檔嘗試耗時達 1,000 毫秒，任一條件都會觸發畫面警示，即使關閉 `F3` 也看得到。此時請存檔並退出。重開同一個世界仍會載入這些區塊。[警示門檻](src/main/java/com/minecraftin/clone/game/ResourceBudget.java) · [探索路線量測程式](src/test/java/com/minecraftin/clone/game/ResourceBudgetProbe.java)

<a id="project-structure"></a>

### 想改功能，從哪裡開始

| 想修改的內容 | 對應程式 |
| --- | --- |
| 地形或移動 | `TerrainGenerator` 用種子和雜訊函式產生地形與洞穴，`Player` 處理走路、飛行與碰撞。[TerrainGenerator](src/main/java/com/minecraftin/clone/world/TerrainGenerator.java) · [Player](src/main/java/com/minecraftin/clone/gameplay/Player.java) |
| 創造背包裡的方塊 | 新增方塊和背包選項，再指定材質圖塊並產生像素。存檔會記錄方塊 ID，修改時要保留既有 ID。[BlockType](src/main/java/com/minecraftin/clone/world/BlockType.java) · [AtlasTiles](src/main/java/com/minecraftin/clone/render/AtlasTiles.java) · [TextureAtlas](src/main/java/com/minecraftin/clone/engine/TextureAtlas.java) |
| 背包介面或畫面效果 | `HudRenderer` 負責繪製快捷欄與背包，也會判斷滑鼠點到哪一格。著色器控制世界、介面和選取外框的繪製方式。[HudRenderer](src/main/java/com/minecraftin/clone/render/HudRenderer.java) · [著色器](src/main/resources/shaders) |
| 存檔格式或退出流程 | `World` 讀寫二進位格式，`SaveController` 處理存檔失敗、重試與放棄確認。變更時可用測試檢查舊檔載入和寫入失敗後的復原行為。[World](src/main/java/com/minecraftin/clone/world/World.java) · [SaveController](src/main/java/com/minecraftin/clone/game/SaveController.java) · [存讀檔測試](src/test/java/com/minecraftin/clone/world/WorldPersistenceTest.java) |

<a id="configuration"></a>

### 調整預設值

先關閉遊戲，編輯 Java 原始碼中的設定常數，再用 Gradle 啟動遊戲，就會重新編譯。[GameConfig.java](src/main/java/com/minecraftin/clone/config/GameConfig.java)

| 設定 | 預設值 | 用途 |
| --- | --- | --- |
| `RENDER_DISTANCE_CHUNKS` | `10` | 以區塊為單位的繪製半徑，調低可減少繪圖量。 |
| `FOV_DEGREES` / `MOUSE_SENSITIVITY` | `70` / `0.12` | 垂直視野角度、轉動滑鼠時的視角靈敏度。 |
| `DEFAULT_WORLD_SEED` / `WORLD_FILE` | `20260219L` / `saves/world.dat` | 新世界的地形種子與存檔路徑。既有世界會使用存檔內的種子。 |

可放置方塊的 X、Z 座標是 −8,192 到 8,191，Y 座標是 0 到 127。變更區塊尺寸會影響存檔中的資料排列，需要一併處理舊檔相容性。[世界邊界](src/main/java/com/minecraftin/clone/world/WorldBounds.java)

<a id="development"></a>

## 建置、測試與打包

遊戲版本為 `0.1.0`，隨附的啟動程式固定使用 **Gradle 9.9.1**。編譯目標是 **Java 17**，邏輯測試使用 **JUnit 6.1.3**。遊戲的 Java 堆積記憶體初始為 1 GiB，上限為 2 GiB。請在專案資料夾執行下列指令。[Gradle 版本設定](gradle/wrapper/gradle-wrapper.properties) · [建置任務](build.gradle)

| 目的 | macOS 或 Linux | Windows PowerShell |
| --- | --- | --- |
| 在不開啟遊戲視窗的情況下執行邏輯測試 | `./gradlew test` | `.\gradlew.bat test` |
| 編譯、測試並產出 ZIP 與 TAR 發行包 | `./gradlew build` | `.\gradlew.bat build` |
| 建立含啟動程式的本機安裝目錄 | `./gradlew installDist` | `.\gradlew.bat installDist` |
| 核對安裝目錄、ZIP、TAR 內的授權文件 | `./gradlew verifyDistributionNotices` | `.\gradlew.bat verifyDistributionNotices` |
| 在支援 OpenGL 的桌面驗證繪圖 | `./gradlew graphicsSmokeTest` | `.\gradlew.bat graphicsSmokeTest` |
| 用臨時世界量測記憶體與存檔時間 | `./gradlew resourceBudgetProbe` | `.\gradlew.bat resourceBudgetProbe` |

**邏輯測試**涵蓋移動、方塊選取、光照、水流與存檔相容性，報告位於 `build/reports/tests/test/index.html`。獨立的圖形測試會驗證著色器、網格上傳、透明效果和背包畫面，截圖存放在 `build/reports/graphics-smoke/`。資源量測會沿著總長 8,064 格的路線取樣 64 個位置，重新載入世界後逐一核對放下的方塊。結果在 `build/reports/resource-budget/exploration.csv`。

GitHub Actions 會在 Ubuntu 24.04 和 macOS 15 上分別使用 JDK 17、21 執行邏輯測試並檢查發行包。手動執行工作流程時，也可以選擇執行 macOS 桌面圖形測試。[CI 執行紀錄](https://github.com/TsengPinRuei/Minecraftin/actions/workflows/verify.yml) · [CI 設定](.github/workflows/verify.yml)

要**啟動打包版**，請先執行 `installDist`，接著在專案資料夾執行 `./build/install/minecraftin-clone/bin/minecraftin-clone`，Windows PowerShell 則執行 `.\build\install\minecraftin-clone\bin\minecraftin-clone.bat`。請將 `bin/`、`lib/` 與授權檔放在一起。ZIP 和 TAR 位於 `build/distributions/`。交付給其他電腦時，應在相符的作業系統與處理器上建置，並在執行端安裝 Java 17 或 21。

<a id="troubleshooting"></a>

## 疑難排解

| 遇到的情況 | 處理方式 |
| --- | --- |
| 找不到 `java`、`javac`，或顯示的版本不對 | 安裝 JDK 21 或 17，重新開啟終端機並檢查兩個版本。若有設定 `JAVA_HOME`，請指向 JDK 的安裝目錄。 |
| 無法執行 Gradle 啟動程式 | 進入含有 `build.gradle` 與 `gradle/wrapper/gradle-wrapper.jar` 的資料夾。macOS／Linux 若出現 `Permission denied`，執行 `chmod +x gradlew`。PowerShell 的 `.\gradlew.bat run` 要保留開頭的 `.\`。 |
| 遊戲視窗開不起來 | 查看終端機錯誤，確認顯示卡驅動程式支援桌面 OpenGL 3.3，並使用與電腦作業系統及處理器相符的發行包。在 macOS 用 Gradle 或打包版啟動時，會自動帶入 `-XstartOnFirstThread`。 |
| 無法放置方塊 | 靠近至五格內、對準實體表面，並讓放置空間避開角色身體。要在門、活板門或箱子旁邊建造時，請瞄準相鄰方塊的表面。 |
| 世界像重新生成，或提示已被開啟 | 到啟動時的工作資料夾確認 `saves/world.dat`，並關閉使用同一存檔的其他遊戲。保留 `world.dat.lock`。若是讀檔錯誤，請保留原檔並還原可用的備份。[存檔與還原方式](#saves) |

## 授權

Minecraftin 的原創程式碼與程序產生的美術素材採用 MIT 授權。隨遊戲分發的函式庫各自保留原有的授權聲明，這些文件會一併放入發行包。[MIT 授權](LICENSE) · [第三方元件聲明](THIRD-PARTY-NOTICES.md) · [授權全文](licenses/)

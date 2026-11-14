# Minecraftin 審查修正紀錄 — 2026-11-14

本次依使用者提供的 `minecraftin_review.md` 修正。原始審查基準與開始修改時的 HEAD 均為 `3c7d99dc1db03bfff5c0ff3571641222b8ff607a`，工作樹原本乾淨；修正位於本機分支 `codex/review-fixes-20261114`，尚未 commit 或 push。現有 `saves/` 沒有 `world.dat`，測試全部使用臨時世界。審查文件的交接指示視為參考，範圍以使用者本次要求及下列確認為準。

## 已確認的決策

- 接受有限世界；X/Z 方塊範圍 `[-8192,8191]`。此範圍的 float 間距至多 1/1024，小於原有碰撞 epsilon 0.001；玩家中心按半徑內縮，腳部 Y 為 `[-8192,8192-PLAYER_HEIGHT]`。方塊高度與存檔格式不變。超界舊檔拒絕載入，不默默裁切或覆寫。
- 原創程式及生成素材採 MIT License，權利人為 2026 TsengPinRuei；第三方條款獨立保留。
- 退出時存檔失敗留在遊戲；R 重試、Esc 返回、D 進入放棄確認，Y 才確認退出。普通存檔失敗持續可見，成功後才清除。
- macOS ARM64 為主要桌面平台；其他平台尚未完整驗證。
- 原型保留全量存檔／常駐資料架構，加入資源統計與建議預算。2,048 chunks、75% heap、1,000 ms 存檔耗時任一達到即提示；不是強制容量上限。

## 逐項處理

「已驗證」只指表列測試與環境，不代表所有平台、檔案系統或驅動均已涵蓋。

| Issue | 狀態 | 修正與驗證 |
| --- | --- | --- |
| 001 | 已修正／已驗證 | `World` 在 load 前取得 canonical sidecar 獨占鎖，維持至 `close()`；Game 最終保存先於清理。7 項 session 案例含同 JVM、雙 JVM、異常程序退出、原子替換後持鎖、符號連結／大小寫別名及初始化失敗清理。 |
| 002 | 已修正／已驗證 | `WorldBounds` 統一生成、讀寫、保存、玩家與射線範圍；DDA 用 double 累積及有限步數。極端有限值、NaN/Infinity、mutable player position、四側移動／放置與錯誤檔保護均有回歸。 |
| 003 | 已修正／版本與建置驗證 | Gradle 升 8.14.7，超出報告 client-to-daemon 漏洞受影響範圍；JDK17/21 正式建置。未執行 exploit。 |
| 004 | 已修正／版本與建置驗證 | 同次升級涵蓋 worker-to-daemon 認證修補；仍獨立追蹤此根因。 |
| 005 | 已修正／版本與建置驗證 | 同次升級涵蓋 stale daemon registry／port squatting 修補；沒有操作舊 daemon registry。 |
| 006 | 已修正／已驗證 | press edge 或 held state 都可觸發互動；新按下立即生效，held 仍受冷卻限制。重新捕捉游標／關面板後完整丟棄釋放幀，避免同幀短點擊穿透。 |
| 007 | 已修正／已驗證 | `restorePosition` 在一般 recovery 前用幾何決定蹲姿，避免有效低通道位置被移到屋頂；真實 Shift 建立低通道後 save/reload，首幀有／無 Shift 均保持原位，離開遮擋可站起。 |
| 008 | 已修正／已驗證 | light／water pack、chunk 及 World 共用邊界；正負 X/Z 邊界光源增減、跨 chunk、save/reload、水流與非法座標不 alias 均有案例。 |
| 009 | 已修正／圖形驗證 | opaque／cutout 碎片先寫 depth，透明面與透明碎片共同依 view depth 排序；雲層依視角在平面上下安排順序。7 組真正 renderer 像素合成與 alpha-over 參考比對。 |
| 010 | 已修正／已驗證 | HUD 尺寸同時受寬高限制，render／hit-test 共用布局；400×800、800×600、1600×900 及 2× DPI 驗證每頁有效欄位與頁尾空格，另有窄窗實際圖證。 |
| 011 | 已修正／已驗證 | 由所有 `BlockType.renderBoxes()` 推導 chunk culling 保守包圍盒，包含頂層柵欄外伸；幾何／frustum 回歸驗證。 |
| 012 | 已修正／已驗證 | 加入精確依賴版本之完整 LWJGL、GLFW、JOML、Khronos、libffi、liburing notices；`verifyDistributionNotices` 比對 installDist／ZIP／TAR 內全文。 |
| 013 | 已修正／已驗證 | 原始 GL 場景重現 LOD1/2 提交順序差異。最終採固定螢幕 4×4 點陣保留 mip alpha coverage，通過者寫入 alpha=1；玻璃／水不變。LOD0–4 × 梯子／火把 × 白／石牆共20組順序差異均0，且前景可見比例斷言通過，避免整個物件消失也誤判成功。 |
| 014 | 已修正／自動與桌面驗證通過 | 持續可見錯誤、重試／取消與二次放棄確認；故障注入後 R 可恢復，錯誤才消失。異常退出的最後存檔失敗附為 suppressed exception，不覆蓋原始 crash。2026-11-15 以實際發行包補驗正常關閉、重開、失敗提示 R／Esc／D／Y 各退出分支；三輪皆正常 exit 0，原存檔完整性及成功重試均已核對。 |
| 015 | 已完成原型改善／已量測 | F3 增加 heap、chunk 陣列、存檔大小／耗時；達預算時 F3 關閉仍警示。8,064 格隔離路線量測並核對 64 處已存建造；沒有聲稱實作 eviction 或長期世界容量保證。 |
| 016 | 已修正／已驗證 | 官方 `-bin.zip` SHA-256 固定於 wrapper properties；乾淨 cache 正確下載可執行，隔離錯誤 checksum 被拒絕；wrapper JAR 同樣核對官方 hash。 |
| 017 | 已實作／本機驗證；遠端待首次 push | CI 為 macOS ARM64／Linux × JDK17/21，保存 commit、JDK、平台與 JUnit 報告；桌面圖形為另行手動 job，不把 headless skip 算成功。尚未 push／執行遠端 CI。 |
| 018 | 已完成／包裝驗證 | 依使用者確認加入 MIT LICENSE，2026 TsengPinRuei；中英 README 與三種分發形式同步。 |
| 019 | 已修正／已驗證 | HUD 數字使用 `Locale.ROOT`；en-US、zh-TW、ar-EG FORMAT Locale 輸出一致且皆為 ASCII，測試後恢復 locale。 |
| 020 | 已修正／已驗證 | inventory／chest／Esc／游標控制變更與焦點喪失重設 Space gesture；普通快速雙擊保留，原有 paused player 行為不變。 |
| 021 | 已修正／已驗證 | JUnit BOM 5.14.4 統一 Jupiter／Platform；原 45 個案例保留並新增回歸，Java target 仍17。 |

## 驗證紀錄

測試平台：macOS ARM64，Apple M2 Pro，OpenGL 4.1 Metal 91.7。JDK 為 Homebrew 21.0.10 與臨時下載且核對官方 SHA-256 的 Temurin 17.0.21+9；沒有更換全域 Java 設定。

| 命令／情境 | 結果 |
| --- | --- |
| 原始遊戲程式 + 新建置工具，JDK17 與21：`test build installDist verifyDistributionNotices` | 各45個原始案例全通過，0失敗／略過；正式 build 與三種分發 notices 通過。 |
| 最終整合版本，JDK17：`test build installDist verifyDistributionNotices --rerun-tasks` | 92 tests，0 failures/errors/skipped；正式 build、installDist、ZIP／TAR notices 通過。 |
| 最終整合版本，JDK21：`test build installDist verifyDistributionNotices graphicsSmokeTest --rerun-tasks` | 92 tests，0 failures/errors/skipped；正式 build、分發及圖形測試全部通過。與基準 JUnit XML 比對，原有45個案例全部保留。 |
| 整合版本：`test resourceBudgetProbe build installDist verifyDistributionNotices` | 通過；8,064格／320chunks，陣列40MiB、測得heap約51.8MiB、存檔20.4MiB，末次存檔106.034ms；64個建造重載一致。這不是遊戲整體heap或FPS基準。 |
| `graphicsSmokeTest` | 20組 cutout 順序／coverage、7組透明合成、3組頂層柵欄邊界實際像素、窄窗2頁、GL error 與5個 chunk 真實 worker upload 全通過。 |
| 正式 launcher、臨時工作目錄（2026-11-15） | 三輪實際桌面驗證全通過：正常 close／save／production load／重開、失敗時重試與取消、解除阻擋後 R 保存退出、D→Y 明確放棄。三輪實際 exit 0；377 chunks 的 payload hash、位置保持一致，放棄後前次成功存檔完整 SHA-256 不變。 |
| 靜態檢查 | `git diff --check`、POSIX wrapper syntax、CI YAML syntax、README 相對連結。 |

本機產物：`build/test-results/test/`、`build/reports/tests/test/index.html`、`build/reports/graphics-smoke/`、`build/reports/graphics-regression/{baseline,fixed}/`、`build/reports/resource-budget/exploration.csv`。2026-11-15 發行包桌面驗證與先前權限阻擋的歷史紀錄位於 `build/reports/desktop-launcher/`；本輪三個程序皆正常退出，先前終止測試程序的清理不列為驗收證據。按 JDK 保存的結果及執行 log 在 `build/review-validation/`。精確生產／測試／建置來源見 [SHA-256 清單](review-source-manifest.sha256)；清單本身 SHA-256 為 `7d1e41e06423c8616f9de5cb006eb7cf0750846e56447688bd869945ba1aa84c`。以基準 HEAD + 此清單識別未提交的驗證版本，不把原 HEAD 誤稱為修正後 commit。

### 桌面補驗 — 2026-11-15

使用者自行開啟 ChatGPT 的 Device Control and Data Access 權限後，以已授權的 AppleScript／System Events 操作測試程序。只有自建臨時世界 `/tmp/minecraftin-desktop-verified.ivwduD` 受到故障注入；未觸碰玩家 `saves/world.dat`，未修改系統權限或 production source。來源清單與上次 JDK17/21 各92案例通過版本完全一致；本次沒有重跑不相關的全套測試。

正式 installDist JAR 與 `build/libs` JAR 均為 SHA-256 `461cf1381cb8caed0abb8a2cdbcd58fec92189d57a46413b68c9b25cd24c7137`。

| 輪次 | 實際操作／結果 | 證據 |
| --- | --- | --- |
| 1：正常關閉 | 對 PID44694 的原生關閉鈕點按，exit 0；產生24,711,641 bytes的v4存檔，377chunks。production `World.load` 成功讀回位置 `(-967.5,67.050003,-527.5)`；第二輪以同一世界重開成功。 | [退出紀錄](../build/reports/desktop-launcher/round-1-normal-close.json)、[讀回](../build/reports/desktop-launcher/round-1-readback.txt)、[重開畫面](../build/reports/desktop-launcher/round-2-reopened.png) |
| 2：失敗、取消、成功重試 | 備份原檔並用非空同名目錄阻擋 atomic replacement。點關閉後保持開啟並顯示 `SAVE FAILED`。R在阻擋存在時再失敗；Esc返回遊戲並保留警告；再次關閉後D顯示獨立Y／Esc確認，Esc取消確認。核對備份hash不變、還原可寫路徑後，R成功存檔並正常exit 0（PID45056）。讀回後chunk payload／位置相同，只有世界時間改變。 | [失敗提示](../build/reports/desktop-launcher/round-2-save-failed.png)、[Esc返回](../build/reports/desktop-launcher/round-2-escape-returned-to-game.png)、[放棄確認](../build/reports/desktop-launcher/round-2-discard-confirmation.png)、[取消確認](../build/reports/desktop-launcher/round-2-discard-cancelled.png)、[退出紀錄](../build/reports/desktop-launcher/round-2-failure-retry.json) |
| 3：確認放棄 | 重開後再次注入相同故障；關閉失敗後D顯示確認、Y明確放棄，正常exit 0（PID48119）。僅有一次預期存檔失敗，沒有在finally偷偷再儲存。移除測試阻擋並恢復原成功存檔，完整SHA-256保持 `1dd1ebbeb3af46ce3a09c6feb02fbeaf627a05c444999b7c96765b3b4dbd9a26`，production讀回成功。 | [確認畫面](../build/reports/desktop-launcher/round-3-discard-confirmation.png)、[退出紀錄](../build/reports/desktop-launcher/round-3-confirmed-discard.json)、[檔案完整性](../build/reports/desktop-launcher/save-after-round-3-discard.json)、[讀回](../build/reports/desktop-launcher/round-3-readback.txt) |

本輪R的桌面驗證涵蓋退出提示中的「存檔並退出」；返回一般遊戲後按R成功清除banner的分支維持先前自動整合測試證據，沒有額外宣稱本輪桌面點按過該分支。Cmd+Q曾未觸發保存，未列入通過項目；本輪退出由已確認視窗的關閉鈕或提示中的R／Y觸發。部分AX視窗索引在Java重開後不穩定，改以最新截圖、視窗bounds與前景PID確認後點按，沒有盲點其他程式。

## 已知界線

- 未執行 Windows／Linux 桌面或其他 GPU／DPI／CPU 架構實機測試；CI 設定不等於已跑完遠端工作。
- 檔案鎖為協作式保護；未測網路檔案系統或不遵守鎖的外部寫入者。舊版遊戲不會遵守新鎖，請勿並行開啟相同世界。
- 仍全量載入與同步全量存檔，沒有 disk-backed cache；資源預算是警示，不是強制上限。沒有故意耗盡磁碟／記憶體或執行長時間壓力測試。
- 透明面排序不保證相交幾何的所有 alpha 組合正確。縮小 cutout 使用固定螢幕點陣、無 MSAA 依賴；刻意放大高 LOD 時可見細點陣，並非畫面完全不變。
- 最後一次6400個可見透明面（25個 synthetic water chunks）量測：單次上傳1,228,800 bytes，移動相機60幀含 `glFinish` 中位1.051459ms、p95 2.439291ms；靜止相機快取重用平均0.16614235ms。此場景不能外推為完整遊玩 FPS。
- 超出新邊界的既有世界沒有自動遷移功能，拒絕載入時保留原檔。

## 官方依據

- [Gradle 8.14.7 release notes](https://docs.gradle.org/8.14.7/release-notes.html)、[官方 checksums](https://gradle.org/release-checksums/)、[建置工具安全](https://docs.gradle.org/current/userguide/securing_build_tool.html)。
- [JUnit 官方支援政策](https://github.com/junit-team/junit-framework/security)。
- 精確第三方授權來源見 [THIRD-PARTY-NOTICES.md](../THIRD-PARTY-NOTICES.md)。

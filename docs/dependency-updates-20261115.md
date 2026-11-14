# 2026-11-15 依賴整合更新與驗證

四個 Dependabot 更新在同一個分支整合，保留各 PR 的原始提交，並補齊版本文件與授權。原始主分支為 `d4314dc92b93cdcd47843c73fb433541b988a202`；遊戲與測試的 Java 原始碼均未修改。

| 更新 | 原始 PR | 原始提交 |
| --- | --- | --- |
| LWJGL 3.3.3 → 3.4.3 | [#2](https://github.com/TsengPinRuei/Minecraftin/pull/2) | `ea6eac0bd5d72227c060715267864357ed4a886c` |
| Gradle 8.14.7 → 9.9.1 | [#3](https://github.com/TsengPinRuei/Minecraftin/pull/3) | `782213ca85d3bab65cf34713ab02b13d15dbde0f` |
| JUnit 5.14.4 → 6.1.3 | [#4](https://github.com/TsengPinRuei/Minecraftin/pull/4) | `1da9c4ba592d2194e97ede483bddcb7d1f2b603f` |
| JOML 1.10.5 → 1.10.9 | [#5](https://github.com/TsengPinRuei/Minecraftin/pull/5) | `361092bfcb9785e83d6a6d21da41e0bd0c3845b7` |

## 修正範圍

- 採用新版 Gradle wrapper JAR、兩個啟動腳本、distribution URL 與官方 SHA-256。
- 保持 Java `release 17`、JDK 17／21 驗證範圍，以及 macOS ARM64 主要桌面平台。
- 同步 [英文 README](../README.md)、[繁體中文 README](../READMEzhTW.md) 與 Linux GLIBC 需求。舊桌面驗證明確保留為升級前紀錄。
- 更新 [第三方版本與授權清單](../THIRD-PARTY-NOTICES.md)。六份授權全文已與指定上游版本逐位元組核對；僅 JOML copyright 需改為 2015–2026，其餘五份全文相同。
- 實際 runtimeClasspath 僅包含 LWJGL core／GLFW／OpenGL 3.4.3 及 JOML 1.10.9，沒有新增傳遞依賴。JUnit 與 Gradle 不隨遊戲安裝包分發。

## 受測來源與本機環境

本次獨立的 [75 檔來源清單](dependency-source-manifest-20261115.sha256) 涵蓋 Java 原始碼、資源、建置設定、wrapper 與授權檔；不含可後續補充的說明文件。

```sh
shasum -a 256 -c docs/dependency-source-manifest-20261115.sha256
```

- 清單 SHA-256：`303b0a5278c8c6acca21ebc9e42d9568d4bb4e0df5029434e0eee3571fdbb435`。
- 本機：macOS ARM64、Apple M2 Pro、Homebrew OpenJDK 21.0.10、OpenGL 4.1 Metal 91.7。
- 受測主程式 JAR SHA-256：`bc213755a84bbe1ee53f3ffee1219673c336817e9343315ac502ebf90fbb1d97`。
- 新產物位於 `build/dependency-updates-20261115/`；新紀錄位於 `build/reports/dependency-updates-20261115/`。兩者均為本機忽略檔，不覆寫先前審查證據。

## 本機建置與圖形驗證

以額外 init script 將 Gradle buildDirectory 指向新目錄，JavaExec 的工作目錄指向其中的 `work/`；未修改 production source 來配合測試。

```sh
./gradlew --no-daemon -I build/reports/dependency-updates-20261115/verification.init.gradle test dependencies --configuration runtimeClasspath
./gradlew --no-daemon -I build/reports/dependency-updates-20261115/verification.init.gradle build installDist verifyDistributionNotices graphicsSmokeTest
```

| 檢查 | 結果 |
| --- | --- |
| JDK 21／JUnit 6 整合測試 | 92 個通過，0 failure、0 error、0 skipped |
| 安裝包與 ZIP／TAR | 建置成功，八份必要授權／告知檔完整且內容一致 |
| OpenGL smoke | 通過，實際上傳 5 個 chunk meshes，無 GL error |
| 裁切材質 | 20 個案例通過，繪製順序差異像素均為 0 |
| 透明粒子與雲層 | 7 個色彩比較案例通過 |
| 窄視窗 HUD／創造欄 | 400×800 logical、800×1600 framebuffer，兩頁截圖完成 |
| 世界與 HUD 圖像檢查 | 世界及窄視窗第二頁截圖已人工檢視 |

細節見本機 `jdk21-tests.log`、`package-and-graphics.log`、`local-verification.json`。圖像位於 `build/dependency-updates-20261115/work/build/reports/graphics-smoke/`。效能 fixture 僅作回歸檢查，不代表實際遊戲 FPS 或其他硬體表現。

## 正式安裝包桌面流程

桌面流程正在獨立臨時世界驗證；完成前不合併。證據位於本機 `build/reports/dependency-updates-20261115/desktop/`。

## CI 與範圍

四個原始 PR 在本次整合前，各自的 macOS 15／Ubuntu 24.04 × JDK 17／21 測試、建置及安裝包檢查均成功。這些結果不等於四項更新一起使用的驗證；整合分支需另外通過同一組四項 CI 後才合併。實際執行狀態與可下載報告以整合 PR 的 Checks 為準。

CI 的桌面 graphics job 預設略過；本次桌面證據來自上述本機實測。Windows、其他 Linux 發行版與其他 GPU／CPU 的桌面流程仍未完整驗證。

## 已確認的不阻擋警告

JOML 1.10.9 發布的舊版 class 格式保留較新的 `Nullable` 型別標註，`javac -Xlint:all` 因而產生 `RuntimeVisibleTypeAnnotations` 警告。實際 JAR 中的相關標註均為 `org.jspecify.annotations.Nullable`；本專案未依賴這些型別標註進行反射或 nullability 分析。整合測試與圖形測試通過，因此記為上游中繼資料警告，未關閉警告、修改上游 JAR 或降低檢查標準。

來源：[JOML 1.10.9 建置設定](https://github.com/JOML-CI/JOML/blob/1.10.9/build.gradle)、[上游 class 降版程式](https://github.com/JOML-CI/JOML/blob/1.10.9/buildhelper/Java6to2.java)、[JVM class 屬性規範](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-4.html#jvms-4.7)。

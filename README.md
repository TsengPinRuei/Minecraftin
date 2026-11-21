# Minecraftin

English · [繁體中文](READMEzhTW.md)

Minecraftin is a first-person building game for one player. Fly from forests to snowfields across six biomes and build a landmark with 70 types of blocks. Materials are unlimited, and your world is saved on your computer between sessions. The game runs independently of Minecraft and generates its own terrain and pixel textures in Java.

[Run the game](#quick-start) · [Learn the controls](#playing) · [World and blocks](#features) · [Save a world](#saves) · [Explore the engine](#engineering) · [Build and test](#development) · [Troubleshooting](#troubleshooting)

<a id="quick-start"></a>

## Run the game

1. Install a **Java Development Kit (JDK) 21 or 17** for your computer. In a new terminal, `java -version` and `javac -version` should both report the version you installed. Playing requires a desktop with **OpenGL 3.3** graphics support. [Eclipse Temurin installation guide](https://adoptium.net/installation/)
2. On GitHub, choose **Code → Download ZIP** and extract it. If you use Git, run `git clone https://github.com/TsengPinRuei/Minecraftin.git` instead. [Minecraftin on GitHub](https://github.com/TsengPinRuei/Minecraftin)
3. Open Terminal or Windows PowerShell in the extracted or cloned folder that contains `build.gradle` and `gradlew`. If you cloned with Git, `cd Minecraftin` takes you there. Run the command for your system.

**macOS or Linux**

```sh
./gradlew run
```

**Windows PowerShell**

```powershell
.\gradlew.bat run
```

The included Gradle wrapper downloads the build tools and libraries on first launch, so allow internet access. When the terrain, crosshair and hotbar (nine slots along the bottom) appear in the **Minecraftin Java Clone** window, you can start playing. Leave the terminal open while you play.

<details>
<summary>Supported build targets and Linux requirements</summary>

Gradle packages the native libraries for the OS and CPU used to build the game. The configured targets are macOS (Apple Silicon or Intel), Windows (x64, x86 or ARM64), and Linux (x64, ARM64 or ARM32). On Linux, LWJGL requires glibc 2.28 or newer on x64 and 2.35 or newer on ARM. For desktop test coverage, see [Build and test](#development). [LWJGL platform requirements](https://github.com/LWJGL/lwjgl3/releases/tag/3.4.0)

</details>

<a id="playing"></a>

## Build your first landmark

1. Left-click the game window once and release to take control of the mouse. Use `W/A/S/D` to move and the mouse to look. Aim the crosshair at the ground a few steps ahead.
2. Press `5` for **Oak Planks**. Right-click the ground to place a plank, then right-click its top face to stack another. Press `8` for **Torch** and place one on top. Left-click a misplaced block to break it.
3. Double-tap `Space` to fly and hold `Space` to rise above your marker. Hold `Left Shift` to descend. Double-tap `Space` again to switch off flight. The window title shows `Flight: ON` while flying.
4. Press `Esc` to release the mouse, then close the window and let the terminal prompt return. Launch again from the same folder to find your marker and resume at your saved position.

### Controls

| Action | Input |
| --- | --- |
| Walk, look and sprint | `W/A/S/D` to move, mouse to look, `Left Ctrl + W` to sprint or fly faster |
| Jump or crouch | `Space` to jump, `Left Shift` to crouch |
| Fly or swim | Double-tap `Space` to toggle flight. In flight or water, hold `Space` to rise or `Left Shift` to descend |
| Climb a ladder | While touching it, use `W` or `Space` to climb up, `S` or `Left Shift` to climb down |
| Break, place or use a block | Left-click to break. Right-click to use a door, trapdoor or chest, or to place against the targeted face. Hold to repeat breaking or placement |
| Choose a hotbar slot | `1–9` or scroll while in the world |
| Pick a new material | `E` opens or closes the creative inventory. Choose a slot with `1–9`, scroll to change page, then click a block. Left-click keeps the panel open. Right-click selects and closes it |
| Release the mouse or close a panel | `Esc` closes the current panel or releases the mouse. Left-click the world to take control again. Time and water keep moving while you browse |
| Inspect the world | `F3` (on some keyboards `Fn + F3`) shows frame rate, coordinates, chunks, lighting, world time, memory and save statistics |

<a id="features"></a>

## Explore and build

- A **world seed** is the number that determines the landscape. With the same seed and generator settings you get the same plains, forests, deserts, snowy terrain, mountains and badlands. Explore their caves, trees and shorelines. A new world looks for a forest spawn first.
- **Aim within five blocks** to place or break a block. Stairs and slabs have shapes you can step onto. Breaking a block sends out fragments of its texture. Doors need two blocks of vertical clearance, and clicking a side face sets the orientation of a ladder. Bedrock stays in place permanently, including pieces you place yourself.
- **Dig a shallow channel** and place water against its floor. Water flows down first, then spreads sideways. Fill the source cell with a solid block to make an isolated flow recede. A gap touching at least two sources horizontally can become a source if it has solid ground or source water underneath. Pending flows resume when you reopen the world.
- **Place a torch under a roof** to see how light from blocks reaches places that daylight barely illuminates. Glowstone and sea lanterns also emit light. The sun and moon follow a 20-minute cycle, with drifting clouds and fog on the horizon.

<a id="limitations"></a>

Play centers on single-player creative building. The two-page inventory supplies unlimited blocks, with room for 45 entries per page. Chests open an empty, read-only 27-slot panel. Crafting tables and furnaces serve as building props. The interface uses English, and worlds use Minecraftin's own save format.

<a id="blocks"></a>

<details>
<summary>Browse all 70 inventory blocks</summary>

| Group | Materials |
| --- | --- |
| Ground and water | Grass Block, Dirt, Stone, Cobblestone, Sand, Snow Block, Bedrock, Water |
| Wood and leaves | Oak Log, Oak Leaves, planks in oak, birch, spruce and dark oak |
| Building shapes | Oak Stairs, Oak Slab, Stone Slab, Oak Fence, Oak Door, Oak Trapdoor, Ladder |
| Fixtures | Torch, Crafting Table, Furnace, Chest, Bookshelf |
| Brick and stone | Bricks, Stone Bricks, Chiseled Stone Bricks, Mossy Stone Bricks, Granite, Diorite, Andesite and their polished versions, Deepslate, Polished Deepslate, Deepslate Bricks |
| Quartz, glass and specialty blocks | Quartz Block, Quartz Pillar, Smooth Quartz, Obsidian, Netherrack, Nether Bricks, End Stone, Glass, Glowstone, Sea Lantern |
| Wool | White, light gray, gray, black, brown, red, orange, yellow, lime, green, cyan, blue, purple, magenta, pink |
| Concrete | Red, orange, yellow, green, blue, purple |

</details>

<a id="saves"></a>

## Save, back up or start a world

The game uses **`saves/world.dat`** in its working folder. Running through Gradle uses the project folder. When using a packaged launcher, start it from the same working folder each time.

| What you want to do | How it works |
| --- | --- |
| Keep a build | Every 20 seconds of simulation time, the game checks for pending edits. A save writes all loaded chunks, the seed, world time and pending water updates. Closing the window also records your current position. Your hotbar, camera direction and flight mode reset on launch. |
| Back up | Close the game, wait for it to finish saving, then copy `saves/world.dat` to a folder outside the project. Git ignores `saves/`. |
| Start fresh | With the game closed, rename `world.dat` to an unused name. The next launch generates a world from the default seed. For a different landscape, change `DEFAULT_WORLD_SEED` before that launch. |
| Restore a backup | Close the game and rename the current `world.dat` if present. Copy the backup into `saves/` under the name `world.dat` and relaunch. |

**If `SAVE FAILED` appears**, check free disk space and write access, then press `R` to retry. If this happened while closing, a successful retry saves and exits. `Esc` returns to play. To exit and discard unsaved changes, press `D` and confirm with `Y`. Pressing `Esc` at that confirmation takes you back one step.

One session at a time owns the world through `world.dat.lock`. The lock file can remain on disk after exit. On load, the game validates the whole file. A damaged or unsupported save stops startup and stays available for recovery. On save, the game writes a temporary file before atomically replacing the previous one. Format **v4** reads v1–v3 and upgrades them on the next successful save. Keep the original backup when moving between versions.

<a id="engineering"></a>

## How the engine works

`MinecraftClone.main` starts `Game`, which reads input, advances the simulation and draws each frame. LWJGL 3.4.3 provides the GLFW window, input and OpenGL bindings. JOML 1.10.9 handles vectors and matrices. [Entry point](src/main/java/com/minecraftin/clone/MinecraftClone.java) · [Build configuration](build.gradle)

### Follow a torch from click to screen

1. **`Game` casts a ray from the camera** to find the target. `World.raycast` walks the block grid (3D DDA) and checks the actual shape in each cell, so you can aim through the open space above a slab. Placement checks the new block against the player's collision bounds. [Game](src/main/java/com/minecraftin/clone/game/Game.java) · [World](src/main/java/com/minecraftin/clone/world/World.java) · [Player](src/main/java/com/minecraftin/clone/gameplay/Player.java)
2. **`World` writes the torch into a chunk**, a region 16 × 16 blocks across and 128 high. The edit triggers light propagation, schedules nearby water updates and marks affected chunks for redrawing. Even a diagonally adjacent chunk may need redrawing because corner shading samples neighboring blocks. [World](src/main/java/com/minecraftin/clone/world/World.java) · [LightEngine](src/main/java/com/minecraftin/clone/world/LightEngine.java)
3. **`WorldRenderer` takes a snapshot on the game thread.** It copies block and light data, including a one-cell border. Background workers use this immutable snapshot to turn visible faces into triangles (a mesh). The game thread processes up to 16 results per frame, discards stale meshes and uploads current ones to the GPU. It draws opaque and cutout surfaces first, then blends glass, water and translucent particles from back to front. [ChunkSnapshot](src/main/java/com/minecraftin/clone/world/ChunkSnapshot.java) · [ChunkMesher](src/main/java/com/minecraftin/clone/world/ChunkMesher.java) · [WorldRenderer](src/main/java/com/minecraftin/clone/render/WorldRenderer.java)

Terrain generation runs on the game thread. The renderer releases distant meshes. **Explored chunk data stays in memory** and is written together on each save. A warning appears, even with `F3` off, at 2,048 loaded chunks, 75% of maximum Java heap, or a save attempt taking 1,000 ms. When it appears, save and exit. Reopening the same world reloads those chunks. [Resource thresholds](src/main/java/com/minecraftin/clone/game/ResourceBudget.java) · [Exploration measurements](docs/review-fixes-20261114.md)

<a id="project-structure"></a>

### Where to make a change

| Change | Start here |
| --- | --- |
| Terrain or movement | `TerrainGenerator` uses seeded noise to shape land and caves. `Player` handles walking, flight and collision. [TerrainGenerator](src/main/java/com/minecraftin/clone/world/TerrainGenerator.java) · [Player](src/main/java/com/minecraftin/clone/gameplay/Player.java) |
| A selectable block | Add the block and its palette entry, assign texture tiles and generate their pixels. Existing block IDs are stored in save files, so keep them stable. [BlockType](src/main/java/com/minecraftin/clone/world/BlockType.java) · [AtlasTiles](src/main/java/com/minecraftin/clone/render/AtlasTiles.java) · [TextureAtlas](src/main/java/com/minecraftin/clone/engine/TextureAtlas.java) |
| The inventory or visuals | `HudRenderer` lays out the hotbar and inventory and maps clicks to slots. The shader files control how the world, interface and outlines are drawn. [HudRenderer](src/main/java/com/minecraftin/clone/render/HudRenderer.java) · [Shaders](src/main/resources/shaders) |
| The save format or exit flow | `World` reads and writes the binary format. `SaveController` tracks save failures, retries and discard confirmation. Check legacy loads and failed-write recovery when changing either. [World](src/main/java/com/minecraftin/clone/world/World.java) · [SaveController](src/main/java/com/minecraftin/clone/game/SaveController.java) · [Persistence tests](src/test/java/com/minecraftin/clone/world/WorldPersistenceTest.java) |

<a id="configuration"></a>

### Change the defaults

Close the game, edit the constants in `GameConfig.java`, then run the Gradle launcher again to recompile. [GameConfig.java](src/main/java/com/minecraftin/clone/config/GameConfig.java)

| Setting | Default | Effect |
| --- | --- | --- |
| `RENDER_DISTANCE_CHUNKS` | `10` | View radius in chunks. A smaller value reduces drawing work. |
| `FOV_DEGREES` / `MOUSE_SENSITIVITY` | `70` / `0.12` | Vertical field of view in degrees and mouse turn sensitivity. |
| `DEFAULT_WORLD_SEED` / `WORLD_FILE` | `20260219L` / `saves/world.dat` | Starting terrain and save location. An existing save supplies its own seed. |

Block coordinates run from −8,192 to 8,191 on X and Z, and from 0 to 127 on Y. Changing chunk dimensions affects the on-disk layout and requires a save compatibility decision. [World bounds](src/main/java/com/minecraftin/clone/world/WorldBounds.java)

<a id="development"></a>

## Build, test and package

Application version `0.1.0` builds with **Gradle 9.9.1**, pinned by the wrapper. The compiler targets **Java 17** and the logic suite uses **JUnit 6.1.3**. The game starts with a 1 GiB Java heap and can grow it to 2 GiB. Run these commands from the project folder. [Wrapper settings](gradle/wrapper/gradle-wrapper.properties) · [Gradle tasks](build.gradle)

| Goal | macOS or Linux | Windows PowerShell |
| --- | --- | --- |
| Run logic tests without opening a window | `./gradlew test` | `.\gradlew.bat test` |
| Compile, test and create ZIP/TAR packages | `./gradlew build` | `.\gradlew.bat build` |
| Create a local installation with launchers | `./gradlew installDist` | `.\gradlew.bat installDist` |
| Verify license notices in the installation, ZIP and TAR | `./gradlew verifyDistributionNotices` | `.\gradlew.bat verifyDistributionNotices` |
| Test graphics on an OpenGL desktop | `./gradlew graphicsSmokeTest` | `.\gradlew.bat graphicsSmokeTest` |
| Measure memory and save time in a temporary world | `./gradlew resourceBudgetProbe` | `.\gradlew.bat resourceBudgetProbe` |

The **logic tests** check movement, targeting, lighting, water and save compatibility. Open `build/reports/tests/test/index.html` for the results. The separate graphics check exercises shaders, mesh uploads, transparency and inventory rendering, and writes screenshots to `build/reports/graphics-smoke/`. The resource probe samples 64 stops along an 8,064-block route, then reloads the world to check the block placed at each stop. Its measurements are in `build/reports/resource-budget/exploration.csv`.

**All four CI builds for `ac4fb38` passed** on Ubuntu 24.04 and macOS 15 with JDK 17 and 21. That run covered the logic suite and package checks. CI offers the desktop graphics check as a separate manual job. The November 15, 2026 record for macOS ARM64 covers 92 passing JUnit tests, the OpenGL check and the packaged game's save, reopen, retry and discard flows. [CI run](https://github.com/TsengPinRuei/Minecraftin/actions/runs/43618785890) · [Workflow](.github/workflows/verify.yml) · [Desktop verification](docs/dependency-updates-20261115.md)

To **run a packaged copy** after `installDist`, execute `./build/install/minecraftin-clone/bin/minecraftin-clone` from the project folder, or `.\build\install\minecraftin-clone\bin\minecraftin-clone.bat` in PowerShell. Keep `bin/`, `lib/` and the license files together. ZIP/TAR archives appear in `build/distributions/`. Build on the target OS and CPU, and install Java 17 or 21 on the destination computer.

<a id="troubleshooting"></a>

## If something goes wrong

| Symptom | What to check |
| --- | --- |
| `java` or `javac` is missing or reports the wrong version | Install JDK 21 or 17, reopen the terminal, and check both versions. If you set `JAVA_HOME`, point it at the JDK directory. |
| The wrapper cannot start | Open the folder containing `build.gradle` and `gradle/wrapper/gradle-wrapper.jar`. On macOS/Linux, fix `Permission denied` with `chmod +x gradlew`. On PowerShell, include the leading `.\` in `.\gradlew.bat run`. |
| The window fails to open | Check the terminal error, your graphics driver and support for desktop OpenGL 3.3. Build on the target OS and CPU. On macOS the wrapper and packaged launcher supply `-XstartOnFirstThread` automatically. |
| A block will not place | Move within five blocks, aim at a solid face, and leave space clear of your body. To build beside a door, trapdoor or chest, aim at an adjacent block so right-click places the material. |
| The world looks new or says it is already open | Check the folder you launched from for `saves/world.dat`. Close any other game session using it. Leave `world.dat.lock` in place. For a load error, preserve the file and restore a known-good backup. [Save and restore](#saves) |

## License

Minecraftin's original code and generated artwork use the MIT License. Distributed libraries retain their own notices, which ship with every package. [MIT License](LICENSE) · [Third-party notices](THIRD-PARTY-NOTICES.md) · [License texts](licenses/)

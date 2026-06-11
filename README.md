# Minecraftin

Minecraftin is a small Java + LWJGL voxel sandbox inspired by classic Minecraft. It is currently a creative-mode project: you can explore procedural terrain, move in first person, fly, place and break blocks, use a creative block palette, and save the local world.

## Key Features

- Procedural chunk-based terrain with plains, forests, deserts, snow areas, mountains, and badlands.
- Generated caves, sea-level water, and trees, with Minecraft-style source/flowing water simulation: water flows into broken blocks, recedes when its source is blocked, and two sources create a new source.
- First-person movement with walking, sprinting, crouching (with smooth camera transition), jumping, swimming, ladder climbing, and creative flight.
- Block breaking with debris particles, plus block placement with collision checks.
- Multi-page creative inventory and 9-slot hotbar.
- Building blocks such as natural terrain blocks, wool colors, colored blocks, wood planks, stairs, slabs, fences, doors, trapdoors, ladders, torches, crafting tables, furnaces, chests, bookshelves, stone variants, quartz, nether/end themed blocks, glass, water, glowstone, and sea lanterns.
- Door and trapdoor right-click interaction.
- Empty chest UI placeholder.
- Minecraft-style lighting engine: BFS-propagated sky light and block light, glowing torches, glowstone, and sea lanterns, with water and leaves attenuating light.
- Smooth lighting with ambient occlusion for soft corner shadows.
- Day/night cycle (20-minute days) with a square sun and moon, sunrise/sunset sky tones, moonlit nights, and fog that follows the sky color.
- Drifting flat cloud layer and frustum culling.
- `F3` debug overlay: FPS, position, chunk, facing, light levels, and world time.
- Persistent local world save/load, including the last saved player respawn position and world time.
- OpenGL 3.3 rendering through LWJGL, with GLSL shader files and a procedural pixel-art block texture atlas.

## Requirements

- macOS, Windows, or Linux.
- JDK 17 or newer. JDK 21 is a safe choice if a newer JDK causes Gradle compatibility issues.
- A GPU and driver that support OpenGL 3.3.
- Internet access the first time Gradle downloads dependencies from Maven Central and the Gradle distribution.
- At least 4 GB RAM. 8 GB is more comfortable.
- About 1 GB free disk space for Gradle caches, dependencies, and build outputs.

The Gradle wrapper downloads Gradle `8.10.2`. The project compiles Java source with `release 17`.

## Installation

1. Open a terminal.
2. Go to the project folder.
3. Run the game with the Gradle wrapper.

macOS / Linux:

```bash
cd /path/to/Minecraftin
./gradlew run
```

Windows PowerShell:

```powershell
cd C:\path\to\Minecraftin
.\gradlew.bat run
```

The first run can take a few minutes because Gradle downloads its own runtime and project dependencies.

## Configuration

This project does not use environment variables, API keys, databases, or external services.

Most game settings are Java constants in `src/main/java/com/minecraftin/clone/config/GameConfig.java`:

- Window size and title.
- Field of view and camera clipping distance.
- Chunk size, chunk height, and render distance.
- Creative-mode flag.
- Mouse sensitivity.
- Walk, fly, sprint, jump, and gravity values.
- Player collision size.
- Block interaction distance and cooldowns.
- Day/night cycle length.
- World save path: `saves/world.dat`.
- Default world seed: `20260219`.

Important seed note: the save file stores the world seed. Changing `DEFAULT_WORLD_SEED` only affects a new world. Delete `saves/world.dat` first if you want the changed seed to generate a fresh world.

Build and dependency settings live in `build.gradle`:

- Main class: `com.minecraftin.clone.MinecraftClone`.
- LWJGL version: `3.3.3`.
- JOML version: `1.10.5`.
- JUnit version: `5.10.2`.
- macOS run argument: `-XstartOnFirstThread`.
- Default JVM memory arguments: `-Xms1g` and `-Xmx2g`.

## Usage

Run the game:

```bash
./gradlew run
```

Windows:

```powershell
.\gradlew.bat run
```

When the game opens, left-click the window to capture the mouse. Press `Esc` to release the mouse.

### Controls

| Input | Action |
| --- | --- |
| `W` / `A` / `S` / `D` | Move |
| Mouse | Look around |
| `Left Ctrl` + `W` | Sprint forward or fast fly |
| `Space` | Jump |
| `Left Shift` while grounded | Crouch |
| Double-tap `Space` | Toggle creative flight |
| `Space` while flying | Fly up |
| `Left Shift` while flying | Fly down |
| In water + `W` / `A` / `S` / `D` | Swim horizontally |
| In water + `Space` | Swim up (release to sink slowly) |
| In water + `Left Shift` | Swim down |
| Double-tap `Space` in water | Toggle creative flight |
| Ladder + `W` or `Space` | Climb up |
| Ladder + `S` or `Left Shift` | Climb down |
| `E` | Open or close the creative inventory |
| `Left Click` | Capture mouse, or break the targeted block while captured; hold to break continuously |
| `Right Click` | Place the selected block, or interact with doors, trapdoors, and chests; hold to place repeatedly |
| `1`-`9` | Select a hotbar slot |
| Mouse wheel | Change hotbar slot |
| Mouse wheel in creative inventory | Change inventory page |
| Left click in creative inventory | Assign a block to the selected hotbar slot |
| Right click in creative inventory | Assign a block and close the inventory |
| `Esc` | Release mouse, close open UI, or return to first-person control |
| `F3` | Toggle the debug overlay |

### Starter Hotbar

1. Grass Block
2. Dirt
3. Stone
4. Cobblestone
5. Oak Planks
6. Oak Log
7. Glass
8. Torch
9. Crafting Table

### Save Files

World data is saved to:

```text
saves/world.dat
```

The game autosaves changed chunks every 20 seconds. It also saves pending world changes, the player respawn position, and world time during clean shutdown.

To reset the world:

macOS / Linux:

```bash
rm -f saves/world.dat
```

Windows PowerShell:

```powershell
Remove-Item .\saves\world.dat -ErrorAction SilentlyContinue
```

Run the game again after deleting the save file.

## Common Commands

macOS / Linux:

```bash
./gradlew run
./gradlew test
./gradlew build
./gradlew clean
./gradlew installDist
```

Windows PowerShell:

```powershell
.\gradlew.bat run
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat clean
.\gradlew.bat installDist
```

Command notes:

- `run` starts the game.
- `test` runs the JUnit test suite. Current automated tests focus on the lighting engine in `src/test/java`.
- `build` compiles the project, runs tests, and creates Gradle build artifacts.
- `clean` removes build outputs.
- `installDist` creates a runnable local application layout under `build/install/minecraftin-clone`.

If your local environment blocks Gradle daemon sockets or shared Gradle cache locations, try a workspace-local Gradle cache and no daemon:

macOS / Linux:

```bash
GRADLE_USER_HOME=.gradle-home ./gradlew --no-daemon run
```

Windows PowerShell:

```powershell
$env:GRADLE_USER_HOME = ".gradle-home"
.\gradlew.bat --no-daemon run
```

## Project Structure

```text
Minecraftin/
├─ build.gradle
│  # Gradle plugins, dependencies, Java version, JVM args, and main class.
├─ settings.gradle
│  # Gradle root project name: minecraftin-clone.
├─ gradlew / gradlew.bat
│  # Gradle wrapper launchers for macOS/Linux and Windows.
├─ gradle/wrapper/
│  # Gradle wrapper metadata and wrapper JAR.
├─ src/main/java/com/minecraftin/clone/
│  ├─ MinecraftClone.java
│  │  # JVM entry point.
│  ├─ config/GameConfig.java
│  │  # Central game constants.
│  ├─ game/Game.java
│  │  # Main loop, input flow, block interaction, autosave, and UI state.
│  ├─ engine/
│  │  # Window, input, camera, shader, mesh, and procedural texture atlas support.
│  ├─ gameplay/
│  │  # Player movement, collision, flight, jumping, and ladder behavior.
│  ├─ render/
│  │  # World renderer, HUD renderer, and atlas tile IDs.
│  ├─ util/
│  │  # Noise, queue, and float array helpers.
│  └─ world/
│     # Chunks, block types, terrain generation, lighting, raycast, water flow, save/load.
├─ src/main/resources/shaders/
│  # GLSL shaders for world, HUD, and line rendering.
├─ src/test/java/com/minecraftin/clone/world/
│  # JUnit tests for world systems such as the lighting engine.
├─ saves/
│  # Local world save data. Ignored by Git.
├─ build/
│  # Gradle build outputs. Ignored by Git.
└─ .gradle/ and .gradle-home/
   # Gradle cache folders. Ignored by Git.
```

## Troubleshooting

### `java: command not found`

Install JDK 17 or newer, reopen your terminal, and verify:

```bash
java -version
```

If you have multiple JDKs installed, make sure `JAVA_HOME` points to a compatible JDK.

### `./gradlew: Permission denied`

macOS / Linux:

```bash
chmod +x gradlew
./gradlew run
```

### Gradle Cannot Download Dependencies

The first run needs internet access. Check your network and retry. If you are behind a company or school proxy, configure Gradle proxy settings.

### Gradle Fails in a Restricted Environment

Some sandboxes block daemon sockets, local cache access, or network access. Try:

```bash
GRADLE_USER_HOME=.gradle-home ./gradlew --no-daemon build
```

If the error mentions `java.net.SocketException: Operation not permitted`, first suspect the execution environment before assuming the project code is broken.

### Gradle Shows a Deprecation Warning

Gradle may print `Deprecated Gradle features were used in this build`. This is a warning, not a failed build. If the command ends with `BUILD SUCCESSFUL`, the command completed. Use `./gradlew --warning-mode all build` when you need the detailed warning source.

### macOS GLFW First Thread Error

Example:

```text
GLFW may only be used on the main thread ... run the JVM with -XstartOnFirstThread
```

Use:

```bash
./gradlew run
```

The Gradle `run` task already includes `-XstartOnFirstThread` on macOS. If you launch the Java application manually, add that JVM argument yourself.

### Black Screen or Immediate Close

Possible causes:

- The GPU or driver does not support OpenGL 3.3.
- The game is running through a remote desktop or virtual machine without proper OpenGL support.
- The display driver is outdated.

Run locally on supported hardware and update your graphics driver.

### World Save Failed

Example:

```text
World save failed: Failed to save world to saves/world.dat
```

Check that the project folder is writable and that the `saves/` directory can be created. Avoid running the project from read-only folders or locked external drives.

### Changed the Default Seed but the World Did Not Change

Delete `saves/world.dat`. Existing saves keep their original seed.
# Minecraftin

Minecraftin is a small Java voxel sandbox inspired by classic Minecraft. It is currently a creative-mode project: you can explore procedural terrain, move in first person, fly, place and break blocks, use a creative block palette, and save the local world.

## Key Features

- Procedural chunk-based terrain with plains, forests, deserts, snow areas, mountains, and badlands.
- Generated caves, sea-level water, local water refill, finite placed-water flow, and trees.
- First-person movement with walking, sprinting, jumping, ladder climbing, and creative flight.
- Block breaking and block placement with collision checks.
- Multi-page creative inventory and 9-slot hotbar.
- Building blocks such as natural terrain blocks, wool colors, colored blocks, wood planks, stairs, slabs, fences, doors, trapdoors, ladders, torches, crafting tables, furnaces, chests, bookshelves, stone variants, quartz, nether/end themed blocks, glass, water, glowstone, and sea lanterns.
- Door and trapdoor right-click interaction.
- Empty chest UI placeholder.
- Persistent local world save/load, including the last saved player respawn position.
- OpenGL 3.3 rendering through LWJGL, with GLSL shader files in `src/main/resources/shaders`.

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
| `Left Ctrl` | Sprint or fast fly |
| `Space` | Jump |
| Double-tap `Space` | Toggle creative flight |
| `Space` while flying | Fly up |
| `Left Shift` while flying | Fly down |
| Ladder + `W` or `Space` | Climb up |
| Ladder + `S` or `Left Shift` | Climb down |
| `E` | Open or close the creative inventory |
| `Left Click` | Capture mouse, or break the targeted block while captured |
| `Right Click` | Place the selected block, or interact with doors, trapdoors, and chests |
| `1`-`9` | Select a hotbar slot |
| Mouse wheel | Change hotbar slot |
| Mouse wheel in creative inventory | Change inventory page |
| Left click in creative inventory | Assign a block to the selected hotbar slot |
| Right click in creative inventory | Assign a block and close the inventory |
| `Esc` | Release mouse, close open UI, or return to first-person control |
| `Q` | Quit the game |

### Starter Hotbar

1. Red block
2. Orange block
3. Yellow block
4. Green block
5. Blue block
6. Purple block
7. Dirt
8. Stone
9. Glass

### Save Files

World data is saved to:

```text
saves/world.dat
```

The game autosaves changed chunks every 20 seconds. It also saves pending world changes and the player respawn position during clean shutdown.

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
- `test` runs the configured JUnit test task. There are currently no test source files in the repository, but the task is configured.
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
│  │  # Noise and float array helpers.
│  └─ world/
│     # Chunks, block types, terrain generation, raycast, water flow, save/load.
├─ src/main/resources/shaders/
│  # GLSL shaders for world, HUD, and line rendering.
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

## Limitations and Notes

- The project is creative-mode only. Survival systems such as health, hunger, crafting, item stacks, and full inventory management are not implemented.
- Chests open an empty UI placeholder. They do not store items yet.
- Furnaces and crafting tables are placeable blocks, but they do not have crafting or smelting behavior yet.
- There are no mobs, entity AI, day/night cycle, or weather systems.
- The procedural texture atlas is generated in code. There are no external block texture image files to replace.
- Save compatibility depends on stable `BlockType.id()` values. Do not reorder existing block IDs when adding blocks.
- Shader files should keep `#version 330 core` as the first line.
- Generated files and local data such as `build/`, `.gradle/`, `.gradle-home/`, and `saves/` are ignored by Git.

## Development Notes

- Keep changes small and focused. Match the existing Java and Gradle style.
- Use `GameConfig.java` for simple gameplay constants before adding new configuration systems.
- Use `./gradlew test` and `./gradlew build` before sharing changes.
- When editing rendering code, keep `ChunkMesher`, `WorldRenderer`, and `world.vert` attribute layouts in sync.
- When editing save-related code, preserve the existing save format rules unless you intentionally add a migration path.
- When adding blocks, append new enum values instead of changing existing IDs.

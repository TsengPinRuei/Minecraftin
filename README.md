# Minecraftin

A beginner-friendly Java + LWJGL voxel sandbox inspired by classic Minecraft.

This project is currently focused on a solid **creative-mode foundation**:
- Procedural chunk terrain
- First-person movement and camera
- Block breaking and placing
- Save/load to local disk

It is intentionally simple to run and easy to extend.

## Who This README Is For

This guide is written for:
- New developers who have never run a Java game project before
- Users who want exact commands and troubleshooting
- Users who want architecture and config entry points

If you only want to run the game quickly, go to **Quick Start**.

## Quick Start

### macOS / Linux

```bash
cd /path/to/Minecraftin
./gradlew run
```

### Windows (PowerShell)

```powershell
cd C:\path\to\Minecraftin
.\gradlew.bat run
```

On first run, Gradle will download dependencies (can take a few minutes).

## Environment Requirements

### Minimum

- OS: macOS, Windows, or Linux
- Java: >= JDK `17` and < JDK `25`
- GPU: OpenGL 3.3 capable
- RAM: 4 GB minimum (8 GB recommended)
- Disk: at least 1 GB free (dependencies + build cache)

### Project-specific notes

- Source is compiled with Java release `17`
- Runtime has been verified on `Java 21`
- On macOS, Gradle run is already configured with `-XstartOnFirstThread`

## Install Java (If Needed)

If `java -version` fails or shows a very old version, install JDK `17+`.

### Check Java

```bash
java -version
```

You should see version `17` or `21`.

### Common setup issues

- `java: command not found`
  - Java is not installed or not on PATH.
- Wrong Java version
  - Install JDK 17+ and make sure your terminal uses that JDK.

## Build and Run Commands

### Run game

- macOS/Linux: `./gradlew run`
- Windows: `gradlew.bat run`

### Build game JAR and distributions

- macOS/Linux: `./gradlew build -x test`
- Windows: `gradlew.bat build -x test`

### Clean build output

- macOS/Linux: `./gradlew clean`
- Windows: `gradlew.bat clean`

### If your environment blocks daemon sockets

- macOS/Linux:
  ```bash
  GRADLE_USER_HOME=.gradle-home ./gradlew --no-daemon run
  ```
- Windows (PowerShell):
  ```powershell
  $env:GRADLE_USER_HOME = ".gradle-home"
  .\gradlew.bat --no-daemon run
  ```

## How to Play

- `W/A/S/D`: move
- `Mouse`: look around
- `Left Ctrl`: sprint / fast fly
- `Space` (double tap): toggle flying on/off
- `Space` (while flying): fly up
- `Left Shift` (while flying): fly down
- `Left Click`: break block
- `Right Click`: place selected block
- `1-9` or `Mouse Wheel`: switch hotbar block
- `Esc`: release mouse cursor
- `Left Click` (when cursor is free): capture mouse again
- `Q`: quit game

### Default blocks

1. Red block
2. Orange block
3. Yellow block
4. Green block
5. Blue block
6. Purple block
7. Dirt
8. Stone
9. Glass

## Save Files and World Reset

### Save location

- `saves/world.dat`

### Autosave

- Checks every 20 seconds and saves only if the world has changed
- Also saved on clean shutdown

### Reset world

If you want a fresh world:

```bash
rm -f saves/world.dat
```

(Windows PowerShell)

```powershell
Remove-Item .\saves\world.dat -ErrorAction SilentlyContinue
```

Then run the game again.

## Troubleshooting

### Error: GLFW must run on first thread (macOS)

Example message:

`GLFW may only be used on the main thread ... run the JVM with -XstartOnFirstThread`

Fix:
- Use `./gradlew run` (already configured)
- If you launch Java manually, add `-XstartOnFirstThread`

### Error: `Permission denied` when running `./gradlew`

Fix:

```bash
chmod +x gradlew
./gradlew run
```

### Error: `java: command not found`

Fix:
- Install JDK 17+
- Reopen terminal
- Verify `java -version`

### Error: black screen or instant close

Possible causes and fixes:
- GPU/OpenGL driver issue -> update graphics drivers
- Remote desktop/VM without OpenGL 3.3 -> run locally on supported hardware

### Error: world save failed

Example message:

`World save failed: Failed to save world to saves/world.dat`

Fix:
- Make sure project folder is writable
- Ensure `saves/` exists or can be created
- Avoid read-only/external locked directories

### Error: dependency download fails

Fix:
- Check internet connection
- Retry command
- If behind strict network policy, configure Gradle proxy

## Scope

- [x] Chunk terrain generation with multiple biomes
- [x] Caves, sea level, local water refill, tree generation
- [x] Creative movement + fly toggle
- [x] Collision-aware breaking/placing
- [x] Land-first / forest-preferred spawn selection
- [x] Persistent world save/load
- [ ] Survival gameplay (health, hunger, crafting, inventory)
- [ ] Entities/mobs AI
- [ ] Day/night cycle and weather

## Project Structure

```text
Minecraftin/
├─ src/
│  └─ main/
│     ├─ java/com/minecraftin/clone/
│     │  ├─ MinecraftClone.java
│     │  │  # Entry point
│     │  ├─ config/
│     │  │  └─ GameConfig.java
│     │  │     # Main config file:
│     │  │     # - window size/title
│     │  │     # - render distance and chunk settings
│     │  │     # - movement/physics values
│     │  │     # - interaction reach/cooldowns
│     │  │     # - save path and default seed
│     │  ├─ game/
│     │  │  └─ Game.java
│     │  │     # Main loop and game flow
│     │  ├─ engine/
│     │  │  # Core systems: window/input/camera/shader/mesh
│     │  ├─ world/
│     │  │  # World data: chunks/generation/raycast/save-load
│     │  ├─ gameplay/
│     │  │  # Player movement and physics behavior
│     │  └─ render/
│     │     # World renderer and HUD renderer
│     └─ resources/shaders/
│        # GLSL shader files
├─ saves/
│  # Local world save data (Git ignored)
├─ .gradle/ / .gradle-home/ / build/
│  # Build and cache outputs (Git ignored)
└─ .gitignore
   # Defines ignored local/build files
```

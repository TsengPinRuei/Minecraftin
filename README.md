# Minecraftin

A beginner-friendly Java + LWJGL voxel sandbox inspired by classic Minecraft.

This project is currently focused on a solid **creative-mode foundation**:
- Procedural chunk terrain
- First-person movement and camera
- Block breaking and placing
- Save/load to local disk

It is intentionally simple to run and easy to extend.

## 1) Who This README Is For

This guide is written for:
- New developers who have never run a Java game project before
- Users who want exact commands and troubleshooting
- Users who want architecture and config entry points

If you only want to run the game quickly, go to **2) Quick Start**.

## 2) Quick Start

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

## 3) Environment Requirements

### Minimum

- OS: macOS, Windows, or Linux
- Java: JDK 17 or newer
- GPU: OpenGL 3.3 capable
- RAM: 4 GB minimum (8 GB recommended)
- Disk: at least 1 GB free (dependencies + build cache)

### Project-specific notes

- Source is compiled with Java release `17`
- Runtime has been verified on Java 21
- On macOS, Gradle run is already configured with `-XstartOnFirstThread`

## 4) Install Java (If Needed)

If `java -version` fails or shows a very old version, install JDK 17+.

### Check Java

```bash
java -version
```

You should see version `17`, `21`, or newer.

### Common setup issues

- `java: command not found`
  - Java is not installed or not on PATH.
- Wrong Java version
  - Install JDK 17+ and make sure your terminal uses that JDK.

## 5) Build and Run Commands

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

## 6) How to Play

When the game starts:
- A world opens directly (no main menu yet)
- Mouse is initially free
- Left click once inside the game window to capture mouse and start controlling player

### Controls

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
- `Q`: quit game

### Default hotbar blocks (9)

1. Red block
2. Orange block
3. Yellow block
4. Green block
5. Blue block
6. Purple block
7. Dirt
8. Stone
9. Glass

### Fullscreen behavior

- Game starts in windowed mode.
- On macOS, use the green window button menu to enter native fullscreen.

## 7) Save Files and World Reset

### Save location

- `saves/world.dat`

### Autosave

- Every 20 seconds
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

## 8) Troubleshooting (Common Errors)

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

### Issue: player cannot move

Usually mouse is not captured.

Fix:
- Left click inside game window once
- Press `Esc` to release mouse, left click again to re-capture

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

## 9) Current Scope vs Full Minecraft

- [x] Chunk terrain generation with multiple biomes
- [x] Caves, sea level, tree generation
- [x] Creative movement + fly toggle
- [x] Collision-aware breaking/placing
- [x] Persistent world save/load
- [ ] Survival gameplay (health, hunger, crafting, inventory)
- [ ] Entities/mobs AI
- [ ] Day/night cycle and weather

## 10) Key Config File

Main config:
- `src/main/java/com/minecraftin/clone/config/GameConfig.java`

Useful values:
- Window size/title
- Render distance and chunk dimensions
- Movement/physics values
- Reach and interaction cooldowns
- Save path and default world seed

## 11) Project Structure

- `src/main/java/com/minecraftin/clone/MinecraftClone.java` (entry point)
- `src/main/java/com/minecraftin/clone/game/Game.java` (main loop)
- `src/main/java/com/minecraftin/clone/engine/*` (window/input/camera/shader/mesh)
- `src/main/java/com/minecraftin/clone/world/*` (chunks/world/gen/raycast)
- `src/main/java/com/minecraftin/clone/gameplay/*` (player physics/input behavior)
- `src/main/java/com/minecraftin/clone/render/*` (world and HUD rendering)
- `src/main/resources/shaders/*` (GLSL shaders)

## 12) FAQ

### Why does the game start in window mode?

This is intentional for compatibility and usability. You can switch to fullscreen from OS window controls.

### Is this survival mode?

Not yet. Current gameplay is creative-mode only.

## 13) Notes for Contributors

- Save files are local-only and ignored by Git (`saves/`)
- Build/cache folders are ignored via `.gitignore`
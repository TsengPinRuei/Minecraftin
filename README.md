# Minecraftin Java Clone

Traditional Chinese version: [READMEzhTW.md](READMEzhTW.md)

A Java + LWJGL voxel sandbox inspired by classic Minecraft.  
Current scope is a polished creative-mode foundation: chunked terrain generation, first-person movement, block interaction, and persistent world save/load.

## Current Scope

### Engine and Rendering

- OpenGL 3.3 core pipeline (LWJGL 3).
- GLFW window/input loop with uncapped framerate.
- Camera yaw/pitch mouse-look with perspective projection.
- Dynamic mesh uploads (VAO/VBO updates per chunk rebuild).
- Shader pipeline for world, wireframe selection, and HUD.
- Procedural texture atlas generated in code.
- Per-face directional shading and distance fog.
- HUD crosshair and 9-slot hotbar with selected-slot highlight.
- Window starts in windowed mode and is clamped to monitor size.

### World and Terrain

- Chunked voxel world (`16 x 16`, height `128`).
- Seeded deterministic terrain generation.
- Biomes: plains, forest, desert, snow, mountain, badlands.
- Sea level + water fill.
- 3D noise cave carving.
- Biome-aware surface layering and strata.
- Procedural tree placement.
- Land-first spawn selection (avoids underwater spawning).

### Gameplay

- Creative mode only (`CREATIVE_MODE_ONLY = true`).
- Double-tap `Space` toggles flying on/off.
- Flying still collides with solid blocks (no noclip).
- Flight-off movement uses gravity, jump, and grounded collision.
- Sprint modifier (`Left Ctrl`) for ground and flight.
- Voxel raycast (DDA) for precise target block and face normal.
- Block break/place cooldowns.
- Placement safety check to prevent placing blocks inside player.
- Hotbar selection via `1-9` or mouse wheel.

### Persistence

- Binary world save/load at `saves/world.dat`.
- Stores world seed and loaded chunk block data.
- Autosave every 20 seconds and on shutdown.

## Hotbar Blocks (Default 9)

1. Red block
2. Orange block
3. Yellow block
4. Green block
5. Blue block
6. Purple block
7. Dirt
8. Stone
9. Glass

## Controls

- `W/A/S/D`: Move
- `Mouse`: Look
- `Left Ctrl`: Sprint / fast fly
- `Space` (double tap): Toggle flight on/off
- `Space` (while flying): Fly up
- `Left Shift` (while flying): Fly down
- `Left Click`: Break block
- `Right Click`: Place selected block
- `1-9` / `Mouse Wheel`: Change selected block
- `Esc`: Release mouse cursor
- `Left Click` (when cursor is free): Capture cursor
- `Q`: Quit

## Build and Run

### Requirements

- JDK 17+ (project is compiled with Java release `17`; tested with Java 21 runtime)
- Gradle wrapper (`./gradlew`, included)

### Build

```bash
./gradlew build -x test
```

### Run

```bash
./gradlew run
```

If your environment blocks Gradle daemon sockets:

```bash
GRADLE_USER_HOME=.gradle-home ./gradlew --no-daemon run
```

On macOS, `-XstartOnFirstThread` is already configured in `build.gradle` for the Gradle run task.

## Project Layout

- `src/main/java/com/minecraftin/clone/MinecraftClone.java`
- `src/main/java/com/minecraftin/clone/game/Game.java`
- `src/main/java/com/minecraftin/clone/engine/*`
- `src/main/java/com/minecraftin/clone/world/*`
- `src/main/java/com/minecraftin/clone/gameplay/*`
- `src/main/java/com/minecraftin/clone/render/*`
- `src/main/resources/shaders/*`

## Tunable Config

Main config file:

- `src/main/java/com/minecraftin/clone/config/GameConfig.java`

Key values you may tune:

- Window size/title
- Render distance and chunk dimensions
- Mouse sensitivity and movement physics
- Reach distance and break/place cooldowns
- Save path and default world seed

## Known Gaps vs Full Minecraft

1. No survival systems (health, hunger, damage, crafting, inventory).
2. No entity/mob AI system.
3. No dynamic skylight/block-light propagation.
4. No day-night cycle or weather.
5. No structure generation pipeline.
6. No multiplayer networking.
7. No worker-thread meshing pipeline.
8. No automated tests yet (`test` task currently has no test sources).

## Notes

- This codebase is structured for extension rather than one-file demo shortcuts.
- Save files are local-only and intentionally ignored by Git (`saves/`).
- To enter native fullscreen on macOS, use the green window button menu.

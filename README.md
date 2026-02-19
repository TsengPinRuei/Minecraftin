# Minecraftin Java Clone

A detailed Java + LWJGL voxel sandbox inspired by classic Minecraft, with chunked procedural terrain, first-person movement, editable blocks, persistent saves, and a complete rendering/input/game loop.

## Goals

- Deliver a **closest practical Minecraft-like foundation** in pure Java desktop tooling.
- Keep architecture modular enough to extend into survival systems, entities, lighting, and multiplayer.
- Make the project runnable with standard Gradle workflows.

## Current Feature Set

### Core Engine

- OpenGL 3.3 core rendering pipeline (LWJGL 3).
- GLFW windowing/input, uncapped framerate loop.
- Camera with yaw/pitch look and perspective projection.
- Mesh abstraction for dynamic VBO/VAO updates.
- GLSL shader pipeline for world, lines, and HUD layers.

### World / Terrain

- Chunked voxel world:
  - Chunk size: `16 x 16`
  - Height: `128`
- Deterministic procedural terrain based on seed (`Noise` FBM/value-noise blend).
- Biome conditions: plains, forest, desert, snow, mountain, badlands.
- Sea level and water fill.
- Cave carving via 3D noise threshold.
- Surface layering with biome-specific topsoil (grass/sand/snow/stone).
- Procedural trees with biome-specific spawn rates.
- Chunk neighbor-aware block access.

### Rendering

- Face culling at mesh-build level (only visible block faces emitted).
- Per-face directional light tinting (top/sides/bottom intensity bias).
- Texture atlas generated in code (procedural Minecraft-like tile textures).
- Fog blended by camera distance.
- Selected block wireframe outline.
- HUD crosshair and visible hotbar bar (with selected-slot highlight).

### Player / Interaction

- Creative mode only movement:
  - Double-tap `Space` to toggle flight mode on/off.
  - Flight mode has solid-block collision (no clipping through terrain).
  - Flight-off movement uses gravity + grounded collision.
  - Faster movement while sprint key is held.
- Block raycast (voxel DDA) with face normal hit info.
- Block breaking (left mouse).
- Block placement with collision safety check (prevents placing inside player).
- Hotbar-style selection with number keys / mouse wheel.

### Persistence

- Binary world save/load to `saves/world.dat`.
- Persistent seed + chunk block data.
- Autosave every 20 seconds and on shutdown.

## Controls

- `W/A/S/D`: Move
- `Mouse`: Look
- `Left Ctrl`: Sprint/Fast fly
- `Space` (double tap): Toggle flight on/off
- `Space` (while flying): Fly up
- `Left Shift` (while flying): Fly down
- `Left Click`: Break block
- `Right Click`: Place selected block
- `1-9` / `Mouse Wheel`: Change selected block
- `Esc`: Release mouse cursor
- `Left Click` (when cursor is free): Capture cursor and resume game control
- `Q`: Quit

## Tech Stack

- Java 17+ source compatibility (build verified on Java 21 runtime)
- Gradle
- LWJGL 3 (`glfw`, `opengl`, `stb`)
- JOML (math)

## Project Layout

- `src/main/java/com/minecraftin/clone/MinecraftClone.java`
- `src/main/java/com/minecraftin/clone/game/Game.java`
- `src/main/java/com/minecraftin/clone/engine/*`
- `src/main/java/com/minecraftin/clone/world/*`
- `src/main/java/com/minecraftin/clone/gameplay/*`
- `src/main/java/com/minecraftin/clone/render/*`
- `src/main/resources/shaders/*`

## Build and Run

### 1) Build

```bash
./gradlew build -x test
```

### 2) Run

```bash
./gradlew run
```

If your environment blocks Gradle daemon sockets, use:

```bash
GRADLE_USER_HOME=.gradle-home ./gradlew --no-daemon run
```

## Tunable Gameplay Config

Adjust values in:

- `src/main/java/com/minecraftin/clone/config/GameConfig.java`

Notable parameters:

- render distance
- movement/jump/gravity values
- reach distance
- chunk/world dimensions
- save path and default seed

## What Makes This Close to Minecraft Already

- Voxel chunks with procedural generation and caves
- Real first-person block interaction loop
- Break/place mechanics with ray hit normals
- World persistence and seeded replayability
- Atlas-based block texturing and directional face lighting
- Crosshair + targeted block outline UX

## What Is Not Implemented Yet (Roadmap to Closer Parity)

1. True skylight + block light flood-fill propagation
2. Chunk mesh rebuild queue on worker threads
3. Biome system with humidity/temperature maps and decorators
4. Full block state system (slabs, stairs, doors, fluids, crops)
5. Entity system (mobs, AI, pathfinding)
6. Item entities, inventory, crafting, furnace, chests
7. Health/hunger/damage/survival loop
8. Day-night cycle + sun/moon shadows
9. Weather (rain/snow), particles, ambient audio
10. Structure generation (villages, caves revamp, strongholds)
11. Chunk serialization by region files (Anvil-like format)
12. Multiplayer server/client protocol layer

## Notes

- This codebase intentionally prioritizes an extendable architecture over one-file demo shortcuts.
- Old project files have been removed; this clone is now the primary implementation in this repository.
- Spawn point selection prefers dry land above sea level (avoids underwater spawning).
- Default hotbar blocks are: red, orange, yellow, green, blue, purple, dirt, stone, glass.
- Game starts in window mode by default; use the macOS green window button menu to enter/exit native fullscreen (separate app Space).

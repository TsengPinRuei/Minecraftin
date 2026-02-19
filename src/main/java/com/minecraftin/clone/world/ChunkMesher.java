package com.minecraftin.clone.world;

import com.minecraftin.clone.config.GameConfig;
import com.minecraftin.clone.engine.TextureAtlas;
import com.minecraftin.clone.util.FloatArrayBuilder;

public final class ChunkMesher {
    public static final int STRIDE_FLOATS = 6; // position xyz, uv, light
    private static final Face[] FACES = Face.values();

    private ChunkMesher() {
    }

    public static float[] build(Chunk chunk, World world, TextureAtlas atlas) {
        FloatArrayBuilder vertices = new FloatArrayBuilder(16384);

        int worldMinX = chunk.worldMinX();
        int worldMinZ = chunk.worldMinZ();

        for (int y = 0; y < GameConfig.CHUNK_HEIGHT; y++) {
            for (int z = 0; z < GameConfig.CHUNK_SIZE; z++) {
                for (int x = 0; x < GameConfig.CHUNK_SIZE; x++) {
                    BlockType block = chunk.get(x, y, z);
                    if (block == BlockType.AIR) {
                        continue;
                    }

                    int worldX = worldMinX + x;
                    int worldZ = worldMinZ + z;

                    for (Face face : FACES) {
                        BlockType neighbor = world.peekBlock(worldX + face.dx(), y + face.dy(), worldZ + face.dz());
                        if (!shouldRenderFace(block, neighbor)) {
                            continue;
                        }

                        int tile = block.tileForFace(face);
                        float u0 = atlas.u0(tile);
                        float v0 = atlas.v0(tile);
                        float u1 = atlas.u1(tile);
                        float v1 = atlas.v1(tile);

                        addFace(vertices, x, y, z, face, u0, v0, u1, v1, face.light());
                    }
                }
            }
        }

        return vertices.toArray();
    }

    private static boolean shouldRenderFace(BlockType current, BlockType neighbor) {
        if (neighbor == BlockType.AIR) {
            return true;
        }

        if (current == BlockType.WATER) {
            return neighbor != BlockType.WATER;
        }

        if (current.isTransparent() && neighbor == current) {
            return false;
        }

        return neighbor.isTransparent();
    }

    private static void addFace(
            FloatArrayBuilder out,
            float x,
            float y,
            float z,
            Face face,
            float u0,
            float v0,
            float u1,
            float v1,
            float light
    ) {
        switch (face) {
            case NORTH -> addQuad(out, x + 1, y, z, x, y, z, x, y + 1, z, x + 1, y + 1, z, u0, v0, u1, v1, light);
            case SOUTH -> addQuad(out, x, y, z + 1, x + 1, y, z + 1, x + 1, y + 1, z + 1, x, y + 1, z + 1, u0, v0, u1, v1, light);
            case WEST -> addQuad(out, x, y, z, x, y, z + 1, x, y + 1, z + 1, x, y + 1, z, u0, v0, u1, v1, light);
            case EAST -> addQuad(out, x + 1, y, z + 1, x + 1, y, z, x + 1, y + 1, z, x + 1, y + 1, z + 1, u0, v0, u1, v1, light);
            case UP -> addQuad(out, x, y + 1, z, x + 1, y + 1, z, x + 1, y + 1, z + 1, x, y + 1, z + 1, u0, v0, u1, v1, light);
            case DOWN -> addQuad(out, x, y, z, x, y, z + 1, x + 1, y, z + 1, x + 1, y, z, u0, v0, u1, v1, light);
        }
    }

    private static void addQuad(
            FloatArrayBuilder out,
            float ax, float ay, float az,
            float bx, float by, float bz,
            float cx, float cy, float cz,
            float dx, float dy, float dz,
            float u0, float v0, float u1, float v1,
            float light
    ) {
        putVertex(out, ax, ay, az, u0, v1, light);
        putVertex(out, bx, by, bz, u1, v1, light);
        putVertex(out, cx, cy, cz, u1, v0, light);

        putVertex(out, cx, cy, cz, u1, v0, light);
        putVertex(out, dx, dy, dz, u0, v0, light);
        putVertex(out, ax, ay, az, u0, v1, light);
    }

    private static void putVertex(FloatArrayBuilder out, float x, float y, float z, float u, float v, float light) {
        out.add(x, y, z, u, v, light);
    }
}

package com.minecraftin.clone.world;

import com.minecraftin.clone.config.GameConfig;
import com.minecraftin.clone.engine.TextureAtlas;
import com.minecraftin.clone.util.FloatArrayBuilder;

// Build chunk-local vertices from a snapshot; keep the layout aligned with WorldRenderer and
// world.vert.
public final class ChunkMesher {

    // Each vertex has eight floats: XYZ, UV, face shade times AO, sky light, and block light.
    public static final int STRIDE_FLOATS = 8;

    // Cache enum values to avoid allocating the same face array for every build.
    private static final Face[] FACES = Face.values();

    // Map corner occlusion levels to brightness; two blocked sides always use the darkest level.
    private static final float[] AO_LEVELS = { 0.55f, 0.70f, 0.85f, 1.0f };

    // Face rows follow Face.ordinal(); each row contains four XYZ corners in a/b/c/d order.
    private static final int[][] FACE_POSITIONS = {
            // NORTH
            { 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0 },
            // SOUTH
            { 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1 },
            // WEST
            { 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0 },
            // EAST
            { 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 1 },
            // UP
            { 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1 },
            // DOWN
            { 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 0 },
    };

    // Use two tangent axes per face to sample neighboring cells around each corner.
    private static final int[][] FACE_TANGENT1 = {
            { 1, 0, 0 }, { 1, 0, 0 }, { 0, 0, 1 }, { 0, 0, 1 }, { 1, 0, 0 }, { 1, 0, 0 },
    };
    private static final int[][] FACE_TANGENT2 = {
            { 0, 1, 0 }, { 0, 1, 0 }, { 0, 1, 0 }, { 0, 1, 0 }, { 0, 0, 1 }, { 0, 0, 1 },
    };

    // Corner signs along the tangent axes must match FACE_POSITIONS and each other.
    private static final int[][] FACE_SU = {
            // NORTH
            { 1, -1, -1, 1 },
            // SOUTH
            { -1, 1, 1, -1 },
            // WEST
            { -1, 1, 1, -1 },
            // EAST
            { 1, -1, -1, 1 },
            // UP
            { -1, 1, 1, -1 },
            // DOWN
            { -1, -1, 1, 1 },
    };
    private static final int[][] FACE_SV = {
            // NORTH
            { -1, -1, 1, 1 },
            // SOUTH
            { -1, -1, 1, 1 },
            // WEST
            { -1, -1, 1, 1 },
            // EAST
            { -1, -1, 1, 1 },
            // UP
            { -1, -1, 1, 1 },
            // DOWN
            { -1, 1, 1, -1 },
    };

    // Choose a quad diagonal from corner shading; reuse index arrays instead of allocating per
    // face.
    private static final int[] QUAD_ORDER_NORMAL = { 0, 1, 2, 2, 3, 0 };
    private static final int[] QUAD_ORDER_FLIPPED = { 1, 2, 3, 3, 0, 1 };

    // Separate opaque and translucent vertices so blending can follow the opaque depth pass.
    public record MeshData(float[] opaqueVertices, float[] translucentVertices) {
    }

    private ChunkMesher() {
    }

    // Cull hidden faces of full cubes; partial blocks emit all faces of their render boxes.
    public static MeshData build(ChunkSnapshot chunk) {
        FloatArrayBuilder opaqueVertices = new FloatArrayBuilder(16384);
        FloatArrayBuilder translucentVertices = new FloatArrayBuilder(4096);
        ChunkSnapshot sampler = chunk;

        // Keep scratch arrays local to this build so concurrent chunk jobs do not share mutable
        // state.
        FaceScratch scratch = new FaceScratch();

        for (int y = 0; y < GameConfig.CHUNK_HEIGHT; y++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new java.util.concurrent.CancellationException("Chunk meshing interrupted");
            }
            for (int z = 0; z < GameConfig.CHUNK_SIZE; z++) {
                for (int x = 0; x < GameConfig.CHUNK_SIZE; x++) {
                    BlockType block = chunk.get(x, y, z);

                    if (block == BlockType.AIR) {
                        continue;
                    }

                    FloatArrayBuilder targetVertices = block.isTranslucent() ? translucentVertices : opaqueVertices;

                    if (!block.isFullCube()) {
                        // Emit every face of partial-block boxes to avoid holes in thin or stepped
                        // geometry.
                        addBlockBoxes(targetVertices, sampler, block, x, y, z);
                        continue;
                    }

                    for (Face face : FACES) {
                        int neighborX = x + face.dx();
                        int neighborY = y + face.dy();
                        int neighborZ = z + face.dz();
                        // Read neighbors from the snapshot border; meshing must not generate or
                        // access mutable chunks.
                        BlockType neighbor = chunk.get(neighborX, neighborY, neighborZ);

                        if (!shouldRenderFace(block, neighbor)) {
                            continue;
                        }

                        int tile = block.tileForFace(face);
                        float u0 = TextureAtlas.u0(tile);
                        float v0 = TextureAtlas.v0(tile);
                        float u1 = TextureAtlas.u1(tile);
                        float v1 = TextureAtlas.v1(tile);

                        addFace(targetVertices, sampler, x, y, z, face, u0, v0, u1, v1, scratch);
                    }
                }
            }
        }

        return new MeshData(opaqueVertices.toArray(), translucentVertices.toArray());
    }

    private static boolean shouldRenderFace(BlockType current, BlockType neighbor) {
        if (neighbor == BlockType.AIR) {
            return true;
        }

        // Hide water-to-water faces and faces against opaque full cubes.
        if (current.isWaterBlock()) {
            return !neighbor.isWaterBlock() && (!neighbor.isFullCube() || neighbor.isTransparent());
        }

        // Keep the face beside partial blocks because their geometry may leave it exposed.
        if (!neighbor.isFullCube()) {
            return true;
        }

        // Hide the shared face between identical transparent full cubes.
        if (current.isTransparent() && neighbor == current) {
            return false;
        }

        return neighbor.isTransparent();
    }

    // Average neighboring light samples at each corner and apply ambient occlusion from nearby full
    // opaque blocks.
    private static void addFace(
            FloatArrayBuilder out,
            ChunkSnapshot sampler,
            int x,
            int y,
            int z,
            Face face,
            float u0,
            float v0,
            float u1,
            float v1,
            FaceScratch scratch) {
        int faceIndex = face.ordinal();
        int[] positions = FACE_POSITIONS[faceIndex];
        int[] t1 = FACE_TANGENT1[faceIndex];
        int[] t2 = FACE_TANGENT2[faceIndex];
        int[] suList = FACE_SU[faceIndex];
        int[] svList = FACE_SV[faceIndex];

        // Use the cell immediately outside the face as the base light sample.
        int baseX = x + face.dx();
        int baseY = y + face.dy();
        int baseZ = z + face.dz();
        float faceShade = face.light();

        float[] shade = scratch.shade;
        float[] sky = scratch.sky;
        float[] blockLight = scratch.blockLight;
        int baseSky = sampler.sky(baseX, baseY, baseZ);
        int baseBlock = sampler.block(baseX, baseY, baseZ);

        for (int i = 0; i < 4; i++) {
            int su = suList[i];
            int sv = svList[i];
            int o1x = t1[0] * su;
            int o1y = t1[1] * su;
            int o1z = t1[2] * su;
            int o2x = t2[0] * sv;
            int o2y = t2[1] * sv;
            int o2z = t2[2] * sv;

            boolean side1 = sampler.occludes(baseX + o1x, baseY + o1y, baseZ + o1z);
            boolean side2 = sampler.occludes(baseX + o2x, baseY + o2y, baseZ + o2z);
            boolean corner = sampler.occludes(baseX + o1x + o2x, baseY + o1y + o2y, baseZ + o1z + o2z);

            int ao = (side1 && side2) ? 0 : 3 - ((side1 ? 1 : 0) + (side2 ? 1 : 0) + (corner ? 1 : 0));
            shade[i] = faceShade * AO_LEVELS[ao];

            // Average four light samples per corner; substitute the base sample for full opaque
            // cells to avoid excess darkening.
            int sumSky = baseSky;
            int sumBlock = baseBlock;

            sumSky += side1 ? baseSky : sampler.sky(baseX + o1x, baseY + o1y, baseZ + o1z);
            sumBlock += side1 ? baseBlock : sampler.block(baseX + o1x, baseY + o1y, baseZ + o1z);
            sumSky += side2 ? baseSky : sampler.sky(baseX + o2x, baseY + o2y, baseZ + o2z);
            sumBlock += side2 ? baseBlock : sampler.block(baseX + o2x, baseY + o2y, baseZ + o2z);
            sumSky += corner ? baseSky : sampler.sky(baseX + o1x + o2x, baseY + o1y + o2y, baseZ + o1z + o2z);
            sumBlock += corner ? baseBlock : sampler.block(baseX + o1x + o2x, baseY + o1y + o2y, baseZ + o1z + o2z);

            sky[i] = sumSky / (4.0f * LightEngine.MAX_LIGHT);
            blockLight[i] = sumBlock / (4.0f * LightEngine.MAX_LIGHT);
        }

        // Map corners a/b/c/d to UV bottom-left/bottom-right/top-right/top-left.
        float[] px = scratch.px;
        float[] py = scratch.py;
        float[] pz = scratch.pz;
        for (int i = 0; i < 4; i++) {
            px[i] = x + positions[i * 3];
            py[i] = y + positions[i * 3 + 1];
            pz[i] = z + positions[i * 3 + 2];
        }
        float[] u = scratch.u;
        float[] v = scratch.v;
        u[0] = u0;
        u[1] = u1;
        u[2] = u1;
        u[3] = u0;
        v[0] = v1;
        v[1] = v1;
        v[2] = v0;
        v[3] = v0;

        // Choose the diagonal from corner shading to reduce directional interpolation artifacts.
        boolean flip = shade[0] + shade[2] < shade[1] + shade[3];
        int[] order = flip ? QUAD_ORDER_FLIPPED : QUAD_ORDER_NORMAL;

        for (int index : order) {
            out.add(px[index], py[index], pz[index], u[index], v[index],
                    shade[index], sky[index], blockLight[index]);
        }
    }

    // Emit all local boxes for a partial block, such as the two stair boxes or three fence boxes.
    private static void addBlockBoxes(FloatArrayBuilder out, ChunkSnapshot sampler, BlockType block, int x, int y,
            int z) {
        // Use the partial block's own cell light without corner AO; emissive blocks therefore light
        // themselves.
        float skyLight = sampler.sky(x, y, z) / (float) LightEngine.MAX_LIGHT;
        float blockLight = sampler.block(x, y, z) / (float) LightEngine.MAX_LIGHT;

        for (BlockBounds bounds : block.renderBoxes()) {
            addBox(out, block, x, y, z, bounds, skyLight, blockLight);
        }
    }

    // Place local boxes in chunk coordinates and crop UVs to their position within a unit block.
    // Keep crop orientation aligned with addBoxFace instead of stretching the entire tile onto each
    // small face.
    private static void addBox(FloatArrayBuilder out, BlockType block, float x, float y, float z,
            BlockBounds bounds, float skyLight, float blockLight) {
        float minX = x + bounds.minX();
        float minY = y + bounds.minY();
        float minZ = z + bounds.minZ();
        float maxX = x + bounds.maxX();
        float maxY = y + bounds.maxY();
        float maxZ = z + bounds.maxZ();

        // Clamp texture fractions for boxes extending above one cell, such as fence posts, to avoid
        // sampling another tile.
        float x0f = clamp01(bounds.minX());
        float y0f = clamp01(bounds.minY());
        float z0f = clamp01(bounds.minZ());
        float x1f = clamp01(bounds.maxX());
        float y1f = clamp01(bounds.maxY());
        float z1f = clamp01(bounds.maxZ());

        for (Face face : FACES) {
            int tile = block.tileForFace(face);
            float u0 = TextureAtlas.u0(tile);
            float v0 = TextureAtlas.v0(tile);
            float uSpan = TextureAtlas.u1(tile) - u0;
            float vSpan = TextureAtlas.v1(tile) - v0;

            // Map the cropped texture top to the box top and the cropped bottom to the box bottom.
            float sideV0 = v0 + (1.0f - y1f) * vSpan;
            float sideV1 = v0 + (1.0f - y0f) * vSpan;

            float subU0;
            float subU1;
            float subV0;
            float subV1;
            switch (face) {
                case NORTH -> {
                    subU0 = u0 + (1.0f - x1f) * uSpan;
                    subU1 = u0 + (1.0f - x0f) * uSpan;
                    subV0 = sideV0;
                    subV1 = sideV1;
                }
                case SOUTH -> {
                    subU0 = u0 + x0f * uSpan;
                    subU1 = u0 + x1f * uSpan;
                    subV0 = sideV0;
                    subV1 = sideV1;
                }
                case WEST -> {
                    subU0 = u0 + z0f * uSpan;
                    subU1 = u0 + z1f * uSpan;
                    subV0 = sideV0;
                    subV1 = sideV1;
                }
                case EAST -> {
                    subU0 = u0 + (1.0f - z1f) * uSpan;
                    subU1 = u0 + (1.0f - z0f) * uSpan;
                    subV0 = sideV0;
                    subV1 = sideV1;
                }
                case UP -> {
                    subU0 = u0 + x0f * uSpan;
                    subU1 = u0 + x1f * uSpan;
                    subV0 = v0 + (1.0f - z1f) * vSpan;
                    subV1 = v0 + (1.0f - z0f) * vSpan;
                }
                default -> {
                    subU0 = u0 + z0f * uSpan;
                    subU1 = u0 + z1f * uSpan;
                    subV0 = v0 + (1.0f - x1f) * vSpan;
                    subV1 = v0 + (1.0f - x0f) * vSpan;
                }
            }

            addBoxFace(out, face, minX, minY, minZ, maxX, maxY, maxZ, subU0, subV0, subU1, subV1,
                    face.light(), skyLight, blockLight);
        }
    }

    private static float clamp01(float value) {
        return Math.min(1.0f, Math.max(0.0f, value));
    }

    // Use the same vertex layout as full-cube faces, with corners taken from the render box.
    private static void addBoxFace(
            FloatArrayBuilder out,
            Face face,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            float u0,
            float v0,
            float u1,
            float v1,
            float shade,
            float skyLight,
            float blockLight) {
        switch (face) {
            case NORTH -> addQuad(out, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ,
                    u0, v0, u1, v1, shade, skyLight, blockLight);
            case SOUTH -> addQuad(out, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ,
                    u0, v0, u1, v1, shade, skyLight, blockLight);
            case WEST -> addQuad(out, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ,
                    u0, v0, u1, v1, shade, skyLight, blockLight);
            case EAST -> addQuad(out, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ,
                    u0, v0, u1, v1, shade, skyLight, blockLight);
            case UP -> addQuad(out, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ,
                    u0, v0, u1, v1, shade, skyLight, blockLight);
            case DOWN -> addQuad(out, minX, minY, minZ, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ,
                    u0, v0, u1, v1, shade, skyLight, blockLight);
        }
    }

    private static void addQuad(
            FloatArrayBuilder out,
            float ax, float ay, float az,
            float bx, float by, float bz,
            float cx, float cy, float cz,
            float dx, float dy, float dz,
            float u0, float v0, float u1, float v1,
            float shade, float skyLight, float blockLight) {
        out.add(ax, ay, az, u0, v1, shade, skyLight, blockLight);
        out.add(bx, by, bz, u1, v1, shade, skyLight, blockLight);
        out.add(cx, cy, cz, u1, v0, shade, skyLight, blockLight);

        out.add(cx, cy, cz, u1, v0, shade, skyLight, blockLight);
        out.add(dx, dy, dz, u0, v0, shade, skyLight, blockLight);
        out.add(ax, ay, az, u0, v1, shade, skyLight, blockLight);
    }

    // Reuse per-face scratch arrays within this build to avoid repeated small allocations.
    private static final class FaceScratch {
        private final float[] shade = new float[4];
        private final float[] sky = new float[4];
        private final float[] blockLight = new float[4];
        private final float[] px = new float[4];
        private final float[] py = new float[4];
        private final float[] pz = new float[4];
        private final float[] u = new float[4];
        private final float[] v = new float[4];
    }

}

// 說明：宣告此檔案所屬的套件。
package com.minecraftin.clone.world;

// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.config.GameConfig;
// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.engine.TextureAtlas;
// 說明：匯入後續會使用到的型別或函式。
import com.minecraftin.clone.util.FloatArrayBuilder;

// 說明：定義主要型別與其結構。
public final class ChunkMesher {
    // 說明：下一行程式碼負責執行目前步驟。
    public static final int STRIDE_FLOATS = 6; // position xyz, uv, light
    // 說明：設定或更新變數的值。
    private static final Face[] FACES = Face.values();

    // 說明：定義類別內部使用的方法。
    private ChunkMesher() {
    }

    // 說明：定義對外可呼叫的方法。
    public static float[] build(Chunk chunk, World world, TextureAtlas atlas) {
        // 說明：宣告並初始化變數。
        FloatArrayBuilder vertices = new FloatArrayBuilder(16384);

        // 說明：宣告並初始化變數。
        int worldMinX = chunk.worldMinX();
        // 說明：宣告並初始化變數。
        int worldMinZ = chunk.worldMinZ();

        // 說明：使用迴圈逐一處理每個元素或區間。
        for (int y = 0; y < GameConfig.CHUNK_HEIGHT; y++) {
            // 說明：使用迴圈逐一處理每個元素或區間。
            for (int z = 0; z < GameConfig.CHUNK_SIZE; z++) {
                // 說明：使用迴圈逐一處理每個元素或區間。
                for (int x = 0; x < GameConfig.CHUNK_SIZE; x++) {
                    // 說明：宣告並初始化變數。
                    BlockType block = chunk.get(x, y, z);
                    // 說明：根據條件決定是否進入此邏輯分支。
                    if (block == BlockType.AIR) {
                        // 說明：跳過本次迴圈剩餘邏輯，直接進入下一次迭代。
                        continue;
                    }

                    // 說明：宣告並初始化變數。
                    int worldX = worldMinX + x;
                    // 說明：宣告並初始化變數。
                    int worldZ = worldMinZ + z;

                    // 說明：使用迴圈逐一處理每個元素或區間。
                    for (Face face : FACES) {
                        // 說明：宣告並初始化變數。
                        BlockType neighbor = world.peekBlock(worldX + face.dx(), y + face.dy(), worldZ + face.dz());
                        // 說明：根據條件決定是否進入此邏輯分支。
                        if (!shouldRenderFace(block, neighbor)) {
                            // 說明：跳過本次迴圈剩餘邏輯，直接進入下一次迭代。
                            continue;
                        }

                        // 說明：宣告並初始化變數。
                        int tile = block.tileForFace(face);
                        // 說明：宣告並初始化變數。
                        float u0 = atlas.u0(tile);
                        // 說明：宣告並初始化變數。
                        float v0 = atlas.v0(tile);
                        // 說明：宣告並初始化變數。
                        float u1 = atlas.u1(tile);
                        // 說明：宣告並初始化變數。
                        float v1 = atlas.v1(tile);

                        // 說明：呼叫方法執行對應功能。
                        addFace(vertices, x, y, z, face, u0, v0, u1, v1, face.light());
                    }
                }
            }
        }

        // 說明：呼叫方法執行對應功能。
        return vertices.toArray();
    }

    // 說明：定義類別內部使用的方法。
    private static boolean shouldRenderFace(BlockType current, BlockType neighbor) {
        // 說明：根據條件決定是否進入此邏輯分支。
        if (neighbor == BlockType.AIR) {
            // 說明：下一行程式碼負責執行目前步驟。
            return true;
        }

        // 說明：根據條件決定是否進入此邏輯分支。
        if (current == BlockType.WATER) {
            // 說明：設定或更新變數的值。
            return neighbor != BlockType.WATER;
        }

        // 說明：根據條件決定是否進入此邏輯分支。
        if (current.isTransparent() && neighbor == current) {
            // 說明：下一行程式碼負責執行目前步驟。
            return false;
        }

        // 說明：呼叫方法執行對應功能。
        return neighbor.isTransparent();
    }

    // 說明：下一行程式碼負責執行目前步驟。
    private static void addFace(
            // 說明：下一行程式碼負責執行目前步驟。
            FloatArrayBuilder out,
            // 說明：下一行程式碼負責執行目前步驟。
            float x,
            // 說明：下一行程式碼負責執行目前步驟。
            float y,
            // 說明：下一行程式碼負責執行目前步驟。
            float z,
            // 說明：下一行程式碼負責執行目前步驟。
            Face face,
            // 說明：下一行程式碼負責執行目前步驟。
            float u0,
            // 說明：下一行程式碼負責執行目前步驟。
            float v0,
            // 說明：下一行程式碼負責執行目前步驟。
            float u1,
            // 說明：下一行程式碼負責執行目前步驟。
            float v1,
            // 說明：下一行程式碼負責執行目前步驟。
            float light
    // 說明：下一行程式碼負責執行目前步驟。
    ) {
        // 說明：依據值切換到對應的分支處理。
        switch (face) {
            // 說明：宣告 switch 的其中一個分支。
            case NORTH -> addQuad(out, x + 1, y, z, x, y, z, x, y + 1, z, x + 1, y + 1, z, u0, v0, u1, v1, light);
            // 說明：宣告 switch 的其中一個分支。
            case SOUTH -> addQuad(out, x, y, z + 1, x + 1, y, z + 1, x + 1, y + 1, z + 1, x, y + 1, z + 1, u0, v0, u1, v1, light);
            // 說明：宣告 switch 的其中一個分支。
            case WEST -> addQuad(out, x, y, z, x, y, z + 1, x, y + 1, z + 1, x, y + 1, z, u0, v0, u1, v1, light);
            // 說明：宣告 switch 的其中一個分支。
            case EAST -> addQuad(out, x + 1, y, z + 1, x + 1, y, z, x + 1, y + 1, z, x + 1, y + 1, z + 1, u0, v0, u1, v1, light);
            // 說明：宣告 switch 的其中一個分支。
            case UP -> addQuad(out, x, y + 1, z, x + 1, y + 1, z, x + 1, y + 1, z + 1, x, y + 1, z + 1, u0, v0, u1, v1, light);
            // 說明：宣告 switch 的其中一個分支。
            case DOWN -> addQuad(out, x, y, z, x, y, z + 1, x + 1, y, z + 1, x + 1, y, z, u0, v0, u1, v1, light);
        }
    }

    // 說明：下一行程式碼負責執行目前步驟。
    private static void addQuad(
            // 說明：下一行程式碼負責執行目前步驟。
            FloatArrayBuilder out,
            // 說明：下一行程式碼負責執行目前步驟。
            float ax, float ay, float az,
            // 說明：下一行程式碼負責執行目前步驟。
            float bx, float by, float bz,
            // 說明：下一行程式碼負責執行目前步驟。
            float cx, float cy, float cz,
            // 說明：下一行程式碼負責執行目前步驟。
            float dx, float dy, float dz,
            // 說明：下一行程式碼負責執行目前步驟。
            float u0, float v0, float u1, float v1,
            // 說明：下一行程式碼負責執行目前步驟。
            float light
    // 說明：下一行程式碼負責執行目前步驟。
    ) {
        // 說明：呼叫方法執行對應功能。
        putVertex(out, ax, ay, az, u0, v1, light);
        // 說明：呼叫方法執行對應功能。
        putVertex(out, bx, by, bz, u1, v1, light);
        // 說明：呼叫方法執行對應功能。
        putVertex(out, cx, cy, cz, u1, v0, light);

        // 說明：呼叫方法執行對應功能。
        putVertex(out, cx, cy, cz, u1, v0, light);
        // 說明：呼叫方法執行對應功能。
        putVertex(out, dx, dy, dz, u0, v0, light);
        // 說明：呼叫方法執行對應功能。
        putVertex(out, ax, ay, az, u0, v1, light);
    }

    // 說明：定義類別內部使用的方法。
    private static void putVertex(FloatArrayBuilder out, float x, float y, float z, float u, float v, float light) {
        // 說明：呼叫方法執行對應功能。
        out.add(x, y, z, u, v, light);
    }
}

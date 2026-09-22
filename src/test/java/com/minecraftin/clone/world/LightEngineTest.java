package com.minecraftin.clone.world;

import com.minecraftin.clone.config.GameConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

// Most cases isolate lighting above generated terrain; the depth test still searches this fixed
// seed for natural water.
final class LightEngineTest {

    @TempDir
    Path tempDir;

    private World world;

    private int x;
    private int z;
    private int airY;

    @BeforeEach
    void setUp() {
        world = new World(tempDir.resolve("world.dat"), 20260219L);
        world.initialize();

        x = 8;
        z = 8;
        airY = buildPlatform(x, z) + 2;
    }

    // Place a platform above nearby terrain with room for the lighting probes.
    private int buildPlatform(int centerX, int centerZ) {
        // Use sea level as the minimum because topSolidY ignores water.
        int areaTop = world.seaLevel();
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                areaTop = Math.max(areaTop, world.topSolidY(centerX + dx, centerZ + dz));
            }
        }

        int platformY = Math.min(areaTop + 6, GameConfig.CHUNK_HEIGHT - 12);
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                world.setBlock(centerX + dx, platformY, centerZ + dz, BlockType.STONE);
            }
        }
        return platformY;
    }

    @Test
    void openSkyHasFullSkyLight() {
        assertEquals(15, world.skyLightAt(x, airY, z), "平台上方的空氣應有滿級天空光");
        assertEquals(15, world.skyLightAt(x, airY + 4, z), "更高的空氣也應有滿級天空光");
    }

    @Test
    void platformCastsShadowBelow() {
        // The platform blocks direct sky light below it; weaker light can still propagate around
        // its edges.
        int belowY = airY - 3;
        assertTrue(world.skyLightAt(x, belowY, z) < 15, "平台正下方的天空光應被遮蔽");
    }

    @Test
    void opaqueBlockCastsShadowAndLateralLightLeaksIn() {
        int y = airY + 2;
        world.setBlock(x, y, z, BlockType.STONE);

        // Blocking direct sky light leaves level 14 from neighboring exposed air.
        assertEquals(14, world.skyLightAt(x, y - 1, z), "遮蔽下方應靠側向繞射取得 14 級光");

        // Removing the blocker should restore full-strength direct sky light.
        world.setBlock(x, y, z, BlockType.AIR);
        assertEquals(15, world.skyLightAt(x, y - 1, z), "移除遮蔽後天空光應恢復 15");
    }

    @Test
    void torchEmitsAndAttenuatesPerBlock() {
        world.setBlock(x, airY, z, BlockType.TORCH);

        assertEquals(14, world.blockLightAt(x, airY, z), "火把所在格的方塊光應為 14");
        assertEquals(13, world.blockLightAt(x + 1, airY, z), "距離 1 格應為 13");
        assertEquals(11, world.blockLightAt(x + 3, airY, z), "距離 3 格應為 11");

        world.setBlock(x, airY, z, BlockType.AIR);
        assertEquals(0, world.blockLightAt(x, airY, z), "拆掉火把後方塊光應歸零");
        assertEquals(0, world.blockLightAt(x + 3, airY, z), "周圍的方塊光也應一併移除");
    }

    @Test
    void torchLightCrossesChunkBorder() {
        // Place the source at the west edge of chunk (1, 0) and verify light reaches chunk (0, 0).
        int borderX = GameConfig.CHUNK_SIZE;
        int borderZ = 8;
        int y = buildPlatform(borderX, borderZ) + 2;

        world.setBlock(borderX, y, borderZ, BlockType.TORCH);

        assertEquals(13, world.blockLightAt(borderX - 1, y, borderZ), "火把的光應跨越 Chunk 邊界");
        assertTrue(world.blockLightAt(borderX - 3, y, borderZ) >= 11,
                "更深入鄰近 Chunk 的格子也應收到衰減後的光");
    }

    @Test
    void glowstoneIsBrighterThanTorch() {
        world.setBlock(x, airY, z, BlockType.GLOWSTONE);
        assertEquals(15, world.blockLightAt(x, airY, z), "螢石所在格的方塊光應為 15");
    }

    @Test
    void removingStrongerLightPreservesNearbyTorch() {
        world.setBlock(x, airY, z, BlockType.GLOWSTONE);
        world.setBlock(x + 1, airY, z, BlockType.TORCH);
        world.setBlock(x, airY, z, BlockType.AIR);
        assertEquals(14, world.blockLightAt(x + 1, airY, z));
        assertEquals(13, world.blockLightAt(x, airY, z));
    }

    @Test
    void removingTopLayerBlockRestoresDirectSunlight() {
        int top = GameConfig.CHUNK_HEIGHT - 1;
        world.setBlock(x, top, z, BlockType.STONE);
        world.setBlock(x, top, z, BlockType.AIR);
        assertEquals(15, world.skyLightAt(x, top, z));
        assertEquals(15, world.skyLightAt(x, top - 1, z));
    }

    @Test
    void rebuildingWaterLightingMatchesIncrementalUpdates() {
        int top = GameConfig.CHUNK_HEIGHT - 1;
        world.setBlock(x, top, z, BlockType.WATER);
        world.setBlock(x, top - 1, z, BlockType.WATER);
        int[] before = new int[4];
        for (int i = 0; i < before.length; i++) {
            before[i] = world.skyLightAt(x, top - i, z);
        }
        new LightEngine(world).onChunkLoaded(world.getChunkIfLoaded(0, 0));
        for (int i = 0; i < before.length; i++) {
            assertEquals(before[i], world.skyLightAt(x, top - i, z));
        }
    }

    @Test
    void opaqueCornerChangeInvalidatesDiagonalMesh() {
        Chunk diagonal = world.getOrCreateChunk(1, 1);
        diagonal.clearMeshDirty();
        world.setBlock(15, airY, 15, BlockType.STONE);
        assertTrue(diagonal.isMeshDirty(), "角落 AO 取樣也會影響對角 Chunk");
    }

    @Test
    void waterAttenuatesSkyLightWithDepth() {
        // Find natural water deep enough to compare surface and underwater sky light.
        for (int sx = 0; sx < 256; sx += 4) {
            for (int sz = 0; sz < 256; sz += 4) {
                int sea = world.seaLevel();
                if (world.getBlock(sx, sea, sz) != BlockType.WATER
                        || world.getBlock(sx, sea - 3, sz) != BlockType.WATER) {
                    continue;
                }

                int surface = world.skyLightAt(sx, sea, sz);
                int deeper = world.skyLightAt(sx, sea - 3, sz);
                assertTrue(surface > deeper, "越深的水天空光應越弱");
                return;
            }
        }
        fail("在搜尋範圍內找不到夠深的水域");
    }
}

package com.minecraftin.clone.world;

import com.minecraftin.clone.config.GameConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

// 光照引擎的行為測試；World 不依賴 OpenGL，可直接以 headless 方式驗證 BFS 傳播。
// 測試不依賴特定種子的地形：先在天上蓋一個石頭平台，再於平台上方驗證光照規則。
// 平台本身由 setBlock 一格格放置，因此 setUp 同時也驗證了增量光照更新不會崩潰。
final class LightEngineTest {

    @TempDir
    Path tempDir;

    private World world;

    // 平台中心與平台上方的空氣高度。
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

    // 在指定位置鋪一個 11x11 的石頭平台，高度取周圍最高地形再加安全邊距，確保上方直通天空。
    private int buildPlatform(int centerX, int centerZ) {
        // topSolidY 會跳過水面，海上區域要以海平面為下限，否則平台會蓋進水裡。
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
        // 平台正下方一格收不到垂直直射，只能靠平台外緣的側向繞射，因此一定暗於 15。
        int belowY = airY - 3;
        assertTrue(world.skyLightAt(x, belowY, z) < 15, "平台正下方的天空光應被遮蔽");
    }

    @Test
    void opaqueBlockCastsShadowAndLateralLightLeaksIn() {
        int y = airY + 2;
        world.setBlock(x, y, z, BlockType.STONE);

        // 正下方被遮住：垂直直射被擋，但四周空氣的側向繞射會帶入 14 級光。
        assertEquals(14, world.skyLightAt(x, y - 1, z), "遮蔽下方應靠側向繞射取得 14 級光");

        // 拆掉方塊後，陽光柱應恢復滿級。
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
        // 在 chunk(1,0) 的最西邊界 x=16 蓋平台，光要能流入西邊的 chunk(0,0)。
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
    void waterAttenuatesSkyLightWithDepth() {
        // 找一片夠深的水域，驗證水中天空光按深度額外衰減。
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

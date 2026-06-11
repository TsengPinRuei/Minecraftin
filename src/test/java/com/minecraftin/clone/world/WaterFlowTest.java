package com.minecraftin.clone.world;

import com.minecraftin.clone.config.GameConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

// 水源/流動水模擬的行為測試；在高空石頭平台上驗證，不依賴特定種子的地形。
final class WaterFlowTest {

    @TempDir
    Path tempDir;

    private World world;

    // 平台中心與平台面上一格的高度。
    private int x;
    private int z;
    private int floorY;

    @BeforeEach
    void setUp() {
        world = new World(tempDir.resolve("world.dat"), 20260219L);
        world.initialize();

        x = 8;
        z = 8;
        floorY = buildPlatform(x, z);
    }

    // 鋪一個 15x15 的石頭平台，邊緣加一圈擋牆避免水流出平台影響判定。
    private int buildPlatform(int centerX, int centerZ) {
        int areaTop = world.seaLevel();
        for (int dx = -9; dx <= 9; dx++) {
            for (int dz = -9; dz <= 9; dz++) {
                areaTop = Math.max(areaTop, world.topSolidY(centerX + dx, centerZ + dz));
            }
        }

        int platformY = Math.min(areaTop + 6, GameConfig.CHUNK_HEIGHT - 12);
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                world.setBlock(centerX + dx, platformY, centerZ + dz, BlockType.STONE);

                boolean edge = Math.abs(dx) == 7 || Math.abs(dz) == 7;
                if (edge) {
                    world.setBlock(centerX + dx, platformY + 1, centerZ + dz, BlockType.STONE);
                }
            }
        }
        return platformY;
    }

    // 推進若干個水流 tick（每 tick 0.25 秒）。
    private void tick(int ticks) {
        for (int i = 0; i < ticks; i++) {
            world.update(0.25f);
        }
    }

    @Test
    void placedSourceSpreadsOutward() {
        world.setBlock(x, floorY + 1, z, BlockType.WATER);
        tick(3);

        assertTrue(world.getBlock(x + 1, floorY + 1, z).isFlowingWater(), "水源旁應出現流動水");
        assertEquals(7, world.getBlock(x + 1, floorY + 1, z).waterStrength(), "緊鄰水源的流動水強度應為 7");

        tick(8);
        assertTrue(world.getBlock(x + 4, floorY + 1, z).isWaterBlock(), "水應持續向外擴散");
        assertEquals(4, world.getBlock(x + 4, floorY + 1, z).waterStrength(), "距離水源 4 格的強度應為 4");
    }

    @Test
    void blockingSourceMakesWaterRecede() {
        world.setBlock(x, floorY + 1, z, BlockType.WATER);
        tick(10);
        assertTrue(world.getBlock(x + 3, floorY + 1, z).isWaterBlock(), "前置條件：水已擴散開");

        // 用石頭蓋掉水源，失去支撐的流動水應逐步退去。
        world.setBlock(x, floorY + 1, z, BlockType.STONE);
        tick(20);

        assertFalse(world.getBlock(x + 1, floorY + 1, z).isWaterBlock(), "水源被擋住後，緊鄰的水應退去");
        assertFalse(world.getBlock(x + 3, floorY + 1, z).isWaterBlock(), "更遠處的水也應全部退去");
    }

    @Test
    void waterFlowsIntoBrokenBlockHole() {
        world.setBlock(x, floorY + 1, z, BlockType.WATER);
        tick(6);
        assertTrue(world.getBlock(x + 2, floorY + 1, z).isWaterBlock(), "前置條件：水流到了洞的上方");

        // 在水面下方挖洞，水應該流進去。
        world.setBlock(x + 2, floorY, z, BlockType.AIR);
        tick(6);

        assertTrue(world.getBlock(x + 2, floorY, z).isWaterBlock(), "破壞方塊後水應自動流入空洞");
    }

    @Test
    void twoSourcesCreateInfiniteWater() {
        // 經典無限水源：兩格水源夾一格空位，中間那格會升級成新的水源。
        world.setBlock(x - 1, floorY + 1, z, BlockType.WATER);
        world.setBlock(x + 1, floorY + 1, z, BlockType.WATER);
        tick(6);

        assertEquals(BlockType.WATER, world.getBlock(x, floorY + 1, z), "兩水源之間應生成新的水源");
    }

    @Test
    void waterfallDriesUpWhenSourceBlocked() {
        // 平台邊牆上的水源往下流出平台形成瀑布。
        int ledgeY = floorY + 4;
        world.setBlock(x, ledgeY, z, BlockType.STONE);
        world.setBlock(x, ledgeY + 1, z, BlockType.WATER);
        tick(10);

        assertTrue(world.getBlock(x + 1, floorY + 1, z).isWaterBlock()
                || world.getBlock(x + 1, ledgeY + 1, z).isWaterBlock(), "前置條件：水從高處流下");

        // 蓋掉水源後，整條瀑布應乾涸。
        world.setBlock(x, ledgeY + 1, z, BlockType.STONE);
        tick(30);

        for (int y = floorY + 1; y <= ledgeY + 1; y++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (world.getBlock(x + dx, y, z + dz).isWaterBlock()) {
                        fail("水源被擋住後，瀑布與積水應全部退去，但 (" + (x + dx) + "," + y + "," + (z + dz)
                                + ") 仍是水");
                    }
                }
            }
        }
    }

    @Test
    void oceanHoleRefillsAndHealsToSource() {
        // 找一片海面，在海面挖出一格空缺，周圍水源應回填並依無限水源規則癒合成水源。
        int sea = world.seaLevel();
        for (int sx = 0; sx < 256; sx += 4) {
            for (int sz = 0; sz < 256; sz += 4) {
                if (world.getBlock(sx, sea, sz) != BlockType.WATER
                        || world.getBlock(sx + 1, sea, sz) != BlockType.WATER
                        || world.getBlock(sx - 1, sea, sz) != BlockType.WATER
                        || world.getBlock(sx, sea - 1, sz) != BlockType.WATER) {
                    continue;
                }

                // 先放石頭擠掉水，再挖掉，模擬「在水中破壞方塊」。
                world.setBlock(sx, sea, sz, BlockType.STONE);
                world.setBlock(sx, sea, sz, BlockType.AIR);
                tick(8);

                assertTrue(world.getBlock(sx, sea, sz).isWaterBlock(), "海面的空缺應被周圍的水回填");

                tick(8);
                assertEquals(BlockType.WATER, world.getBlock(sx, sea, sz), "回填的水應依無限水源規則癒合成水源");
                return;
            }
        }
        fail("在搜尋範圍內找不到合適的海面測試點");
    }
}

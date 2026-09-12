package com.minecraftin.clone.world;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class TerrainGeneratorTest {
    @Test
    void generatedTerrainMatchesOriginalIncludingTreesAndNegativeCoordinates() {
        // 修改前取得的方塊陣列校驗值；保護既有 seed 的地形與樹冠。
        int[][] cases = {{0, 0, -233657283}, {-3, 2, -498815393}, {40, -30, -2003365409},
                {-16, -6, 469392088}, {-14, -6, 2130032968}};
        TerrainGenerator generator = new TerrainGenerator(20260219L, 62);
        for (int[] sample : cases) {
            Chunk chunk = new Chunk(sample[0], sample[1]);
            generator.generate(chunk);
            assertEquals(sample[2], Arrays.hashCode(chunk.rawBlocks()), chunk.pos().toString());
        }
    }
}

// 說明：宣告此檔案所屬的套件。
package com.minecraftin.clone.util;

// 說明：定義主要型別與其結構。
public final class Noise {
    // 說明：定義類別內部使用的方法。
    private Noise() {
    }

    // 說明：定義對外可呼叫的方法。
    public static float fbm2(float x, float z, int octaves, float lacunarity, float gain, long seed) {
        // 說明：宣告並初始化變數。
        float amplitude = 1.0f;
        // 說明：宣告並初始化變數。
        float frequency = 1.0f;
        // 說明：宣告並初始化變數。
        float sum = 0.0f;
        // 說明：宣告並初始化變數。
        float norm = 0.0f;

        // 說明：使用迴圈逐一處理每個元素或區間。
        for (int i = 0; i < octaves; i++) {
            // 說明：設定或更新變數的值。
            sum += value2(x * frequency, z * frequency, seed + i * 1013L) * amplitude;
            // 說明：設定或更新變數的值。
            norm += amplitude;
            // 說明：設定或更新變數的值。
            amplitude *= gain;
            // 說明：設定或更新變數的值。
            frequency *= lacunarity;
        }

        // 說明：宣告並初始化變數。
        return norm == 0.0f ? 0.0f : sum / norm;
    }

    // 說明：定義對外可呼叫的方法。
    public static float fbm3(float x, float y, float z, int octaves, float lacunarity, float gain, long seed) {
        // 說明：宣告並初始化變數。
        float amplitude = 1.0f;
        // 說明：宣告並初始化變數。
        float frequency = 1.0f;
        // 說明：宣告並初始化變數。
        float sum = 0.0f;
        // 說明：宣告並初始化變數。
        float norm = 0.0f;

        // 說明：使用迴圈逐一處理每個元素或區間。
        for (int i = 0; i < octaves; i++) {
            // 說明：設定或更新變數的值。
            sum += value3(x * frequency, y * frequency, z * frequency, seed + i * 8191L) * amplitude;
            // 說明：設定或更新變數的值。
            norm += amplitude;
            // 說明：設定或更新變數的值。
            amplitude *= gain;
            // 說明：設定或更新變數的值。
            frequency *= lacunarity;
        }

        // 說明：宣告並初始化變數。
        return norm == 0.0f ? 0.0f : sum / norm;
    }

    // 說明：定義對外可呼叫的方法。
    public static float value2(float x, float z, long seed) {
        // 說明：宣告並初始化變數。
        int x0 = fastFloor(x);
        // 說明：宣告並初始化變數。
        int z0 = fastFloor(z);
        // 說明：宣告並初始化變數。
        int x1 = x0 + 1;
        // 說明：宣告並初始化變數。
        int z1 = z0 + 1;

        // 說明：宣告並初始化變數。
        float tx = x - x0;
        // 說明：宣告並初始化變數。
        float tz = z - z0;

        // 說明：宣告並初始化變數。
        float v00 = hash2ToUnit(x0, z0, seed);
        // 說明：宣告並初始化變數。
        float v10 = hash2ToUnit(x1, z0, seed);
        // 說明：宣告並初始化變數。
        float v01 = hash2ToUnit(x0, z1, seed);
        // 說明：宣告並初始化變數。
        float v11 = hash2ToUnit(x1, z1, seed);

        // 說明：宣告並初始化變數。
        float sx = smooth(tx);
        // 說明：宣告並初始化變數。
        float sz = smooth(tz);

        // 說明：宣告並初始化變數。
        float nx0 = lerp(v00, v10, sx);
        // 說明：宣告並初始化變數。
        float nx1 = lerp(v01, v11, sx);
        // 說明：呼叫方法執行對應功能。
        return lerp(nx0, nx1, sz);
    }

    // 說明：定義對外可呼叫的方法。
    public static float value3(float x, float y, float z, long seed) {
        // 說明：宣告並初始化變數。
        int x0 = fastFloor(x);
        // 說明：宣告並初始化變數。
        int y0 = fastFloor(y);
        // 說明：宣告並初始化變數。
        int z0 = fastFloor(z);
        // 說明：宣告並初始化變數。
        int x1 = x0 + 1;
        // 說明：宣告並初始化變數。
        int y1 = y0 + 1;
        // 說明：宣告並初始化變數。
        int z1 = z0 + 1;

        // 說明：宣告並初始化變數。
        float tx = x - x0;
        // 說明：宣告並初始化變數。
        float ty = y - y0;
        // 說明：宣告並初始化變數。
        float tz = z - z0;

        // 說明：宣告並初始化變數。
        float sx = smooth(tx);
        // 說明：宣告並初始化變數。
        float sy = smooth(ty);
        // 說明：宣告並初始化變數。
        float sz = smooth(tz);

        // 說明：宣告並初始化變數。
        float c000 = hash3ToUnit(x0, y0, z0, seed);
        // 說明：宣告並初始化變數。
        float c100 = hash3ToUnit(x1, y0, z0, seed);
        // 說明：宣告並初始化變數。
        float c010 = hash3ToUnit(x0, y1, z0, seed);
        // 說明：宣告並初始化變數。
        float c110 = hash3ToUnit(x1, y1, z0, seed);
        // 說明：宣告並初始化變數。
        float c001 = hash3ToUnit(x0, y0, z1, seed);
        // 說明：宣告並初始化變數。
        float c101 = hash3ToUnit(x1, y0, z1, seed);
        // 說明：宣告並初始化變數。
        float c011 = hash3ToUnit(x0, y1, z1, seed);
        // 說明：宣告並初始化變數。
        float c111 = hash3ToUnit(x1, y1, z1, seed);

        // 說明：宣告並初始化變數。
        float c00 = lerp(c000, c100, sx);
        // 說明：宣告並初始化變數。
        float c10 = lerp(c010, c110, sx);
        // 說明：宣告並初始化變數。
        float c01 = lerp(c001, c101, sx);
        // 說明：宣告並初始化變數。
        float c11 = lerp(c011, c111, sx);

        // 說明：宣告並初始化變數。
        float c0 = lerp(c00, c10, sy);
        // 說明：宣告並初始化變數。
        float c1 = lerp(c01, c11, sy);
        // 說明：呼叫方法執行對應功能。
        return lerp(c0, c1, sz);
    }

    // 說明：定義對外可呼叫的方法。
    public static long hash(long x) {
        // 說明：設定或更新變數的值。
        x ^= (x >>> 30);
        // 說明：設定或更新變數的值。
        x *= 0xBF58476D1CE4E5B9L;
        // 說明：設定或更新變數的值。
        x ^= (x >>> 27);
        // 說明：設定或更新變數的值。
        x *= 0x94D049BB133111EBL;
        // 說明：設定或更新變數的值。
        x ^= (x >>> 31);
        // 說明：下一行程式碼負責執行目前步驟。
        return x;
    }

    // 說明：定義對外可呼叫的方法。
    public static int hashInt(int x, int y, int z, long seed) {
        // 說明：宣告並初始化變數。
        long h = seed;
        // 說明：設定或更新變數的值。
        h ^= 0x9E3779B97F4A7C15L * x;
        // 說明：設定或更新變數的值。
        h ^= 0xC2B2AE3D27D4EB4FL * y;
        // 說明：設定或更新變數的值。
        h ^= 0x165667B19E3779F9L * z;
        // 說明：呼叫方法執行對應功能。
        return (int) hash(h);
    }

    // 說明：定義類別內部使用的方法。
    private static float hash2ToUnit(int x, int z, long seed) {
        // 說明：宣告並初始化變數。
        long h = seed;
        // 說明：設定或更新變數的值。
        h ^= 0x9E3779B97F4A7C15L * x;
        // 說明：設定或更新變數的值。
        h ^= 0xC2B2AE3D27D4EB4FL * z;
        // 說明：呼叫方法執行對應功能。
        return toUnit(hash(h));
    }

    // 說明：定義類別內部使用的方法。
    private static float hash3ToUnit(int x, int y, int z, long seed) {
        // 說明：宣告並初始化變數。
        long h = seed;
        // 說明：設定或更新變數的值。
        h ^= 0x9E3779B97F4A7C15L * x;
        // 說明：設定或更新變數的值。
        h ^= 0xC2B2AE3D27D4EB4FL * y;
        // 說明：設定或更新變數的值。
        h ^= 0x165667B19E3779F9L * z;
        // 說明：呼叫方法執行對應功能。
        return toUnit(hash(h));
    }

    // 說明：定義類別內部使用的方法。
    private static float toUnit(long value) {
        // 說明：宣告並初始化變數。
        int bits = (int) (value & 0x7FFFFFFF);
        // 說明：宣告並初始化變數。
        float normalized = bits / (float) 0x7FFFFFFF;
        // 說明：下一行程式碼負責執行目前步驟。
        return normalized * 2.0f - 1.0f;
    }

    // 說明：定義類別內部使用的方法。
    private static int fastFloor(float v) {
        // 說明：宣告並初始化變數。
        int i = (int) v;
        // 說明：下一行程式碼負責執行目前步驟。
        return v < i ? i - 1 : i;
    }

    // 說明：定義類別內部使用的方法。
    private static float smooth(float t) {
        // 說明：呼叫方法執行對應功能。
        return t * t * (3.0f - 2.0f * t);
    }

    // 說明：定義類別內部使用的方法。
    private static float lerp(float a, float b, float t) {
        // 說明：呼叫方法執行對應功能。
        return a + (b - a) * t;
    }
}

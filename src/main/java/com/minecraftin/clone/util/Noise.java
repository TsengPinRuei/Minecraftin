package com.minecraftin.clone.util;

// Deterministic value noise for terrain and caves; results depend only on inputs and seed.
public final class Noise {

    private Noise() {
    }

    // Combine layers at different frequencies and amplitudes.
    // Lacunarity scales frequency and gain scales amplitude; normalize by total amplitude.
    public static float fbm2(float x, float z, int octaves, float lacunarity, float gain, long seed) {
        float amplitude = 1.0f;
        float frequency = 1.0f;
        float sum = 0.0f;
        float norm = 0.0f;

        for (int i = 0; i < octaves; i++) {
            sum += value2(x * frequency, z * frequency, seed + i * 1013L) * amplitude;
            norm += amplitude;
            amplitude *= gain;
            frequency *= lacunarity;
        }

        // Normalize by total amplitude so adding octaves does not increase the output range.
        return norm == 0.0f ? 0.0f : sum / norm;
    }

    // Three-dimensional layered noise for cave shapes, normalized by total amplitude.
    public static float fbm3(float x, float y, float z, int octaves, float lacunarity, float gain, long seed) {
        float amplitude = 1.0f;
        float frequency = 1.0f;
        float sum = 0.0f;
        float norm = 0.0f;

        for (int i = 0; i < octaves; i++) {
            sum += value3(x * frequency, y * frequency, z * frequency, seed + i * 8191L) * amplitude;
            norm += amplitude;
            amplitude *= gain;
            frequency *= lacunarity;
        }

        return norm == 0.0f ? 0.0f : sum / norm;
    }

    // Smoothly interpolate the four surrounding lattice values.
    public static float value2(float x, float z, long seed) {
        int x0 = fastFloor(x);
        int z0 = fastFloor(z);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        float tx = x - x0;
        float tz = z - z0;

        float v00 = hash2ToUnit(x0, z0, seed);
        float v10 = hash2ToUnit(x1, z0, seed);
        float v01 = hash2ToUnit(x0, z1, seed);
        float v11 = hash2ToUnit(x1, z1, seed);

        float sx = smooth(tx);
        float sz = smooth(tz);

        float nx0 = lerp(v00, v10, sx);
        float nx1 = lerp(v01, v11, sx);
        return lerp(nx0, nx1, sz);
    }

    // Smoothly interpolate the eight surrounding lattice values.
    public static float value3(float x, float y, float z, long seed) {
        int x0 = fastFloor(x);
        int y0 = fastFloor(y);
        int z0 = fastFloor(z);
        int x1 = x0 + 1;
        int y1 = y0 + 1;
        int z1 = z0 + 1;

        float tx = x - x0;
        float ty = y - y0;
        float tz = z - z0;

        float sx = smooth(tx);
        float sy = smooth(ty);
        float sz = smooth(tz);

        float c000 = hash3ToUnit(x0, y0, z0, seed);
        float c100 = hash3ToUnit(x1, y0, z0, seed);
        float c010 = hash3ToUnit(x0, y1, z0, seed);
        float c110 = hash3ToUnit(x1, y1, z0, seed);
        float c001 = hash3ToUnit(x0, y0, z1, seed);
        float c101 = hash3ToUnit(x1, y0, z1, seed);
        float c011 = hash3ToUnit(x0, y1, z1, seed);
        float c111 = hash3ToUnit(x1, y1, z1, seed);

        float c00 = lerp(c000, c100, sx);
        float c10 = lerp(c010, c110, sx);
        float c01 = lerp(c001, c101, sx);
        float c11 = lerp(c011, c111, sx);

        float c0 = lerp(c00, c10, sy);
        float c1 = lerp(c01, c11, sy);
        return lerp(c0, c1, sz);
    }

    // Mix bits deterministically for procedural variation; this is not a cryptographic hash.
    public static long hash(long x) {
        x ^= (x >>> 30);
        x *= 0xBF58476D1CE4E5B9L;
        x ^= (x >>> 27);
        x *= 0x94D049BB133111EBL;
        x ^= (x >>> 31);
        return x;
    }

    public static int hashInt(int x, int y, int z, long seed) {
        long h = seed;
        h ^= 0x9E3779B97F4A7C15L * x;
        h ^= 0xC2B2AE3D27D4EB4FL * y;
        h ^= 0x165667B19E3779F9L * z;
        return (int) hash(h);
    }

    // Map a 2D lattice point and seed to a deterministic value in [-1, 1].
    private static float hash2ToUnit(int x, int z, long seed) {
        long h = seed;
        h ^= 0x9E3779B97F4A7C15L * x;
        h ^= 0xC2B2AE3D27D4EB4FL * z;
        return toUnit(hash(h));
    }

    // Map a 3D lattice point and seed to a deterministic value in [-1, 1].
    private static float hash3ToUnit(int x, int y, int z, long seed) {
        long h = seed;
        h ^= 0x9E3779B97F4A7C15L * x;
        h ^= 0xC2B2AE3D27D4EB4FL * y;
        h ^= 0x165667B19E3779F9L * z;
        return toUnit(hash(h));
    }

    private static float toUnit(long value) {
        int bits = (int) (value & 0x7FFFFFFF);
        float normalized = bits / (float) 0x7FFFFFFF;
        return normalized * 2.0f - 1.0f;
    }

    // Correct truncation toward zero so negative fractional coordinates use the lower grid cell.
    private static int fastFloor(float v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    // Use a cubic curve with zero slope at lattice boundaries to smooth interpolation.
    private static float smooth(float t) {
        return t * t * (3.0f - 2.0f * t);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}

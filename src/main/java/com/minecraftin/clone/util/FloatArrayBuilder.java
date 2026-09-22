package com.minecraftin.clone.util;

import java.util.Arrays;

// Accumulate mesh data in a reusable primitive array without boxing float values.
public final class FloatArrayBuilder {

    private float[] data;

    private int size;

    public FloatArrayBuilder(int initialCapacity) {
        data = new float[Math.max(16, initialCapacity)];
    }

    public void add(float a, float b, float c) {
        ensure(size + 3);
        data[size++] = a;
        data[size++] = b;
        data[size++] = c;
    }

    // Append one world vertex: position, UV, and three lighting components.
    public void add(float a, float b, float c, float d, float e, float f, float g, float h) {
        ensure(size + 8);
        data[size++] = a;
        data[size++] = b;
        data[size++] = c;
        data[size++] = d;
        data[size++] = e;
        data[size++] = f;
        data[size++] = g;
        data[size++] = h;
    }

    // Append one HUD vertex: position, RGBA color, and UV.
    public void add(float a, float b, float c, float d, float e, float f, float g, float h, float i) {
        ensure(size + 9);
        data[size++] = a;
        data[size++] = b;
        data[size++] = c;
        data[size++] = d;
        data[size++] = e;
        data[size++] = f;
        data[size++] = g;
        data[size++] = h;
        data[size++] = i;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public float[] toArray() {
        return Arrays.copyOf(data, size);
    }

    // Reset the length while retaining capacity for the next mesh.
    public void clear() {
        size = 0;
    }

    private void ensure(int needed) {
        if (needed <= data.length) {
            return;
        }

        int newCap = data.length;

        // Double capacity to amortize growth while appending vertices.
        while (newCap < needed) {
            newCap *= 2;
        }

        data = Arrays.copyOf(data, newCap);
    }
}

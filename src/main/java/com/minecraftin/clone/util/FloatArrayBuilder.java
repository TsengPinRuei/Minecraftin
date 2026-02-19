package com.minecraftin.clone.util;

import java.util.Arrays;

public final class FloatArrayBuilder {
    private float[] data;
    private int size;

    public FloatArrayBuilder() {
        this(1024);
    }

    public FloatArrayBuilder(int initialCapacity) {
        data = new float[Math.max(16, initialCapacity)];
    }

    public void add(float value) {
        ensure(size + 1);
        data[size++] = value;
    }

    public void add(float a, float b, float c) {
        ensure(size + 3);
        data[size++] = a;
        data[size++] = b;
        data[size++] = c;
    }

    public void add(float a, float b, float c, float d, float e, float f) {
        ensure(size + 6);
        data[size++] = a;
        data[size++] = b;
        data[size++] = c;
        data[size++] = d;
        data[size++] = e;
        data[size++] = f;
    }

    public void add(float... values) {
        ensure(size + values.length);
        System.arraycopy(values, 0, data, size, values.length);
        size += values.length;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public float[] toArray() {
        return Arrays.copyOf(data, size);
    }

    public void clear() {
        size = 0;
    }

    private void ensure(int needed) {
        if (needed <= data.length) {
            return;
        }
        int newCap = data.length;
        while (newCap < needed) {
            newCap *= 2;
        }
        data = Arrays.copyOf(data, newCap);
    }
}

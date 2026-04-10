package com.minecraftin.clone.util;

import java.util.Arrays;

// 用來動態累積 float 資料，類似專門給 float 使用的可變長度陣列。
public final class FloatArrayBuilder {

    // 真正儲存資料的陣列。
    private float[] data;

    // 目前已經放入多少筆資料。
    private int size;

    // 建立物件時，使用預設初始容量。
    public FloatArrayBuilder() {
        this(1024);
    }

    // 建立物件時，可自行指定初始容量。
    public FloatArrayBuilder(int initialCapacity) {
        data = new float[Math.max(16, initialCapacity)];
    }

    // 加入一個 float 值。
    public void add(float value) {
        ensure(size + 1);
        data[size++] = value;
    }

    // 一次加入三個 float 值。
    public void add(float a, float b, float c) {
        ensure(size + 3);
        data[size++] = a;
        data[size++] = b;
        data[size++] = c;
    }

    // 一次加入六個 float 值。
    public void add(float a, float b, float c, float d, float e, float f) {
        ensure(size + 6);
        data[size++] = a;
        data[size++] = b;
        data[size++] = c;
        data[size++] = d;
        data[size++] = e;
        data[size++] = f;
    }

    // 一次加入多個 float 值。
    public void add(float... values) {
        ensure(size + values.length);
        System.arraycopy(values, 0, data, size, values.length);
        size += values.length;
    }

    // 回傳目前儲存了多少個 float。
    public int size() {
        return size;
    }

    // 檢查目前是否沒有任何資料。
    public boolean isEmpty() {
        return size == 0;
    }

    // 回傳一個剛好符合資料長度的新陣列。
    public float[] toArray() {
        return Arrays.copyOf(data, size);
    }

    // 清空目前資料，但保留原本陣列空間，方便重複使用。
    public void clear() {
        size = 0;
    }

    // 確保陣列容量足夠放入指定數量的資料。
    private void ensure(int needed) {
        if (needed <= data.length) {
            return;
        }

        int newCap = data.length;

        // 容量不足時，持續擴大為原本的兩倍，直到足夠為止。
        while (newCap < needed) {
            newCap *= 2;
        }

        data = Arrays.copyOf(data, newCap);
    }
}
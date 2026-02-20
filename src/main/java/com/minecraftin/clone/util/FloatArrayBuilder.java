// 說明：宣告此檔案所屬的套件。
package com.minecraftin.clone.util;

// 說明：匯入後續會使用到的型別或函式。
import java.util.Arrays;

// 說明：定義主要型別與其結構。
public final class FloatArrayBuilder {
    // 說明：下一行程式碼負責執行目前步驟。
    private float[] data;
    // 說明：下一行程式碼負責執行目前步驟。
    private int size;

    // 說明：定義對外可呼叫的方法。
    public FloatArrayBuilder() {
        // 說明：呼叫方法執行對應功能。
        this(1024);
    }

    // 說明：定義對外可呼叫的方法。
    public FloatArrayBuilder(int initialCapacity) {
        // 說明：設定或更新變數的值。
        data = new float[Math.max(16, initialCapacity)];
    }

    // 說明：定義對外可呼叫的方法。
    public void add(float value) {
        // 說明：呼叫方法執行對應功能。
        ensure(size + 1);
        // 說明：設定或更新變數的值。
        data[size++] = value;
    }

    // 說明：定義對外可呼叫的方法。
    public void add(float a, float b, float c) {
        // 說明：呼叫方法執行對應功能。
        ensure(size + 3);
        // 說明：設定或更新變數的值。
        data[size++] = a;
        // 說明：設定或更新變數的值。
        data[size++] = b;
        // 說明：設定或更新變數的值。
        data[size++] = c;
    }

    // 說明：定義對外可呼叫的方法。
    public void add(float a, float b, float c, float d, float e, float f) {
        // 說明：呼叫方法執行對應功能。
        ensure(size + 6);
        // 說明：設定或更新變數的值。
        data[size++] = a;
        // 說明：設定或更新變數的值。
        data[size++] = b;
        // 說明：設定或更新變數的值。
        data[size++] = c;
        // 說明：設定或更新變數的值。
        data[size++] = d;
        // 說明：設定或更新變數的值。
        data[size++] = e;
        // 說明：設定或更新變數的值。
        data[size++] = f;
    }

    // 說明：定義對外可呼叫的方法。
    public void add(float... values) {
        // 說明：呼叫方法執行對應功能。
        ensure(size + values.length);
        // 說明：呼叫方法執行對應功能。
        System.arraycopy(values, 0, data, size, values.length);
        // 說明：設定或更新變數的值。
        size += values.length;
    }

    // 說明：定義對外可呼叫的方法。
    public int size() {
        // 說明：下一行程式碼負責執行目前步驟。
        return size;
    }

    // 說明：定義對外可呼叫的方法。
    public boolean isEmpty() {
        // 說明：宣告並初始化變數。
        return size == 0;
    }

    // 說明：定義對外可呼叫的方法。
    public float[] toArray() {
        // 說明：呼叫方法執行對應功能。
        return Arrays.copyOf(data, size);
    }

    // 說明：定義對外可呼叫的方法。
    public void clear() {
        // 說明：設定或更新變數的值。
        size = 0;
    }

    // 說明：定義類別內部使用的方法。
    private void ensure(int needed) {
        // 說明：根據條件決定是否進入此邏輯分支。
        if (needed <= data.length) {
            // 說明：下一行程式碼負責執行目前步驟。
            return;
        }
        // 說明：宣告並初始化變數。
        int newCap = data.length;
        // 說明：在條件成立時重複執行此區塊。
        while (newCap < needed) {
            // 說明：設定或更新變數的值。
            newCap *= 2;
        }
        // 說明：設定或更新變數的值。
        data = Arrays.copyOf(data, newCap);
    }
}

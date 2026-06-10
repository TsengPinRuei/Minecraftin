package com.minecraftin.clone.util;

// 簡單的 long 環狀佇列，給光照 BFS 使用，避免 ArrayDeque<Long> 的裝箱成本。
public final class LongQueue {

    private long[] data;
    private int head;
    private int tail;
    private int size;

    public LongQueue() {
        this(1024);
    }

    public LongQueue(int initialCapacity) {
        data = new long[Math.max(16, initialCapacity)];
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void add(long value) {
        if (size == data.length) {
            grow();
        }
        data[tail] = value;
        tail = (tail + 1) % data.length;
        size++;
    }

    public long poll() {
        long value = data[head];
        head = (head + 1) % data.length;
        size--;
        return value;
    }

    public void clear() {
        head = 0;
        tail = 0;
        size = 0;
    }

    private void grow() {
        long[] next = new long[data.length * 2];
        for (int i = 0; i < size; i++) {
            next[i] = data[(head + i) % data.length];
        }
        data = next;
        head = 0;
        tail = size;
    }
}

package com.minecraftin.clone.util;

import java.util.NoSuchElementException;

// 簡單的 long 環狀佇列，給光照 BFS 使用，避免 ArrayDeque<Long> 的裝箱成本。
public final class LongQueue {

    // head 指向下一個 poll 的位置，tail 指向下一個 add 寫入的位置；兩者都以環狀方式前進。
    private long[] data;
    private int head;
    private int tail;
    private int size;

    public LongQueue(int initialCapacity) {
        data = new long[Math.max(16, initialCapacity)];
    }

    public boolean isEmpty() {
        return size == 0;
    }

    // 加入新值；容量不足時會先擴容，確保 tail 永遠指向可寫入的位置。
    public void add(long value) {
        if (size == data.length) {
            grow();
        }
        data[tail] = value;
        tail++;
        if (tail == data.length) {
            tail = 0;
        }
        size++;
    }

    // 呼叫端必須先用 isEmpty() 確認有資料；LightEngine 的 BFS 迴圈都遵守這個約定。
    public long poll() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        long value = data[head];
        head++;
        if (head == data.length) {
            head = 0;
        }
        size--;
        return value;
    }

    public void clear() {
        head = 0;
        tail = 0;
        size = 0;
    }

    // 擴容時從 head 開始攤平成連續陣列，保留 FIFO 順序並重設 head/tail。
    private void grow() {
        long[] next = new long[data.length * 2];
        int first = Math.min(size, data.length - head);
        System.arraycopy(data, head, next, 0, first);
        System.arraycopy(data, 0, next, first, size - first);
        data = next;
        head = 0;
        tail = size;
    }
}

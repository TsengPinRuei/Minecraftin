package com.minecraftin.clone.util;

import java.util.NoSuchElementException;

// Primitive ring queue for lighting propagation, avoiding boxed Long values.
public final class LongQueue {

    private long[] data;
    // head is the next read position; tail is the next write position, with both wrapping at
    // capacity.
    private int head;
    private int tail;
    private int size;

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
        tail++;
        if (tail == data.length) {
            tail = 0;
        }
        size++;
    }

    // Removing from an empty queue throws NoSuchElementException.
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

    // Flatten the wrapped contents in FIFO order before resetting head and tail.
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

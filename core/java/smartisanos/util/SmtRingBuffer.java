// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package smartisanos.util;

/**
 * Fixed-capacity ring buffer that drops the oldest element when full. Reconstructed from the
 * PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class SmtRingBuffer<T> {
    private static final int DEFAULT_BUFFER_SIZE = 128;

    private Object[] elements;
    private int start;
    private int end;
    private boolean full;
    private int maxSize;

    public SmtRingBuffer() {
        this(DEFAULT_BUFFER_SIZE);
    }

    public SmtRingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Illegal Capacity: " + capacity);
        }
        elements = new Object[capacity];
        maxSize = elements.length;
    }

    public int size() {
        int size;
        if (end < start) {
            size = maxSize - start + end;
        } else if (end == start) {
            size = full ? maxSize : 0;
        } else {
            size = end - start;
        }
        return size;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean isFull() {
        return size() == maxSize;
    }

    public boolean add(T item) {
        if (isFull()) {
            removeFront();
        }
        elements[end++] = item;
        if (end >= maxSize) {
            end = 0;
        }
        if (end == start) {
            full = true;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public T removeFront() {
        if (isEmpty()) {
            throw new RuntimeException("The buffer is already empty");
        }
        T element = (T) elements[start];
        elements[start] = null;
        if (++start >= maxSize) {
            start = 0;
        }
        full = false;
        return element;
    }

    public boolean removeFront(int count) {
        if (count > size()) {
            throw new IndexOutOfBoundsException("The buffer does not has count:" + count
                    + ", size=" + size());
        }
        for (int i = 0; i < count; i++) {
            int index = (start + i) % maxSize;
            elements[index] = null;
        }
        start = (start + count) % maxSize;
        full = false;
        return true;
    }

    @SuppressWarnings("unchecked")
    public T get(int i) {
        if (i >= size()) {
            throw new IndexOutOfBoundsException("index=" + i + ", size=" + size());
        }
        return (T) elements[(start + i) % maxSize];
    }

    public T getLast() {
        final int N = size();
        if (N > 0) {
            return get(N - 1);
        }
        return null;
    }

    public int capacity() {
        return maxSize;
    }
}

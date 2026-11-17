package com.example.vitality.ui.activity;

final class StepSession {
    final long startedAt;
    private final long startedAtNanos;
    private final boolean cumulative;
    private long lastTimestamp = Long.MIN_VALUE;
    private long lastCounter = -1;
    private long steps;

    StepSession(long startedAt, long startedAtNanos, boolean cumulative) {
        this.startedAt = startedAt;
        this.startedAtNanos = startedAtNanos;
        this.cumulative = cumulative;
    }

    boolean record(long timestamp, float value) {
        if (timestamp <= lastTimestamp) return false;
        if (!cumulative) {
            if (timestamp < startedAtNanos || value != 1f) return false;
            lastTimestamp = timestamp;
            steps++;
            return true;
        }

        if (!Float.isFinite(value) || value < 0) return false;
        long counter = (long) value;
        boolean firstSample = !isReady();
        // Android có thể gửi giá trị đã đọc trước đó làm mốc ngay khi bật cảm biến.
        long added = firstSample || timestamp < startedAtNanos ? 0 : Math.max(0, counter - lastCounter);
        lastTimestamp = timestamp;
        lastCounter = counter;
        steps += added;
        return firstSample || added > 0;
    }

    boolean isReady() {
        return !cumulative || lastCounter >= 0;
    }

    long steps() {
        return steps;
    }
}

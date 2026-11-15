package com.example.vitality.data.export;

import com.example.vitality.utils.Formats;

public final class HealthDataFilter {
    public enum Type {
        ALL("Tất cả dữ liệu", null),
        BODY("Số đo cơ thể", null),
        ACTIVITY("Hoạt động", "activity"),
        NUTRITION("Dinh dưỡng", "nutrition"),
        WATER("Nước uống", "water"),
        SLEEP("Giấc ngủ", "sleep"),
        CYCLE("Chu kỳ sinh hoạt", null),
        SYMPTOM("Triệu chứng", null),
        PERIOD("Chu kỳ kinh nguyệt", null);

        public final String label;
        final String journalKind;

        Type(String label, String journalKind) {
            this.label = label;
            this.journalKind = journalKind;
        }
    }

    public final Type type;
    public final long from;
    public final long through;

    public HealthDataFilter(Type type, long from, long through) {
        this.type = type;
        this.from = from;
        this.through = through;
    }

    public boolean isValid() { return from <= through; }
    long endExclusive() { return Formats.addDays(through, 1); }
    boolean includes(Type source) { return type == Type.ALL || type == source; }
}

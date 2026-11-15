package com.example.vitality.data.export;

/** A metric displayed in the preview and the original CSV export. */
public final class HealthDataRecord {
    public final String category;
    public final long id;
    public final String metric;
    public final String value;
    public final String unit;
    public final long time;
    public final String note;

    HealthDataRecord(HealthDataFilter.Type category, long id, String metric,
                     Object value, String unit, long time, String note) {
        this(category.label, id, metric, String.valueOf(value), unit, time, note);
    }

    HealthDataRecord(String category, long id, String metric, String value, String unit,
                     long time, String note) {
        this.category = category;
        this.id = id;
        this.metric = metric == null ? "" : metric;
        this.value = value == null ? "" : value;
        this.unit = unit == null ? "" : unit;
        this.time = time;
        this.note = note == null ? "" : note;
    }
}

package com.example.vitality.data.export;

import com.example.vitality.data.database.VitalityDatabase;
import com.example.vitality.data.entity.BodyMeasurement;
import com.example.vitality.data.entity.HabitOccurrence;
import com.example.vitality.data.entity.JournalEntry;
import com.example.vitality.data.entity.PeriodEntry;
import com.example.vitality.data.entity.Symptom;
import com.example.vitality.data.export.HealthDataFilter.Type;
import com.example.vitality.utils.Formats;
import com.example.vitality.utils.HealthMath;
import com.example.vitality.utils.PeriodLogic;
import com.example.vitality.utils.SymptomLogic;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class HealthDataReader {
    private final VitalityDatabase db;

    public HealthDataReader(VitalityDatabase db) {
        this.db = db;
    }

    public HealthDataReport read(HealthDataFilter filter) {
        if (!filter.isValid()) throw new IllegalArgumentException("Khoảng thời gian không hợp lệ");
        List<HealthDataRecord> records = new ArrayList<>();
        Map<HealthDataTable, List<List<Object>>> tables = new EnumMap<>(HealthDataTable.class);
        for (HealthDataTable table : HealthDataTable.values()) {
            if (filter.includes(table.type)) tables.put(table, new ArrayList<>());
        }
        long from = filter.from;
        long until = filter.endExclusive();
        db.runInTransaction(() -> {
            if (filter.includes(Type.BODY)) {
                for (BodyMeasurement m : db.measurements().between(from, until)) {
                    addMeasurement(records, m);
                    addRow(tables, HealthDataTable.BODY, m.measuredAt, m.measuredAt, m.weightKg, m.heightCm,
                            m.waistCm, m.chestCm, m.hipCm, m.upperArmCm, m.thighCm, m.calfCm, m.note);
                }
            }
            if (filter.type == Type.ALL || filter.type.journalKind != null) {
                List<JournalEntry> journal = filter.type == Type.ACTIVITY
                        ? db.wellness().activityBetween(from, until)
                        : db.wellness().journalBetween(filter.type.journalKind, from, until);
                for (JournalEntry entry : journal) {
                    addJournal(records, tables, entry);
                }
            }
            if (filter.includes(Type.CYCLE)) {
                for (HabitOccurrence occurrence : db.wellness().occurrencesBetween(from, until)) {
                    String status = habitStatus(occurrence);
                    records.add(new HealthDataRecord(Type.CYCLE, occurrence.id, "Lần sinh hoạt",
                            status, "", occurrence.dueAt, occurrence.title));
                    addRow(tables, HealthDataTable.CYCLE, occurrence.dueAt, occurrence.dueAt,
                            occurrence.title, status);
                }
            }
            if (filter.includes(Type.SYMPTOM)) {
                for (Symptom symptom : db.symptoms().between(from, until)) {
                    String severity = SymptomLogic.severityLabel(symptom.severity);
                    records.add(new HealthDataRecord(Type.SYMPTOM, symptom.id, symptom.name,
                            severity, "", symptom.occurredAt, symptom.note));
                    addRow(tables, HealthDataTable.SYMPTOM, symptom.occurredAt, symptom.occurredAt,
                            symptom.name, severity, symptom.note);
                }
            }
            if (filter.includes(Type.PERIOD)) {
                for (PeriodEntry period : db.periods().between(from, until)) {
                    records.add(new HealthDataRecord(Type.PERIOD, period.id, "Kỳ kinh",
                            Formats.day(period.startDay) + " – " + Formats.day(period.endDay), "",
                            period.startDay, join(period.symptoms, period.note)));
                    int days = PeriodLogic.calendarDays(period.startDay, period.endDay) + 1;
                    addRow(tables, HealthDataTable.PERIOD, period.startDay, period.endDay,
                            days, period.symptoms, period.note);
                }
            }
        });
        records.sort(Comparator.comparingLong((HealthDataRecord row) -> row.time).reversed()
                .thenComparing(row -> row.category)
                .thenComparingLong(row -> row.id)
                .thenComparing(row -> row.metric));
        return new HealthDataReport(records, tables);
    }

    private static void addRow(Map<HealthDataTable, List<List<Object>>> tables,
                               HealthDataTable table, Object... values) {
        tables.get(table).add(Arrays.asList(values));
    }

    private static void addMeasurement(List<HealthDataRecord> rows, BodyMeasurement m) {
        measurement(rows, m, "Chiều cao", m.heightCm, "cm");
        measurement(rows, m, "Cân nặng", m.weightKg, "kg");
        measurement(rows, m, "Vòng ngực", m.chestCm, "cm");
        measurement(rows, m, "Vòng eo", m.waistCm, "cm");
        measurement(rows, m, "Vòng hông", m.hipCm, "cm");
        measurement(rows, m, "Vòng tay", m.upperArmCm, "cm");
        measurement(rows, m, "Vòng đùi", m.thighCm, "cm");
        measurement(rows, m, "Vòng bắp chân", m.calfCm, "cm");
    }

    private static void measurement(List<HealthDataRecord> rows, BodyMeasurement m,
                                    String name, Double value, String unit) {
        if (value != null) rows.add(new HealthDataRecord(Type.BODY, m.id, name, value, unit, m.measuredAt, m.note));
    }

    private static void addJournal(List<HealthDataRecord> rows, Map<HealthDataTable, List<List<Object>>> tables,
                                   JournalEntry entry) {
        if (entry.kind == null) return;
        switch (entry.kind) {
            case "water":
                journal(rows, Type.WATER, entry, "Lượng nước", entry.amount, "ml", entry.startedAt, entry.note);
                addRow(tables, HealthDataTable.WATER, entry.startedAt, entry.startedAt, entry.amount, entry.note);
                break;
            case "activity":
                String activity = entry.nameOr("Hoạt động");
                String activityNote = join(activity, entry.note);
                journal(rows, Type.ACTIVITY, entry, "Thời lượng", entry.amount, "phút", entry.startedAt, activityNote);
                if (entry.distanceKm != null) journal(rows, Type.ACTIVITY, entry,
                        "Quãng đường", entry.distanceKm, "km", entry.startedAt, activityNote);
                if (entry.calories != null) journal(rows, Type.ACTIVITY, entry,
                        "Calo tiêu thụ", entry.calories, "kcal", entry.startedAt, activityNote);
                addRow(tables, HealthDataTable.ACTIVITY, entry.startedAt, entry.startedAt,
                        activity, entry.amount, entry.distanceKm, entry.calories, entry.note, null);
                break;
            case JournalEntry.KIND_STEPS:
                long steps = Math.round(entry.amount);
                journal(rows, Type.ACTIVITY, entry, "Số bước", steps, "bước", entry.startedAt, entry.note);
                addRow(tables, HealthDataTable.ACTIVITY, entry.startedAt, entry.startedAt,
                        entry.nameOr("Bước chân"), null, null, null, entry.note, steps);
                break;
            case "nutrition":
                String meal = entry.nameOr("Bữa ăn");
                String mealNote = join(join(entry.category, entry.portion), entry.note);
                journal(rows, Type.NUTRITION, entry, "Món ăn", meal, "", entry.startedAt, mealNote);
                if (entry.calories != null) journal(rows, Type.NUTRITION, entry,
                        "Calo nạp vào", entry.calories, "kcal", entry.startedAt, join(meal, mealNote));
                addRow(tables, HealthDataTable.NUTRITION, entry.startedAt, entry.startedAt,
                        entry.category, meal, entry.portion, entry.calories, entry.note);
                break;
            case "sleep":
                if (entry.endedAt == null) break;
                String span = join(Formats.date(entry.startedAt) + " – " + Formats.date(entry.endedAt), entry.note);
                double slept = HealthMath.sleepMinutesOrZero(entry.startedAt, entry.endedAt, entry.awakeMinutes);
                if (slept > 0) journal(rows, Type.SLEEP, entry, "Thời lượng ngủ", slept, "phút", entry.endedAt, span);
                journal(rows, Type.SLEEP, entry, "Thức giữa giấc", entry.awakeMinutes, "phút", entry.endedAt, span);
                journal(rows, Type.SLEEP, entry, "Chất lượng tự đánh giá", entry.quality, "/5", entry.endedAt, span);
                addRow(tables, HealthDataTable.SLEEP, entry.startedAt, entry.endedAt,
                        entry.awakeMinutes, slept, entry.quality, entry.note);
                break;
            default:
                break;
        }
    }

    private static void journal(List<HealthDataRecord> rows, Type type, JournalEntry entry,
                                String metric, Object value, String unit, long time, String note) {
        rows.add(new HealthDataRecord(type, entry.id, metric, value, unit, time, note));
    }

    private static String join(String first, String second) {
        if (first == null || first.trim().isEmpty()) return second;
        if (second == null || second.trim().isEmpty() || first.equals(second)) return first;
        return first + " • " + second;
    }

    private static String habitStatus(HabitOccurrence occurrence) {
        if ("done".equals(occurrence.status)) return "Hoàn thành";
        if ("skipped".equals(occurrence.status)) return "Bỏ qua";
        if ("missed".equals(occurrence.status)) return "Bỏ lỡ";
        return "Chưa hoàn thành";
    }
}

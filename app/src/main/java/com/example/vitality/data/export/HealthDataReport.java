package com.example.vitality.data.export;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Both export formats use this snapshot of the records matching a single preview. */
public final class HealthDataReport {
    public static final HealthDataReport EMPTY = new HealthDataReport(
            Collections.emptyList(), Collections.emptyMap());

    public final List<HealthDataRecord> preview;
    final Map<HealthDataTable, List<List<Object>>> tables;

    HealthDataReport(List<HealthDataRecord> preview, Map<HealthDataTable, List<List<Object>>> tables) {
        this.preview = Collections.unmodifiableList(new ArrayList<>(preview));
        Map<HealthDataTable, List<List<Object>>> sections = new EnumMap<>(HealthDataTable.class);
        tables.forEach((table, rows) -> sections.put(table, Collections.unmodifiableList(new ArrayList<>(rows))));
        this.tables = Collections.unmodifiableMap(sections);
    }

    public boolean canExport(HealthDataExportFormat format) {
        return !preview.isEmpty() && (format == HealthDataExportFormat.CSV || !tables.isEmpty());
    }

    int tableRecordCount() {
        int count = 0;
        for (List<List<Object>> records : tables.values()) count += records.size();
        return count;
    }
}

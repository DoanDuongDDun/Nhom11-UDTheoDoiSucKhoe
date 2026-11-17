package com.example.vitality.data.export;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import com.example.vitality.utils.CsvExporter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class HealthDataDocumentStore {
    private final Context app;
    private final ContentResolver resolver;

    public HealthDataDocumentStore(Context context) {
        app = context.getApplicationContext();
        resolver = app.getContentResolver();
    }

    /** Keeps a preview through the system picker, including if Android recreates the app. */
    public String saveDraft(HealthDataReport report) throws IOException {
        File directory = directory();
        if (!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory()) {
            throw new IOException("Could not create the export draft directory");
        }
        File draft = File.createTempFile("preview-", ".json", directory);
        try (JsonWriter json = new JsonWriter(new OutputStreamWriter(
                new BufferedOutputStream(new FileOutputStream(draft)), StandardCharsets.UTF_8))) {
            json.beginObject();
            json.name("preview").beginArray();
            for (HealthDataRecord row : report.preview) {
                json.beginArray();
                json.value(row.category).value(row.id).value(row.metric).value(row.value)
                        .value(row.unit).value(row.time).value(row.note);
                json.endArray();
            }
            json.endArray();
            json.name("sections").beginObject();
            for (Map.Entry<HealthDataTable, List<List<Object>>> section : report.tables.entrySet()) {
                json.name(section.getKey().name()).beginArray();
                for (List<Object> row : section.getValue()) {
                    json.beginArray();
                    for (Object cell : row) writeDraftCell(json, cell);
                    json.endArray();
                }
                json.endArray();
            }
            json.endObject();
            json.endObject();
        } catch (IOException | RuntimeException error) {
            draft.delete();
            throw error;
        }
        return draft.getName();
    }

    private static void writeDraftCell(JsonWriter json, Object cell) throws IOException {
        if (cell instanceof Number) {
            Number value = (Number) cell;
            if (Double.isFinite(value.doubleValue())) json.value(value);
            else json.nullValue();
        } else {
            json.value((String) cell);
        }
    }

    public ExportResult write(Uri destination, HealthDataExportFormat format, HealthDataReport report)
            throws IOException {
        if (!report.canExport(format)) throw new IOException("No rows in export preview");
        OutputStream output = resolver.openOutputStream(destination, "wt");
        if (output == null) throw new IOException("The selected document could not be opened");
        if (format == HealthDataExportFormat.EXCEL) {
            ExcelHealthDataWriter.write(new BufferedOutputStream(output), report);
            return new ExportResult(displayName(destination), report.tableRecordCount());
        }
        try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(output), StandardCharsets.UTF_8)) {
            CsvExporter.writeHealthData(writer, report.preview);
        }
        return new ExportResult(displayName(destination), report.preview.size());
    }

    public boolean deleteIncomplete(Uri destination) {
        try {
            return DocumentsContract.deleteDocument(resolver, destination);
        } catch (Exception rejected) {
            return false;
        }
    }

    public void deleteDraft(String name) {
        if (isDraftName(name)) new File(directory(), name).delete();
    }

    public HealthDataReport readDraft(String name) throws IOException {
        if (!isDraftName(name)) throw new IOException("No saved export preview");
        List<HealthDataRecord> rows = new ArrayList<>();
        Map<HealthDataTable, List<List<Object>>> sections = new EnumMap<>(HealthDataTable.class);
        try (JsonReader json = new JsonReader(new InputStreamReader(
                new BufferedInputStream(new FileInputStream(new File(directory(), name))), StandardCharsets.UTF_8))) {
            if (json.peek() == JsonToken.BEGIN_ARRAY) {
                readPreview(json, rows);
            } else {
                json.beginObject();
                while (json.hasNext()) {
                    switch (json.nextName()) {
                        case "preview": readPreview(json, rows); break;
                        case "sections": readSections(json, sections); break;
                        default: json.skipValue();
                    }
                }
                json.endObject();
            }
        }
        return new HealthDataReport(rows, sections);
    }

    private static void readPreview(JsonReader json, List<HealthDataRecord> rows) throws IOException {
        json.beginArray();
        while (json.hasNext()) {
            json.beginArray();
            rows.add(new HealthDataRecord(json.nextString(), json.nextLong(), json.nextString(),
                    json.nextString(), json.nextString(), json.nextLong(), json.nextString()));
            json.endArray();
        }
        json.endArray();
    }

    private static void readSections(JsonReader json, Map<HealthDataTable, List<List<Object>>> sections)
            throws IOException {
        json.beginObject();
        while (json.hasNext()) {
            HealthDataTable table = HealthDataTable.valueOf(json.nextName());
            List<List<Object>> rows = new ArrayList<>();
            json.beginArray();
            while (json.hasNext()) {
                List<Object> row = new ArrayList<>(table.columns.length);
                json.beginArray();
                for (int i = 0; i < table.columns.length; i++) {
                    // Drafts from before step tracking have no value for the new last column.
                    if (table == HealthDataTable.ACTIVITY && i == table.columns.length - 1 && !json.hasNext()) {
                        row.add(null);
                    } else {
                        row.add(readDraftCell(json, table.columns[i].cellType));
                    }
                }
                json.endArray();
                rows.add(row);
            }
            json.endArray();
            sections.put(table, rows);
        }
        json.endObject();
    }

    private static Object readDraftCell(JsonReader json, HealthDataTable.CellType type) throws IOException {
        if (json.peek() == JsonToken.NULL) {
            json.nextNull();
            return null;
        }
        switch (type) {
            case TEXT: return json.nextString();
            case NUMBER: return json.nextDouble();
            default: return json.nextLong();
        }
    }

    private File directory() {
        return new File(app.getNoBackupFilesDir(), "health-export-drafts");
    }

    private static boolean isDraftName(String name) {
        return name != null && name.matches("preview-[a-zA-Z0-9-]+\\.json");
    }

    private String displayName(Uri destination) {
        if ("file".equals(destination.getScheme()) && destination.getPath() != null) {
            return new File(destination.getPath()).getName();
        }
        try (Cursor cursor = resolver.query(destination, new String[]{OpenableColumns.DISPLAY_NAME},
                null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int name = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (name >= 0) {
                    String value = cursor.getString(name);
                    if (value != null && !value.trim().isEmpty()) return value;
                }
            }
        } catch (RuntimeException unavailable) {
            // Some document providers omit metadata after writing.
        }
        return null;
    }

    public static final class ExportResult {
        public final String name;
        public final int rows;

        private ExportResult(String name, int rows) {
            this.name = name;
            this.rows = rows;
        }
    }
}

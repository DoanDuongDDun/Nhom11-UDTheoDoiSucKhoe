package com.example.vitality.data.export;

public enum HealthDataExportFormat {
    EXCEL("Excel", "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    CSV("CSV", "csv", "text/csv");

    public final String label;
    public final String fileName;
    public final String mimeType;

    HealthDataExportFormat(String label, String extension, String mimeType) {
        this.label = label;
        this.fileName = "Vitality-du-lieu-suc-khoe." + extension;
        this.mimeType = mimeType;
    }
}

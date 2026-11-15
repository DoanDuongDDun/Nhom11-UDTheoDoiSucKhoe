package com.example.vitality.viewmodel;

import android.app.Application;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import com.example.vitality.data.database.VitalityDatabase;
import com.example.vitality.data.export.HealthDataDocumentStore;
import com.example.vitality.data.export.HealthDataExportFormat;
import com.example.vitality.data.export.HealthDataFilter;
import com.example.vitality.data.export.HealthDataFilter.Type;
import com.example.vitality.data.export.HealthDataReader;
import com.example.vitality.data.export.HealthDataRecord;
import com.example.vitality.data.export.HealthDataReport;
import com.example.vitality.data.repository.WellnessRepository;
import com.example.vitality.utils.Formats;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class HealthDataExportViewModel extends AndroidViewModel {
    private static final String EMPTY_PROMPT = "Chọn Xem dữ liệu để áp dụng bộ lọc.";
    private static final String DRAFT = "csvDraft";
    private static final String PICKER_OPEN = "csvPickerOpen";
    private static final String WRITING = "csvWriting";
    private static final String FORMAT = "exportFormat";
    private static final ExecutorService FILES = Executors.newSingleThreadExecutor();

    private final SavedStateHandle saved;
    private final HealthDataDocumentStore documents;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final MutableLiveData<Screen> screen = new MutableLiveData<>();
    private final MutableLiveData<HealthDataExportFormat> chooseFile = new MutableLiveData<>();
    private HealthDataReport preview = HealthDataReport.EMPTY;
    private boolean busy;
    private boolean cleared;

    public HealthDataExportViewModel(@NonNull Application app, SavedStateHandle saved) {
        super(app);
        this.saved = saved;
        documents = new HealthDataDocumentStore(app);
        if (!saved.contains("from")) {
            long today = Formats.dayStart(System.currentTimeMillis());
            saved.set("from", Formats.addDays(today, -29));
            saved.set("through", today);
            saved.set("type", Type.ALL.ordinal());
        }
        String draft = saved.get(DRAFT);
        if (draft == null) {
            show(EMPTY_PROMPT);
            return;
        }
        busy = true;
        if (Boolean.TRUE.equals(saved.get(WRITING))) {
            discardDraft("Không thể xác nhận kết quả xuất trước đó. Hãy kiểm tra tệp tại nơi lưu.");
        }
        else {
            HealthDataExportFormat format = pendingFormat();
            show(pickerMessage(format));
            if (!Boolean.TRUE.equals(saved.get(PICKER_OPEN))) chooseFile.setValue(format);
        }
    }

    public LiveData<Screen> screen() { return screen; }
    public LiveData<HealthDataExportFormat> chooseFile() { return chooseFile; }

    private HealthDataExportFormat pendingFormat() {
        String name = saved.get(FORMAT);
        return name == null ? HealthDataExportFormat.CSV : HealthDataExportFormat.valueOf(name);
    }

    private static String pickerMessage(HealthDataExportFormat format) {
        return "Chọn tên và nơi lưu tệp " + format.label + ".";
    }

    public HealthDataFilter filter() {
        Integer selected = saved.get("type");
        return new HealthDataFilter(Type.values()[selected], saved.get("from"), saved.get("through"));
    }

    public void setType(int selected) {
        if (!busy && selected != filter().type.ordinal()) {
            saved.set("type", selected);
            invalidatePreview();
        }
    }

    public void setDate(boolean start, long date) {
        HealthDataFilter filter = filter();
        if (busy || date == (start ? filter.from : filter.through)) return;
        saved.set(start ? "from" : "through", date);
        invalidatePreview();
    }

    private void invalidatePreview() {
        preview = HealthDataReport.EMPTY;
        show(EMPTY_PROMPT);
    }

    public void viewData() {
        if (busy) return;
        HealthDataFilter requested = filter();
        preview = HealthDataReport.EMPTY;
        if (!requested.isValid()) {
            show("Khoảng thời gian không hợp lệ. Chọn lại ngày bắt đầu và ngày kết thúc.");
            return;
        }
        busy = true;
        show("Đang đọc dữ liệu…");
        WellnessRepository.IO.execute(() -> {
            try {
                HealthDataReport report = new HealthDataReader(VitalityDatabase.get(getApplication())).read(requested);
                main.post(() -> {
                    if (cleared) return;
                    preview = report;
                    busy = false;
                    show(report.preview.isEmpty() ? "Không có dữ liệu trong khoảng thời gian đã chọn" : "");
                });
            } catch (RuntimeException error) {
                Log.w("VitalityExport", "Could not read the data preview", error);
                main.post(() -> finish("Không thể đọc dữ liệu. Vui lòng thử lại."));
            }
        });
    }

    public void export(HealthDataExportFormat format) {
        if (busy || !preview.canExport(format)) return;
        HealthDataReport report = preview;
        busy = true;
        show("Đang chuẩn bị dữ liệu…");
        FILES.execute(() -> {
            try {
                String draft = documents.saveDraft(report);
                main.post(() -> {
                    if (cleared) {
                        FILES.execute(() -> documents.deleteDraft(draft));
                        return;
                    }
                    saved.set(DRAFT, draft);
                    saved.set(FORMAT, format.name());
                    show(pickerMessage(format));
                    chooseFile.setValue(format);
                });
            } catch (IOException | RuntimeException error) {
                Log.w("VitalityExport", "Could not create the export draft", error);
                main.post(() -> finish("Không thể chuẩn bị dữ liệu " + format.label + ". Vui lòng thử lại."));
            }
        });
    }

    public boolean claimPicker(HealthDataExportFormat format) {
        if (chooseFile.getValue() != format) return false;
        saved.set(PICKER_OPEN, true);
        chooseFile.setValue(null);
        return true;
    }

    public void pickerUnavailable() {
        discardDraft("Không thể mở trình chọn nơi lưu.");
    }

    public void documentChosen(Uri destination) {
        if (!Boolean.TRUE.equals(saved.get(PICKER_OPEN)) || Boolean.TRUE.equals(saved.get(WRITING))) return;
        if (destination == null) {
            discardDraft("Đã hủy xuất dữ liệu.");
            return;
        }
        saved.set(WRITING, true);
        write(destination, saved.get(DRAFT), pendingFormat());
    }

    private void discardDraft(String message) {
        String draft = saved.get(DRAFT);
        clearPending();
        boolean restorePreview = preview.preview.isEmpty() && draft != null;
        if (!restorePreview) {
            FILES.execute(() -> documents.deleteDraft(draft));
            finish(message);
            return;
        }
        show(message);
        FILES.execute(() -> {
            HealthDataReport report = readDraft(draft);
            documents.deleteDraft(draft);
            main.post(() -> {
                if (cleared) return;
                preview = report;
                finish(message);
            });
        });
    }

    private void write(Uri destination, String draft, HealthDataExportFormat format) {
        HealthDataReport visible = preview;
        show("Đang lưu " + format.label + "…");
        FILES.execute(() -> {
            HealthDataReport report = visible.canExport(format) ? visible : readDraft(draft);
            String message;
            try {
                HealthDataDocumentStore.ExportResult result = documents.write(destination, format, report);
                message = "Xuất " + format.label + " thành công\n"
                        + (result.name == null ? "Android không cung cấp tên tệp." : "Tệp: " + result.name)
                        + "\nSố bản ghi: " + result.rows;
            } catch (IOException | RuntimeException error) {
                Log.w("VitalityExport", "Could not write the selected document", error);
                boolean deleted = documents.deleteIncomplete(destination);
                message = "Xuất dữ liệu không thành công. " + (deleted
                        ? "Đã xóa tệp dở dang." : "Không thể xóa tệp dở dang tại nơi lưu.");
            }
            String resultMessage = message;
            main.post(() -> {
                FILES.execute(() -> documents.deleteDraft(draft));
                if (cleared) return;
                clearPending();
                preview = report;
                finish(resultMessage);
            });
        });
    }

    private HealthDataReport readDraft(String name) {
        try {
            return documents.readDraft(name);
        } catch (IOException | RuntimeException error) {
            Log.w("VitalityExport", "Could not recover the data preview", error);
            return HealthDataReport.EMPTY;
        }
    }

    private void clearPending() {
        saved.remove(DRAFT);
        saved.remove(PICKER_OPEN);
        saved.remove(WRITING);
        saved.remove(FORMAT);
    }

    private void finish(String status) {
        if (cleared) return;
        busy = false;
        show(status);
    }

    private void show(String status) {
        screen.setValue(new Screen(preview, busy, status));
    }

    @Override protected void onCleared() {
        cleared = true;
        String draft = saved.get(DRAFT);
        if (draft != null && !Boolean.TRUE.equals(saved.get(WRITING))) {
            FILES.execute(() -> documents.deleteDraft(draft));
        }
    }

    public static final class Screen {
        public final List<HealthDataRecord> rows;
        public final boolean excelAvailable;
        public final boolean busy;
        public final String status;

        private Screen(HealthDataReport report, boolean busy, String status) {
            this.rows = report.preview;
            this.excelAvailable = report.canExport(HealthDataExportFormat.EXCEL);
            this.busy = busy;
            this.status = status;
        }
    }
}

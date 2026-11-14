package com.example.vitality.ui.period;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import java.util.Calendar;
import java.util.List;
import com.example.vitality.data.entity.PeriodEntry;
import com.example.vitality.utils.Formats;
import com.example.vitality.viewmodel.VitalityViewModel;

public class PeriodEditorDialog extends DialogFragment {
    private PeriodEntry entry;
    private long startDay, endDay;
    private EditText symptoms, note;
    private VitalityViewModel vm;

    public static PeriodEditorDialog create(long id) {
        PeriodEditorDialog dialog = new PeriodEditorDialog();
        Bundle args = new Bundle(); args.putLong("id", id); dialog.setArguments(args);
        return dialog;
    }

    @Override public Dialog onCreateDialog(Bundle state) {
        vm = new ViewModelProvider(requireActivity()).get(VitalityViewModel.class);
        entry = new PeriodEntry();
        entry.startDay = entry.endDay = Formats.dayStart(System.currentTimeMillis());
        long id = requireArguments().getLong("id");
        List<PeriodEntry> rows = vm.periods.getValue();
        if (rows != null) for (PeriodEntry row : rows) if (row.id == id) entry = row;
        startDay = state == null ? entry.startDay : state.getLong("start", entry.startDay);
        endDay = state == null ? entry.endDay : state.getLong("end", entry.endDay);
        ScrollView scroll = new ScrollView(requireContext());
        LinearLayout root = new LinearLayout(requireContext()); root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(20 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad); scroll.addView(root);
        Button start = new Button(requireContext()), end = new Button(requireContext());
        root.addView(start); root.addView(end);
        Runnable labels = () -> { start.setText("Bắt đầu: " + Formats.day(startDay)); end.setText("Kết thúc: " + Formats.day(endDay)); };
        labels.run();
        start.setOnClickListener(v -> pick(startDay, day -> { startDay = day; labels.run(); }));
        end.setOnClickListener(v -> pick(endDay, day -> { endDay = day; labels.run(); }));
        symptoms = new EditText(requireContext()); symptoms.setHint("Triệu chứng (tùy chọn)");
        symptoms.setSingleLine(false); symptoms.setText(state == null ? entry.symptoms : state.getString("symptoms")); root.addView(symptoms);
        note = new EditText(requireContext()); note.setHint("Ghi chú (tùy chọn)");
        note.setSingleLine(false); note.setText(state == null ? entry.note : state.getString("note")); root.addView(note);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setTitle(id == 0 ? "Ghi kỳ kinh" : "Sửa kỳ kinh")
            .setView(scroll).setNegativeButton("Hủy", null).setPositiveButton("Lưu", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> save(dialog)));
        return dialog;
    }

    private void pick(long value, java.util.function.LongConsumer selected) {
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(value);
        DatePickerDialog dialog = new DatePickerDialog(requireContext(), (picker, y, m, d) -> {
            Calendar date = Calendar.getInstance(); date.clear(); date.set(y, m, d);
            selected.accept(date.getTimeInMillis());
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis()); dialog.show();
    }

    private void save(AlertDialog dialog) {
        if (endDay < startDay) { Toast.makeText(requireContext(), "Ngày kết thúc phải từ ngày bắt đầu trở đi", Toast.LENGTH_LONG).show(); return; }
        PeriodEntry value = new PeriodEntry(); value.id = entry.id; value.startDay = startDay; value.endDay = endDay;
        value.symptoms = symptoms.getText().toString().trim(); value.note = note.getText().toString().trim();
        long before = vm.saved.getValue() == null ? 0 : vm.saved.getValue();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        vm.saved.observe(this, count -> { if (count != null && count > before) dismiss(); });
        vm.busy.observe(this, busy -> { if (getDialog() != null) dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!busy); });
        android.content.Context app = requireContext().getApplicationContext();
        vm.run(() -> { vm.wellness.savePeriod(value); }, true);
    }

    @Override public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out); out.putLong("start", startDay); out.putLong("end", endDay);
        out.putString("symptoms", symptoms.getText().toString()); out.putString("note", note.getText().toString());
    }
}

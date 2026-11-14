package com.example.vitality.ui.period;

import android.os.Bundle;
import android.os.Build;
import android.Manifest;
import android.widget.CheckBox;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import com.example.vitality.R;
import com.example.vitality.data.entity.PeriodEntry;
import com.example.vitality.notification.ReminderScheduler;
import com.example.vitality.ui.main.PageFragment;
import com.example.vitality.utils.Formats;
import com.example.vitality.utils.PeriodLogic;

public class PeriodFragment extends PageFragment {
    private final ActivityResultLauncher<String> permission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
        if (!granted && getContext() != null) {
            requireContext().getSharedPreferences("reminders", 0).edit().putBoolean("period", false).apply();
            ReminderScheduler.schedule(requireContext());
            if (vm.periods.getValue() != null) render(vm.periods.getValue());
        }
    });
    private int monthOffset;
    private long selectedDay;

    @Override public void onViewCreated(View v, Bundle state) {
        if (state != null) { monthOffset = state.getInt("month"); selectedDay = state.getLong("day"); }
        heading("Chu kỳ kinh nguyệt", "Lịch sử và ngày dự kiến chỉ mang tính tham khảo");
        vm.periods.observe(getViewLifecycleOwner(), rows -> render(rows == null ? Collections.emptyList() : rows));
    }

    private void render(List<PeriodEntry> rows) {
        if (binding == null) return;
        clear();
        button("+ Ghi kỳ kinh", () -> PeriodEditorDialog.create(0).show(getParentFragmentManager(), "period_editor"));
        Long next = PeriodLogic.nextStart(rows);
        text(next == null ? "Ghi ít nhất hai kỳ kinh để ước tính ngày tiếp theo." : "Dự kiến kỳ tiếp theo: " + Formats.day(next) + " (ước tính từ các chu kỳ đã ghi)");
        CheckBox reminder = new CheckBox(requireContext());
        reminder.setText("Nh\u1eafc tr\u01b0\u1edbc ng\u00e0y d\u1ef1 ki\u1ebfn 2 ng\u00e0y");
        reminder.setChecked(requireContext().getSharedPreferences("reminders", 0).getBoolean("period", false));
        binding.content.addView(reminder);
        reminder.setOnCheckedChangeListener((button, checked) -> {
            requireContext().getSharedPreferences("reminders", 0).edit().putBoolean("period", checked).apply();
            ReminderScheduler.schedule(requireContext());
            if (checked && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS);
        });
        Calendar month = Calendar.getInstance(); month.set(Calendar.DAY_OF_MONTH, 1); month.add(Calendar.MONTH, monthOffset);
        int year = month.get(Calendar.YEAR), monthIndex = month.get(Calendar.MONTH);
        LinearLayout navigation = new LinearLayout(requireContext()); navigation.setOrientation(LinearLayout.HORIZONTAL);
        binding.content.addView(navigation);
        Button previous = new Button(requireContext()), current = new Button(requireContext()), nextMonth = new Button(requireContext());
        previous.setText("‹"); current.setText("Tháng " + (monthIndex + 1) + "/" + year); nextMonth.setText("›");
        navigation.addView(previous, new LinearLayout.LayoutParams(0, -2, 1));
        navigation.addView(current, new LinearLayout.LayoutParams(0, -2, 3));
        navigation.addView(nextMonth, new LinearLayout.LayoutParams(0, -2, 1));
        previous.setOnClickListener(v -> { monthOffset--; render(rows); });
        current.setOnClickListener(v -> { monthOffset = 0; render(rows); });
        nextMonth.setOnClickListener(v -> { monthOffset++; render(rows); });
        GridLayout grid = new GridLayout(requireContext()); grid.setColumnCount(7); binding.content.addView(grid);
        String[] weekdays = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
        for (String label : weekdays) { Button cell = new Button(requireContext()); cell.setText(label); cell.setEnabled(false); grid.addView(cell, cellParams()); }
        int blank = (month.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        for (int i = 0; i < blank; i++) grid.addView(new View(requireContext()), cellParams());
        int days = month.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int d = 1; d <= days; d++) {
            month.set(Calendar.DAY_OF_MONTH, d);
            long day = Formats.dayStart(month.getTimeInMillis());
            boolean recorded = false;
            for (PeriodEntry entry : rows) if (day >= entry.startDay && day <= entry.endDay) { recorded = true; break; }
            Button cell = new Button(requireContext()); cell.setText(String.valueOf(d)); cell.setTextSize(12); cell.setAllCaps(false);
            cell.setMinWidth(0); cell.setMinimumWidth(0); cell.setPadding(0, 0, 0, 0);
            if (recorded) cell.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFEBA0A8));
            else if (next != null && day == next) cell.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF9DD8C4));
            if (day == selectedDay) cell.setTextColor(0xFFB00055);
            grid.addView(cell, cellParams());
            cell.setOnClickListener(v -> { selectedDay = day; render(rows); });
        }
        text("Hồng: ngày đã ghi   •   Xanh: ngày dự kiến");
        if (selectedDay != 0) {
            List<PeriodEntry> selected = new ArrayList<>();
            for (PeriodEntry entry : rows) if (selectedDay >= entry.startDay && selectedDay <= entry.endDay) selected.add(entry);
            text("Ngày " + Formats.day(selectedDay) + (selected.isEmpty() ? ": chưa có bản ghi" : ": kỳ kinh đã ghi"));
            for (PeriodEntry entry : selected) periodRow(entry);
        }
        text("Lịch sử kỳ kinh");
        if (rows.isEmpty()) text("Chưa có bản ghi.");
        for (PeriodEntry entry : rows) periodRow(entry);
    }

    private GridLayout.LayoutParams cellParams() {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0; params.height = dp(48); params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        return params;
    }

    private void periodRow(PeriodEntry entry) {
        String detail = Formats.day(entry.startDay) + " – " + Formats.day(entry.endDay);
        if (entry.symptoms != null && !entry.symptoms.isEmpty()) detail += "\nTriệu chứng: " + entry.symptoms;
        if (entry.note != null && !entry.note.isEmpty()) detail += "\n" + entry.note;
        button(detail, () -> new AlertDialog.Builder(requireContext()).setItems(new String[]{"Sửa", "Xóa"}, (dialog, which) -> {
            if (which == 0) PeriodEditorDialog.create(entry.id).show(getParentFragmentManager(), "period_editor");
            else new AlertDialog.Builder(requireContext()).setTitle("Xóa kỳ kinh?").setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (d, w) -> {
                    android.content.Context app = requireContext().getApplicationContext();
                    vm.run(() -> { vm.wellness.periods.delete(entry); ReminderScheduler.schedule(app); }, true);
                }).show();
        }).show());
    }

    @Override public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state); state.putInt("month", monthOffset); state.putLong("day", selectedDay);
    }
}

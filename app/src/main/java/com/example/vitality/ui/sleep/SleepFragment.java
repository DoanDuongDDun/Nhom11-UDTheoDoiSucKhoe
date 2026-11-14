package com.example.vitality.ui.sleep;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import java.util.*;
import com.example.vitality.R;
import com.example.vitality.data.entity.*;
import com.example.vitality.ui.diary.JournalEditorDialog;
import com.example.vitality.ui.main.PageFragment;
import com.example.vitality.utils.*;

public class SleepFragment extends PageFragment {
    private int range = 1;
    @Override public void onViewCreated(View view, Bundle state) {
        if (state != null) range = state.getInt("range", 1);
        heading("Giấc ngủ", "Thời lượng thực ngủ và mục tiêu của bạn");
        observeAll(this::render, vm.journals, vm.profile, vm.goals);
    }
    private void render() {
        if (binding == null) return;
        clear();
        List<JournalEntry> rows = vm.journals.getValue();
        if (rows == null) rows = new ArrayList<>();
        UserProfile profile = vm.profile.getValue();
        double goal = GoalProgress.target(vm.goals.getValue(), "sleep", profile == null ? 8 : profile.sleepGoal);
        long today = Formats.dayStart(System.currentTimeMillis());
        Calendar cal = Calendar.getInstance(); cal.setTimeInMillis(today);
        int days; long since;
        if (range == 0) { since = today; days = 1; }
        else if (range == 1) {
            int offset = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7;
            since = Formats.addDays(today, -offset); days = offset + 1;
        } else {
            since = Formats.addDays(today, 1 - cal.get(Calendar.DAY_OF_MONTH));
            days = cal.get(Calendar.DAY_OF_MONTH);
        }
        TreeMap<Long, Double> daily = new TreeMap<>();
        List<JournalEntry> sleeps = new ArrayList<>(); JournalEntry active = null;
        for (JournalEntry e : rows) if ("sleep".equals(e.kind)) {
            sleeps.add(e);
            if (e.endedAt == null) active = e;
            else if (e.endedAt >= since && e.endedAt <= System.currentTimeMillis()) {
                long day = Formats.dayStart(e.endedAt);
                daily.put(day, daily.getOrDefault(day, 0d) + HealthMath.sleepMinutesOrZero(e.startedAt, e.endedAt, e.awakeMinutes) / 60d);
            }
        }
        double total = 0; for (double hours : daily.values()) total += hours;
        double average = total / days;
        hero(R.drawable.bg_sleep_card, R.drawable.ic_moon, range == 0 ? "HÔM NAY" : range == 1 ? "TUẦN NÀY" : "THÁNG NÀY",
            Formats.number(average), "giờ / ngày", "Mục tiêu: " + Formats.number(goal) + " giờ/ngày • " +
            (average >= goal ? "Đạt mục tiêu" : "Thiếu " + Formats.number(goal - average) + " giờ/ngày"));
        button("Ngày" + (range == 0 ? " ✓" : ""), () -> { range = 0; render(); });
        button("Tuần" + (range == 1 ? " ✓" : ""), () -> { range = 1; render(); });
        button("Tháng" + (range == 2 ? " ✓" : ""), () -> { range = 2; render(); });
        text("Tổng: " + Formats.number(total) + " giờ • " + daily.size() + " ngày có bản ghi / " + days + " ngày");
        for (Map.Entry<Long, Double> day : daily.descendingMap().entrySet())
            text(Formats.day(day.getKey()) + ": " + Formats.number(day.getValue()) + " giờ" + (day.getValue() >= goal ? " • Đạt mục tiêu" : ""));
        if (active == null) button("Bắt đầu ngủ", () -> {
            JournalEntry e = new JournalEntry(); e.kind = "sleep";
            vm.run(() -> vm.wellness.saveJournal(e), true);
        }); else {
            JournalEntry current = active;
            text("Đang ngủ từ " + Formats.date(current.startedAt));
            button("Kết thúc ngủ", () -> JournalEditorDialog.create("sleep", current.id).show(getParentFragmentManager(), "sleep_editor"));
        }
        goalProgress("sleep");
        button("Nhập giấc ngủ thủ công", () -> JournalEditorDialog.create("sleep", 0).show(getParentFragmentManager(), "sleep_editor"));
        text("Lịch sử giấc ngủ");
        if (sleeps.isEmpty()) text("Chưa có bản ghi.");
        listStyle(R.drawable.ic_moon, R.drawable.bg_icon_violet, 0xFF7360DD);
        list(sleeps, e -> Formats.date(e.startedAt) + "\n" + (e.endedAt == null ? "Đang ngủ" :
            Formats.date(e.endedAt) + " • " + Formats.number(HealthMath.sleepMinutesOrZero(e.startedAt, e.endedAt, e.awakeMinutes) / 60) + " giờ"
            + " • Thức " + Formats.number(e.awakeMinutes) + " phút • Chất lượng " + e.quality + "/5")
            + (e.note == null || e.note.isEmpty() ? "" : "\n" + e.note), e ->
            new AlertDialog.Builder(requireContext()).setItems(new String[]{"Sửa / hoàn tất", "Xóa"}, (dialog, choice) -> {
                if (choice == 0) JournalEditorDialog.create("sleep", e.id).show(getParentFragmentManager(), "sleep_editor");
                else new AlertDialog.Builder(requireContext()).setTitle("Xóa phiên ngủ?").setNegativeButton("Hủy", null)
                    .setPositiveButton("Xóa", (d, w) -> vm.run(() -> vm.wellness.dao.delete(e), true)).show();
            }).show());
    }
    @Override public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state); state.putInt("range", range);
    }
}

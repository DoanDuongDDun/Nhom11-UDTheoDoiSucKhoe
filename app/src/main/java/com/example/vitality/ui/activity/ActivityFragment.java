package com.example.vitality.ui.activity;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import com.example.vitality.R;
import com.example.vitality.data.entity.JournalEntry;
import com.example.vitality.data.entity.UserProfile;
import com.example.vitality.ui.main.PageFragment;
import com.example.vitality.utils.Formats;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

/** Màn hình QLVĐ & Thể chất Hoạt động */
public class ActivityFragment extends PageFragment {

    @Override
    public void onViewCreated(View v, Bundle b) {
        heading("Hoạt động", "Theo dõi thể chất, bước chân và thời lượng vận động");
        observeAll(this::render, vm.journals, vm.profile,vm.goals);
    }

    private void render() {
        if (binding == null) return;
        clear();

        UserProfile p = vm.profile.getValue();
        long now = System.currentTimeMillis();
        long today = Formats.dayStart(now);
        double activityGoal = com.example.vitality.utils.GoalProgress.target(vm.goals.getValue(),"activity",p == null ? 30 : p.activityGoal);

        double totalMinutes = 0;
        int activityCount = 0;
        List<JournalEntry> todayActivities = new ArrayList<>();
        List<JournalEntry> allJournals = vm.journals.getValue();

        if (allJournals != null) {
            for (JournalEntry e : allJournals) {
                if ("activity".equals(e.kind) && e.startedAt >= today) {
                    todayActivities.add(e);
                    activityCount++;
                    totalMinutes += e.amount;
                }
            }
        }

        // Hero Card
        hero(
            R.drawable.bg_hero_circle,
            R.drawable.ic_shoe,
            "TỔNG THỜI GIAN VẬN ĐỘNG HÔM NAY",
            Formats.number(totalMinutes),
            "phút",
            totalMinutes >= activityGoal
                ? "Tuyệt vời! Bạn đã hoàn thành mục tiêu " + Formats.number(activityGoal) + " phút!"
                : "Còn " + Formats.number(Math.max(0, activityGoal - totalMinutes)) + " phút nữa để đạt mục tiêu " + Formats.number(activityGoal) + " phút"
        );

        goalProgress("activity");

        // Lối tắt thêm nhanh
        card(null, "Thêm nhanh vận động", null, null);
        button("+ 15 phút đi bộ", () -> addQuickActivity("Đi bộ", 15));
        button("+ 30 phút chạy bộ", () -> addQuickActivity("Chạy bộ", 30));
        button("+ 60 phút thể thao", () -> addQuickActivity("Tập thể thao", 60));
        endCard();

        // Form ghi nhận vận động
        card(null, "Ghi nhận hoạt động tùy chỉnh", null, null);
        TextInputEditText nameField = field("Tên hoạt động (vd: Chạy bộ, Bơi lội, Đạp xe)", "", false);
        TextInputEditText durationField = field("Thời gian (phút)", "", true);

        button("Ghi nhận vận động", () -> {
            String actName = value(nameField);
            Double mins = optional(durationField, 1, 1440);

            if (actName.isEmpty()) {
                nameField.setError("Vui lòng nhập tên hoạt động");
                nameField.requestFocus();
                return;
            }
            if (mins == null) {
                durationField.setError("Vui lòng nhập số phút hợp lệ");
                durationField.requestFocus();
                return;
            }

            JournalEntry entry = new JournalEntry();
            entry.kind = "activity";
            entry.note = actName;
            entry.amount = mins;
            entry.startedAt = System.currentTimeMillis();

            vm.run(() -> vm.wellness.saveJournal(entry), true);
        });
        endCard();

        // Danh sách hoạt động hôm nay
        if (!todayActivities.isEmpty()) {
            card(null, "Các hoạt động hôm nay", null, null);
            listStyle(R.drawable.ic_shoe, R.drawable.bg_icon_mint, 0xFF1D9367);
            list(todayActivities, e -> {
                String name = e.note == null || e.note.trim().isEmpty() ? "Hoạt động" : e.note.trim();
                return name + "\n" + Formats.date(e.startedAt) + " • " + Formats.number(e.amount) + " phút";
            }, e -> new AlertDialog.Builder(requireContext())
                .setTitle(e.note != null ? e.note : "Hoạt động")
                .setMessage("Bạn muốn xóa lượt vận động này?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> vm.run(() -> vm.wellness.dao.delete(e), true))
                .show()
            );
            endCard();
        }

        
    }

    private void addQuickActivity(String name, double minutes) {
        JournalEntry entry = new JournalEntry();
        entry.kind = "activity";
        entry.note = name;
        entry.amount = minutes;
        entry.startedAt = System.currentTimeMillis();
        vm.run(() -> vm.wellness.saveJournal(entry), true);
    }
}

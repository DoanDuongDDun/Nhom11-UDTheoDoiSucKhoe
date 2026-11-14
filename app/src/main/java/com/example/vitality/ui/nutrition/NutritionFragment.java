package com.example.vitality.ui.nutrition;

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

/** Màn hình QLVN & Calo Dinh dưỡng */
public class NutritionFragment extends PageFragment {

    @Override
    public void onViewCreated(View v, Bundle b) {
        heading("Dinh dưỡng", "Quản lý khẩu phần ăn và lượng Calo tiêu thụ");
        observeAll(this::render, vm.journals, vm.profile, vm.goals);
    }

    private void render() {
        if (binding == null) return;
        clear();

        UserProfile p = vm.profile.getValue();
        long now = System.currentTimeMillis();
        long today = Formats.dayStart(now);

        double totalCalories = 0;
        int mealCount = 0;
        List<JournalEntry> todayMeals = new ArrayList<>();
        List<JournalEntry> allJournals = vm.journals.getValue();

        if (allJournals != null) {
            for (JournalEntry e : allJournals) {
                if ("nutrition".equals(e.kind) && e.startedAt >= today) {
                    todayMeals.add(e);
                    mealCount++;
                    if (e.calories != null) {
                        totalCalories += e.calories;
                    }
                }
            }
        }

        // Hero Card
        hero(
            R.drawable.bg_calories_card,
            R.drawable.ic_fork,
            "TỔNG CALO HÔM NAY",
            String.format(java.util.Locale.getDefault(), "%,.0f", totalCalories),
            "kcal",
            mealCount == 0 ? "Chưa có món ăn nào hôm nay" : "Đã ghi nhận " + mealCount + " bữa ăn"
        );

        goalProgress("nutrition", "water");

        // Form thêm món ăn
        card(null, "Thêm món ăn mới", null, null);
        TextInputEditText foodNameField = field("Tên món ăn (vd: Phở bò, Cơm tấm)", "", false);
        TextInputEditText caloriesField = field("Lượng Calo (kcal)", "", true);

        button("Thêm Món Ăn", () -> {
            String foodName = value(foodNameField);
            Double calVal = optional(caloriesField, 1, 5000);

            if (foodName.isEmpty()) {
                foodNameField.setError("Vui lòng nhập tên món ăn");
                foodNameField.requestFocus();
                return;
            }
            if (calVal == null) {
                caloriesField.setError("Vui lòng nhập số calo hợp lệ");
                caloriesField.requestFocus();
                return;
            }

            JournalEntry entry = new JournalEntry();
            entry.kind = "nutrition";
            entry.note = foodName;
            entry.calories = calVal;
            entry.amount = 1;
            entry.startedAt = System.currentTimeMillis();

            vm.run(() -> vm.wellness.saveJournal(entry), true);
        });
        endCard();

        // Danh sách món ăn hôm nay
        if (!todayMeals.isEmpty()) {
            card(null, "Danh sách món ăn hôm nay", null, null);
            listStyle(R.drawable.ic_fork, R.drawable.bg_icon_orange, 0xFFEB791F);
            list(todayMeals, e -> {
                String name = e.note == null || e.note.trim().isEmpty() ? "Bữa ăn" : e.note.trim();
                String calStr = e.calories != null ? Formats.number(e.calories) + " kcal" : "";
                return name + "\n" + Formats.date(e.startedAt) + (calStr.isEmpty() ? "" : " • " + calStr);
            }, e -> new AlertDialog.Builder(requireContext())
                .setTitle(e.note != null ? e.note : "Món ăn")
                .setMessage("Bạn muốn xóa món ăn này?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> vm.run(() -> vm.wellness.dao.delete(e), true))
                .show()
            );
            endCard();
        }

        
    }
}

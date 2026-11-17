package com.example.vitality.ui.nutrition;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import com.example.vitality.R;
import com.example.vitality.data.entity.JournalEntry;
import com.example.vitality.ui.main.PageFragment;
import com.example.vitality.utils.Formats;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Màn hình QLVN & Calo Dinh dưỡng */
public class NutritionFragment extends PageFragment {

    @Override
    public void onViewCreated(View v, Bundle b) {
        heading("Dinh dưỡng", "Quản lý khẩu phần ăn và lượng Calo tiêu thụ");
        observeAll(this::render, vm.journals, vm.goals);
    }

    private void render() {
        if (binding == null) return;
        clear();

        long now = System.currentTimeMillis();
        long today = Formats.dayStart(now);
        double calories = 0;
        List<JournalEntry> todayMeals = new ArrayList<>();
        List<JournalEntry> journal = vm.journals.getValue();
        if (journal != null) {
            for (JournalEntry entry : journal) {
                if ("nutrition".equals(entry.kind) && entry.startedAt >= today && entry.startedAt <= now) {
                    todayMeals.add(entry);
                    if (entry.calories != null) calories += entry.calories;
                }
            }
        }
        int mealCount = todayMeals.size();

        // Hero Card
        hero(
            R.drawable.bg_calories_card,
            R.drawable.ic_fork,
            "TỔNG CALO HÔM NAY",
            String.format(Locale.getDefault(), "%,.0f", calories),
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
            if (foodName.isEmpty()) {
                foodNameField.setError("Vui lòng nhập tên món ăn");
                foodNameField.requestFocus();
                return;
            }
            try {
                JournalEntry entry = new JournalEntry();
                entry.kind = "nutrition";
                entry.title = foodName;
                entry.calories = number(caloriesField, 1, 5000);
                entry.amount = 1;
                vm.run(() -> vm.wellness.saveJournal(entry), true);
            } catch (IllegalArgumentException invalid) {
                // number() puts the error on the input.
            }
        });
        endCard();

        // Danh sách món ăn hôm nay
        if (!todayMeals.isEmpty()) {
            card(null, "Danh sách món ăn hôm nay", null, null);
            listStyle(R.drawable.ic_fork, R.drawable.bg_icon_orange, 0xFFEB791F);
            list(todayMeals, e -> {
                String name = e.nameOr("Bữa ăn");
                String calStr = e.calories != null ? Formats.number(e.calories) + " kcal" : "";
                return name + "\n" + Formats.date(e.startedAt) + (calStr.isEmpty() ? "" : " • " + calStr);
            }, e -> new AlertDialog.Builder(requireContext())
                .setTitle(e.nameOr("Món ăn"))
                .setMessage("Bạn muốn xóa món ăn này?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> vm.run(() -> vm.wellness.dao.delete(e), true))
                .show()
            );
            endCard();
        }

        
    }
}

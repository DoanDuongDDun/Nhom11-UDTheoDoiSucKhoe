package com.example.vitality.ui.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.example.vitality.R;
import com.example.vitality.data.entity.JournalEntry;
import com.example.vitality.data.entity.UserProfile;
import com.example.vitality.databinding.WidgetFitnessTrackerBinding;
import com.example.vitality.ui.diary.JournalEditorDialog;
import com.example.vitality.ui.main.PageFragment;
import com.example.vitality.utils.Formats;
import com.example.vitality.utils.GoalProgress;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Nhật ký vận động và bước chân trong ngày. */
public class ActivityFragment extends PageFragment implements SensorEventListener {
    private WidgetFitnessTrackerBinding tracker;
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private StepSession stepSession;
    private StepSessionViewModel stepSave;
    private long dayShown;
    private long savedStepsToday;
    private int stepFeedback;
    private boolean hasDeclaredStepSensor;
    private boolean startStepsWhenResumed;

    private final ActivityResultLauncher<String> stepPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startStepsWhenResumed = true;
                    if (isResumed()) startStepSession();
                } else {
                    stepFeedback = R.string.fitness_tracker_step_permission_needed;
                    updateStepControls();
                }
            });

    @Override
    public void onViewCreated(View v, Bundle b) {
        heading(getString(R.string.fitness_tracker_title), getString(R.string.fitness_tracker_description));
        PackageManager packages = requireContext().getPackageManager();
        hasDeclaredStepSensor = packages.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER)
                || packages.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_DETECTOR);
        if (!needsStepPermission()) findStepSensor();
        stepSave = new ViewModelProvider(requireActivity()).get(StepSessionViewModel.class);
        stepSave.pending().observe(getViewLifecycleOwner(), ignored -> updateStepControls());
        observeAll(this::render, vm.journals, vm.profile, vm.goals);
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean needsPermission = needsStepPermission();
        if (!needsPermission && sensorManager == null) findStepSensor();
        if (!needsPermission && stepFeedback == R.string.fitness_tracker_step_permission_needed) {
            stepFeedback = 0;
        }
        if (startStepsWhenResumed) {
            startStepSession();
        } else {
            updateStepControls();
        }
    }

    @Override
    public void onPause() {
        finishStepSession();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        tracker = null;
        stepSensor = null;
        sensorManager = null;
        super.onDestroyView();
    }

    private void render() {
        if (binding == null) return;
        clear();

        UserProfile p = vm.profile.getValue();
        double activityGoal = GoalProgress.target(vm.goals.getValue(), "activity", p == null ? 30 : p.activityGoal);
        long now = System.currentTimeMillis();
        long today = Formats.dayStart(now);
        double minutes = 0;
        long steps = 0;
        List<JournalEntry> activities = new ArrayList<>();
        List<JournalEntry> journal = vm.journals.getValue();
        if (journal != null) {
            for (JournalEntry entry : journal) {
                if (entry.startedAt < today || entry.startedAt > now) continue;
                if ("activity".equals(entry.kind)) {
                    minutes += entry.amount;
                    activities.add(entry);
                } else if (JournalEntry.KIND_STEPS.equals(entry.kind)) {
                    steps += Math.round(entry.amount);
                    activities.add(entry);
                }
            }
        }

        dayShown = today;
        savedStepsToday = steps;
        showFitnessTracker(minutes, activityGoal);

        goalProgress("activity");

        // Lối tắt thêm nhanh
        card(null, "Vận động thường dùng", null, null);
        workoutShortcut("+ 15 phút đi bộ", "Đi bộ", 15);
        workoutShortcut("+ 30 phút chạy bộ", "Chạy bộ", 30);
        workoutShortcut("+ 60 phút thể thao", "Tập thể thao", 60);
        endCard();

        // Form ghi nhận vận động
        card(null, "Ghi nhận hoạt động tùy chỉnh", null, null);
        TextInputEditText nameField = field("Tên hoạt động (vd: Chạy bộ, Bơi lội, Đạp xe)", "", false);
        TextInputEditText durationField = field("Thời gian (phút)", "", true);

        button("Ghi nhận vận động", () -> {
            String actName = value(nameField);
            if (actName.isEmpty()) {
                nameField.setError("Vui lòng nhập tên hoạt động");
                nameField.requestFocus();
                return;
            }
            try {
                saveEntry("activity", actName, number(durationField, 1, 1440));
            } catch (IllegalArgumentException invalid) {
                // number() puts the error on the input.
            }
        });
        endCard();

        // Danh sách hoạt động hôm nay
        if (!activities.isEmpty()) {
            card(null, "Các hoạt động hôm nay", null, null);
            listStyle(R.drawable.ic_shoe, R.drawable.bg_icon_mint, 0xFF1D9367);
            list(activities, e -> e.nameOr("Hoạt động") + "\n"
                    + Formats.date(e.startedAt) + " • " + Formats.activityAmount(e),
                    e -> new AlertDialog.Builder(requireContext())
                .setTitle(e.nameOr("Hoạt động"))
                .setMessage("Bạn muốn xóa bản ghi này?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> vm.run(() -> vm.wellness.dao.delete(e), true))
                .show()
            );
            endCard();
        }

    }

    private void showFitnessTracker(double minutes, double goal) {
        tracker = WidgetFitnessTrackerBinding.inflate(getLayoutInflater(), binding.content, true);
        tracker.fitnessWorkoutValue.setText(getString(R.string.fitness_tracker_workout_value,
                Formats.number(minutes)));
        tracker.fitnessWorkoutGoal.setText(minutes >= goal
                ? getString(R.string.fitness_tracker_goal_met, Formats.number(goal))
                : getString(R.string.fitness_tracker_goal_remaining, Formats.number(goal - minutes)));
        tracker.fitnessToggleSteps.setOnClickListener(v -> toggleStepSession());
        tracker.fitnessAddSteps.setOnClickListener(v -> saveEntry(JournalEntry.KIND_STEPS, "Bước chân", 250));
        tracker.fitnessCustomSteps.setOnClickListener(v -> JournalEditorDialog.create(JournalEntry.KIND_STEPS, 0)
                .show(getParentFragmentManager(), "activity_step_editor"));
        tracker.fitnessAddWorkout.setOnClickListener(v -> saveEntry("activity", "Tập luyện", 10));
        updateStepControls();
    }

    private void toggleStepSession() {
        if (stepSession != null) {
            finishStepSession();
            return;
        }
        StepSessionViewModel.PendingSave pending = stepSave.pending().getValue();
        if (pending != null) {
            if (!pending.saving) stepSave.save(vm, pending.entry);
            return;
        }
        if (needsStepPermission()) {
            stepPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION);
        } else {
            startStepSession();
        }
    }

    private boolean needsStepPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED;
    }

    private void findStepSensor() {
        // Khi chưa có quyền, Android có thể ẩn cảm biến khỏi danh sách của SensorManager.
        sensorManager = requireContext().getSystemService(SensorManager.class);
        if (sensorManager == null) return;
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (stepSensor == null) stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
    }

    private void startStepSession() {
        startStepsWhenResumed = false;
        if (stepSession != null || stepSave.pending().getValue() != null) return;
        stepFeedback = 0;
        if (sensorManager == null && !needsStepPermission()) findStepSensor();
        if (stepSensor == null) {
            updateStepControls();
            return;
        }
        boolean cumulative = stepSensor.getType() == Sensor.TYPE_STEP_COUNTER;
        StepSession session = new StepSession(System.currentTimeMillis(), SystemClock.elapsedRealtimeNanos(), cumulative);
        try {
            if (sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)) {
                stepSession = session;
                binding.getRoot().setKeepScreenOn(true);
            } else {
                stepFeedback = R.string.fitness_tracker_step_start_failed;
            }
        } catch (SecurityException permissionDenied) {
            stepFeedback = R.string.fitness_tracker_step_permission_needed;
        }
        updateStepControls();
    }

    private void finishStepSession() {
        StepSession finished = stepSession;
        if (finished == null) return;
        stepSession = null;
        sensorManager.unregisterListener(this);
        binding.getRoot().setKeepScreenOn(false);

        if (finished.steps() == 0) {
            stepFeedback = R.string.fitness_tracker_step_empty;
            updateStepControls();
            return;
        }
        JournalEntry entry = new JournalEntry();
        entry.kind = JournalEntry.KIND_STEPS;
        entry.title = getString(R.string.fitness_tracker_step_entry_title);
        entry.startedAt = finished.startedAt;
        entry.endedAt = Math.max(entry.startedAt, System.currentTimeMillis());
        entry.amount = finished.steps();
        stepSave.save(vm, entry);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (stepSession != null && event.sensor.getType() == stepSensor.getType()
                && stepSession.record(event.timestamp, event.values[0])) {
            updateStepControls();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void updateStepControls() {
        if (tracker == null) return;
        StepSessionViewModel.PendingSave pending = stepSave.pending().getValue();
        boolean canStart = stepSensor != null || (hasDeclaredStepSensor && needsStepPermission());
        long total = savedStepsToday;
        int action = R.string.fitness_tracker_step_start;
        String status = null;
        if (stepSession != null) {
            if (stepSession.startedAt >= dayShown) total += stepSession.steps();
            action = R.string.fitness_tracker_step_stop;
            status = stepSession.isReady()
                    ? getString(R.string.fitness_tracker_step_counting, groupedSteps(stepSession.steps()))
                    : getString(R.string.fitness_tracker_step_preparing);
        } else if (pending != null) {
            action = pending.saving ? R.string.fitness_tracker_step_saving : R.string.fitness_tracker_step_retry;
            int message = pending.saving ? R.string.fitness_tracker_step_counting : R.string.fitness_tracker_step_save_failed;
            status = getString(message, groupedSteps(Math.round(pending.entry.amount)));
        } else if (!canStart) {
            status = getString(R.string.fitness_tracker_step_unavailable);
        } else if (stepFeedback != 0) {
            status = getString(stepFeedback);
        }

        tracker.fitnessStepsValue.setText(getString(R.string.fitness_tracker_steps_value, groupedSteps(total)));
        tracker.fitnessToggleSteps.setVisibility(canStart || pending != null ? View.VISIBLE : View.GONE);
        tracker.fitnessToggleSteps.setText(action);
        tracker.fitnessToggleSteps.setEnabled(pending == null || !pending.saving);
        tracker.fitnessStepStatus.setText(status);
        tracker.fitnessStepStatus.setVisibility(status == null ? View.GONE : View.VISIBLE);
    }

    private static String groupedSteps(long steps) {
        return String.format(Locale.getDefault(), "%,d", steps);
    }

    private void workoutShortcut(String label, String name, double minutes) {
        MaterialButton shortcut = button(label, () -> saveEntry("activity", name, minutes));
        shortcut.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.green_light)));
        shortcut.setTextColor(ContextCompat.getColor(requireContext(), R.color.nav_active));
    }

    private void saveEntry(String kind, String name, double amount) {
        JournalEntry entry = new JournalEntry();
        entry.kind = kind;
        entry.title = name;
        entry.amount = amount;
        vm.run(() -> vm.wellness.saveJournal(entry), true);
    }
}

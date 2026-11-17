package com.example.vitality.notification;

import android.content.Context;
import android.util.Log;
import com.example.vitality.data.dao.BodyMeasurementDao;
import com.example.vitality.data.database.VitalityDatabase;
import com.example.vitality.data.entity.BodyMeasurement;
import com.example.vitality.data.entity.Goal;
import com.example.vitality.data.entity.JournalEntry;
import com.example.vitality.data.entity.UserProfile;
import com.example.vitality.notification.NotificationEvent.Group;
import com.example.vitality.notification.NotificationEvent.Type;
import com.example.vitality.utils.Formats;
import com.example.vitality.utils.GoalProgress;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HealthNotificationEvents {
    private HealthNotificationEvents() {}

    public static void journalSaved(Context app, VitalityDatabase db, JournalEntry entry,
                                    long id, JournalEntry previous) {
        if (id <= 0) return;
        try {
            boolean steps = JournalEntry.KIND_STEPS.equals(entry.kind);
            if (previous == null && ("activity".equals(entry.kind) || steps)) {
                NotificationController.send(app, new NotificationEvent(Type.ACTIVITY_SAVED,
                        Long.toString(id), id, entry.nameOr(steps ? "Bước chân" : "Hoạt động")
                                + " • " + Formats.activityAmount(entry)));
            }
            if ("sleep".equals(entry.kind) && entry.endedAt != null
                    && (previous == null || previous.endedAt == null)) {
                NotificationController.send(app, new NotificationEvent(
                        Type.SLEEP_FINISHED, Long.toString(id), id, null));
            }
            if (!steps && NotificationController.canSend(app, Group.GOALS)) {
                checkDailyGoals(app, db, entry, id, previous);
            }
        } catch (RuntimeException error) {
            Log.w("VitalityNotifications", "Could not process the saved journal entry", error);
        }
    }

    private static void checkDailyGoals(Context app, VitalityDatabase db, JournalEntry entry,
                                        long id, JournalEntry previous) {
        long now = System.currentTimeMillis();
        long day = Formats.dayStart(now);
        Long recordedAt = "sleep".equals(entry.kind) ? entry.endedAt : Long.valueOf(entry.startedAt);
        if (recordedAt == null || recordedAt < day || recordedAt > now || entry.kind == null) return;

        List<Goal> goals = db.goals().ofKind(entry.kind);
        List<JournalEntry> after = db.wellness().journalBetween(entry.kind, day, Formats.addDays(day, 1));
        List<JournalEntry> before = new ArrayList<>();
        for (JournalEntry row : after) if (row.id != id) before.add(row);
        if (previous != null) before.add(previous);
        boolean hasGeneralGoal = false;
        for (Goal goal : goals) {
            hasGeneralGoal |= goal.keyword == null || goal.keyword.trim().isEmpty();
            if (goal.createdAt > now || !crossed(goal, before, after, day)) continue;
            announceGoal(app, goal, goal.id + ":" + day, Type.GOAL_REACHED);
        }
        if (!hasGeneralGoal) {
            Goal fallback = profileGoal(db.wellness().savedProfile(), entry.kind);
            if (fallback != null && crossed(fallback, before, after, day)) {
                announceGoal(app, fallback, fallback.kind + ":" + day, Type.PROFILE_GOAL_REACHED);
            }
        }
    }

    private static boolean crossed(Goal goal, List<JournalEntry> before,
                                    List<JournalEntry> after, long day) {
        double oldValue = GoalProgress.current(goal, before, null, day);
        double newValue = GoalProgress.current(goal, after, null, day);
        return goal.target > 0 && oldValue < goal.target && newValue >= goal.target;
    }

    private static Goal profileGoal(UserProfile profile, String kind) {
        if (profile == null) profile = new UserProfile();
        Goal goal = new Goal();
        goal.kind = kind;
        switch (kind) {
            case "water":
                goal.target = profile.waterGoal;
                goal.title = "Uống nước";
                break;
            case "activity":
                goal.target = profile.activityGoal;
                goal.title = "Vận động";
                break;
            case "sleep":
                goal.target = profile.sleepGoal;
                goal.title = "Giấc ngủ";
                break;
            default:
                return null;
        }
        return goal;
    }

    /** Capture the currently displayed measurement before saving a replacement or new reading. */
    public static BodyMeasurement latestBeforeSave(Context app, BodyMeasurementDao dao) {
        try {
            if (NotificationController.canSend(app, Group.GOALS)) return dao.latest();
        } catch (RuntimeException error) {
            Log.w("VitalityNotifications", "Could not read the previous measurement", error);
        }
        return null;
    }

    public static void measurementSaved(Context app, VitalityDatabase db, BodyMeasurement previous) {
        if (previous == null || !NotificationController.canSend(app, Group.GOALS)) return;
        try {
            BodyMeasurement latest = db.measurements().latest();
            if (latest == null) return;
            long day = Formats.dayStart(System.currentTimeMillis());
            for (Goal goal : db.goals().bodyGoals()) {
                double before = GoalProgress.current(goal, null, Collections.singletonList(previous), day);
                double after = GoalProgress.current(goal, null, Collections.singletonList(latest), day);
                if (!reachedBodyGoal(goal, before) && reachedBodyGoal(goal, after)) {
                    announceGoal(app, goal, Long.toString(goal.id), Type.GOAL_REACHED);
                }
            }
        } catch (RuntimeException error) {
            Log.w("VitalityNotifications", "Could not check body goals after saving", error);
        }
    }

    private static boolean reachedBodyGoal(Goal goal, double value) {
        if (goal.target < goal.baseline) return value <= goal.target;
        if (goal.target > goal.baseline) return value >= goal.target;
        return Math.abs(value - goal.target) < 0.1;
    }

    private static void announceGoal(Context app, Goal goal, String key, Type type) {
        String target = NumberFormat.getNumberInstance().format(goal.target) + " " + GoalProgress.unit(goal);
        String detail = "Bạn đã hoàn thành mục tiêu “" + goal.title + "” (" + target + ").";
        NotificationController.send(app, new NotificationEvent(type, key, goal.id, detail));
    }
}

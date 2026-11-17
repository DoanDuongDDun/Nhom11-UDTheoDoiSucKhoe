package com.example.vitality.notification;

import android.content.Intent;
import android.util.Log;
import androidx.annotation.Nullable;
import com.example.vitality.data.database.VitalityDatabase;
import com.example.vitality.data.entity.Goal;
import com.example.vitality.data.entity.HabitOccurrence;
import com.example.vitality.data.entity.JournalEntry;
import com.example.vitality.notification.NotificationEvent.Type;
import com.example.vitality.utils.Formats;
import com.example.vitality.utils.GoalProgress;
import com.example.vitality.utils.HealthMath;

/** Resolves a notification's record by ID when it is opened, rather than carrying health data in the intent. */
public final class NotificationDestination {
    public final Type type;
    private final long recordId;

    private NotificationDestination(Type type, long recordId) {
        this.type = type;
        this.recordId = recordId;
    }

    @Nullable public static NotificationDestination take(Intent intent) {
        if (intent == null || !NotificationController.OPEN_ACTION.equals(intent.getAction())) return null;
        try {
            return new NotificationDestination(Type.valueOf(intent.getStringExtra(NotificationController.EXTRA_TYPE)),
                    intent.getLongExtra(NotificationController.EXTRA_RECORD, 0));
        } catch (IllegalArgumentException | NullPointerException invalid) {
            return null;
        } finally {
            intent.setAction(null);
            intent.removeExtra(NotificationController.EXTRA_TYPE);
            intent.removeExtra(NotificationController.EXTRA_RECORD);
        }
    }

    public boolean hasRecord() { return recordId > 0; }

    @Nullable public String loadDetail(VitalityDatabase db) {
        if (!hasRecord()) return null;
        try {
            switch (type) {
                case HABIT_DUE:
                    HabitOccurrence occurrence = db.wellness().findActiveOccurrence(recordId);
                    return occurrence == null ? null : occurrence.title + "\n" + Formats.date(occurrence.dueAt);
                case GOAL_REACHED:
                    Goal goal = db.goals().byId(recordId);
                    return goal == null ? null : goal.title + "\n"
                            + Formats.number(goal.target) + " " + GoalProgress.unit(goal);
                case ACTIVITY_SAVED:
                    JournalEntry activity = db.wellness().journal(recordId);
                    if (activity == null) return null;
                    boolean steps = JournalEntry.KIND_STEPS.equals(activity.kind);
                    if (!steps && !"activity".equals(activity.kind)) return null;
                    return activity.nameOr(steps ? "Bước chân" : "Hoạt động")
                            + "\n" + Formats.date(activity.startedAt) + "\n" + Formats.activityAmount(activity);
                case SLEEP_FINISHED:
                    JournalEntry sleep = db.wellness().journal(recordId);
                    if (sleep == null || !"sleep".equals(sleep.kind) || sleep.endedAt == null) return null;
                    return Formats.date(sleep.startedAt) + " – " + Formats.date(sleep.endedAt)
                            + "\n" + Formats.number(HealthMath.sleepMinutesOrZero(
                                    sleep.startedAt, sleep.endedAt, sleep.awakeMinutes) / 60)
                            + " giờ • Chất lượng " + sleep.quality + "/5";
                default:
                    return null;
            }
        } catch (RuntimeException error) {
            Log.w("VitalityNotifications", "Could not load the linked record", error);
            return null;
        }
    }
}

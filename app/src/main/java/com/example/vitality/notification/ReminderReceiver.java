package com.example.vitality.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import com.example.vitality.data.entity.HabitOccurrence;
import com.example.vitality.data.repository.WellnessRepository;
import com.example.vitality.notification.NotificationEvent.Group;
import com.example.vitality.notification.NotificationEvent.Type;
import com.example.vitality.utils.Formats;
import com.example.vitality.utils.PeriodLogic;
import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {
    private static final long HOUR = 60 * 60_000L;

    @Override public void onReceive(Context context, Intent intent) {
        Context app = context.getApplicationContext();
        if (!ReminderScheduler.ACTION.equals(intent.getAction())) {
            ReminderScheduler.schedule(app, true);
            return;
        }
        PendingResult result = goAsync();
        WellnessRepository.IO.execute(() -> {
            try {
                checkDueReminders(app);
            } catch (RuntimeException error) {
                Log.w("VitalityNotifications", "Could not check scheduled reminders", error);
            } finally {
                result.finish();
            }
        });
    }

    static void checkDueReminders(Context app) {
        WellnessRepository wellness = new WellnessRepository(app);
        wellness.refreshHabits();
        if (!NotificationController.systemAllows(app)) return;
        Calendar clock = Calendar.getInstance();
        long now = clock.getTimeInMillis();
        long day = Formats.dayStart(now);
        int hour = clock.get(Calendar.HOUR_OF_DAY);
        SharedPreferences legacy = app.getSharedPreferences("reminders", Context.MODE_PRIVATE);
        daily(app, legacy, "measure", Type.MEASUREMENT_REMINDER, 8, hour, day);
        daily(app, legacy, "water", Type.WATER_REMINDER, 10, hour, day);
        daily(app, legacy, "bed", Type.BEDTIME_REMINDER, 22, hour, day);
        remindPeriod(app, legacy, wellness, hour, day);
        if (NotificationController.canSend(app, Group.CYCLE)) {
            for (HabitOccurrence occurrence : wellness.dao.dueReminders(now - HOUR, now)) {
                if (legacy.contains("occurrence_" + occurrence.id)) continue;
                NotificationController.send(app, new NotificationEvent(Type.HABIT_DUE,
                        Long.toString(occurrence.id), occurrence.id,
                        occurrence.title + " • " + Formats.date(occurrence.dueAt)));
            }
        }
    }

    private static void remindPeriod(Context app, SharedPreferences legacy, WellnessRepository wellness,
                                     int hour, long day) {
        if (hour < 8 || !legacy.getBoolean("period", false)) return;
        Long next = PeriodLogic.nextStart(wellness.periods.all());
        if (next == null || day < Formats.addDays(next, -2) || day > next
                || legacy.getBoolean("period_" + next, false)) return;
        NotificationController.send(app, new NotificationEvent(Type.PERIOD_REMINDER,
                Long.toString(next), 0, "Kỳ kinh dự kiến bắt đầu ngày " + Formats.day(next)));
    }

    private static void daily(Context app, SharedPreferences legacy, String key, Type type,
                              int dueHour, int hour, long day) {
        if (hour >= dueHour && legacy.getBoolean(key, false) && !legacy.contains(key + Formats.day(day))) {
            NotificationController.send(app, new NotificationEvent(type, Long.toString(day), 0, null));
        }
    }
}

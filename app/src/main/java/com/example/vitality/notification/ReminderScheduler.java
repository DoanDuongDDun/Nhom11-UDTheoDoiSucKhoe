package com.example.vitality.notification;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import com.example.vitality.notification.NotificationEvent.Group;
import java.io.IOException;

public final class ReminderScheduler  {
    static final String ACTION = "com.example.vitality.CHECK_HEALTH_REMINDERS";
    private static final String OLD_ACTION = "com.example.vitality.REMIND";
    private static final long INTERVAL = 15 * 60_000L;

    private ReminderScheduler() {}

    private static PendingIntent pending(Context context, String action, int flags) {
        return PendingIntent.getBroadcast(context, 71,
                new Intent(context, ReminderReceiver.class).setAction(action),
                PendingIntent.FLAG_IMMUTABLE | flags);
    }

    public static void schedule(Context context) {
        schedule(context, false);
    }

    static synchronized void schedule(Context context, boolean reset) {
        Context app = context.getApplicationContext();
        AlarmManager manager = app.getSystemService(AlarmManager.class);
        if (manager == null) return;
        try {
            PendingIntent oldAlarm = pending(app, OLD_ACTION, PendingIntent.FLAG_NO_CREATE);
            if (oldAlarm != null) {
                manager.cancel(oldAlarm);
                oldAlarm.cancel();
            }
            NotificationController.createChannels(app);
            NotificationSettings settings = NotificationSettings.read(app);
            SharedPreferences legacy = app.getSharedPreferences("reminders", Context.MODE_PRIVATE);
            boolean enabled = settings.allows(Group.CYCLE)
                    || (settings.checked(NotificationSettings.MASTER) && legacy.getBoolean("period", false));
            PendingIntent alarm = pending(app, ACTION, PendingIntent.FLAG_NO_CREATE);
            if (alarm != null && (reset || !enabled)) {
                manager.cancel(alarm);
                alarm.cancel();
                alarm = null;
            }
            if (enabled && alarm == null) {
                alarm = pending(app, ACTION, 0);
                try {
                    manager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis() + 60_000L, INTERVAL, alarm);
                } catch (RuntimeException error) {
                    alarm.cancel();
                    throw error;
                }
            }
        } catch (IOException | RuntimeException error) {
            Log.w("VitalityNotifications", "Could not schedule reminders", error);
        }
    }
}

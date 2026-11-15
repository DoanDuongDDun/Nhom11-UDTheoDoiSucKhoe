package com.example.vitality.notification;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.example.vitality.MainActivity;
import com.example.vitality.R;
import com.example.vitality.notification.NotificationEvent.Group;
import com.example.vitality.notification.NotificationEvent.Type;
import java.io.IOException;

/** Applies app settings before handing a notification to Android. */
public final class NotificationController {
    static final String REMINDER_CHANNEL = "health";
    static final String OPEN_ACTION = "com.example.vitality.OPEN_HEALTH_NOTIFICATION";
    static final String EXTRA_TYPE = "health_notification_type";
    static final String EXTRA_RECORD = "health_notification_record";
    private static final String TAG_PREFIX = "vitality:";
    private static final long DELIVERY_RETENTION = 30 * 24 * 60 * 60_000L;
    private static final long PRUNE_INTERVAL = 24 * 60 * 60_000L;
    private static final String LAST_PRUNE = "_last_prune";

    private NotificationController() {}

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        manager.createNotificationChannel(new NotificationChannel(
                REMINDER_CHANNEL, "Nhắc nhở sức khỏe", NotificationManager.IMPORTANCE_DEFAULT));
        for (Group group : Group.values()) {
            NotificationChannel channel = new NotificationChannel(channel(group), group.title,
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(group.eventLabel);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            manager.createNotificationChannel(channel);
        }
    }

    public static boolean needsPermission(Context context) {
        return Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED;
    }

    public static boolean systemAllows(Context context) {
        return !needsPermission(context) && NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public static boolean channelAllows(Context context, Group group) {
        if (Build.VERSION.SDK_INT < 26) return true;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return false;
        NotificationChannel channel = manager.getNotificationChannel(channel(group));
        return channel == null || channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
    }

    public static boolean canSend(Context context, Group group) {
        try {
            return systemAllows(context) && channelAllows(context, group)
                    && NotificationSettings.read(context).allows(group);
        } catch (IOException | RuntimeException error) {
            Log.w("VitalityNotifications", "Cannot read notification preferences", error);
            return false;
        }
    }

    public static synchronized boolean send(Context context, NotificationEvent event) {
        try {
            Group group = event.type.group;
            NotificationSettings settings = NotificationSettings.read(context);
            if (!settings.checked(NotificationSettings.MASTER)
                    || (group != null && !settings.allows(group))
                    || !systemAllows(context)) return false;

            createChannels(context);
            if (!channelAllows(context, group)) return false;
            SharedPreferences sent = context.getSharedPreferences("sent_health_notifications", Context.MODE_PRIVATE);
            if (sent.contains(event.key)) return true;

            String text = event.type.generalMessage;
            boolean preview = group == null || (group.supportsPreview() && settings.checked(group.previewKey()));
            if (preview && event.detail != null && !event.detail.isEmpty()) {
                text = event.detail;
            }
            String title = group == null ? "Nhắc nhở sức khỏe" : group.title;
            String channel = channel(group);
            Intent open = new Intent(context, MainActivity.class)
                    .setAction(OPEN_ACTION)
                    .setData(new Uri.Builder().scheme("vitality").authority("notification")
                            .appendPath(event.key).build())
                    .putExtra(EXTRA_TYPE, event.type.name())
                    .putExtra(EXTRA_RECORD, event.recordId)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent tap = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_IMMUTABLE);
            Notification publicVersion = new NotificationCompat.Builder(context, channel)
                    .setSmallIcon(R.drawable.ic_health_notice)
                    .setContentTitle("Vitality")
                    .setContentText("Bạn có thông báo sức khỏe mới.")
                    .build();
            Notification notice = new NotificationCompat.Builder(context, channel)
                    .setSmallIcon(R.drawable.ic_health_notice)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                    .setContentIntent(tap)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setPublicVersion(publicVersion)
                    .build();
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return false;
            if (!recordDelivery(sent, event)) {
                sent.edit().remove(event.key).apply();
                return false;
            }
            try {
                manager.notify(TAG_PREFIX + event.key, 0, notice);
            } catch (RuntimeException error) {
                sent.edit().remove(event.key).apply();
                throw error;
            }
            return true;
        } catch (IOException | RuntimeException error) {
            Log.w("VitalityNotifications", "Could not send a health notification", error);
            return false;
        }
    }

    private static boolean recordDelivery(SharedPreferences sent, NotificationEvent event) {
        long now = System.currentTimeMillis();
        SharedPreferences.Editor changes = sent.edit();
        long lastPrune = sent.getLong(LAST_PRUNE, 0);
        if (now - lastPrune >= PRUNE_INTERVAL || now < lastPrune) {
            for (java.util.Map.Entry<String, ?> marker : sent.getAll().entrySet()) {
                if (!LAST_PRUNE.equals(marker.getKey()) && marker.getValue() instanceof Long
                        && (Long) marker.getValue() < now - DELIVERY_RETENTION) changes.remove(marker.getKey());
            }
            changes.putLong(LAST_PRUNE, now);
        }
        // Journal/goal IDs remain valid after edits. Calendar and reminder keys expire with their time window.
        boolean oneTime = event.type == Type.ACTIVITY_SAVED || event.type == Type.SLEEP_FINISHED
                || (event.type == Type.GOAL_REACHED && event.key.indexOf(':') == event.key.lastIndexOf(':'));
        if (oneTime) changes.putBoolean(event.key, true);
        else changes.putLong(event.key, now);
        return changes.commit();
    }

    public static synchronized void cancelVisible(Context context, Group group) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        try {
            if (group == null) {
                manager.cancelAll();
                return;
            }
            for (StatusBarNotification notice : manager.getActiveNotifications()) {
                if (belongsTo(notice.getTag(), group)) manager.cancel(notice.getTag(), notice.getId());
            }
        } catch (RuntimeException error) {
            Log.w("VitalityNotifications", "Could not dismiss notifications", error);
        }
    }

    private static boolean belongsTo(String tag, Group group) {
        if (tag == null || !tag.startsWith(TAG_PREFIX)) return false;
        for (Type type : Type.values()) {
            if (type.group == group && tag.startsWith(TAG_PREFIX + type.name() + ":")) return true;
        }
        return false;
    }

    private static String channel(Group group) {
        return group == null ? REMINDER_CHANNEL : "vitality." + group.key;
    }
}

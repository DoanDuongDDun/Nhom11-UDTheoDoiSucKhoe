package com.example.vitality.notification;

import android.content.Context;
import android.util.AtomicFile;
import com.example.vitality.notification.NotificationEvent.Group;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Device-local choices, stored separately from Android's notification channels. */
public final class NotificationSettings {
    public static final String MASTER = "enabled";
    private static final String FILE_NAME = "notification-settings.properties";
    private final Properties choices;
    private final boolean legacyHabitEnabled;

    private NotificationSettings(Properties choices, boolean legacyHabitEnabled) {
        this.choices = choices;
        this.legacyHabitEnabled = legacyHabitEnabled;
    }

    public boolean checked(String key) {
        String value = choices.getProperty(key);
        if (value != null) return Boolean.parseBoolean(value);
        if (Group.CYCLE.eventKey().equals(key)) return legacyHabitEnabled;
        return !key.endsWith(".preview");
    }

    public boolean allows(Group group) {
        return checked(MASTER) && checked(group.enabledKey()) && checked(group.eventKey());
    }

    public static synchronized NotificationSettings read(Context context) throws IOException {
        AtomicFile file = file(context);
        Properties choices = new Properties();
        try (InputStream in = file.openRead()) {
            choices.load(in);
        } catch (FileNotFoundException missing) {
            if (file.getBaseFile().exists()) throw missing;
        }
        for (String value : choices.stringPropertyNames()) {
            String choice = choices.getProperty(value);
            if (!"true".equals(choice) && !"false".equals(choice)) {
                throw new IOException("Invalid notification setting");
            }
        }
        boolean legacyHabit = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
                .getBoolean("habit", false);
        return new NotificationSettings(choices, legacyHabit);
    }

    public static synchronized NotificationSettings save(Context context, String key, boolean checked)
            throws IOException {
        NotificationSettings settings = read(context);
        settings.choices.setProperty(key, Boolean.toString(checked));
        AtomicFile file = file(context);
        FileOutputStream out = file.startWrite();
        try {
            settings.choices.store(out, null);
        } catch (IOException | RuntimeException error) {
            file.failWrite(out);
            throw error;
        }
        file.finishWrite(out);
        NotificationSettings saved = read(context);
        if (!settings.choices.equals(saved.choices)) throw new IOException("Settings were not saved");
        return saved;
    }

    private static AtomicFile file(Context context) {
        return new AtomicFile(new File(context.getFilesDir(), FILE_NAME));
    }
}

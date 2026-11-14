package com.example.vitality.notification;
import android.app.*;
import android.content.*;
import android.os.Build;
import java.util.*;
public final class ReminderScheduler  {
    public static PendingIntent pending(Context c) {
        return PendingIntent.getBroadcast(c,71,new Intent(c,ReminderReceiver.class).setAction("com.example.vitality.REMIND"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    public static void schedule(Context context) {
        Context c=context.getApplicationContext();
        android.content.SharedPreferences prefs=c.getSharedPreferences("reminders",0);
        AlarmManager manager=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pending(c));
        if(prefs.getBoolean("measure",false)||prefs.getBoolean("water",false)||prefs.getBoolean("bed",false)||prefs.getBoolean("habit",false)||prefs.getBoolean("period",false))manager.setInexactRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+60000,15*60000L,pending(c));
        if(Build.VERSION.SDK_INT>=26) {
            NotificationManager nm=c.getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel("health","Nhắc nhở sức khỏe",NotificationManager.IMPORTANCE_DEFAULT));
        }
    }
}

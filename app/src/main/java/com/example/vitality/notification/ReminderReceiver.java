package com.example.vitality.notification;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import java.util.*;
import com.example.vitality.R;
import com.example.vitality.data.repository.WellnessRepository;
import com.example.vitality.data.entity.HabitOccurrence;
import com.example.vitality.utils.Formats;
public class ReminderReceiver extends BroadcastReceiver  {
    @Override public void onReceive(Context c,Intent i) {
        Context app=c.getApplicationContext();
        if(!"com.example.vitality.REMIND".equals(i.getAction())) {
            ReminderScheduler.schedule(app);
            return;
        }
        PendingResult result=goAsync();
        WellnessRepository.IO.execute(()-> {
            try {
                WellnessRepository repo=new WellnessRepository(app);repo.refreshHabits();if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(app,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;android.content.SharedPreferences p=app.getSharedPreferences("reminders",0);Calendar now=Calendar.getInstance();long time=now.getTimeInMillis();for(String key:new String[] {
                    "measure","water","bed"
                }
                ) {
                    int hour=key.equals("measure")?8:key.equals("water")?10:22;String marker=key+Formats.day(time);if(p.getBoolean(key,false)&&now.get(Calendar.HOUR_OF_DAY)>=hour&&!p.getBoolean(marker,false)) {
                        notify(app,key.hashCode(),key.equals("measure")?"Đã đến lúc cập nhật số đo":key.equals("water")?"Ghi nhận nước uống hôm nay":"Chuẩn bị cho giấc ngủ");p.edit().putBoolean(marker,true).apply();
                    }
                }
                if(p.getBoolean("habit",false))for(HabitOccurrence o:repo.dao.upcoming(time-3600000,time)) {
                    String marker="occurrence_"+o.id;if(!p.getBoolean(marker,false)) {
                        notify(app,(int)o.id,o.title);p.edit().putBoolean(marker,true).apply();
                    }
                }
            }
            finally {
                result.finish();
            }
        }
        );
    }
    private void notify(Context c,int id,String title) {
        PendingIntent open=PendingIntent.getActivity(c,0,new Intent(c,com.example.vitality.MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(id,new NotificationCompat.Builder(c,"health").setSmallIcon(R.drawable.ic_body).setContentTitle("Vitality").setContentText(title).setContentIntent(open).setAutoCancel(true).build());
    }
}

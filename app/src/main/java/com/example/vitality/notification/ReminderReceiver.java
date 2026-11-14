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
import com.example.vitality.utils.PeriodLogic;
public class ReminderReceiver extends BroadcastReceiver  {
    private static final String[] DAILY= {
        "measure","water","bed"
    };
    private static final int[] DAILY_HOUR= {
        8,10,22
    };
    private static final String[] DAILY_TEXT= {
        "Đã đến lúc cập nhật số đo","Ghi nhận nước uống hôm nay","Chuẩn bị cho giấc ngủ"
    };
    private static final long HOUR_MS=3600000L;
    @Override public void onReceive(Context c,Intent i) {
        Context app=c.getApplicationContext();
        if(!"com.example.vitality.REMIND".equals(i.getAction())) {
            ReminderScheduler.schedule(app);
            return;
        }
        PendingResult result=goAsync();
        WellnessRepository.IO.execute(()-> {
            try {
                WellnessRepository repo=new WellnessRepository(app);
                repo.refreshHabits();
                if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(app,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
                SharedPreferences p=app.getSharedPreferences("reminders",0);
                Calendar now=Calendar.getInstance();
                long time=now.getTimeInMillis();
                String today=Formats.day(time);
                // Các cờ "đã nhắc" được ghi gộp một lần và dọn các cờ cũ, để file SharedPreferences không phình dần theo ngày.
                SharedPreferences.Editor edit=p.edit();
                pruneMarkers(p,edit,today,time);
                for(int k=0;k<DAILY.length;k++) {
                    String key=DAILY[k],marker=key+today;
                    if(p.getBoolean(key,false)&&now.get(Calendar.HOUR_OF_DAY)>=DAILY_HOUR[k]&&!p.getBoolean(marker,false)) {
                        notify(app,key.hashCode(),DAILY_TEXT[k]);
                        edit.putBoolean(marker,true);
                    }
                }
                if(p.getBoolean("habit",false))for(HabitOccurrence o:repo.dao.upcoming(time-HOUR_MS,time)) {
                    String marker="occurrence_"+o.id;
                    if(!p.contains(marker)) {
                        notify(app,(int)o.id,o.title);
                        edit.putLong(marker,o.dueAt);
                    }
                }
                if(p.getBoolean("period",false)) {
                    Long next=PeriodLogic.nextStart(repo.periods.all());
                    if(next!=null) {
                        long remindDay=Formats.addDays(next,-2);
                        String marker="period_"+next;
                        if(Formats.dayStart(time)>=remindDay&&Formats.dayStart(time)<=next&&now.get(Calendar.HOUR_OF_DAY)>=8&&!p.getBoolean(marker,false)) {
                            notify(app,72,"K\u1ef3 kinh d\u1ef1 ki\u1ebfn b\u1eaft \u0111\u1ea7u ng\u00e0y "+Formats.day(next));
                            edit.putBoolean(marker,true);
                        }
                    }
                }
                edit.apply();
            }
            finally {
                result.finish();
            }
        }
        );
    }
    /** Xóa cờ nhắc hằng ngày của các ngày trước và cờ của lần sinh hoạt đã quá hạn nhắc (giá trị = giờ đến hạn; cờ kiểu cũ không có giờ thì cũng bỏ). */
    private static void pruneMarkers(SharedPreferences p,SharedPreferences.Editor edit,String today,long time) {
        for(Map.Entry<String,?> entry:p.getAll().entrySet()) {
            String k=entry.getKey();
            if(k.startsWith("occurrence_")) {
                Object v=entry.getValue();
                if(!(v instanceof Long)||(Long)v<time-2*HOUR_MS)edit.remove(k);
                continue;
            }
            if(k.startsWith("period_")) {
                try { if(Long.parseLong(k.substring(7))<Formats.addDays(Formats.dayStart(time),-7))edit.remove(k); }
                catch(NumberFormatException e) { edit.remove(k); }
                continue;
            }
            for(String key:DAILY)if(k.length()>key.length()&&k.startsWith(key)&&!k.endsWith(today))edit.remove(k);
        }
    }
    private void notify(Context c,int id,String title) {
        PendingIntent open=PendingIntent.getActivity(c,0,new Intent(c,com.example.vitality.MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(id,new NotificationCompat.Builder(c,"health").setSmallIcon(R.drawable.ic_body).setContentTitle("Vitality").setContentText(title).setContentIntent(open).setAutoCancel(true).build());
    }
}

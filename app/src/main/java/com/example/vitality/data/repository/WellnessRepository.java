package com.example.vitality.data.repository;
import android.content.Context;
import com.example.vitality.data.database.VitalityDatabase;
import com.example.vitality.data.dao.WellnessDao;
import com.example.vitality.data.entity.*;
import com.example.vitality.utils.*;
import java.util.*;
import java.util.concurrent.*;
public class WellnessRepository  {
    public static final ExecutorService IO=Executors.newSingleThreadExecutor();
    public final WellnessDao dao;
    private final VitalityDatabase db;
    public WellnessRepository(Context c) {
        db=VitalityDatabase.get(c);
        dao=db.wellness();
    }
    public void saveJournal(JournalEntry e) {
        db.runInTransaction(()-> {
            if(e.kind.equals("sleep")) {
                if(e.endedAt!=null)HealthMath.sleepMinutes(e.startedAt,e.endedAt,e.awakeMinutes); for(JournalEntry other:dao.sleeps())if(other.id!=e.id) {
                    if(e.endedAt==null&&other.endedAt==null)throw new IllegalArgumentException("Đã có một phiên ngủ đang hoạt động");if(HealthMath.overlaps(e.startedAt,e.endedAt==null?Long.MAX_VALUE:e.endedAt,other.startedAt,other.endedAt==null?Long.MAX_VALUE:other.endedAt))throw new IllegalArgumentException("Khoảng ngủ trùng với phiên đã lưu");
                }
            }
            if(e.id==0)dao.insert(e);else dao.update(e);
        }
        );
    }
    public void refreshHabits() {
        db.runInTransaction(()-> {
            long now=System.currentTimeMillis();for(HabitSchedule h:dao.allHabits())generate(h,now,now+8*86400000L);dao.markMissed(now);
        }
        );
    }
    private void generate(HabitSchedule h,long now,long until) {
        if(h.paused||h.deleted)return;
        Calendar c=Calendar.getInstance();
        c.setTimeInMillis(Math.max(h.startsAt,h.generatedThrough));
        c.set(Calendar.HOUR_OF_DAY,h.hour);
        c.set(Calendar.MINUTE,h.minute);
        c.set(Calendar.SECOND,0);
        c.set(Calendar.MILLISECOND,0);
        while(c.getTimeInMillis()<=until) {
            long t=c.getTimeInMillis();
            int day=(c.get(Calendar.DAY_OF_WEEK)+5)%7;
            if(t>=h.startsAt&&t>h.generatedThrough&&(h.endsAt==null||t<=h.endsAt)&&(h.weekdays&(1<<day))!=0) {
                HabitOccurrence o=new HabitOccurrence();
                o.scheduleId=h.id;
                o.title=h.title;
                o.dueAt=t;
                dao.insert(o);
            }
            c.add(Calendar.DATE,1);
        }
        h.generatedThrough=until;
        dao.update(h);
    }
    public void saveHabit(HabitSchedule h) {
        db.runInTransaction(()-> {
            long now=System.currentTimeMillis();if(h.id==0)h.id=dao.insert(h);else {
                dao.removeFuture(h.id,now);h.generatedThrough=now;dao.update(h);
            }
            generate(h,now,now+8*86400000L);
        }
        );
    }
}

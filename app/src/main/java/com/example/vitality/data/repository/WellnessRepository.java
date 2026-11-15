package com.example.vitality.data.repository;
import android.content.Context;
import com.example.vitality.data.database.VitalityDatabase;
import com.example.vitality.data.dao.WellnessDao;
import com.example.vitality.data.entity.*;
import com.example.vitality.notification.HealthNotificationEvents;
import com.example.vitality.utils.*;
import java.util.*;
import java.util.concurrent.*;
public class WellnessRepository  {
    public static final ExecutorService IO=Executors.newSingleThreadExecutor();
    /** Sinh trước các lần thực hiện của 8 ngày tới; chỉ sinh lại khi phần đã sinh cách mốc mới ≥ 1 giờ. */
    private static final long LOOKAHEAD_MS=8*86400000L,REGENERATE_GAP_MS=3600000L;
    public final WellnessDao dao;
    public final com.example.vitality.data.dao.PeriodDao periods;
    private final VitalityDatabase db;
    private final Context app;
    public WellnessRepository(Context c) {
        app=c.getApplicationContext();
        db=VitalityDatabase.get(c);
        dao=db.wellness();periods=db.periods();
    }
    public void saveJournal(JournalEntry e) {
        JournalEntry[] before={null};
        long[] savedId={e.id};
        db.runInTransaction(()-> {
            if(e.kind.equals("sleep")) {
                if(e.endedAt!=null)HealthMath.sleepMinutes(e.startedAt,e.endedAt,e.awakeMinutes);
                if(e.endedAt==null&&dao.activeSleepsExcept(e.id)>0)throw new IllegalArgumentException("Đã có một phiên ngủ đang hoạt động");
                if(dao.overlappingSleeps(e.id,e.startedAt,e.endedAt==null?Long.MAX_VALUE:e.endedAt)>0)throw new IllegalArgumentException("Khoảng ngủ trùng với phiên đã lưu");
            }
            if(e.id==0)savedId[0]=dao.insert(e);
            else {
                before[0]=dao.journal(e.id);
                dao.update(e);
            }
        }
        );
        if(e.id==0||before[0]!=null)HealthNotificationEvents.journalSaved(app,db,e,savedId[0],before[0]);
    }
    public void savePeriod(PeriodEntry e) {
        db.runInTransaction(() -> {
            long today=Formats.dayStart(System.currentTimeMillis());
            if(e.startDay>e.endDay||e.startDay>today||e.endDay>today)throw new IllegalArgumentException("Ngày ghi không hợp lệ");
            if(periods.overlaps(e.id,e.startDay,e.endDay)>0)throw new IllegalArgumentException("Kỳ kinh trùng với bản ghi đã có");
            if(e.id==0)periods.insert(e);else periods.update(e);
        });
    }
    public void refreshHabits() {
        db.runInTransaction(()-> {
            long now=System.currentTimeMillis(),until=now+LOOKAHEAD_MS;
            for(HabitSchedule h:dao.allHabits())if(until-h.generatedThrough>=REGENERATE_GAP_MS)generate(h,now,until);
            dao.markMissed(now);
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
            generate(h,now,now+LOOKAHEAD_MS);
        }
        );
    }
}

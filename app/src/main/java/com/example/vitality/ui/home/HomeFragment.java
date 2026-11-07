package com.example.vitality.ui.home;
import android.os.Bundle;
import android.view.View;
import java.util.*;
import com.example.vitality.R;
import com.example.vitality.ui.main.PageFragment;
import com.example.vitality.graphics.*;
import com.example.vitality.data.entity.*;
import com.example.vitality.utils.*;
public class HomeFragment extends PageFragment  {
    @Override public void onViewCreated(View v,Bundle b) {
        vm.measurements.observe(getViewLifecycleOwner(),x->render());
        vm.journals.observe(getViewLifecycleOwner(),x->render());
        vm.profile.observe(getViewLifecycleOwner(),x->render());
        vm.occurrences.observe(getViewLifecycleOwner(),x->render());
    }
    private void render() {
        clear();
        UserProfile p=vm.profile.getValue();
        heading("Xin chào"+(p==null||p.name.isEmpty()?"!":", "+p.name),"Vitality • "+Formats.day(System.currentTimeMillis()));
        BodyShapeView body=widget(R.layout.widget_body);
        List<BodyMeasurement> rows=vm.measurements.getValue();
        if(rows==null||rows.isEmpty())text("Hình minh họa mặc định. Nhập số đo để xem mô hình của bạn.");
        else {
            BodyMeasurement m=rows.get(0);
            body.setMeasurement(m);
            text("Cập nhật: "+Formats.date(m.measuredAt));
            text(Formats.number(m.weightKg)+" kg   •   "+Formats.number(m.heightCm)+" cm   •   BMI "+Formats.number(HealthMath.bmi(m.weightKg,m.heightCm)));
            if(rows.size()>1) {
                BodyMeasurement old=rows.get(1);
                text("Cân nặng: "+Formats.delta(old.weightKg,m.weightKg)+"\nVòng eo: "+Formats.delta(old.waistCm,m.waistCm));
            }
            text(getString(R.string.illustration));
        }
        button("Cập nhật số đo",()->editor(0));
        button("So sánh cơ thể",()->navigate(R.id.comparison));
        double water=0,activity=0,sleep=0;
        long today=Formats.dayStart(System.currentTimeMillis());
        List<JournalEntry> journal=vm.journals.getValue();
        if(journal!=null)for(JournalEntry e:journal) {
            if(e.startedAt>=today) {
                if(e.kind.equals("water"))water+=e.amount;
                if(e.kind.equals("activity"))activity+=e.amount;
            }
            if(e.kind.equals("sleep")&&e.endedAt!=null&&e.endedAt>=today)sleep+=HealthMath.sleepMinutes(e.startedAt,e.endedAt,e.awakeMinutes)/60;
        }
        text("Hôm nay\nNước: "+Formats.number(water)+" / "+Formats.number(p==null?2000:p.waterGoal)+" ml\nVận động: "+Formats.number(activity)+" phút\nNgủ (ngày thức dậy): "+Formats.number(sleep)+" giờ");
        List<HabitOccurrence> occurrences=vm.occurrences.getValue();
        if(occurrences!=null) {
            int count=0,done=0;
            for(HabitOccurrence o:occurrences)if(o.dueAt>=today&&o.dueAt<today+86400000L) {
                count++;
                if(o.status.equals("done"))done++;
            }
            text("Lịch sinh hoạt hôm nay: "+done+" / "+count+" đã hoàn thành");
        }
    }
}

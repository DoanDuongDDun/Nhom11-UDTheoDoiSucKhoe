package com.example.vitality.ui.statistics;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import com.example.vitality.R;
import com.example.vitality.ui.main.PageFragment;
import com.example.vitality.data.entity.*;
import com.example.vitality.graphics.TrendView;
import com.example.vitality.utils.*;
public class StatisticsFragment extends PageFragment  {
    private int days=30;
    private long from=0,to=Long.MAX_VALUE;
    private List<BodyMeasurement> exportRows=new ArrayList<>();
    private final ActivityResultLauncher<String> exporter=registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"),uri-> {
        if(uri==null)return;android.content.ContentResolver resolver=requireContext().getContentResolver();List<BodyMeasurement> snapshot=new ArrayList<>(exportRows);vm.run(()-> {
            try(OutputStream out=resolver.openOutputStream(uri,"wt")) {
                if(out==null)throw new IOException("Không mở được file");try(Writer w=new OutputStreamWriter(out,StandardCharsets.UTF_8)) {
                    CsvExporter.write(w,snapshot);
                }
            }
            catch(IOException e) {
                throw new IllegalStateException("Xuất CSV thất bại: "+e.getMessage(),e);
            }
        }
        ,true);
    }
    );
    @Override public void onViewCreated(View v,Bundle b) {
        if(b!=null) {
            days=b.getInt("days",30);
            from=b.getLong("from");
            to=b.getLong("to",Long.MAX_VALUE);
        }
        heading("Thống kê","Chấm là dữ liệu đã ghi nhận; đường nối thể hiện xu hướng giữa các mốc.");
        vm.measurements.observe(getViewLifecycleOwner(),x->render());
        vm.journals.observe(getViewLifecycleOwner(),x->render());
        vm.occurrences.observe(getViewLifecycleOwner(),x->render());
    }
    private boolean includes(long t) {
        return days==0?t>=from&&t<=to:t>=System.currentTimeMillis()-days*86400000L&&t<=System.currentTimeMillis();
    }
    private void render() {
        clear();
        button("Khoảng xem: "+(days==0?Formats.day(from)+" – "+Formats.day(to):days+" ngày"),()->new AlertDialog.Builder(requireContext()).setItems(new String[] {
            "7 ngày","30 ngày","90 ngày","Khoảng tùy chọn"
        }
        ,(d,w)-> {
            if(w<3) {
                days=new int[] {
                    7,30,90
                }
                [w];render();
            }
            else pickRange();
        }
        ).show());
        exportRows=new ArrayList<>();
        List<BodyMeasurement> body=vm.measurements.getValue();
        if(body!=null)for(BodyMeasurement m:body)if(includes(m.measuredAt))exportRows.add(m);
        button("Xuất số đo trong khoảng này (CSV)",()-> {
            if(exportRows.isEmpty())vm.message.setValue("Chưa có số đo để xuất");else exporter.launch("Vitality-so-do.csv");
        }
        );
        for(int metric=0;metric<2;metric++) {
            text(metric==0?"Cân nặng (kg)":"Vòng eo (cm)");
            List<Long> t=new ArrayList<>();
            List<Double> val=new ArrayList<>();
            for(BodyMeasurement m:exportRows) {
                t.add(m.measuredAt);
                val.add(metric==0?m.weightKg:m.waistCm);
            }
            TrendView chart=widget(R.layout.widget_chart);
            chart.setData(t,val);
        }
        List<JournalEntry> journal=vm.journals.getValue();
        for(String kind:new String[] {
            "water","activity","sleep"
        }
        ) {
            text(kind.equals("water")?"Nước uống (ml/ngày)":kind.equals("activity")?"Vận động (phút/ngày)":"Giấc ngủ (giờ/ngày thức dậy)");
            TreeMap<Long,Double> sums=new TreeMap<>();
            if(journal!=null)for(JournalEntry e:journal)if(e.kind.equals(kind)) {
                long time=kind.equals("sleep")?(e.endedAt==null?0:e.endedAt):e.startedAt;
                if(time==0||!includes(time))continue;
                long day=Formats.dayStart(time);
                double value=kind.equals("sleep")?HealthMath.sleepMinutes(e.startedAt,e.endedAt,e.awakeMinutes)/60:e.amount;
                sums.put(day,sums.getOrDefault(day,0d)+value);
            }
            TrendView chart=widget(R.layout.widget_chart);
            chart.setData(new ArrayList<>(sums.keySet()),new ArrayList<>(sums.values()));
            if(!sums.isEmpty()) {
                double avg=0;
                for(double n:sums.values())avg+=n;
                text("Trung bình trên ngày có ghi nhận: "+Formats.number(avg/sums.size()));
                if(kind.equals("sleep")&&vm.profile.getValue()!=null)text("So với mục tiêu ngủ: "+Formats.number(avg/sums.size()-vm.profile.getValue().sleepGoal)+" giờ/ngày");
            }
        }
        text("Tỷ lệ hoàn thành lịch sinh hoạt (%)");
        TreeMap<Long,int[]> counts=new TreeMap<>();
        List<HabitOccurrence> occurrences=vm.occurrences.getValue();
        if(occurrences!=null)for(HabitOccurrence o:occurrences)if(o.dueAt<=System.currentTimeMillis()&&includes(o.dueAt)) {
            int[] c=counts.computeIfAbsent(Formats.dayStart(o.dueAt),x->new int[2]);
            c[1]++;
            if(o.status.equals("done"))c[0]++;
        }
        List<Long> t=new ArrayList<>();
        List<Double> vals=new ArrayList<>();
        for(Map.Entry<Long,int[]> e:counts.entrySet()) {
            t.add(e.getKey());
            vals.add(HealthMath.completion(e.getValue()[0],e.getValue()[1]));
        }
        TrendView chart=widget(R.layout.widget_chart);
        chart.setData(t,vals);
    }
    private void pickRange() {
        Calendar c=Calendar.getInstance();
        new DatePickerDialogCompat(requireContext(),(y,m,d)-> {
            Calendar start=Calendar.getInstance();start.set(y,m,d,0,0,0);start.set(Calendar.MILLISECOND,0);new DatePickerDialogCompat(requireContext(),(yy,mm,dd)-> {
                Calendar end=Calendar.getInstance();end.set(yy,mm,dd,23,59,59);if(end.before(start)) {
                    vm.message.setValue("Ngày kết thúc phải sau ngày bắt đầu");return;
                }
                from=start.getTimeInMillis();to=end.getTimeInMillis();days=0;render();
            }
            ,c).show();
        }
        ,c).show();
    }
    private static class DatePickerDialogCompat extends android.app.DatePickerDialog {
        interface Pick {
            void on(int y,int m,int d);
        }
        DatePickerDialogCompat(android.content.Context c,Pick p,Calendar t) {
            super(c,(v,y,m,d)->p.on(y,m,d),t.get(Calendar.YEAR),t.get(Calendar.MONTH),t.get(Calendar.DAY_OF_MONTH));
        }
    }
    @Override public void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        b.putInt("days",days);
        b.putLong("from",from);
        b.putLong("to",to);
    }
}

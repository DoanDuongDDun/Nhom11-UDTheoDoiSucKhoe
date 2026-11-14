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
import com.example.vitality.databinding.*;
import android.view.ViewGroup;
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
        heading("Báo cáo","Theo dõi xu hướng sức khỏe của bạn");
        observeAll(this::render,vm.measurements,vm.journals,vm.occurrences,vm.profile);
    }
    private boolean includes(long t) {
        if(days==0)return t>=from&&t<=to;
        long now=System.currentTimeMillis();
        return t>=now-days*Formats.DAY_MS&&t<=now;
    }
    /** Tổng theo ngày của một loại nhật ký trong khoảng đang xem (giấc ngủ tính theo ngày thức dậy). */
    private TreeMap<Long,Double> dailySums(String kind) {
        TreeMap<Long,Double> sums=new TreeMap<>();
        List<JournalEntry> journal=vm.journals.getValue();
        if(journal!=null)for(JournalEntry e:journal)if(e.kind.equals(kind)) {
            long time=kind.equals("sleep")?(e.endedAt==null?0:e.endedAt):e.startedAt;
            if(time==0||!includes(time))continue;
            long day=Formats.dayStart(time);
            double value=kind.equals("sleep")?HealthMath.sleepMinutesOrZero(e.startedAt,e.endedAt,e.awakeMinutes)/60:e.amount;
            sums.merge(day,value,Double::sum);
        }
        return sums;
    }
    private static double average(Map<Long,Double> sums) {
        if(sums.isEmpty())return Double.NaN;
        double total=0;
        for(double v:sums.values())total+=v;
        return total/sums.size();
    }
    private void tile(LinearLayout row,int icon,int iconBg,int tint,String label,String value,String unit,String badge) {
        ItemReportTileBinding t=ItemReportTileBinding.inflate(getLayoutInflater(),row,false);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1f);
        lp.setMargins(dp(4),0,dp(4),0);
        row.addView(t.getRoot(),lp);
        t.rtIcon.setImageResource(icon);
        t.rtIcon.setColorFilter(tint);
        t.rtIconBox.setBackgroundResource(iconBg);
        t.rtLabel.setText(label);
        t.rtValue.setText(value);
        t.rtUnit.setText(unit);
        t.rtBadge.setText(badge);
    }
    private LinearLayout tileRow() {
        LinearLayout r=new LinearLayout(requireContext());
        r.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(-dp(4),dp(4),-dp(4),dp(4));
        binding.content.addView(r,lp);
        return r;
    }
    private void goal(WidgetCardBinding card,String name,double percent) {
        ItemGoalLineBinding g=ItemGoalLineBinding.inflate(getLayoutInflater(),card.cardBody,false);
        card.cardBody.addView(g.getRoot());
        g.goalName.setText(name);
        boolean none=Double.isNaN(percent);
        g.goalBar.setProgress(none?0:(int)Math.max(0,Math.min(100,Math.round(percent))));
        g.goalPct.setText(none?"–":Math.round(percent)+"%");
    }
    private void chartCard(String title,List<Long> t,List<Double> v,String caption) {
        card("XU HƯỚNG",title,null,null);
        TrendView chart=widget(R.layout.widget_trend);
        chart.setData(t,v);
        if(caption!=null) {
            TextView c=text(caption);
            c.setTextSize(12);
            c.setTextColor(0xFF718096);
        }
        endCard();
    }
    private void render() {
        if(binding==null)return;
        clear();
        UserProfile profile=vm.profile.getValue();
        double waterGoal=profile==null?2000:profile.waterGoal,activityGoal=profile==null?30:profile.activityGoal,sleepGoal=profile==null?8:profile.sleepGoal;
        // Tab khoảng thời gian
        WidgetRangeTabsBinding tabs=WidgetRangeTabsBinding.inflate(getLayoutInflater(),binding.content,false);
        binding.content.addView(tabs.getRoot());
        TextView[] all= {
            tabs.tab7,tabs.tab30,tabs.tab90,tabs.tabCustom
        }
        ;
        int[] values= {
            7,30,90,0
        }
        ;
        for(int i=0;i<all.length;i++) {
            boolean active=days==values[i];
            all[i].setBackgroundResource(active?R.drawable.bg_range_active:0);
            all[i].setTextColor(active?0xFF205F4B:0xFF78857E);
            int value=values[i];
            all[i].setOnClickListener(x-> {
                if(value==0)pickRange();else {
                    days=value;render();
                }
            }
            );
        }
        if(days==0)text("Khoảng đang xem: "+Formats.day(from)+" – "+Formats.day(to));
        exportRows=new ArrayList<>();
        List<BodyMeasurement> body=vm.measurements.getValue();
        if(body!=null)for(BodyMeasurement m:body)if(includes(m.measuredAt))exportRows.add(m);
        TreeMap<Long,Double> water=dailySums("water"),activity=dailySums("activity"),sleep=dailySums("sleep");
        double waterAvg=average(water),activityAvg=average(activity),sleepAvg=average(sleep);
        // Bốn ô chỉ số
        LinearLayout row1=tileRow(),row2=tileRow();
        String weightValue="–",weightBadge="Cần ít nhất 2 lần đo";
        if(exportRows.size()>=2) {
            double d=exportRows.get(0).weightKg-exportRows.get(exportRows.size()-1).weightKg;
            weightValue=Math.abs(d)<0.005?"0":(d>0?"+":"−")+Formats.number(Math.abs(d));
            weightBadge=exportRows.size()+" lần đo";
        }
        else if(exportRows.size()==1)weightBadge="1 lần đo";
        tile(row1,R.drawable.ic_scale,R.drawable.bg_icon_peach,0xFFDF7160,"Cân nặng",weightValue,"kg",weightBadge);
        tile(row1,R.drawable.ic_drop,R.drawable.bg_icon_blue,0xFF3785DC,"Nước TB/ngày",Double.isNaN(waterAvg)?"–":Formats.number(waterAvg),"ml",Double.isNaN(waterAvg)?"Chưa có dữ liệu":Math.round(waterAvg/waterGoal*100)+"% mục tiêu");
        tile(row2,R.drawable.ic_shoe,R.drawable.bg_icon_mint,0xFF1D9367,"Vận động TB",Double.isNaN(activityAvg)?"–":Formats.number(activityAvg),"phút",Double.isNaN(activityAvg)?"Chưa có dữ liệu":Math.round(activityAvg/activityGoal*100)+"% mục tiêu");
        tile(row2,R.drawable.ic_moon,R.drawable.bg_icon_violet,0xFF7360DD,"Giấc ngủ TB",Double.isNaN(sleepAvg)?"–":Formats.number(sleepAvg),"giờ",Double.isNaN(sleepAvg)?"Chưa có dữ liệu":sleepAvg>=sleepGoal?"Đạt mục tiêu":"Thiếu "+Formats.number(sleepGoal-sleepAvg)+" giờ");
        // Biểu đồ cân nặng
        List<Long> t=new ArrayList<>();
        List<Double> val=new ArrayList<>();
        for(BodyMeasurement m:exportRows) {
            t.add(m.measuredAt);
            val.add(m.weightKg);
        }
        chartCard("Cân nặng (kg)",t,val,null);
        // Mức độ hoàn thành mục tiêu
        TreeMap<Long,int[]> counts=new TreeMap<>();
        int done=0,due=0;
        List<HabitOccurrence> occurrences=vm.occurrences.getValue();
        if(occurrences!=null)for(HabitOccurrence o:occurrences)if(o.dueAt<=System.currentTimeMillis()&&includes(o.dueAt)) {
            int[] c=counts.computeIfAbsent(Formats.dayStart(o.dueAt),x->new int[2]);
            c[1]++;
            due++;
            if(o.status.equals("done")) {
                c[0]++;
                done++;
            }
        }
        WidgetCardBinding goals=card("MỤC TIÊU HẰNG NGÀY","Mức độ hoàn thành",null,null);
        endCard();
        goal(goals,"Nước uống",Double.isNaN(waterAvg)?Double.NaN:waterAvg/waterGoal*100);
        goal(goals,"Vận động",Double.isNaN(activityAvg)?Double.NaN:activityAvg/activityGoal*100);
        goal(goals,"Giấc ngủ",Double.isNaN(sleepAvg)?Double.NaN:sleepAvg/sleepGoal*100);
        goal(goals,"Lịch sinh hoạt",due==0?Double.NaN:HealthMath.completion(done,due));
        // Các biểu đồ còn lại
        t=new ArrayList<>();
        val=new ArrayList<>();
        for(BodyMeasurement m:exportRows) {
            t.add(m.measuredAt);
            val.add(m.waistCm);
        }
        chartCard("Vòng eo (cm)",t,val,null);
        chartCard("Nước uống (ml/ngày)",new ArrayList<>(water.keySet()),new ArrayList<>(water.values()),Double.isNaN(waterAvg)?null:"Trung bình trên ngày có ghi nhận: "+Formats.number(waterAvg));
        chartCard("Vận động (phút/ngày)",new ArrayList<>(activity.keySet()),new ArrayList<>(activity.values()),Double.isNaN(activityAvg)?null:"Trung bình trên ngày có ghi nhận: "+Formats.number(activityAvg));
        chartCard("Giấc ngủ (giờ/ngày thức dậy)",new ArrayList<>(sleep.keySet()),new ArrayList<>(sleep.values()),Double.isNaN(sleepAvg)?null:"Trung bình: "+Formats.number(sleepAvg)+" giờ • so với mục tiêu: "+Formats.number(sleepAvg-sleepGoal)+" giờ/ngày");
        t=new ArrayList<>();
        val=new ArrayList<>();
        for(Map.Entry<Long,int[]> e:counts.entrySet()) {
            t.add(e.getKey());
            val.add(HealthMath.completion(e.getValue()[0],e.getValue()[1]));
        }
        chartCard("Tỷ lệ hoàn thành lịch sinh hoạt (%)",t,val,null);
        button("Xuất số đo trong khoảng này (CSV)",()-> {
            if(exportRows.isEmpty())vm.message.setValue("Chưa có số đo để xuất");else exporter.launch("Vitality-so-do.csv");
        }
        );
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

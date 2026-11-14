package com.example.vitality.ui.home;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import java.util.*;
import com.example.vitality.R;
import com.example.vitality.databinding.*;
import com.example.vitality.ui.main.PageFragment;
import com.example.vitality.graphics.*;
import com.example.vitality.data.entity.*;
import com.example.vitality.utils.*;
/** Màn hình Tổng quan theo phong cách Health Checker: dữ liệu thật của người dùng, bố cục lấy từ bản demo Figma. */
public class HomeFragment extends PageFragment  {
    private static final String[] DAYS= {
        "CHỦ NHẬT","THỨ HAI","THỨ BA","THỨ TƯ","THỨ NĂM","THỨ SÁU","THỨ BẢY"
    }
    ;
    private static final int WHITE_CARD=0,SLEEP_CARD=1,CALORIE_CARD=2;
    private static final int GLASS_ML=250,MEAL_TARGET=3;
    /** Số đo đã được hoạt họa lên mô hình: render lại vì lý do khác (thêm nước...) không chạy lại hoạt họa từ hình mặc định. */
    private BodyMeasurement shownBody;
    @Override public void onViewCreated(View v,Bundle b) {
        observeAll(this::render,vm.measurements,vm.journals,vm.profile,vm.occurrences,vm.symptoms);
    }
    @Override public void onDestroyView() {
        super.onDestroyView();
        shownBody=null;
    }
    private void render() {
        if(binding==null)return;
        clear();
        binding.pageHeader.setVisibility(View.GONE);
        UserProfile p=vm.profile.getValue();
        long now=System.currentTimeMillis(),today=Formats.dayStart(now);
        double waterGoal=p==null?2000:p.waterGoal,activityGoal=p==null?30:p.activityGoal,sleepGoal=p==null?8:p.sleepGoal;
        // Dữ liệu hôm nay
        double water=0,activity=0,sleepMin=0,calories=0;
        int meals=0;
        List<JournalEntry> journal=vm.journals.getValue();
        if(journal!=null)for(JournalEntry e:journal) {
            if(e.startedAt>=today) {
                if(e.kind.equals("water"))water+=e.amount;
                if(e.kind.equals("activity"))activity+=e.amount;
                if(e.kind.equals("nutrition")) {
                    meals++;
                    if(e.calories!=null)calories+=e.calories;
                }
            }
            if(e.kind.equals("sleep")&&e.endedAt!=null&&e.endedAt>=today)sleepMin+=HealthMath.sleepMinutesOrZero(e.startedAt,e.endedAt,e.awakeMinutes);
        }
        int habitCount=0,habitDone=0;
        List<HabitOccurrence> occurrences=vm.occurrences.getValue();
        if(occurrences!=null)for(HabitOccurrence o:occurrences)if(o.dueAt>=today&&o.dueAt<today+Formats.DAY_MS) {
            habitCount++;
            if(o.status.equals("done"))habitDone++;
        }
        // Đầu trang
        WidgetHomeHeaderBinding head=WidgetHomeHeaderBinding.inflate(getLayoutInflater(),binding.content,false);
        binding.content.addView(head.getRoot());
        String name=p==null?"":p.name.trim();
        head.tagline.setText(name.isEmpty()?getString(R.string.tagline):"Xin chào, "+name);
        head.avatar.setText(Formats.initials(name));
        head.avatar.setContentDescription("Hồ sơ của bạn");
        head.avatar.setOnClickListener(x->navigate(R.id.profile));
        // Danh mục
        WidgetHomeCategoriesBinding cats=WidgetHomeCategoriesBinding.inflate(getLayoutInflater(),binding.content,false);
        binding.content.addView(cats.getRoot());
        cats.catActivity.setOnClickListener(x->navigate(R.id.activity));
        cats.catSleep.setOnClickListener(x->navigate(R.id.sleep));
        cats.catNutrition.setOnClickListener(x->navigate(R.id.nutrition));
        // Thẻ tiến độ hôm nay
        double[] ratios=new double[4];
        int n=0;
        if(waterGoal>0)ratios[n++]=water/waterGoal;
        if(activityGoal>0)ratios[n++]=activity/activityGoal;
        if(sleepGoal>0)ratios[n++]=sleepMin/60/sleepGoal;
        if(habitCount>0)ratios[n++]=habitDone/(double)habitCount;
        int score=HealthMath.progressScore(Arrays.copyOf(ratios,n));
        boolean noData=water==0&&activity==0&&sleepMin==0&&meals==0&&habitDone==0;
        WidgetHomeEnergyBinding energy=WidgetHomeEnergyBinding.inflate(getLayoutInflater(),binding.content,false);
        binding.content.addView(energy.getRoot());
        energy.energyScore.setText(String.valueOf(score));
        energy.energyStatus.setText(noData?"Chưa có dữ liệu hôm nay · Hãy ghi nhận chỉ số đầu tiên":score>=75?"Tốt · Bạn đang đi đúng hướng":score>=50?"Khá · Gần đạt mục tiêu hôm nay":"Cần cố gắng · Hãy bổ sung nước, vận động và giấc ngủ");
        // Lưới chỉ số
        Calendar c=Calendar.getInstance();
        sectionHeading(DAYS[c.get(Calendar.DAY_OF_WEEK)-1]+", "+c.get(Calendar.DAY_OF_MONTH)+" THÁNG "+(c.get(Calendar.MONTH)+1),"Chỉ số của bạn","Xem tất cả",()->navigate(R.id.statistics));
        WidgetHomeGridBinding grid=WidgetHomeGridBinding.inflate(getLayoutInflater(),binding.content,false);
        binding.content.addView(grid.getRoot());
        LinearLayout row1=row(grid.metricGrid),row2=row(grid.metricGrid),row3=row(grid.metricGrid);
        int activityPct=activityGoal>0?(int)Math.min(100,Math.round(activity/activityGoal*100)):0;
        CardMetricBinding act=metric(row1,R.drawable.ic_shoe,R.drawable.bg_icon_mint,0xFF1D9367,"Vận động",Formats.number(activity),"phút",activityPct+"% mục tiêu "+Formats.number(activityGoal)+" phút",WHITE_CARD,()->navigate(R.id.diary));
        act.metricProgress.setVisibility(View.VISIBLE);
        act.metricProgress.setProgress(activityPct);
        int sleepH=(int)(sleepMin/60),sleepM=(int)Math.round(sleepMin%60);
        metric(row1,R.drawable.ic_moon,R.drawable.bg_icon_violet,0xFF7360DD,"Giấc ngủ",String.format(Locale.getDefault(),"%dh %02dp",sleepH,sleepM),"",sleepMin==0?"Chưa ghi nhận hôm nay":sleepMin/60>=sleepGoal?"Đạt mục tiêu ngủ":"Mục tiêu "+Formats.number(sleepGoal)+" giờ",SLEEP_CARD,()->navigate(R.id.sleep));
        metric(row2,R.drawable.ic_flame,R.drawable.bg_icon_orange,0xFFEB791F,"Calo",String.format(Locale.getDefault(),"%,.0f",calories),"kcal","Đã nạp hôm nay (nhập tay)",CALORIE_CARD,()->navigate(R.id.diary));
        int total=Math.max(1,Math.min(12,(int)Math.ceil(waterGoal/GLASS_ML))),drunk=(int)(water/GLASS_ML);
        CardMetricBinding wat=metric(row2,R.drawable.ic_drop,R.drawable.bg_icon_blue,0xFF3785DC,"Nước",String.valueOf(drunk),"/ "+total+" ly",Formats.number(water)+" / "+Formats.number(waterGoal)+" ml · chạm để thêm 250 ml",WHITE_CARD,this::addWater);
        wat.metricPill.setVisibility(View.VISIBLE);
        wat.metricPill.setText("+ 1 ly");
        wat.metricGlasses.setVisibility(View.VISIBLE);
        for(int i=0;i<total;i++) {
            View glass=new View(requireContext());
            LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(0,dp(15),1f);
            gp.setMargins(dp(1),0,dp(1),0);
            glass.setBackgroundResource(i<Math.min(drunk,total)?R.drawable.glass_filled:R.drawable.glass_empty);
            wat.metricGlasses.addView(glass,gp);
        }
        List<BodyMeasurement> rows=vm.measurements.getValue();
        boolean hasBody=rows!=null&&!rows.isEmpty();
        String weightCaption="Nhấn để nhập số đo";
        if(hasBody&&rows.size()>1) {
            double d=rows.get(0).weightKg-rows.get(1).weightKg;
            weightCaption=(d>0?"↑ ":d<0?"↓ ":"= ")+Formats.number(Math.abs(d))+" kg so với lần đo trước";
        }
        else if(hasBody)weightCaption="Lần đo đầu tiên";
        metric(row3,R.drawable.ic_scale,R.drawable.bg_icon_peach,0xFFDF7160,"Cân nặng",hasBody?Formats.number(rows.get(0).weightKg):"–","kg",weightCaption,WHITE_CARD,()->navigate(R.id.body));
        metric(row3,R.drawable.ic_fork,R.drawable.bg_icon_lime,0xFF618E27,"Bữa ăn",String.valueOf(meals),"/ "+MEAL_TARGET+" bữa",meals>=MEAL_TARGET?"Đã ghi đủ các bữa":"Ghi lại bữa ăn tiếp theo",WHITE_CARD,()->navigate(R.id.diary));
        // Thẻ triệu chứng
        WidgetHomeSymptomBinding sym=WidgetHomeSymptomBinding.inflate(getLayoutInflater(),binding.content,false);
        binding.content.addView(sym.getRoot());
        int recent=0;
        Symptom latest=null;
        List<Symptom> symptoms=vm.symptoms.getValue();
        if(symptoms!=null)for(Symptom s:symptoms)if(s.occurredAt>=now-7*Formats.DAY_MS&&s.occurredAt<=now) {
            recent++;
            if(latest==null)latest=s;
        }
        sym.symptomCount.setText(String.valueOf(recent));
        sym.symptomCaption.setText(latest==null?"Chưa ghi nhận triệu chứng":"Gần nhất: "+latest.name+" • "+SymptomLogic.severityLabel(latest.severity));
        sym.getRoot().setOnClickListener(x->navigate(R.id.symptom));
        // Cơ thể (giữ nguyên chức năng cũ)
        sectionHeading("CƠ THỂ","Mô hình của bạn","Cập nhật",()->editor(0));
        BodyShapeView body=widget(R.layout.widget_body);
        if(!hasBody) {
            shownBody=null;
            text("Hình minh họa mặc định. Nhập số đo để xem mô hình của bạn.");
        }
        else {
            BodyMeasurement m=rows.get(0);
            boolean unchanged=shownBody!=null&&shownBody.id==m.id&&shownBody.updatedAt==m.updatedAt;
            if(unchanged)body.setInitialMeasurement(m);else body.setMeasurement(m);
            shownBody=m;
            text("Cập nhật: "+Formats.date(m.measuredAt));
            text(Formats.number(m.weightKg)+" kg   •   "+Formats.number(m.heightCm)+" cm   •   BMI "+Formats.number(HealthMath.bmi(m.weightKg,m.heightCm)));
            if(rows.size()>1) {
                BodyMeasurement old=rows.get(1);
                text("Cân nặng: "+Formats.delta(old.weightKg,m.weightKg)+"\nVòng eo: "+Formats.delta(old.waistCm,m.waistCm));
            }
            String bmiNote=HealthMath.bmiWarning(HealthMath.bmi(m.weightKg,m.heightCm));
            if(bmiNote!=null)text(bmiNote);
            text(getString(R.string.illustration));
        }
        button("Cập nhật số đo",()->editor(0));
        button("So sánh cơ thể",()->navigate(R.id.comparison));
        button("Số đo cơ thể",()->navigate(R.id.body));
        if(habitCount>0)text("Lịch sinh hoạt hôm nay: "+habitDone+" / "+habitCount+" đã hoàn thành");
    }
    private void addWater() {
        JournalEntry e=new JournalEntry();
        e.amount=GLASS_ML;
        vm.run(()->vm.wellness.saveJournal(e),true);
    }
    private void sectionHeading(String kicker,String title,String action,Runnable click) {
        WidgetHomeHeadingBinding h=WidgetHomeHeadingBinding.inflate(getLayoutInflater(),binding.content,false);
        binding.content.addView(h.getRoot());
        h.headingKicker.setText(kicker);
        h.headingTitle.setText(title);
        h.headingAction.setText(action);
        h.headingAction.setOnClickListener(x->click.run());
    }
    private LinearLayout row(LinearLayout grid) {
        LinearLayout r=new LinearLayout(requireContext());
        r.setOrientation(LinearLayout.HORIZONTAL);
        grid.addView(r,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        return r;
    }
    private CardMetricBinding metric(LinearLayout row,int icon,int iconBg,int tint,String label,String value,String unit,String caption,int style,Runnable click) {
        CardMetricBinding m=CardMetricBinding.inflate(getLayoutInflater(),row,false);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1f);
        lp.setMargins(dp(5),dp(5),dp(5),dp(5));
        row.addView(m.getRoot(),lp);
        m.metricIcon.setImageResource(icon);
        m.metricIcon.setColorFilter(tint);
        m.metricIconBox.setBackgroundResource(iconBg);
        m.metricLabel.setText(label);
        m.metricValue.setText(value);
        m.metricUnit.setText(unit);
        m.metricCaption.setText(caption);
        if(style!=WHITE_CARD) {
            m.getRoot().setBackgroundResource(style==SLEEP_CARD?R.drawable.bg_sleep_card:R.drawable.bg_calories_card);
            m.metricLabel.setTextColor(0xCCFFFFFF);
            m.metricValue.setTextColor(0xFFFFFFFF);
            m.metricUnit.setTextColor(0xB3FFFFFF);
            m.metricCaption.setTextColor(0xCCFFFFFF);
        }
        m.getRoot().setOnClickListener(x->click.run());
        return m;
    }
}

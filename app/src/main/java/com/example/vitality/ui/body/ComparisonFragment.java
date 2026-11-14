package com.example.vitality.ui.body;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import java.util.*;
import com.example.vitality.R;
import com.example.vitality.databinding.WidgetStatHeroBinding;
import com.example.vitality.ui.main.PageFragment;
import com.example.vitality.data.entity.*;
import com.example.vitality.graphics.*;
import com.example.vitality.utils.*;
public class ComparisonFragment extends PageFragment  {
    private List<BodyMeasurement> rows;
    private int a=1,b=0;
    private long aid,bid;
    private boolean overlay;
    private BodyComparisonView shapes;
    private TextView table,legend,opacityLabel;
    private SeekBar opacitySlider;
    private android.view.ViewGroup metricContainer;
    private WidgetStatHeroBinding heroView;
    private int opacity=110;
    @Override public void onViewCreated(View v,Bundle state) {
        if(state!=null) {
            aid=state.getLong("a");
            bid=state.getLong("b");
            overlay=state.getBoolean("overlay");
            opacity=state.getInt("opacity",110);
        }
        heading("So sánh giai đoạn","Cùng tỷ lệ hiển thị, căn tại bàn chân");
        vm.measurements.observe(getViewLifecycleOwner(),data-> {
            rows=data;
            shapes=null;table=null;metricContainer=null;
            clear();
            button("Quay lại",this::back);
            if(rows.size()<2) {
                text("Cần ít nhất hai lần đo để so sánh. Hãy thêm số đo mới.");return;
            }
            a=1;b=0;
            for(int i=0;i<rows.size();i++) {
                if(rows.get(i).id==aid)a=i;
                if(rows.get(i).id==bid)b=i;
            }
            card("VÓC DÁNG","So sánh hình",null,null);
            shapes=widget(R.layout.widget_comparison);
            com.google.android.material.button.MaterialButtonToggleGroup modes=widget(R.layout.widget_comparison_mode);
            modes.check(overlay?R.id.mode_overlay:R.id.mode_side);
            modes.addOnButtonCheckedListener((group,id,checked)->{if(checked){overlay=id==R.id.mode_overlay;update();}});
            legend=text("");legend.setTextSize(13);
            opacityLabel=text("Độ đậm hình trước");opacityLabel.setTextSize(13);
            opacitySlider=widget(R.layout.widget_seek);
            opacitySlider.setProgress(opacity);
            opacitySlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar s,int p,boolean user){opacity=p;update();}
                public void onStartTrackingTouch(SeekBar s){}
                public void onStopTrackingTouch(SeekBar s){}
            });
            endCard();
            card("GIAI ĐOẠN","Chọn hai lần đo",null,null);
            text("Mốc thứ nhất").setTextSize(13);
            String[] names=new String[rows.size()];
            for(int i=0;i<rows.size();i++)names[i]=Formats.date(rows.get(i).measuredAt)+" · #"+rows.get(i).id;
            Spinner first=spinner(names,p->{a=p;update();});first.setSelection(a);
            text("Mốc thứ hai").setTextSize(13);
            Spinner second=spinner(names,p->{b=p;update();});second.setSelection(b);
            text("Hai mốc được tự sắp theo thời gian trước → sau.").setTextSize(12);
            endCard();
            heroView=hero(R.drawable.bg_energy,R.drawable.ic_scale,"THAY ĐỔI CÂN NẶNG","--","kg","Chọn hai mốc để so sánh");
            metricContainer=card("CHI TIẾT","Số đo thay đổi",null,null).cardBody;
            table=text("");table.setTextSize(13);
            endCard();
            text("Mô hình minh họa; vùng thiếu số đo tay/chân được ước lượng.").setTextSize(12);
            update();
        }
        );
    }
    private void update() {
        if(shapes==null||table==null||rows==null||rows.size()<2)return;
        opacityLabel.setVisibility(overlay?View.VISIBLE:View.GONE);
        opacitySlider.setVisibility(overlay?View.VISIBLE:View.GONE);
        metricContainer.removeViews(1,metricContainer.getChildCount()-1);
        if(a==b) {
            table.setText("Hãy chọn hai bản ghi khác nhau.");
            if(heroView!=null)updateHero(heroView,"THAY ĐỔI CÂN NẶNG","--","kg","Hãy chọn hai bản ghi khác nhau");
            shapes.setVisibility(View.GONE);
            legend.setText("Chọn hai lần đo khác nhau để xem hình.");
            return;
        }
        shapes.setVisibility(View.VISIBLE);
        BodyMeasurement x=rows.get(a),y=rows.get(b);
        aid=x.id;
        bid=y.id;
        if(x.measuredAt>y.measuredAt||(x.measuredAt==y.measuredAt&&x.id>y.id)) {
            BodyMeasurement t=x;
            x=y;
            y=t;
        }
        shapes.setMeasurements(x,y);
        double weightChange=y.weightKg-x.weightKg;
        if(heroView!=null)updateHero(heroView,"THAY ĐỔI CÂN NẶNG",(weightChange>0?"+":weightChange<0?"−":"")+Formats.number(Math.abs(weightChange)),"kg","Vòng eo "+Formats.delta(x.waistCm,y.waistCm)+" • "+Formats.day(x.measuredAt)+" → "+Formats.day(y.measuredAt));
        shapes.setOverlay(overlay);
        shapes.setOpacity(opacity);
        legend.setText("● Trước · đỏ nhạt: "+Formats.date(x.measuredAt)+"\n● Sau · đỏ đậm: "+Formats.date(y.measuredAt));
        table.setText("Đối chiếu từng chỉ số giữa hai lần đo");
        row("Chiều cao · cm",x.heightCm,y.heightCm);
        row("Cân nặng · kg",x.weightKg,y.weightKg);
        row("Vòng ngực · cm",x.chestCm,y.chestCm);
        row("Vòng eo · cm",x.waistCm,y.waistCm);
        row("Vòng hông · cm",x.hipCm,y.hipCm);
        row("BMI",HealthMath.bmi(x.weightKg,x.heightCm),HealthMath.bmi(y.weightKg,y.heightCm));
        if(x.upperArmCm!=null&&y.upperArmCm!=null)row("Cánh tay · cm",x.upperArmCm,y.upperArmCm);
        if(x.thighCm!=null&&y.thighCm!=null)row("Đùi · cm",x.thighCm,y.thighCm);
        if(x.calfCm!=null&&y.calfCm!=null)row("Bắp chân · cm",x.calfCm,y.calfCm);
    }
    private void row(String name,double x,double y) {
        com.example.vitality.databinding.ItemComparisonMetricBinding item=com.example.vitality.databinding.ItemComparisonMetricBinding.inflate(getLayoutInflater(),metricContainer,false);
        item.metricName.setText(name);
        item.beforeValue.setText("Trước\n"+Formats.number(x));
        item.afterValue.setText("Sau\n"+Formats.number(y));
        double delta=y-x;
        item.changeValue.setText((delta>0?"Tăng ":delta<0?"Giảm ":"Không đổi ")+Formats.number(Math.abs(delta))+"\n"+Formats.number(HealthMath.percent(x,y))+"%");
        metricContainer.addView(item.getRoot());
    }

    @Override public void onSaveInstanceState(Bundle s) {
        super.onSaveInstanceState(s);
        s.putLong("a",aid);
        s.putLong("b",bid);
        s.putBoolean("overlay",overlay);
        s.putInt("opacity",opacity);
    }
    @Override public void onDestroyView() {
        super.onDestroyView();
        shapes=null;
        heroView=null;
        table=null;
        legend=null;opacityLabel=null;opacitySlider=null;metricContainer=null;
    }
}

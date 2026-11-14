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
    private TextView table;
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
            rows=data;clear();heroView=rows.size()>=2?hero(R.drawable.bg_energy,R.drawable.ic_scale,"THAY ĐỔI CÂN NẶNG","--","kg","Chọn hai mốc để so sánh"):null;button("Quay lại",this::back);if(rows.size()<2) {
                text("Cần ít nhất hai lần đo để so sánh. Hãy thêm số đo mới.");return;
            }
            a=1;b=0;for(int i=0;i<rows.size();i++) {
                if(rows.get(i).id==aid)a=i;if(rows.get(i).id==bid)b=i;
            }
            shapes=widget(R.layout.widget_comparison);
            String[] names=new String[rows.size()];for(int i=0;i<rows.size();i++)names[i]=Formats.date(rows.get(i).measuredAt)+" #"+rows.get(i).id;text("Mốc thứ nhất");Spinner first=spinner(names,p-> {
                a=p;update();
            }
            );first.setSelection(a);text("Mốc thứ hai");Spinner second=spinner(names,p-> {
                b=p;update();
            }
            );second.setSelection(b);spinner(new String[] {
                "Đặt cạnh nhau","Chồng hình"
            }
            ,p-> {
                overlay=p==1;update();
            }
            ).setSelection(overlay?1:0);text("Trước: đỏ nhạt • Sau: đỏ đậm. Hai mốc tự sắp theo thời gian.");text("Độ đậm hình trước khi chồng");SeekBar seek=widget(R.layout.widget_seek);seek.setProgress(opacity);seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar s,int p,boolean user) {
                    opacity=p;update();
                }
                public void onStartTrackingTouch(SeekBar s) {
                }
                public void onStopTrackingTouch(SeekBar s) {
                }
            }
            );table=text("");update();
        }
        );
    }
    private void update() {
        if(shapes==null||table==null||rows==null||rows.size()<2)return;
        if(a==b) {
            table.setText("Hãy chọn hai bản ghi khác nhau.");
            if(heroView!=null)updateHero(heroView,"THAY ĐỔI CÂN NẶNG","--","kg","Hãy chọn hai bản ghi khác nhau");
            shapes.setVisibility(View.GONE);
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
        StringBuilder s=new StringBuilder("Trước: "+Formats.date(x.measuredAt)+" #"+x.id+"\nSau: "+Formats.date(y.measuredAt)+" #"+y.id+"\n\n");
        row(s,"Chiều cao (cm)",x.heightCm,y.heightCm);
        row(s,"Cân nặng (kg)",x.weightKg,y.weightKg);
        row(s,"Ngực (cm)",x.chestCm,y.chestCm);
        row(s,"Eo (cm)",x.waistCm,y.waistCm);
        row(s,"Hông (cm)",x.hipCm,y.hipCm);
        row(s,"BMI",HealthMath.bmi(x.weightKg,x.heightCm),HealthMath.bmi(y.weightKg,y.heightCm));
        if(x.upperArmCm!=null&&y.upperArmCm!=null)row(s,"Tay (cm)",x.upperArmCm,y.upperArmCm);
        if(x.thighCm!=null&&y.thighCm!=null)row(s,"Đùi (cm)",x.thighCm,y.thighCm);
        if(x.calfCm!=null&&y.calfCm!=null)row(s,"Bắp chân (cm)",x.calfCm,y.calfCm);
        s.append("\nMô hình minh họa; vùng thiếu số đo tay/chân được ước lượng.");
        table.setText(s);
    }
    private void row(StringBuilder s,String name,double x,double y) {
        s.append(name).append(": ").append(Formats.number(x)).append(" → ").append(Formats.number(y)).append("\n").append(Formats.delta(x,y)).append("\n\n");
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
    }
}

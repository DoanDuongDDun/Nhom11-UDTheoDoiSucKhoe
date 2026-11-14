package com.example.vitality.ui.body;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import java.util.*;
import com.example.vitality.R;
import com.example.vitality.databinding.*;
import com.example.vitality.ui.main.PageFragment;
import com.example.vitality.data.entity.BodyMeasurement;
import com.example.vitality.graphics.TrendView;
import com.example.vitality.utils.*;
public class BodyFragment extends PageFragment  {
    private List<BodyMeasurement> rows=new ArrayList<>();
    private long selected=0;
    private int days=0;
    private long from=0,to=Long.MAX_VALUE;
    private BodyMeasurement previous;
    @Override public void onViewCreated(View v,Bundle b) {
        if(b!=null) {
            selected=b.getLong("selected");
            days=b.getInt("days");
            from=b.getLong("from");
            to=b.getLong("to",Long.MAX_VALUE);
        }
        heading("Cơ thể của bạn","Lưu từng giai đoạn • Nhìn lại sự thay đổi");
        vm.measurements.observe(getViewLifecycleOwner(),data-> {
            rows=data;render();
        }
        );
    }
    private static String longDate(long t) {
        Calendar c=Calendar.getInstance();
        c.setTimeInMillis(t);
        return c.get(Calendar.DAY_OF_MONTH)+" tháng "+(c.get(Calendar.MONTH)+1)+", "+c.get(Calendar.YEAR);
    }
    private static String relative(long t) {
        long days=Math.round((Formats.dayStart(System.currentTimeMillis())-Formats.dayStart(t))/(double)Formats.DAY_MS);
        if(days<=0)return "Hôm nay";
        if(days<7)return days+" ngày trước";
        if(days<30)return (days/7)+" tuần trước";
        return (days/30)+" tháng trước";
    }
    private static String signed(double d,String unit) {
        if(Math.abs(d)<0.005)return "Không đổi";
        return (d>0?"+":"−")+Formats.number(Math.abs(d))+" "+unit;
    }
    private void showFilter() {
        new AlertDialog.Builder(requireContext()).setItems(new String[] {
            "Tất cả","7 ngày","30 ngày","90 ngày","Khoảng tùy chọn"
        }
        ,(d,w)-> {
            if(w==4) {
                pickRange();return;
            }
            days=new int[] {
                0,7,30,90
            }
            [w];render();
        }
        ).show();
    }
    private void render() {
        clear();
        WidgetBodyOverviewBinding o=WidgetBodyOverviewBinding.inflate(getLayoutInflater(),binding.content,false);
        binding.content.addView(o.getRoot());
        BodyMeasurement current=null;
        if(rows.isEmpty()) {
            o.bodyDate.setText("Chưa có số đo");
            o.bodyEdit.setVisibility(View.GONE);
            o.bodyNote.setText("Hình minh họa mặc định, chưa dựa trên dữ liệu cá nhân.");
        }
        else {
            current=rows.get(0);
            for(BodyMeasurement m:rows)if(m.id==selected)current=m;
            selected=current.id;
            BodyMeasurement chosen=current;
            o.bodyKicker.setText(chosen==rows.get(0)?"LẦN ĐO GẦN NHẤT":"LẦN ĐO ĐÃ CHỌN");
            o.bodyDate.setText(longDate(chosen.measuredAt));
            o.bodyEdit.setOnClickListener(v->editor(chosen.id));
            if(previous!=null)o.bodyShape.setInitialMeasurement(previous);
            o.bodyShape.setMeasurement(chosen);
            previous=chosen;
            double bmi=HealthMath.bmi(chosen.weightKg,chosen.heightCm);
            String bmiNote=HealthMath.bmiWarning(bmi);
            o.num1Value.setText(Formats.number(chosen.weightKg));
            o.num2Value.setText(Formats.number(bmi));
            o.num2Label.setText(bmiNote==null?"Trong mức thông thường":"Ngoài mức thông thường");
            o.num3Value.setText(Formats.number(chosen.waistCm));
            StringBuilder note=new StringBuilder(getString(R.string.illustration));
            if(chosen.upperArmCm==null||chosen.thighCm==null||chosen.calfCm==null)note.append("\nƯớc lượng: ").append(chosen.upperArmCm==null?"cánh tay ":"").append(chosen.thighCm==null?"đùi ":"").append(chosen.calfCm==null?"bắp chân":"");
            if(bmiNote!=null)note.append("\n").append(bmiNote);
            o.bodyNote.setText(note.toString());
        }
        WidgetActionPairBinding pair=WidgetActionPairBinding.inflate(getLayoutInflater(),binding.content,false);
        binding.content.addView(pair.getRoot());
        pair.actionPrimary.setText("+ Nhập số đo mới");
        pair.actionPrimary.setOnClickListener(v->editor(0));
        pair.actionSecondary.setText("So sánh giai đoạn");
        pair.actionSecondary.setOnClickListener(v->navigate(R.id.comparison));
        if(rows.isEmpty()) {
            text("Chưa có số đo. Hãy nhập lần đo đầu tiên để bắt đầu.");
            return;
        }
        BodyMeasurement chosen=current;
        button("Xóa lần đo đang xem",()->new AlertDialog.Builder(requireContext()).setTitle("Xóa số đo?").setMessage("Thao tác này không thể hoàn tác.").setNegativeButton("Hủy",null).setPositiveButton("Xóa",(d,w)->vm.delete(chosen)).show());
        List<BodyMeasurement> filtered=new ArrayList<>();
        long since=days>0?System.currentTimeMillis()-days*Formats.DAY_MS:0;
        for(BodyMeasurement m:rows)if(days==-1?m.measuredAt>=from&&m.measuredAt<=to:m.measuredAt>=since)filtered.add(m);
        // Thẻ lịch sử / timeline
        WidgetCardBinding timeline=card("LỊCH SỬ / TIMELINE","Các lần đo",days==-1?Formats.day(from)+" – "+Formats.day(to):days==0?"Tất cả":days+" ngày",this::showFilter);
        endCard();
        if(filtered.isEmpty())timeline.cardBody.addView(plainText("Chưa ghi nhận trong khoảng này"));
        int at=-1;
        for(int i=0;i<filtered.size();i++) {
            BodyMeasurement m=filtered.get(i);
            do at++;while(at<rows.size()&&rows.get(at)!=m);
            BodyMeasurement older=at>=0&&at+1<rows.size()?rows.get(at+1):null;
            ItemTimelineRowBinding r=ItemTimelineRowBinding.inflate(getLayoutInflater(),timeline.cardBody,false);
            timeline.cardBody.addView(r.getRoot());
            r.tlDate.setText(Formats.date(m.measuredAt));
            r.tlSub.setText(m==rows.get(0)?"Hiện tại":relative(m.measuredAt));
            r.tlValue.setText(Formats.number(m.weightKg)+" kg");
            r.tlDelta.setText(older==null?"Bắt đầu":signed(m.weightKg-older.weightKg,"kg"));
            r.tlDot.setBackgroundResource(m.id==selected?R.drawable.dot_red:R.drawable.dot_gray);
            if(i==filtered.size()-1)r.tlDivider.setVisibility(View.GONE);
            r.getRoot().setOnClickListener(v-> {
                selected=m.id;render();
            }
            );
        }
        // Thẻ biểu đồ cân nặng và BMI
        if(!filtered.isEmpty())for(int metric=0;metric<2;metric++) {
            String badge=null;
            if(metric==0&&filtered.size()>1) {
                double d=filtered.get(0).weightKg-filtered.get(filtered.size()-1).weightKg;
                badge=(d>0?"↑ ":d<0?"↓ ":"= ")+Formats.number(Math.abs(d))+" kg";
            }
            WidgetCardBinding chartCard=card(days==-1?"KHOẢNG TÙY CHỌN":days==0?"TẤT CẢ":days+" NGÀY",metric==0?"Biểu đồ cân nặng":"Biểu đồ BMI",badge,()-> {
            }
            );
            chartCard.cardAction.setClickable(false);
            List<Long> ct=new ArrayList<>();List<Double> cv=new ArrayList<>();
            for(BodyMeasurement m:filtered) {
                ct.add(m.measuredAt);cv.add(metric==0?m.weightKg:HealthMath.bmi(m.weightKg,m.heightCm));
            }
            TrendView chart=widget(R.layout.widget_trend);chart.setData(ct,cv);
            endCard();
        }
    }
    private android.widget.TextView plainText(String s) {
        android.widget.TextView t=new android.widget.TextView(requireContext());
        t.setText(s);
        t.setTextColor(0xFF718096);
        t.setPadding(0,dp(12),0,dp(12));
        return t;
    }
    private void pickRange() {
        Calendar now=Calendar.getInstance();
        new android.app.DatePickerDialog(requireContext(),(v,y,m,d)-> {
            Calendar start=Calendar.getInstance();start.set(y,m,d,0,0,0);start.set(Calendar.MILLISECOND,0);new android.app.DatePickerDialog(requireContext(),(vv,yy,mm,dd)-> {
                Calendar end=Calendar.getInstance();end.set(yy,mm,dd,23,59,59);if(end.before(start)) {
                    vm.message.setValue("Ngày kết thúc phải sau ngày bắt đầu");return;
                }
                from=start.getTimeInMillis();to=end.getTimeInMillis();days=-1;render();
            }
            ,now.get(Calendar.YEAR),now.get(Calendar.MONTH),now.get(Calendar.DAY_OF_MONTH)).show();
        }
        ,now.get(Calendar.YEAR),now.get(Calendar.MONTH),now.get(Calendar.DAY_OF_MONTH)).show();
    }
    @Override public void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        b.putLong("selected",selected);
        b.putInt("days",days);
        b.putLong("from",from);
        b.putLong("to",to);
    }
}

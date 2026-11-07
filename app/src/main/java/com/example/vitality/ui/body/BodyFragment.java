package com.example.vitality.ui.body;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import java.util.*;
import com.example.vitality.R;
import com.example.vitality.ui.main.PageFragment;
import com.example.vitality.data.entity.BodyMeasurement;
import com.example.vitality.graphics.BodyShapeView;
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
    private void render() {
        clear();
        button("+ Nhập số đo mới",()->editor(0));
        button("So sánh hai giai đoạn",()->navigate(R.id.comparison));
        if(rows.isEmpty()) {
            text("Chưa có số đo. Hãy nhập lần đo đầu tiên để bắt đầu.");
            widget(R.layout.widget_body);
            text("Hình minh họa mặc định, chưa dựa trên dữ liệu cá nhân.");
            return;
        }
        BodyMeasurement current=rows.get(0);
        for(BodyMeasurement m:rows)if(m.id==selected)current=m;
        selected=current.id;
        BodyShapeView body=widget(R.layout.widget_body);
        if(previous!=null)body.setInitialMeasurement(previous);
        body.setMeasurement(current);
        previous=current;
        text(Formats.date(current.measuredAt)+" • "+Formats.number(current.weightKg)+" kg • BMI "+Formats.number(HealthMath.bmi(current.weightKg,current.heightCm)));
        text(getString(R.string.illustration));
        if(current.upperArmCm==null||current.thighCm==null||current.calfCm==null)text("Ước lượng: "+(current.upperArmCm==null?"cánh tay ":"")+(current.thighCm==null?"đùi ":"")+(current.calfCm==null?"bắp chân":""));
        BodyMeasurement chosen=current;
        button("Sửa lần đo đang xem",()->editor(chosen.id));
        button("Xóa lần đo đang xem",()->new AlertDialog.Builder(requireContext()).setTitle("Xóa số đo?").setMessage("Thao tác này không thể hoàn tác.").setNegativeButton("Hủy",null).setPositiveButton("Xóa",(d,w)->vm.delete(chosen)).show());
        text("Lịch sử / timeline — chạm một mốc để xem mô hình");
        button("Bộ lọc: "+(days==-1?Formats.day(from)+" – "+Formats.day(to):days==0?"Tất cả":days+" ngày"),()->new AlertDialog.Builder(requireContext()).setItems(new String[] {
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
        ).show());
        List<BodyMeasurement> filtered=new ArrayList<>();
        for(BodyMeasurement m:rows)if(days==-1?m.measuredAt>=from&&m.measuredAt<=to:days==0||m.measuredAt>=System.currentTimeMillis()-days*86400000L)filtered.add(m);
        if(filtered.isEmpty())text("Chưa ghi nhận trong khoảng này");
        list(filtered,m->Formats.date(m.measuredAt)+"  #"+m.id+"\n"+Formats.number(m.weightKg)+" kg • Eo "+Formats.number(m.waistCm)+" cm",m-> {
            selected=m.id;render();
        }
        );
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

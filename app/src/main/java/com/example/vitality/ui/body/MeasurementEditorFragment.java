package com.example.vitality.ui.body;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.*;
import java.util.*;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.example.vitality.ui.main.PageFragment;
import com.example.vitality.data.entity.*;
import com.example.vitality.utils.*;
public class MeasurementEditorFragment extends PageFragment  {
    private long id;
    private boolean loaded=false,waiting=false;
    private BodyMeasurement original;
    private final List<TextInputEditText> inputs=new ArrayList<>();
    private TextInputEditText time,note;
    private int template;
    @Override public void onViewCreated(View v,Bundle state) {
        id=getArguments()==null?0:getArguments().getLong("id");
        heading(id==0?"Thêm lần đo":"Chỉnh sửa số đo","Đơn vị cm và kg • Mỗi lần đo là một giai đoạn độc lập");
        vm.measurements.observe(getViewLifecycleOwner(),rows-> {
            if(loaded)return;original=new BodyMeasurement();if(id!=0) {
                original=null;for(BodyMeasurement m:rows)if(m.id==id)original=m;if(original==null) {
                    text("Bản ghi đã bị xóa");return;
                }
            }
            loaded=true;build(state);
        }
        );
    }
    private void build(Bundle state) {
        time=field("Ngày giờ đo (dd/MM/yyyy HH:mm)",Formats.date(original.measuredAt),false);
        String[] names= {
            "Chiều cao (cm)","Cân nặng (kg)","Vòng ngực (cm)","Vòng eo (cm)","Vòng hông (cm)","Vòng cánh tay (cm, tùy chọn)","Vòng đùi (cm, tùy chọn)","Vòng bắp chân (cm, tùy chọn)"
        }
        ;
        Double[] vals= {
            original.heightCm,original.weightKg,original.chestCm,original.waistCm,original.hipCm,original.upperArmCm,original.thighCm,original.calfCm
        }
        ;
        for(int i=0;i<names.length;i++)inputs.add(field(names[i],id==0?"":n(vals[i]),true));
        note=field("Ghi chú",original.note,false);
        text("Khi bỏ trống tay, đùi hoặc bắp chân, vùng đó dùng tỷ lệ ước lượng. Mẫu hiển thị độc lập với giới tính.");
        template=id==0&&vm.profile.getValue()!=null?vm.profile.getValue().bodyTemplate:original.bodyTemplate;
        android.widget.Spinner s=spinner(new String[] {
            "Mẫu cân đối","Mẫu vai rộng"
        }
        ,p->template=p);
        s.setSelection(template);
        if(state!=null) {
            time.setText(state.getString("time"));
            note.setText(state.getString("note"));
            for(int i=0;i<inputs.size();i++)inputs.get(i).setText(state.getString("f"+i));
            s.setSelection(state.getInt("template"));
        }
        MaterialButton save=button("Lưu số đo",()-> {
            try {
                BodyMeasurement m=new BodyMeasurement();m.id=id;m.createdAt=original.createdAt;m.measuredAt=date(time);if(m.measuredAt>System.currentTimeMillis()) {
                    time.setError("Thời điểm đo không được ở tương lai");return;
                }
                double[] a=new double[5];for(int i=0;i<5;i++)a[i]=number(inputs.get(i),Validation.BODY_RANGES[i][0],Validation.BODY_RANGES[i][1]);m.heightCm=a[0];m.weightKg=a[1];m.chestCm=a[2];m.waistCm=a[3];m.hipCm=a[4];m.upperArmCm=optional(inputs.get(5),5,100);m.thighCm=optional(inputs.get(6),10,150);m.calfCm=optional(inputs.get(7),5,100);m.note=value(note);m.bodyTemplate=template;waiting=true;vm.save(m);
            }
            catch(IllegalArgumentException ignored) {
            }
        }
        );
        vm.busy.observe(getViewLifecycleOwner(),busy->save.setEnabled(!busy));
        long initial=vm.saved.getValue();
        vm.saved.observe(getViewLifecycleOwner(),value-> {
            if(waiting&&value>initial) {
                waiting=false;back();
            }
        }
        );
        button("Quay lại",this::back);
    }
    @Override public void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        if(time!=null) {
            b.putString("time",value(time));
            b.putString("note",value(note));
            b.putInt("template",template);
            for(int i=0;i<inputs.size();i++)b.putString("f"+i,value(inputs.get(i)));
        }
    }
    @Override public void onDestroyView() {
        super.onDestroyView();
        loaded=false;
        inputs.clear();
        time=null;
        note=null;
    }
}

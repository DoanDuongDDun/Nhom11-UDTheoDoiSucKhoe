package com.example.vitality.ui.symptom;
import android.os.Bundle;
import android.view.View;
import android.widget.Spinner;
import androidx.appcompat.app.AlertDialog;
import java.util.*;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.example.vitality.ui.main.PageFragment;
import com.example.vitality.data.entity.Symptom;
import com.example.vitality.utils.*;
public class SymptomEditorFragment extends PageFragment  {
    private static final String[] NAMES=buildNames();
    private static final String[] LEVELS= {
        "-- Chọn mức độ --","Nhẹ","Vừa","Nặng"
    }
    ;
    private long id;
    private boolean loaded=false,waiting=false;
    private Symptom original;
    private TextInputEditText custom,time,note;
    private int namePos=0,levelPos=0;
    private static String[] buildNames() {
        String[] a=new String[SymptomLogic.PRESETS.length+2];
        a[0]="-- Chọn triệu chứng --";
        System.arraycopy(SymptomLogic.PRESETS,0,a,1,SymptomLogic.PRESETS.length);
        a[a.length-1]="Khác (tự nhập)";
        return a;
    }
    private int otherIndex() {
        return NAMES.length-1;
    }
    @Override public void onViewCreated(View v,Bundle state) {
        id=getArguments()==null?0:getArguments().getLong("id");
        heading(id==0?"Thêm triệu chứng":"Sửa triệu chứng","Chọn triệu chứng, mức độ và thời điểm xuất hiện");
        vm.symptoms.observe(getViewLifecycleOwner(),rows-> {
            if(loaded)return;original=new Symptom();if(id!=0) {
                original=null;for(Symptom s:rows)if(s.id==id)original=s;if(original==null) {
                    text("Bản ghi đã bị xóa");return;
                }
            }
            loaded=true;build(state);
        }
        );
    }
    private void build(Bundle state) {
        text("Triệu chứng");
        Spinner nameSpinner=spinner(NAMES,p->namePos=p);
        custom=field("Tên triệu chứng khác (chỉ dùng khi chọn \"Khác\")","",false);
        text("Mức độ");
        Spinner levelSpinner=spinner(LEVELS,p->levelPos=p);
        time=field("Thời gian xuất hiện (dd/mm/yyyy HH:mm)",Formats.date(original.occurredAt),false);
        note=field("Ghi chú",original.note,false);
        if(id!=0) {
            int index=0;
            for(int i=1;i<otherIndex();i++)if(NAMES[i].equals(original.name))index=i;
            if(index==0) {
                index=otherIndex();custom.setText(original.name);
            }
            nameSpinner.setSelection(index);
            levelSpinner.setSelection(Math.max(1,Math.min(3,original.severity)));
        }
        if(state!=null) {
            time.setText(state.getString("time"));
            note.setText(state.getString("note"));
            custom.setText(state.getString("custom"));
            nameSpinner.setSelection(state.getInt("namePos"));
            levelSpinner.setSelection(state.getInt("levelPos"));
        }
        MaterialButton save=button("Lưu",this::trySave);
        vm.busy.observe(getViewLifecycleOwner(),busy->save.setEnabled(!busy));
        long initial=vm.saved.getValue();
        vm.saved.observe(getViewLifecycleOwner(),value-> {
            if(waiting&&value>initial) {
                waiting=false;back();
            }
        }
        );
        button("Hủy bỏ",this::back);
    }
    private void trySave() {
        String name=namePos==0?"":namePos==otherIndex()?value(custom):NAMES[namePos];
        if(name.isEmpty()||levelPos==0) {
            vm.message.setValue("Vui lòng chọn triệu chứng và mức độ!");return;
        }
        if(name.length()>60) {
            custom.setError("Tối đa 60 ký tự");vm.message.setValue("Dữ liệu không hợp lệ!");return;
        }
        long when;
        try {
            when=Validation.date(value(time));
        }
        catch(IllegalArgumentException x) {
            time.setError(x.getMessage());vm.message.setValue("Thời gian không hợp lệ!");return;
        }
        if(!SymptomLogic.validTime(when,System.currentTimeMillis())) {
            time.setError("Thời gian xuất hiện không được ở tương lai");vm.message.setValue("Thời gian không hợp lệ!");return;
        }
        Symptom s=new Symptom();
        s.id=id;
        s.createdAt=original.createdAt;
        s.name=name;
        s.severity=levelPos;
        s.occurredAt=when;
        s.note=value(note);
        int streak=SymptomLogic.streakDays(vm.symptoms.getValue(),s);
        if(SymptomLogic.needsMedicalAdvice(s.severity,streak)) {
            new AlertDialog.Builder(requireContext()).setTitle("Khuyến nghị đi khám").setMessage(SymptomLogic.warningText(s,streak)).setNegativeButton("Quay lại sửa",null).setPositiveButton("Đã hiểu, lưu",(d,w)->commit(s)).show();
        }
        else commit(s);
    }
    private void commit(Symptom s) {
        waiting=true;
        vm.saveSymptom(s);
    }
    @Override public void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        if(time!=null) {
            b.putString("time",value(time));
            b.putString("note",value(note));
            b.putString("custom",value(custom));
            b.putInt("namePos",namePos);
            b.putInt("levelPos",levelPos);
        }
    }
    @Override public void onDestroyView() {
        super.onDestroyView();
        loaded=false;
        custom=null;
        time=null;
        note=null;
    }
}

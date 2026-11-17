package com.example.vitality.ui.diary;
import android.app.Dialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import java.util.*;
import com.example.vitality.R;
import com.example.vitality.data.entity.*;
import com.example.vitality.viewmodel.*;
import com.example.vitality.utils.*;
import com.google.android.material.textfield.*;
public class JournalEditorDialog extends DialogFragment  {
    private final List<TextInputEditText> fields=new ArrayList<>();
    private JournalEntry entry;
    private VitalityViewModel vm;
    public static JournalEditorDialog create(String kind,long id) {
        JournalEditorDialog d=new JournalEditorDialog();
        Bundle b=new Bundle();
        b.putString("kind",kind);
        b.putLong("id",id);
        d.setArguments(b);
        return d;
    }
    private TextInputEditText field(LinearLayout root,String label,String value,boolean numeric) {
        TextInputLayout box=(TextInputLayout)getLayoutInflater().inflate(R.layout.widget_field,root,false);
        box.setHint(label);
        TextInputEditText e=(TextInputEditText)box.getEditText();
        e.setId(android.view.View.generateViewId());
        if(numeric)e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setText(value);
        root.addView(box);
        fields.add(e);
        return e;
    }
    private String value(int i) {
        return fields.get(i).getText().toString().trim();
    }
    private double num(int i,double min,double max) {
        try {
            return Validation.number(value(i),min,max);
        }
        catch(Exception e) {
            fields.get(i).setError(e.getMessage());
            throw e;
        }
    }
    private Double opt(int i) {
        return value(i).isEmpty()?null:num(i,0,100000);
    }
    private long date(int i) {
        try {
            return Validation.date(value(i));
        }
        catch(Exception e) {
            fields.get(i).setError(e.getMessage());
            throw e;
        }
    }
    @Override public Dialog onCreateDialog(Bundle saved) {
        vm=new ViewModelProvider(requireActivity()).get(VitalityViewModel.class);
        entry=new JournalEntry();
        entry.kind=getArguments().getString("kind");
        long id=getArguments().getLong("id");
        entry.id=id;
        List<JournalEntry> data=vm.journals.getValue();
        if(id!=0&&data!=null)for(JournalEntry e:data)if(e.id==id)entry=e;
        ScrollView scroll=new ScrollView(requireContext());
        LinearLayout root=new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int pad=(int)(20*getResources().getDisplayMetrics().density);
        root.setPadding(pad,0,pad,0);
        scroll.addView(root);
        field(root,"Ngày giờ (dd/mm/yyyy HH:mm)",Formats.date(entry.startedAt),false);
        switch(entry.kind) {
            case "nutrition":field(root,"Tên món",entry.nameOr(""),false);
            field(root,"Loại bữa (sáng / trưa / tối / phụ)",entry.category,false);
            field(root,"Khẩu phần",entry.portion,false);
            field(root,"Năng lượng kcal (tùy chọn)",entry.calories==null?"":Formats.number(entry.calories),true);
            break;
            case "activity":field(root,"Loại hoạt động",entry.nameOr(""),false);
            field(root,"Thời lượng (phút)",entry.amount==0?"":Formats.number(entry.amount),true);
            field(root,"Quãng đường km (tùy chọn)",entry.distanceKm==null?"":Formats.number(entry.distanceKm),true);
            field(root,"Năng lượng kcal (tùy chọn)",entry.calories==null?"":Formats.number(entry.calories),true);
            break;
            case JournalEntry.KIND_STEPS:
            field(root,"Số bước",Long.toString(Math.round(entry.amount)),true)
                    .setInputType(InputType.TYPE_CLASS_NUMBER);
            break;
            case "sleep":field(root,"Kết thúc (dd/mm/yyyy HH:mm)",Formats.date(entry.endedAt==null?System.currentTimeMillis():entry.endedAt),false);
            field(root,"Thức giữa giấc (phút)",Formats.number(entry.awakeMinutes),true);
            field(root,"Chất lượng tự đánh giá (1–5)",String.valueOf(entry.quality),true);
            break;
            default:field(root,"Dung tích (ml)",entry.amount==0?"250":Formats.number(entry.amount),true);
        }
        field(root,"Ghi chú",entry.note,false);
        if(saved!=null)for(int i=0;i<fields.size();i++)fields.get(i).setText(saved.getString("f"+i));
        AlertDialog dialog=new AlertDialog.Builder(requireContext()).setTitle("Ghi nhật ký").setView(scroll).setNegativeButton("Hủy",null).setPositiveButton("Lưu",null).create();
        dialog.setOnShowListener(d-> {
            dialog.getButton(-1).setOnClickListener(v->save());
        }
        );
        return dialog;
    }
    private void save() {
        try {
            JournalEntry e=new JournalEntry();
            e.id=entry.id;
            e.kind=entry.kind;
            e.startedAt=date(0);
            if(e.startedAt>System.currentTimeMillis()) {
                fields.get(0).setError("Không ghi nhật ký ở tương lai");
                return;
            }
            switch(e.kind) {
                case "nutrition":e.title=value(1);
                e.category=value(2);
                e.portion=value(3);
                e.calories=opt(4);
                if(e.title.isEmpty()||e.category.isEmpty()||e.portion.isEmpty())throw new IllegalArgumentException("Nhập tên món, loại bữa và khẩu phần");
                break;
                case "activity":e.title=value(1);
                e.amount=num(2,1,1440);
                e.distanceKm=opt(3);
                e.calories=opt(4);
                if(e.title.isEmpty())throw new IllegalArgumentException("Nhập loại hoạt động");
                break;
                case JournalEntry.KIND_STEPS:
                e.title=entry.nameOr("Bước chân");
                e.amount=num(1,1,Integer.MAX_VALUE);
                break;
                case "sleep":e.endedAt=date(1);
                e.awakeMinutes=num(2,0,1440);
                double q=num(3,1,5);
                if(q!=Math.rint(q))throw new IllegalArgumentException("Chất lượng là số nguyên từ 1 đến 5");
                e.quality=(int)q;
                if(e.endedAt>System.currentTimeMillis())throw new IllegalArgumentException("Giờ kết thúc không được ở tương lai");
                HealthMath.sleepMinutes(e.startedAt,e.endedAt,e.awakeMinutes);
                break;
                default:e.amount=num(1,1,10000);
            }
            e.note=value(fields.size()-1);
            AlertDialog d=(AlertDialog)getDialog();
            d.getButton(-1).setEnabled(false);
            long initial=vm.saved.getValue();
            vm.saved.observe(this,x-> {
                if(x>initial)dismiss();
            }
            );
            vm.busy.observe(this,b-> {
                if(getDialog()!=null)((AlertDialog)getDialog()).getButton(-1).setEnabled(!b);
            }
            );
            vm.run(()->vm.wellness.saveJournal(e),true);
        }
        catch(Exception e) {
            Toast.makeText(requireContext(),e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }
    @Override public void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        for(int i=0;i<fields.size();i++)b.putString("f"+i,value(i));
    }
}

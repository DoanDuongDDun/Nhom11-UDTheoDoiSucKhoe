package com.example.vitality.ui.habit;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.*;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import java.util.*;
import com.google.android.material.textfield.*;
import com.example.vitality.R;
import com.example.vitality.data.entity.*;
import com.example.vitality.viewmodel.*;
import com.example.vitality.utils.*;
public class HabitEditorDialog extends DialogFragment  {
    private final List<TextInputEditText> fields=new ArrayList<>();
    private HabitSchedule old;
    private VitalityViewModel vm;
    public static HabitEditorDialog create(long id) {
        HabitEditorDialog d=new HabitEditorDialog();
        Bundle b=new Bundle();
        b.putLong("id",id);
        d.setArguments(b);
        return d;
    }
    @Override public Dialog onCreateDialog(Bundle state) {
        vm=new ViewModelProvider(requireActivity()).get(VitalityViewModel.class);
        old=new HabitSchedule();
        old.id=getArguments().getLong("id");
        if(vm.habits.getValue()!=null)for(HabitSchedule h:vm.habits.getValue())if(h.id==getArguments().getLong("id"))old=h;
        ScrollView scroll=new ScrollView(requireContext());
        LinearLayout root=new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(30,10,30,10);
        scroll.addView(root);
        String days="";
        for(int i=0;i<7;i++)if((old.weekdays&(1<<i))!=0)days+=(i+2)+",";
        String[] labels= {
            "Tên lịch","Nội dung","Giờ nhắc HH:mm","Ngày trong tuần: 2,3,4,5,6,7,8 (8 = CN)","Bắt đầu dd/MM/yyyy HH:mm","Kết thúc (tùy chọn) dd/MM/yyyy HH:mm"
        }
        ;
        String[] values= {
            old.title,old.note,String.format(Locale.US,"%02d:%02d",old.hour,old.minute),days,Formats.date(old.startsAt),old.endsAt==null?"":Formats.date(old.endsAt)
        }
        ;
        for(int i=0;i<labels.length;i++) {
            TextInputLayout box=(TextInputLayout)getLayoutInflater().inflate(R.layout.widget_field,root,false);
            box.setHint(labels[i]);
            TextInputEditText e=(TextInputEditText)box.getEditText();
        e.setId(android.view.View.generateViewId());
            e.setText(state==null?values[i]:state.getString("f"+i));
            fields.add(e);
            root.addView(box);
        }
        AlertDialog d=new AlertDialog.Builder(requireContext()).setTitle("Lịch sinh hoạt").setView(scroll).setNegativeButton("Hủy",null).setPositiveButton("Lưu",null).create();
        d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->save(d)));
        return d;
    }
    private String value(int i) {
        return fields.get(i).getText().toString().trim();
    }
    private void save(AlertDialog d) {
        try {
            HabitSchedule h=new HabitSchedule();
            h.id=old.id;
            h.paused=old.paused;
            h.title=value(0);
            h.note=value(1);
            if(h.title.isEmpty())throw new IllegalArgumentException("Nhập tên lịch");
            String[] clock=value(2).split(":");
            if(clock.length!=2)throw new IllegalArgumentException("Giờ phải có dạng HH:mm");
            h.hour=Integer.parseInt(clock[0]);
            h.minute=Integer.parseInt(clock[1]);
            if(h.hour<0||h.hour>23||h.minute<0||h.minute>59)throw new IllegalArgumentException("Giờ không hợp lệ");
            h.weekdays=0;
            for(String day:value(3).split(",")) {
                int n=Integer.parseInt(day.trim());
                if(n<2||n>8)throw new IllegalArgumentException("Ngày từ 2 đến 8");
                h.weekdays|=1<<(n-2);
            }
            h.startsAt=Validation.date(value(4));
            h.endsAt=value(5).isEmpty()?null:Validation.date(value(5));
            if(h.endsAt!=null&&h.endsAt<=h.startsAt)throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
            long initial=vm.saved.getValue();
            vm.saved.observe(this,n-> {
                if(n>initial)dismiss();
            }
            );
            vm.busy.observe(this,b-> {
                if(getDialog()!=null)d.getButton(-1).setEnabled(!b);
            }
            );
            vm.run(()-> {
                vm.wellness.saveHabit(h);com.example.vitality.notification.ReminderScheduler.schedule(vm.getApplication());
            }
            ,true);
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

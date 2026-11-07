package com.example.vitality.ui.habit;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import java.util.*;
import com.example.vitality.ui.main.PageFragment;
import com.example.vitality.data.entity.*;
import com.example.vitality.utils.*;
public class HabitFragment extends PageFragment  {
    @Override public void onViewCreated(View v,Bundle b) {
        heading("Chu kỳ sinh hoạt","Hoàn thành trong vòng 1 giờ sau giờ nhắc");
        vm.habits.observe(getViewLifecycleOwner(),x->render());
        vm.occurrences.observe(getViewLifecycleOwner(),x->render());
    }
    private void render() {
        clear();
        button("Quay lại",this::back);
        button("+ Tạo lịch sinh hoạt",()->HabitEditorDialog.create(0).show(getParentFragmentManager(),"habit_editor"));
        List<HabitSchedule> schedules=vm.habits.getValue();
        if(schedules!=null)list(schedules,h->h.title+" • "+String.format(java.util.Locale.getDefault(),"%02d:%02d",h.hour,h.minute)+(h.paused?" • Tạm dừng":" • Đang bật"),h->new AlertDialog.Builder(requireContext()).setItems(new String[] {
            "Sửa",h.paused?"Tiếp tục":"Tạm dừng","Xóa lịch (giữ lịch sử)"
        }
        ,(d,w)-> {
            if(w==0)HabitEditorDialog.create(h.id).show(getParentFragmentManager(),"habit_editor");else if(w==1)vm.run(()-> {
                h.paused=!h.paused;vm.wellness.saveHabit(h);com.example.vitality.notification.ReminderScheduler.schedule(vm.getApplication());
            }
            ,true);else new AlertDialog.Builder(requireContext()).setTitle("Xóa lịch?").setNegativeButton("Hủy",null).setPositiveButton("Xóa",(a,b)->vm.run(()-> {
                h.deleted=true;vm.wellness.saveHabit(h);com.example.vitality.notification.ReminderScheduler.schedule(vm.getApplication());
            }
            ,true)).show();
        }
        ).show());
        List<HabitOccurrence> all=vm.occurrences.getValue();
        if(all==null)return;
        long now=System.currentTimeMillis();
        List<HabitOccurrence> visible=new ArrayList<>();
        int due=0,done=0;
        for(HabitOccurrence o:all) {
            if(o.dueAt<=now&&o.dueAt>=now-30*86400000L) {
                due++;
                if(o.status.equals("done"))done++;
            }
            if(o.dueAt>=now-30*86400000L&&o.dueAt<=now+86400000L)visible.add(o);
        }
        text(due==0?"Chưa có lần đến hạn":"30 ngày: "+Formats.number(HealthMath.completion(done,due))+"% hoàn thành");
        text("Lịch sử 30 ngày và 24 giờ sắp tới");
        list(visible,o->Formats.date(o.dueAt)+" • "+o.title+"\n"+status(o,now),o-> {
            if(o.dueAt>now) {
                vm.message.setValue("Lần này chưa đến hạn");return;
            }
            if(!o.status.equals("pending")||o.dueAt+3600000L<now) {
                vm.message.setValue("Lần thực hiện đã kết thúc");return;
            }
            new AlertDialog.Builder(requireContext()).setItems(new String[] {
                "Hoàn thành","Bỏ qua"
            }
            ,(d,w)->vm.run(()-> {
                o.status=w==0?"done":"skipped";vm.wellness.dao.update(o);
            }
            ,true)).show();
        }
        );
    }
    private String status(HabitOccurrence o,long now) {
        switch(o.status) {
            case "done":return "Hoàn thành";
            case "skipped":return "Bỏ qua";
            case "missed":return "Bỏ lỡ";
            default:return o.dueAt>now?"Chưa đến hạn":o.dueAt+3600000<now?"Bỏ lỡ":"Đang đến hạn";
        }
    }
}

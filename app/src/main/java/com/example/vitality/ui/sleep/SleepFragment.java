package com.example.vitality.ui.sleep;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import java.util.*;
import com.example.vitality.ui.main.PageFragment;
import com.example.vitality.ui.diary.JournalEditorDialog;
import com.example.vitality.data.entity.*;
import com.example.vitality.utils.*;
public class SleepFragment extends PageFragment  {
    @Override public void onViewCreated(View v,Bundle b) {
        heading("Giấc ngủ","Thống kê theo ngày thức dậy • Chất lượng do bạn tự đánh giá");
        vm.journals.observe(getViewLifecycleOwner(),rows-> {
            clear();button("Quay lại",this::back);JournalEntry active=null;List<JournalEntry> sleeps=new ArrayList<>();double total=0;int count=0;for(JournalEntry e:rows)if(e.kind.equals("sleep")) {
                sleeps.add(e);if(e.endedAt==null)active=e;else if(e.endedAt>=System.currentTimeMillis()-7*86400000L) {
                    total+=HealthMath.sleepMinutes(e.startedAt,e.endedAt,e.awakeMinutes)/60;count++;
                }
            }
            if(active==null)button("Bắt đầu ngủ",()-> {
                JournalEntry e=new JournalEntry();e.kind="sleep";vm.run(()->vm.wellness.saveJournal(e),true);
            }
            );else {
                JournalEntry current=active;text("Đang ngủ từ "+Formats.date(current.startedAt));button("Kết thúc ngủ",()->JournalEditorDialog.create("sleep",current.id).show(getParentFragmentManager(),"sleep_editor"));
            }
            button("Nhập giấc ngủ thủ công",()->JournalEditorDialog.create("sleep",0).show(getParentFragmentManager(),"sleep_editor"));text(count==0?"Tuần này chưa có giấc ngủ hoàn tất":"Trung bình 7 ngày: "+Formats.number(total/count)+" giờ / giấc");list(sleeps,e->Formats.date(e.startedAt)+"\n"+(e.endedAt==null?"Đang hoạt động":Formats.date(e.endedAt)+" • "+Formats.number(HealthMath.sleepMinutes(e.startedAt,e.endedAt,e.awakeMinutes)/60)+" giờ • Chất lượng "+e.quality+"/5"),e->new AlertDialog.Builder(requireContext()).setItems(new String[] {
                "Sửa / hoàn tất","Xóa"
            }
            ,(d,w)-> {
                if(w==0)JournalEditorDialog.create("sleep",e.id).show(getParentFragmentManager(),"sleep_editor");else new AlertDialog.Builder(requireContext()).setTitle("Xóa phiên ngủ?").setNegativeButton("Hủy",null).setPositiveButton("Xóa",(a,c)->vm.run(()->vm.wellness.dao.delete(e),true)).show();
            }
            ).show());
        }
        );
    }
}

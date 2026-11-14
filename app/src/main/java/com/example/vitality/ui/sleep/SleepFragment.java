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
        observeAll(()-> {
            List<JournalEntry> rows=vm.journals.getValue();if(rows==null)rows=Collections.emptyList();
            clear();JournalEntry active=null;List<JournalEntry> sleeps=new ArrayList<>();double total=0;int count=0;for(JournalEntry e:rows)if(e.kind.equals("sleep")) {
                sleeps.add(e);if(e.endedAt==null)active=e;else if(e.endedAt>=System.currentTimeMillis()-7*Formats.DAY_MS) {
                    total+=HealthMath.sleepMinutesOrZero(e.startedAt,e.endedAt,e.awakeMinutes)/60;count++;
                }
            }
            hero(com.example.vitality.R.drawable.bg_sleep_card,com.example.vitality.R.drawable.ic_moon,"TRUNG BÌNH 7 NGÀY",count==0?"--":Formats.number(total/count),"giờ / giấc",count==0?"Tuần này chưa có giấc ngủ hoàn tất":active!=null?"Đang ngủ từ "+Formats.date(active.startedAt):"Chất lượng do bạn tự đánh giá");if(active==null)button("Bắt đầu ngủ",()-> {
                JournalEntry e=new JournalEntry();e.kind="sleep";vm.run(()->vm.wellness.saveJournal(e),true);
            }
            );else {
                JournalEntry current=active;text("Đang ngủ từ "+Formats.date(current.startedAt));button("Kết thúc ngủ",()->JournalEditorDialog.create("sleep",current.id).show(getParentFragmentManager(),"sleep_editor"));
            }
            goalProgress("sleep");
            button("Nhập giấc ngủ thủ công",()->JournalEditorDialog.create("sleep",0).show(getParentFragmentManager(),"sleep_editor"));text(count==0?"Tuần này chưa có giấc ngủ hoàn tất":"Trung bình 7 ngày: "+Formats.number(total/count)+" giờ / giấc");listStyle(com.example.vitality.R.drawable.ic_moon,com.example.vitality.R.drawable.bg_icon_violet,0xFF7360DD);list(sleeps,e->Formats.date(e.startedAt)+"\n"+(e.endedAt==null?"Đang hoạt động":Formats.date(e.endedAt)+" • "+Formats.number(HealthMath.sleepMinutesOrZero(e.startedAt,e.endedAt,e.awakeMinutes)/60)+" giờ • Chất lượng "+e.quality+"/5"),e->new AlertDialog.Builder(requireContext()).setItems(new String[] {
                "Sửa / hoàn tất","Xóa"
            }
            ,(d,w)-> {
                if(w==0)JournalEditorDialog.create("sleep",e.id).show(getParentFragmentManager(),"sleep_editor");else new AlertDialog.Builder(requireContext()).setTitle("Xóa phiên ngủ?").setNegativeButton("Hủy",null).setPositiveButton("Xóa",(a,c)->vm.run(()->vm.wellness.dao.delete(e),true)).show();
            }
            ).show());
        }
        ,vm.journals,vm.goals);
    }
}

package com.example.vitality.ui.diary;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import java.util.*;
import com.example.vitality.R;
import com.example.vitality.ui.main.PageFragment;
import com.example.vitality.data.entity.*;
import com.example.vitality.utils.*;
public class DiaryFragment extends PageFragment  {
    @Override public void onViewCreated(View v,Bundle b) {
        heading("Nhật ký sức khỏe","Ghi nhận theo ngày • Dữ liệu lưu trên thiết bị");
        vm.journals.observe(getViewLifecycleOwner(),rows-> {
            clear();button("+ Bữa ăn",()->edit("nutrition",0));button("+ Hoạt động thể chất",()->edit("activity",0));button("+ 250 ml nước",()-> {
                JournalEntry e=new JournalEntry();e.amount=250;vm.run(()->vm.wellness.saveJournal(e),true);
            }
            );button("Nhập nước uống",()->edit("water",0));button("Giấc ngủ",()->navigate(R.id.sleep));button("Chu kỳ sinh hoạt",()->navigate(R.id.habit));text("Lịch sử bữa ăn, nước và vận động");List<JournalEntry> filtered=new ArrayList<>();for(JournalEntry e:rows)if(!e.kind.equals("sleep"))filtered.add(e);if(filtered.isEmpty())text("Chưa có nhật ký. Chọn một mục ở trên để bắt đầu.");list(filtered,DiaryFragment::label,this::actions);
        }
        );
    }
    public static String label(JournalEntry e) {
        String detail=e.kind.equals("water")?Formats.number(e.amount)+" ml nước":e.kind.equals("activity")?e.title+" • "+Formats.number(e.amount)+" phút":e.title+" • "+e.category+" • "+e.portion;
        return Formats.date(e.startedAt)+"\n"+detail+(e.calories==null?"":" • "+Formats.number(e.calories)+" kcal")+(e.note.isEmpty()?"":"\n"+e.note);
    }
    private void edit(String kind,long id) {
        JournalEditorDialog.create(kind,id).show(getParentFragmentManager(),"journal_editor");
    }
    private void actions(JournalEntry e) {
        new AlertDialog.Builder(requireContext()).setTitle("Nhật ký").setItems(new String[] {
            "Sửa","Xóa"
        }
        ,(d,w)-> {
            if(w==0)edit(e.kind,e.id);else new AlertDialog.Builder(requireContext()).setTitle("Xóa nhật ký?").setNegativeButton("Hủy",null).setPositiveButton("Xóa",(a,b)->vm.run(()->vm.wellness.dao.delete(e),true)).show();
        }
        ).show();
    }
}

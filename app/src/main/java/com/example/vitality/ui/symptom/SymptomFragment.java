package com.example.vitality.ui.symptom;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import java.util.*;
import com.google.android.material.textfield.TextInputEditText;
import com.example.vitality.R;
import com.example.vitality.ui.main.PageFragment;
import com.example.vitality.data.entity.Symptom;
import com.example.vitality.graphics.TrendView;
import com.example.vitality.utils.*;
public class SymptomFragment extends PageFragment  {
    private int days=7;
    private String query="";
    private int listStart=0;
    private List<Symptom> rows=new ArrayList<>();
    @Override public void onViewCreated(View v,Bundle b) {
        if(b!=null) {
            days=b.getInt("days",7);
            query=b.getString("query","");
        }
        heading("Triệu chứng","Nhật ký theo ngày • Chỉ mang tính tham khảo, không thay thế chẩn đoán y tế");
        vm.symptoms.observe(getViewLifecycleOwner(),data-> {
            rows=data==null?new ArrayList<>():data;render();
        }
        );
    }
    private void render() {
        clear();
        long heroNow=System.currentTimeMillis(),heroSince=heroNow-days*86400000L;
        int heroTotal=0,heroSevere=0;
        for(Symptom hs:rows)if(hs.occurredAt>=heroSince&&hs.occurredAt<=heroNow) {
            heroTotal++;
            if(hs.severity>=3)heroSevere++;
        }
        hero(R.drawable.bg_heart_card,R.drawable.ic_heart,"THEO DÕI TRIỆU CHỨNG",String.valueOf(heroTotal),"lần / "+days+" ngày",heroTotal==0?"Chưa ghi nhận triệu chứng trong khoảng này":heroSevere>0?heroSevere+" lần ở mức nặng, bạn nên cân nhắc đi khám":"Chỉ mang tính tham khảo, không thay thế chẩn đoán");
        button("+ Thêm triệu chứng",()->openEditor(0));
        button("Gợi ý chăm sóc (tham khảo)",()->new AlertDialog.Builder(requireContext()).setTitle("Gợi ý chăm sóc (tham khảo)").setMessage(SymptomLogic.advice(rows,System.currentTimeMillis())).setPositiveButton("Đóng",null).show());
        button("Quay lại",this::back);
        statistics();
        TextInputEditText search=field("Tìm triệu chứng (gõ có dấu hoặc không dấu)",query,false);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int a,int c,int d) {
            }
            public void onTextChanged(CharSequence s,int a,int c,int d) {
            }
            public void afterTextChanged(Editable e) {
                query=e.toString();renderList();
            }
        }
        );
        listStart=binding.content.getChildCount();
        renderList();
    }
    private void statistics() {
        button("Thống kê: "+(days==7?"7 ngày (tuần)":"30 ngày (tháng)"),()->new AlertDialog.Builder(requireContext()).setItems(new String[] {
            "7 ngày (tuần)","30 ngày (tháng)"
        }
        ,(d,w)-> {
            days=w==0?7:30;render();
        }
        ).show());
        long now=System.currentTimeMillis(),since=now-days*Formats.DAY_MS;
        int[] bySeverity=new int[4];
        TreeMap<String,int[]> byName=new TreeMap<>();
        TreeMap<Long,Double> perDay=new TreeMap<>();
        int total=0;
        for(Symptom s:rows) {
            if(s.occurredAt<since||s.occurredAt>now)continue;
            total++;
            bySeverity[Math.max(1,Math.min(3,s.severity))]++;
            int[] c=byName.computeIfAbsent(s.name,x->new int[2]);
            c[0]++;
            c[1]=Math.max(c[1],s.severity);
            long day=Formats.dayStart(s.occurredAt);
            perDay.put(day,perDay.getOrDefault(day,0d)+1);
        }
        if(total==0) {
            text("Chưa ghi nhận triệu chứng trong khoảng này");return;
        }
        StringBuilder b=new StringBuilder("Tổng "+total+" lần • Nhẹ "+bySeverity[1]+" • Vừa "+bySeverity[2]+" • Nặng "+bySeverity[3]);
        for(Map.Entry<String,int[]> e:byName.entrySet())b.append("\n").append(e.getKey()).append(": ").append(e.getValue()[0]).append(" lần (nặng nhất: ").append(SymptomLogic.severityLabel(e.getValue()[1]).toLowerCase(Locale.ROOT)).append(")");
        text(b.toString());
        text("Số lần ghi nhận mỗi ngày (chỉ tính ngày có ghi nhận)");
        TrendView chart=widget(R.layout.widget_chart);
        chart.setData(new ArrayList<>(perDay.keySet()),new ArrayList<>(perDay.values()));
    }
    /** Dựng lại riêng phần danh sách để ô tìm kiếm không mất tiêu điểm khi gõ. */
    private void renderList() {
        if(binding==null)return;
        while(binding.content.getChildCount()>listStart)binding.content.removeViewAt(listStart);
        LinkedHashMap<Long,List<Symptom>> byDay=new LinkedHashMap<>();
        String q=SymptomLogic.normalize(query);
        for(Symptom s:rows)if(SymptomLogic.matchesNormalized(s.name+" "+s.note,q))byDay.computeIfAbsent(Formats.dayStart(s.occurredAt),x->new ArrayList<>()).add(s);
        if(byDay.isEmpty()) {
            text(rows.isEmpty()?"Chưa có triệu chứng. Nhấn \"+ Thêm triệu chứng\" để bắt đầu.":"Không có triệu chứng phù hợp.");return;
        }
        for(Map.Entry<Long,List<Symptom>> e:byDay.entrySet()) {
            text(Formats.day(e.getKey())+" • "+e.getValue().size()+" triệu chứng");
            listStyle(R.drawable.ic_heart,R.drawable.bg_icon_peach,0xFFDF7160);list(e.getValue(),SymptomFragment::label,this::actions);
        }
    }
    public static String label(Symptom s) {
        String time=Formats.clock(s.occurredAt);
        return time+" • "+s.name+" • "+SymptomLogic.severityLabel(s.severity)+(s.note==null||s.note.isEmpty()?"":"\n"+s.note);
    }
    private void openEditor(long id) {
        Bundle b=new Bundle();
        b.putLong("id",id);
        androidx.navigation.fragment.NavHostFragment.findNavController(this).navigate(R.id.symptomEditor,b);
    }
    private void actions(Symptom s) {
        new AlertDialog.Builder(requireContext()).setTitle(s.name).setItems(new String[] {
            "Sửa","Xóa"
        }
        ,(d,w)-> {
            if(w==0)openEditor(s.id);else new AlertDialog.Builder(requireContext()).setTitle("Xóa triệu chứng?").setMessage("Thao tác này không thể hoàn tác.").setNegativeButton("Hủy",null).setPositiveButton("Xóa",(a,c)->vm.deleteSymptom(s)).show();
        }
        ).show();
    }
    @Override public void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        b.putInt("days",days);
        b.putString("query",query);
    }
}

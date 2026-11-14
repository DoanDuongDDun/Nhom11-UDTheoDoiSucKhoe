package com.example.vitality.ui.diary;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import java.util.*;
import com.example.vitality.R;
import com.example.vitality.databinding.*;
import com.example.vitality.ui.main.PageFragment;
import com.example.vitality.data.entity.*;
import com.example.vitality.utils.*;
/** Nhật ký sức khỏe: bố cục lấy từ bản demo (thẻ tổng kết, lối tắt nhanh, dòng thời gian). Dữ liệu và chức năng giữ nguyên. */
public class DiaryFragment extends PageFragment  {
    private static final String[] WEEKDAYS= {
        "Chủ nhật","Thứ hai","Thứ ba","Thứ tư","Thứ năm","Thứ sáu","Thứ bảy"
    }
    ;
    private static final String[] FILTER_KEYS= {
        "all","nutrition","water","activity"
    }
    ,FILTER_NAMES= {
        "Tất cả","Bữa ăn","Nước uống","Vận động"
    }
    ;
    private String filter="all";
    private boolean toolsOpen=false;
    @Override public void onViewCreated(View v,Bundle b) {
        if(b!=null) {
            filter=b.getString("filter","all");
            toolsOpen=b.getBoolean("tools");
        }
        heading("Nhật ký sức khỏe","Ghi nhận theo ngày • Dữ liệu lưu trên thiết bị");
        observeAll(this::render,vm.journals,vm.occurrences,vm.symptoms);
    }
    private static boolean blank(String s) {
        return s==null||s.trim().isEmpty();
    }
    private void render() {
        if(binding==null)return;
        clear();
        List<JournalEntry> all=vm.journals.getValue()==null?Collections.<JournalEntry>emptyList():vm.journals.getValue();
        long now=System.currentTimeMillis(),today=Formats.dayStart(now);
        // Thẻ tổng kết hôm nay
        int todayCount=0;
        for(JournalEntry e:all)if(e.startedAt>=today)todayCount++;
        Calendar c=Calendar.getInstance();
        WidgetDiaryHeroBinding hero=WidgetDiaryHeroBinding.inflate(getLayoutInflater(),binding.content,false);
        binding.content.addView(hero.getRoot());
        hero.heroDate.setText(WEEKDAYS[c.get(Calendar.DAY_OF_WEEK)-1]+", "+c.get(Calendar.DAY_OF_MONTH)+" tháng "+(c.get(Calendar.MONTH)+1));
        hero.heroSub.setText("Bạn đã ghi nhận "+todayCount+" hoạt động");
        hero.heroCount.setText(String.valueOf(todayCount));
        // Lối tắt nhanh
        LinearLayout row1=quickRow(),row2=quickRow();
        quick(row1,"Bữa ăn",R.drawable.ic_fork,R.drawable.bg_icon_lime,0xFF618E27,()->edit("nutrition",0));
        quick(row1,"Vận động",R.drawable.ic_shoe,R.drawable.bg_icon_mint,0xFF1D9367,()->edit("activity",0));
        quick(row2,"250 ml nước",R.drawable.ic_drop,R.drawable.bg_icon_blue,0xFF3785DC,()-> {
            JournalEntry e=new JournalEntry();e.amount=250;vm.run(()->vm.wellness.saveJournal(e),true);
        }
        );
        quick(row2,"Giấc ngủ",R.drawable.ic_moon,R.drawable.bg_icon_violet,0xFF7360DD,()->navigate(R.id.sleep));
        // Dòng thời gian
        List<JournalEntry> filtered=new ArrayList<>();
        for(JournalEntry e:all)if(!e.kind.equals("sleep")&&(filter.equals("all")||e.kind.equals(filter)))filtered.add(e);
        int filterIndex=Math.max(0,Arrays.asList(FILTER_KEYS).indexOf(filter));
        WidgetCardBinding timeline=card("DÒNG THỜI GIAN","Bữa ăn, nước và vận động","Lọc: "+FILTER_NAMES[filterIndex],()->new AlertDialog.Builder(requireContext()).setItems(FILTER_NAMES,(d,w)-> {
            filter=FILTER_KEYS[w];render();
        }
        ).show());
        endCard();
        if(filtered.isEmpty()) {
            android.widget.TextView empty=new android.widget.TextView(requireContext());
            empty.setText("Chưa có nhật ký. Chọn một mục ở trên để bắt đầu.");
            empty.setTextColor(0xFF718096);
            empty.setPadding(0,dp(12),0,dp(12));
            timeline.cardBody.addView(empty);
        }
        for(int i=0;i<filtered.size();i++) {
            JournalEntry e=filtered.get(i);
            ItemJournalRowBinding r=ItemJournalRowBinding.inflate(getLayoutInflater(),timeline.cardBody,false);
            timeline.cardBody.addView(r.getRoot());
            bindRow(r,e,today);
            if(i==filtered.size()-1)r.jrDivider.setVisibility(View.GONE);
            r.getRoot().setOnClickListener(x->actions(e));
            r.jrMore.setOnClickListener(x->actions(e));
        }
        // Công cụ sức khỏe
        button("Giấc ngủ, chu kỳ sinh hoạt và triệu chứng   "+(toolsOpen?"⌄":"›"),()-> {
            toolsOpen=!toolsOpen;render();
        }
        );
        if(toolsOpen) {
            double sleepSum=0;
            TreeMap<Long,Double> sleepByDay=new TreeMap<>();
            for(JournalEntry e:all)if(e.kind.equals("sleep")&&e.endedAt!=null&&e.endedAt>=now-7*Formats.DAY_MS&&e.endedAt<=now) {
                long day=Formats.dayStart(e.endedAt);
                sleepByDay.put(day,sleepByDay.getOrDefault(day,0d)+HealthMath.sleepMinutesOrZero(e.startedAt,e.endedAt,e.awakeMinutes)/60);
            }
            for(double h:sleepByDay.values())sleepSum+=h;
            int due=0,done=0;
            if(vm.occurrences.getValue()!=null)for(HabitOccurrence o:vm.occurrences.getValue())if(o.dueAt>=today&&o.dueAt<today+Formats.DAY_MS) {
                due++;
                if(o.status.equals("done"))done++;
            }
            int symptomCount=0;
            if(vm.symptoms.getValue()!=null)for(Symptom s:vm.symptoms.getValue())if(s.occurredAt>=now-7*Formats.DAY_MS&&s.occurredAt<=now)symptomCount++;
            WidgetCardBinding tools=card("CÔNG CỤ","Sức khỏe hằng ngày",null,null);
            endCard();
            tool(tools,R.drawable.ic_moon,R.drawable.bg_icon_violet,0xFF7360DD,"Giấc ngủ",sleepByDay.isEmpty()?"Chưa ghi nhận trong 7 ngày":"Trung bình 7 ngày: "+Formats.number(sleepSum/sleepByDay.size())+" giờ",()->navigate(R.id.sleep),false);
            tool(tools,R.drawable.ic_spark,R.drawable.bg_icon_mint,0xFF1D9367,"Chu kỳ sinh hoạt",due==0?"Chưa có lịch hôm nay":done+" / "+due+" đã hoàn thành hôm nay",()->navigate(R.id.habit),false);
            tool(tools,R.drawable.ic_heart,R.drawable.bg_icon_peach,0xFFDF7160,"Triệu chứng",symptomCount+" lần ghi nhận trong 7 ngày",()->navigate(R.id.symptom),false);
            tool(tools,R.drawable.ic_drop,R.drawable.bg_icon_blue,0xFF3785DC,"Nhập lượng nước khác","Tự nhập số ml đã uống",()->edit("water",0),true);
        }
    }
    private LinearLayout quickRow() {
        LinearLayout r=new LinearLayout(requireContext());
        r.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(-dp(4),dp(2),-dp(4),dp(2));
        binding.content.addView(r,lp);
        return r;
    }
    private void quick(LinearLayout row,String label,int icon,int iconBg,int tint,Runnable click) {
        ItemQuickBinding q=ItemQuickBinding.inflate(getLayoutInflater(),row,false);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);
        lp.setMargins(dp(4),0,dp(4),0);
        row.addView(q.getRoot(),lp);
        q.quickIcon.setImageResource(icon);
        q.quickIcon.setColorFilter(tint);
        q.quickIconBox.setBackgroundResource(iconBg);
        q.quickLabel.setText(label);
        q.getRoot().setOnClickListener(x->click.run());
    }
    private void tool(WidgetCardBinding card,int icon,int iconBg,int tint,String title,String sub,Runnable click,boolean last) {
        ItemToolRowBinding t=ItemToolRowBinding.inflate(getLayoutInflater(),card.cardBody,false);
        card.cardBody.addView(t.getRoot());
        t.toolIcon.setImageResource(icon);
        t.toolIcon.setColorFilter(tint);
        t.toolIconBox.setBackgroundResource(iconBg);
        t.toolTitle.setText(title);
        t.toolSub.setText(sub);
        if(last)t.toolDivider.setVisibility(View.GONE);
        t.getRoot().setOnClickListener(x->click.run());
    }
    private void bindRow(ItemJournalRowBinding r,JournalEntry e,long today) {
        int icon,iconBg,tint;
        String title,detail,value;
        if(e.kind.equals("water")) {
            icon=R.drawable.ic_drop;iconBg=R.drawable.bg_icon_blue;tint=0xFF3785DC;
            title="Nước uống";
            detail=blank(e.note)?"Uống nước":e.note;
            value=Formats.number(e.amount)+" ml";
        }
        else if(e.kind.equals("activity")) {
            icon=R.drawable.ic_shoe;iconBg=R.drawable.bg_icon_mint;tint=0xFF1D9367;
            title=blank(e.title)?"Vận động":e.title;
            detail=blank(e.note)?"Hoạt động thể chất":e.note;
            value=Formats.number(e.amount)+" phút";
        }
        else {
            icon=R.drawable.ic_fork;iconBg=R.drawable.bg_icon_lime;tint=0xFF618E27;
            title=blank(e.title)?"Bữa ăn":e.title;
            StringBuilder d=new StringBuilder();
            if(!blank(e.category))d.append(e.category);
            if(!blank(e.portion))d.append(d.length()>0?" • ":"").append(e.portion);
            if(!blank(e.note))d.append(d.length()>0?" • ":"").append(e.note);
            detail=d.length()==0?"Bữa ăn":d.toString();
            value=e.calories==null?"—":Formats.number(e.calories)+" kcal";
        }
        r.jrIcon.setImageResource(icon);
        r.jrIcon.setColorFilter(tint);
        r.jrIconBox.setBackgroundResource(iconBg);
        r.jrTitle.setText(title);
        r.jrDetail.setText(detail);
        r.jrValue.setText(value);
        r.jrTime.setText(Formats.shortTime(e.startedAt,today));
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
    @Override public void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        b.putString("filter",filter);
        b.putBoolean("tools",toolsOpen);
    }
}

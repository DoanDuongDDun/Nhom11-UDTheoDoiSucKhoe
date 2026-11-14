package com.example.vitality.ui.main;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.*;
import java.util.*;
import java.util.function.*;
import com.example.vitality.R;
import com.example.vitality.databinding.*;
import com.example.vitality.viewmodel.VitalityViewModel;
import com.example.vitality.utils.*;
import com.google.android.material.textfield.*;
import com.google.android.material.button.MaterialButton;
public abstract class PageFragment extends Fragment  {
    protected FragmentPageBinding binding;
    protected VitalityViewModel vm;
    private ViewGroup target;
    private boolean avatarObserved=false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPageBinding.inflate(inflater, container, false);
        vm = new ViewModelProvider(requireActivity()).get(VitalityViewModel.class);
        return binding.getRoot();
    }
    /**
     * Theo dõi nhiều LiveData nhưng chỉ dựng lại giao diện một lần: các thay đổi đến liên tiếp (ví dụ lúc mở màn hình,
     * khi cả 4-5 nguồn dữ liệu phát lần đầu) được gộp thành một lần render trên luồng chính.
     */
    protected void observeAll(Runnable render,LiveData<?>... sources) {
        Runnable coalesced=()-> {
            if(binding!=null)render.run();
        };
        for(LiveData<?> source:sources)source.observe(getViewLifecycleOwner(),x-> {
            if(binding==null)return;
            View root=binding.getRoot();
            root.removeCallbacks(coalesced);
            root.post(coalesced);
        }
        );
    }
    protected void heading(String title,String subtitle) {
        binding.title.setText(title);
        binding.subtitle.setText(subtitle);
    }
    protected void clear() {
        target=null;
        while(binding.content.getChildCount()>1)binding.content.removeViewAt(1);
    }
    /** Các widget()/text()/button()/field() được thêm vào thẻ đang mở (nếu có), ngược lại vào trang. */
    protected <T extends View>T widget(int layout) {
        ViewGroup parent=target!=null?target:binding.content;
        T v=(T)getLayoutInflater().inflate(layout,parent,false);
        parent.addView(v);
        return v;
    }
    /** Mở một thẻ (card) có tiêu đề; mọi widget thêm sau đó nằm trong thẻ cho tới khi gọi endCard(). */
    protected WidgetCardBinding card(String kicker,String title,String action,Runnable onAction) {
        WidgetCardBinding c=WidgetCardBinding.inflate(getLayoutInflater(),binding.content,false);
        binding.content.addView(c.getRoot());
        if(kicker!=null&&!kicker.isEmpty()) {
            c.cardKicker.setText(kicker);
            c.cardKicker.setVisibility(View.VISIBLE);
        }
        c.cardTitle.setText(title);
        if(action!=null) {
            c.cardAction.setText(action);
            c.cardAction.setVisibility(View.VISIBLE);
            c.cardAction.setOnClickListener(v->onAction.run());
        }
        target=c.cardBody;
        return c;
    }
    protected void endCard() {
        target=null;
    }
    /** Thẻ số liệu lớn ở đầu màn hình (nền gradient, số to, ghi chú, icon tròn bên phải). */
    protected WidgetStatHeroBinding hero(int background,int icon,String kicker,String value,String unit,String note) {
        WidgetStatHeroBinding h=WidgetStatHeroBinding.inflate(getLayoutInflater(),binding.content,false);
        h.getRoot().setBackgroundResource(background);
        binding.content.addView(h.getRoot());
        h.statIcon.setImageResource(icon);
        updateHero(h,kicker,value,unit,note);
        return h;
    }
    protected void updateHero(WidgetStatHeroBinding h,String kicker,String value,String unit,String note) {
        h.statKicker.setText(kicker);
        h.statValue.setText(value);
        h.statUnit.setText(unit);
        h.statNote.setText(note);
    }
    protected int dp(int value) {
        return Math.round(value*getResources().getDisplayMetrics().density);
    }
    /** Đặt chữ cái đầu trên ảnh đại diện và mở Cá nhân khi chạm. */
    @Override public void onStart() {
        super.onStart();
        if(binding==null||avatarObserved)return;
        avatarObserved=true;
        binding.avatar.setOnClickListener(v-> {
            androidx.navigation.NavController nav=NavHostFragment.findNavController(this);
            if(nav.getCurrentDestination()==null||nav.getCurrentDestination().getId()!=R.id.profile)nav.navigate(R.id.profile);
        }
        );
        vm.profile.observe(getViewLifecycleOwner(),p-> {
            if(binding!=null)binding.avatar.setText(Formats.initials(p==null?"":p.name));
        }
        );
    }
    protected TextView text(String s) {
        TextView v=widget(R.layout.widget_text);
        v.setText(s);
        return v;
    }
    protected MaterialButton button(String s,Runnable action) {
        MaterialButton b=widget(R.layout.widget_button);
        b.setText(s);
        b.setOnClickListener(v->action.run());
        if(s.startsWith("+")||s.startsWith("Lưu")||s.equals("Cập nhật số đo")) {
            b.setBackgroundTintList(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(),R.color.primary)));
            b.setTextColor(Color.WHITE);
            b.setStrokeWidth(0);
        }
        return b;
    }
    protected TextInputEditText field(String label,String value,boolean numeric) {
        TextInputLayout box=widget(R.layout.widget_field);
        box.setHint(label);
        TextInputEditText e=(TextInputEditText)box.getEditText();
        e.setId(View.generateViewId());
        if(numeric)e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setText(value);
        return e;
    }
    protected double number(TextInputEditText e,double min,double max) {
        try {
            return Validation.number(e.getText().toString(),min,max);
        }
        catch(IllegalArgumentException x) {
            e.setError(x.getMessage());
            e.requestFocus();
            throw x;
        }
    }
    protected Double optional(TextInputEditText e,double min,double max) {
        return e.getText().toString().trim().isEmpty()?null:number(e,min,max);
    }
    protected long date(TextInputEditText e) {
        try {
            return Validation.date(e.getText().toString());
        }
        catch(IllegalArgumentException x) {
            e.setError(x.getMessage());
            throw x;
        }
    }
    protected String value(TextInputEditText e) {
        return e.getText().toString().trim();
    }
    protected String n(Double d) {
        return d==null?"":Formats.number(d);
    }
    protected void navigate(int id) {
        NavHostFragment.findNavController(this).navigate(id);
    }
    protected void editor(long id) {
        Bundle b=new Bundle();
        b.putLong("id",id);
        NavHostFragment.findNavController(this).navigate(R.id.measurementEditor,b);
    }
    protected void back() {
        NavHostFragment.findNavController(this).popBackStack();
    }
    protected Spinner spinner(String[] labels,Consumer<Integer> onSelect) {
        Spinner s=widget(R.layout.widget_spinner);
        s.setAdapter(new ArrayAdapter<>(requireContext(),android.R.layout.simple_spinner_dropdown_item,labels));
        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id) {
                onSelect.accept(pos);
            }
            public void onNothingSelected(AdapterView<?> p) {
            }
        }
        );
        return s;
    }
    private int listIcon=R.drawable.ic_spark,listIconBg=R.drawable.bg_icon_mint,listTint=0xFF1D9367;
    /** Chọn icon cho danh sách tiếp theo (áp dụng cho một lần gọi list()). */
    protected void listStyle(int icon,int iconBg,int tint) {
        listIcon=icon;
        listIconBg=iconBg;
        listTint=tint;
    }
    /** Danh sách dạng thẻ: mỗi dòng có ô icon màu, dòng đầu đậm, các dòng sau nhỏ và nhạt (theo kiểu Nhật ký trong Figma). */
    protected <T> void list(List<T> rows,Function<T,String> label,Consumer<T> click) {
        final int icon=listIcon,iconBg=listIconBg,tint=listTint;
        listIcon=R.drawable.ic_spark;
        listIconBg=R.drawable.bg_icon_mint;
        listTint=0xFF1D9367;
        RecyclerView r=widget(R.layout.widget_list);
        r.setBackgroundResource(R.drawable.card);
        r.setLayoutManager(new LinearLayoutManager(requireContext()));
        r.setAdapter(new RecyclerView.Adapter<RowHolder>() {
            public RowHolder onCreateViewHolder(ViewGroup p,int t) {
                return new RowHolder(ItemJournalRowBinding.inflate(getLayoutInflater(),p,false));
            }
            public void onBindViewHolder(RowHolder h,int pos) {
                T row=rows.get(pos);
                String text=label.apply(row);
                int cut=text.indexOf('\n');
                ItemJournalRowBinding b=h.b;
                b.jrTitle.setText(cut<0?text:text.substring(0,cut));
                b.jrTitle.setMaxLines(2);
                b.jrDetail.setText(cut<0?"":text.substring(cut+1));
                b.jrDetail.setVisibility(cut<0?View.GONE:View.VISIBLE);
                b.jrIcon.setImageResource(icon);
                b.jrIcon.setColorFilter(tint);
                b.jrIconBox.setBackgroundResource(iconBg);
                b.jrValue.setVisibility(View.GONE);
                b.jrTime.setVisibility(View.GONE);
                b.jrDivider.setVisibility(pos==rows.size()-1?View.GONE:View.VISIBLE);
                b.getRoot().setOnClickListener(v->click.accept(row));
                b.jrMore.setOnClickListener(v->click.accept(row));
            }
            public int getItemCount() {
                return rows.size();
            }
        }
        );
    }
    /** Hai ô nhập đặt cạnh nhau trên một hàng (như hộp nhập số đo trong Figma). */
    protected TextInputEditText[] fieldPair(String label1,String value1,boolean numeric1,String label2,String value2,boolean numeric2) {
        LinearLayout row=new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        ViewGroup parent=target!=null?target:binding.content;
        parent.addView(row,new LinearLayout.LayoutParams(-1,-2));
        TextInputEditText[] out=new TextInputEditText[2];
        String[] labels= {
            label1,label2
        }
        ;
        String[] values= {
            value1,value2
        }
        ;
        boolean[] numeric= {
            numeric1,numeric2
        }
        ;
        for(int i=0;i<2;i++) {
            TextInputLayout box=(TextInputLayout)getLayoutInflater().inflate(R.layout.widget_field,row,false);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1f);
            if(i==0)lp.setMarginEnd(dp(5));
            else lp.setMarginStart(dp(5));
            row.addView(box,lp);
            box.setHint(labels[i]);
            TextInputEditText e=(TextInputEditText)box.getEditText();
            e.setId(View.generateViewId());
            if(numeric[i])e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
            e.setText(values[i]);
            out[i]=e;
        }
        return out;
    }
    static class RowHolder extends RecyclerView.ViewHolder {
        final ItemJournalRowBinding b;
        RowHolder(ItemJournalRowBinding b) {
            super(b.getRoot());
            this.b=b;
        }
    }
    @Override public void onDestroyView() {
        super.onDestroyView();
        binding=null;
        target=null;
        avatarObserved=false;
    }
}

package com.example.vitality.ui.main;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
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
    @Override public View onCreateView(LayoutInflater i,ViewGroup c,Bundle b) {
        binding=FragmentPageBinding.inflate(i,c,false);
        vm=new ViewModelProvider(requireActivity()).get(VitalityViewModel.class);
        return binding.getRoot();
    }
    protected void heading(String title,String subtitle) {
        binding.title.setText(title);
        binding.subtitle.setText(subtitle);
    }
    protected void clear() {
        while(binding.content.getChildCount()>2)binding.content.removeViewAt(2);
    }
    protected <T extends View>T widget(int layout) {
        T v=(T)getLayoutInflater().inflate(layout,binding.content,false);
        binding.content.addView(v);
        return v;
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
    protected <T> void list(List<T> rows,Function<T,String> label,Consumer<T> click) {
        RecyclerView r=widget(R.layout.widget_list);
        r.setLayoutManager(new LinearLayoutManager(requireContext()));
        r.setAdapter(new RecyclerView.Adapter<RowHolder>() {
            public RowHolder onCreateViewHolder(ViewGroup p,int t) {
                return new RowHolder(ItemTextBinding.inflate(getLayoutInflater(),p,false));
            }
            public void onBindViewHolder(RowHolder h,int pos) {
                T row=rows.get(pos);h.b.text.setText(label.apply(row));h.itemView.setOnClickListener(v->click.accept(row));
            }
            public int getItemCount() {
                return rows.size();
            }
        }
        );
    }
    static class RowHolder extends RecyclerView.ViewHolder {
        final ItemTextBinding b;
        RowHolder(ItemTextBinding b) {
            super(b.getRoot());
            this.b=b;
        }
    }
    @Override public void onDestroyView() {
        super.onDestroyView();
        binding=null;
    }
}

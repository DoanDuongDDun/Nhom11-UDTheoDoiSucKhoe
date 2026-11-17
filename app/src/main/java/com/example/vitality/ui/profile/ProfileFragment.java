package com.example.vitality.ui.profile;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.*;
import com.example.vitality.databinding.*;
import com.example.vitality.R;
import com.example.vitality.ui.main.PageFragment;
import com.example.vitality.data.entity.*;
import com.example.vitality.utils.*;
/** Cá nhân: tóm tắt, hồ sơ và cài đặt. */
public class ProfileFragment extends PageFragment  {
    private boolean loaded;
    private final List<TextInputEditText> fields=new ArrayList<>();
    private WidgetProfileSummaryBinding summary;
    private int template,gender;
    @Override public void onViewCreated(View v,Bundle state) {
        heading("Cá nhân","Hồ sơ và cài đặt");
        vm.profile.observe(getViewLifecycleOwner(),p-> {
            if(!loaded) {
                loaded=true;build(p==null?new UserProfile():p,state);
            }
            updateSummary();
        }
        );
        observeAll(this::updateSummary,vm.measurements,vm.journals,vm.symptoms);
    }
    private void updateSummary() {
        if(summary==null||binding==null)return;
        UserProfile p=vm.profile.getValue();
        String name=p==null?"":p.name.trim();
        summary.psAvatar.setText(Formats.initials(name));
        summary.psName.setText(name.isEmpty()?"Chưa đặt tên":name);
        Set<Long> days=new HashSet<>();
        if(vm.measurements.getValue()!=null)for(BodyMeasurement m:vm.measurements.getValue())days.add(Formats.dayStart(m.measuredAt));
        if(vm.journals.getValue()!=null)for(JournalEntry e:vm.journals.getValue())days.add(Formats.dayStart(e.startedAt));
        if(vm.symptoms.getValue()!=null)for(Symptom s:vm.symptoms.getValue())days.add(Formats.dayStart(s.occurredAt));
        summary.psDays.setText(String.valueOf(days.size()));
        summary.psStreak.setText(String.valueOf(HealthMath.currentStreak(days,Formats.dayStart(System.currentTimeMillis()))));
    }
    private void label(String s) {
        TextView t=text(s);
        t.setTextSize(12);
        t.setTextColor(0xFF65736B);
    }
    private void build(UserProfile profile,Bundle state) {
        summary=WidgetProfileSummaryBinding.inflate(getLayoutInflater(),binding.content,false);
        binding.content.addView(summary.getRoot());
        card(null,"Cài đặt và dữ liệu",null,null);
        button("Cài đặt thông báo",()->navigate(R.id.notificationSettings));
        button("Xem và xuất dữ liệu sức khỏe",()->navigate(R.id.healthDataExport));
        button("Chu kỳ sinh hoạt",()->navigate(R.id.habit));
        endCard();
        // Hồ sơ
        card("THÔNG TIN CƠ BẢN","Hồ sơ của bạn",null,null);
        fields.add(field("Tên / biệt danh",profile.name,false));
        TextInputEditText birthdayField = field("Ngày sinh (dd/mm/yyyy, tùy chọn)",profile.birthday,false);
        fields.add(birthdayField);
        TextInputLayout birthdayInput = (TextInputLayout) birthdayField.getParent().getParent();
        String[] genders= {
            "Không cung cấp","Nữ","Nam","Khác"
        }
        ;
        label("Giới tính");
        gender=state==null?Arrays.asList(genders).indexOf(profile.gender):state.getInt("gender");
        spinner(genders,x->gender=x).setSelection(Math.max(0,gender));
        label("Mẫu hình cơ thể");
        template=state==null?profile.bodyTemplate:state.getInt("template");
        spinner(new String[] {
            "Mẫu cân đối","Mẫu vai rộng"
        }
        ,x->template=x).setSelection(template);
        endCard();
        if(state!=null)for(int i=0;i<fields.size();i++)fields.get(i).setText(state.getString("f"+i));
        button("Lưu hồ sơ",()-> {
            String birthday = value(birthdayField);
            try {
                if (!birthday.isEmpty() && Validation.dateOnly(birthday) > System.currentTimeMillis()) {
                    throw new IllegalArgumentException("Ngày sinh không được ở tương lai");
                }
            } catch (IllegalArgumentException invalid) {
                birthdayInput.setError(invalid.getMessage());
                birthdayField.requestFocus();
                return;
            }
            birthdayInput.setError(null);
            String name=value(fields.get(0));
            String selectedGender=genders[Math.max(0,gender)];
            int selectedTemplate=template;
            vm.run(()->vm.wellness.saveProfileDetails(name,birthday,selectedGender,selectedTemplate),true);
        }
        );
        TextView privacy=text("Dữ liệu chỉ lưu trên thiết bị. BMI hiển thị giá trị, không dùng để chẩn đoán.");
        privacy.setTextSize(11);
        privacy.setTextColor(0xFF939D97);
        privacy.setGravity(android.view.Gravity.CENTER);
    }
    @Override public void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        b.putInt("gender",gender);
        b.putInt("template",template);
        for(int i=0;i<fields.size();i++)b.putString("f"+i,value(fields.get(i)));
    }
    @Override public void onDestroyView() {
        super.onDestroyView();
        loaded=false;
        summary=null;
        fields.clear();
    }
}

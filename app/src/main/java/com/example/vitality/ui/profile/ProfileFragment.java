package com.example.vitality.ui.profile;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import android.os.Build;
import android.Manifest;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.material.textfield.TextInputEditText;
import java.util.*;
import com.example.vitality.databinding.*;
import com.example.vitality.R;
import com.example.vitality.ui.main.PageFragment;
import com.example.vitality.data.entity.*;
import com.example.vitality.notification.ReminderScheduler;
import com.example.vitality.utils.*;
/** Cá nhân: thẻ tóm tắt, hồ sơ, mục tiêu và nhắc nhở theo bố cục demo; logic lưu hồ sơ và nhắc nhở giữ nguyên. */
public class ProfileFragment extends PageFragment  {
    private boolean loaded;
    private final List<TextInputEditText> fields=new ArrayList<>();
    private WidgetProfileSummaryBinding summary;
    private int template,gender;
    private final ActivityResultLauncher<String> permission=registerForActivityResult(new ActivityResultContracts.RequestPermission(),granted-> {
        vm.message.setValue(granted?"Đã cấp quyền thông báo":"Chưa cấp quyền thông báo; nhật ký vẫn hoạt động");ReminderScheduler.schedule(requireContext());
    }
    );
    @Override public void onViewCreated(View v,Bundle state) {
        heading("Cá nhân","Hồ sơ, mục tiêu và nhắc nhở");
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
        fields.add(field("Ngày sinh (dd/MM/yyyy, tùy chọn)",profile.birthday,false));
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
        // Mục tiêu
        card("MỤC TIÊU","Chỉ số mỗi ngày",null,null);
        fields.add(field("Mục tiêu cân nặng (kg, tùy chọn)",n(profile.targetWeight),true));
        fields.add(field("Mục tiêu vòng eo (cm, tùy chọn)",n(profile.targetWaist),true));
        fields.add(field("Mục tiêu nước (ml/ngày)",n(profile.waterGoal),true));
        fields.add(field("Mục tiêu vận động (phút/ngày)",n(profile.activityGoal),true));
        fields.add(field("Mục tiêu ngủ (giờ/ngày)",n(profile.sleepGoal),true));
        endCard();
        if(state!=null)for(int i=0;i<fields.size();i++)fields.get(i).setText(state.getString("f"+i));
        // Nhắc nhở
        WidgetCardBinding reminders=card("NHẮC NHỞ","Lịch chăm sóc sức khỏe",null,null);
        endCard();
        TextView note=new TextView(requireContext());
        note.setText("Android có thể gửi trễ do tiết kiệm pin. Từ chối quyền thông báo không ảnh hưởng việc lưu nhật ký.");
        note.setTextSize(12);
        note.setTextColor(0xFF718096);
        note.setPadding(0,dp(6),0,dp(6));
        reminders.cardBody.addView(note);
        String[][] items= {
            {
                "measure","Đo cơ thể","08:00 • Hằng ngày"
            }
            ,{
                "water","Uống nước","10:00 • Hằng ngày"
            }
            ,{
                "bed","Đi ngủ","22:00 • Hằng ngày"
            }
        }
        ;
        android.content.SharedPreferences prefs=requireContext().getSharedPreferences("reminders",0);
        for(int i=0;i<items.length;i++) {
            String key=items[i][0],title=items[i][1];
            ItemReminderRowBinding r=ItemReminderRowBinding.inflate(getLayoutInflater(),reminders.cardBody,false);
            reminders.cardBody.addView(r.getRoot());
            r.remTitle.setText(title);
            r.remSub.setText(items[i][2]);
            if(i==items.length-1)r.remDivider.setVisibility(View.GONE);
            r.remSwitch.setChecked(prefs.getBoolean(key,false));
            r.remSwitch.setOnCheckedChangeListener((btn,checked)-> {
                prefs.edit().putBoolean(key,checked).apply();
                if(checked&&Build.VERSION.SDK_INT>=33)permission.launch(Manifest.permission.POST_NOTIFICATIONS);
                ReminderScheduler.schedule(requireContext());
                vm.message.setValue(title+": "+(checked?"Đã bật":"Đã tắt"));
            }
            );
        }
        button("Lưu hồ sơ",()-> {
            try {
                UserProfile x=new UserProfile();
                x.name=value(fields.get(0));
                x.birthday=value(fields.get(1));
                if(!x.birthday.isEmpty()&&Validation.date(x.birthday+" 00:00")>System.currentTimeMillis())throw new IllegalArgumentException("Ngày sinh không được ở tương lai");
                x.gender=genders[Math.max(0,gender)];
                x.bodyTemplate=template;
                x.targetWeight=optional(fields.get(2),2,500);
                x.targetWaist=optional(fields.get(3),20,250);
                x.waterGoal=number(fields.get(4),100,10000);
                x.activityGoal=number(fields.get(5),1,1440);
                x.sleepGoal=number(fields.get(6),1,24);
                vm.run(()->vm.wellness.dao.save(x),true);
            }
            catch(Exception e) {
                vm.message.setValue(e.getMessage());
            }
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

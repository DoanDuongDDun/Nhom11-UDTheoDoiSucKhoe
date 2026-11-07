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
import com.example.vitality.ui.main.PageFragment;
import com.example.vitality.data.entity.*;
import com.example.vitality.notification.ReminderScheduler;
import com.example.vitality.utils.*;
public class ProfileFragment extends PageFragment  {
    private boolean loaded;
    private final List<TextInputEditText> fields=new ArrayList<>();
    private int template,gender;
    private final ActivityResultLauncher<String> permission=registerForActivityResult(new ActivityResultContracts.RequestPermission(),granted-> {
        vm.message.setValue(granted?"Đã cấp quyền thông báo":"Chưa cấp quyền thông báo; nhật ký vẫn hoạt động");ReminderScheduler.schedule(requireContext());
    }
    );
    @Override public void onViewCreated(View v,Bundle state) {
        heading("Cá nhân","Hồ sơ, mục tiêu và nhắc nhở");
        vm.profile.observe(getViewLifecycleOwner(),p-> {
            if(loaded)return;loaded=true;UserProfile profile=p==null?new UserProfile():p;fields.add(field("Tên / biệt danh",profile.name,false));fields.add(field("Ngày sinh (dd/MM/yyyy, tùy chọn)",profile.birthday,false));String[] genders= {
                "Không cung cấp","Nữ","Nam","Khác"
            }
            ;gender=state==null?Arrays.asList(genders).indexOf(profile.gender):state.getInt("gender");spinner(genders,x->gender=x).setSelection(Math.max(0,gender));template=state==null?profile.bodyTemplate:state.getInt("template");spinner(new String[] {
                "Mẫu cân đối","Mẫu vai rộng"
            }
            ,x->template=x).setSelection(template);fields.add(field("Mục tiêu cân nặng (kg, tùy chọn)",n(profile.targetWeight),true));fields.add(field("Mục tiêu vòng eo (cm, tùy chọn)",n(profile.targetWaist),true));fields.add(field("Mục tiêu nước (ml/ngày)",n(profile.waterGoal),true));fields.add(field("Mục tiêu vận động (phút/ngày)",n(profile.activityGoal),true));fields.add(field("Mục tiêu ngủ (giờ/ngày)",n(profile.sleepGoal),true));if(state!=null)for(int i=0;i<fields.size();i++)fields.get(i).setText(state.getString("f"+i));button("Lưu hồ sơ",()-> {
                try {
                    UserProfile x=new UserProfile();x.name=value(fields.get(0));x.birthday=value(fields.get(1));if(!x.birthday.isEmpty()&&Validation.date(x.birthday+" 00:00")>System.currentTimeMillis())throw new IllegalArgumentException("Ngày sinh không được ở tương lai");x.gender=genders[Math.max(0,gender)];x.bodyTemplate=template;x.targetWeight=optional(fields.get(2),2,500);x.targetWaist=optional(fields.get(3),20,250);x.waterGoal=number(fields.get(4),100,10000);x.activityGoal=number(fields.get(5),1,1440);x.sleepGoal=number(fields.get(6),1,24);vm.run(()->vm.wellness.dao.save(x),true);
                }
                catch(Exception e) {
                    vm.message.setValue(e.getMessage());
                }
            }
            ); text("Nhắc nhở — Android có thể gửi trễ do tiết kiệm pin. Từ chối quyền không ảnh hưởng lưu nhật ký.");for(String key:new String[] {
                "measure","water","bed","habit"
            }
            ) {
                String label=key.equals("measure")?"Đo cơ thể lúc 08:00":key.equals("water")?"Uống nước lúc 10:00":key.equals("bed")?"Đi ngủ lúc 22:00":"Lịch sinh hoạt";button(label+" — bật / tắt",()-> {
                    android.content.SharedPreferences prefs=requireContext().getSharedPreferences("reminders",0);boolean enabled=!prefs.getBoolean(key,false);prefs.edit().putBoolean(key,enabled).apply();if(enabled&&Build.VERSION.SDK_INT>=33)permission.launch(Manifest.permission.POST_NOTIFICATIONS);ReminderScheduler.schedule(requireContext());vm.message.setValue(label+": "+(enabled?"Đã bật":"Đã tắt"));
                }
                );
            }
            text("Dữ liệu chỉ lưu trên thiết bị. BMI hiển thị giá trị, không dùng để chẩn đoán.");
        }
        );
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
        fields.clear();
    }
}

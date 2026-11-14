package com.example.vitality.ui.goal;
import android.os.Bundle;import android.view.View;import android.widget.Spinner;import com.google.android.material.textfield.TextInputEditText;import com.google.android.material.button.MaterialButton;import com.example.vitality.ui.main.PageFragment;import com.example.vitality.data.entity.*;import com.example.vitality.utils.*;
public class GoalEditorFragment extends PageFragment {
 private final String[] kinds={"weight","waist","nutrition","activity","water","sleep"};
 private int type;private TextInputEditText title,target,keyword;private boolean saving;private android.widget.TextView guidance;
 @Override public void onViewCreated(View view,Bundle state){heading("Đặt mục tiêu","Bạn chọn đích đến, Vitality theo dõi tiến độ từ dữ liệu thực tế");
 int category=getArguments()==null?0:getArguments().getInt("category",0);
 int[][] groups={{0,1},{2,4},{3},{5}};int[] choices=groups[category];
 String[] allLabels={"Cân nặng mong muốn · kg","Vòng eo mong muốn · cm","Ăn món / bữa ăn · lần mỗi ngày","Vận động · phút mỗi ngày","Nước uống · ml mỗi ngày","Giấc ngủ · giờ mỗi ngày"};String[] labels=new String[choices.length];for(int i=0;i<choices.length;i++)labels[i]=allLabels[choices[i]];
 type=choices[0];text("Loại mục tiêu");Spinner spinner=spinner(labels,i->{type=choices[i];updateLabels();});

 title=field("Tên mục tiêu","",false);target=field("Giá trị muốn đạt","",true);keyword=field("Từ khóa (tùy chọn)","",false);
 guidance=text("");guidance.setTextSize(13);
 updateLabels();
 if(state!=null){type=state.getInt("type");for(int i=0;i<choices.length;i++)if(choices[i]==type)spinner.setSelection(i);title.setText(state.getString("title"));target.setText(state.getString("target"));keyword.setText(state.getString("keyword"));}
 updateLabels();
 MaterialButton save=button("Lưu mục tiêu",()->{try{Goal g=new Goal();g.kind=kinds[type];g.title=value(title);g.keyword=(type==2||type==3)?value(keyword):"";if(g.title.isEmpty()){title.setError("Nhập tên mục tiêu");return;}
 double[] min={2,20,1,1,1,1},max={500,250,30,1440,10000,24};g.target=number(target,min[type],max[type]);if(type==2&&g.target!=Math.rint(g.target)){target.setError("Số lần phải là số nguyên");return;}
 if(GoalProgress.body(g)){if(vm.measurements.getValue()==null||vm.measurements.getValue().isEmpty()){vm.message.setValue("Hãy nhập số đo cơ thể trước khi đặt mục tiêu");return;}BodyMeasurement m=vm.measurements.getValue().get(0);g.baseline=type==0?m.weightKg:m.waistCm;}
 saving=true;vm.run(()->vm.goalRepo.save(g),true);
 }catch(IllegalArgumentException ignored){}});
 vm.busy.observe(getViewLifecycleOwner(),busy->save.setEnabled(!busy));long initial=vm.saved.getValue();vm.saved.observe(getViewLifecycleOwner(),n->{if(saving&&n>initial){saving=false;back();}});
 }
 private com.google.android.material.textfield.TextInputLayout box(TextInputEditText field){
  android.view.ViewParent parent=field.getParent();
  while(!(parent instanceof com.google.android.material.textfield.TextInputLayout))parent=parent.getParent();
  return (com.google.android.material.textfield.TextInputLayout)parent;
 }
 private void updateLabels(){
  if(title==null||guidance==null)return;
  String[] examples={"Đạt cân nặng mong muốn","Đạt vòng eo mong muốn","Ăn rau mỗi ngày","Chạy bộ mỗi ngày","Uống đủ nước mỗi ngày","Ngủ đủ giấc mỗi ngày"};
  String[] units={"Cân nặng muốn đạt (kg)","Vòng eo muốn đạt (cm)","Số lần ăn mỗi ngày","Thời gian vận động (phút/ngày)","Lượng nước (ml/ngày)","Thời gian ngủ (giờ/ngày)"};
  box(title).setHint("Tên mục tiêu");box(title).setHelperText("Ví dụ: "+examples[type]);
  box(target).setHint(units[type]);
  box(keyword).setVisibility(type==2||type==3?View.VISIBLE:View.GONE);
  box(keyword).setHint(type==2?"Tên món cần tính (tùy chọn)":"Tên hoạt động (tùy chọn)");
  String[] help={
   "Lấy cân nặng gần nhất làm mốc ban đầu. Hãy nhập số đo trước khi đặt mục tiêu.",
   "Lấy vòng eo gần nhất làm mốc ban đầu. Hãy nhập số đo trước khi đặt mục tiêu.",
   "Ví dụ: nhập 1 lần, từ khóa “rau”. Nhật ký món có chữ “rau” được tính. Để trống từ khóa để tính mọi bữa ăn.",
   "Ví dụ: nhập 30 phút, từ khóa “chạy”. Nhật ký chạy bộ được tính. Để trống từ khóa để tính mọi hoạt động.",
   "2 lít = 2.000 ml. Đặt 2.000 ml, ghi uống 1.000 ml thì thanh tiến độ đạt 50%.",
   "Tổng thời gian ngủ được tính theo ngày thức dậy, trừ thời gian thức giữa giấc."};
  guidance.setText(help[type]);
 }
 @Override public void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putInt("type",type);b.putString("title",value(title));b.putString("target",value(target));b.putString("keyword",value(keyword));}
}

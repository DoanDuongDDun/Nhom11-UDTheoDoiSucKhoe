package com.example.vitality.viewmodel;
import android.app.Application;
import android.os.*;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import java.util.*;
import com.example.vitality.data.entity.*;
import com.example.vitality.data.repository.*;
public class VitalityViewModel extends AndroidViewModel  {
    public final BodyRepository body;
    public final GoalRepository goalRepo;
    public final LiveData<List<Goal>> goals;
    public final LiveData<List<PeriodEntry>> periods;
    public final WellnessRepository wellness;
    public final SymptomRepository symptomRepo;
    public final LiveData<List<Symptom>> symptoms;
    public final LiveData<List<BodyMeasurement>> measurements;
    public final LiveData<List<JournalEntry>> journals;
    public final LiveData<UserProfile> profile;
    public final LiveData<List<HabitSchedule>> habits;
    public final LiveData<List<HabitOccurrence>> occurrences;
    public final MutableLiveData<String> message=new MutableLiveData<>();
    public final MutableLiveData<Boolean> busy=new MutableLiveData<>(false);
    public final MutableLiveData<Long> saved=new MutableLiveData<>(0L);
    public VitalityViewModel(@NonNull Application app) {
        super(app);
        body=new BodyRepository(app);
        goalRepo=new GoalRepository(app);goals=goalRepo.observe();
        wellness=new WellnessRepository(app);periods=wellness.periods.observeAll();
        symptomRepo=new SymptomRepository(app);
        symptoms=symptomRepo.observeAll();
        measurements=body.observeAll();
        journals=wellness.dao.journals();
        profile=wellness.dao.profile();
        habits=wellness.dao.habits();
        occurrences=wellness.dao.occurrences();
        refresh();
    }
    public void refresh() {
        run(()-> {
            wellness.refreshHabits();com.example.vitality.notification.ReminderScheduler.schedule(getApplication());
        }
        ,false);
    }
    /** Một Handler dùng chung thay vì tạo mới cho mỗi lần lưu. */
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    /** Kết thúc một thao tác ghi (gọi từ luồng nền): trả về luồng chính, tắt busy, và tăng bộ đếm lưu + hiện thông báo tương ứng. */
    private void finish(String successMessage,String errorMessage) {
        MAIN.post(()-> {
            busy.setValue(false);
            if(successMessage!=null) {
                saved.setValue(saved.getValue()+1);
                message.setValue(successMessage);
            }
            else message.setValue(errorMessage);
        }
        );
    }
    public void run(Runnable action,boolean notify) {
        busy.setValue(true);
        WellnessRepository.IO.execute(()-> {
            try {
                action.run();
                MAIN.post(()-> {
                    busy.setValue(false);
                    if(notify) {
                        saved.setValue(saved.getValue()+1);
                        message.setValue("Đã lưu");
                    }
                }
                );
            }
            catch(Exception e) {
                finish(null,"Không thể lưu: "+e.getMessage());
            }
        }
        );
    }
    public void save(BodyMeasurement m) {
        busy.setValue(true);
        body.save(m,e->finish(e==null?"Lưu thành công":null,"Lỗi lưu dữ liệu!"));
    }
    public void saveOverwrite(BodyMeasurement m,List<BodyMeasurement> conflicts) {
        busy.setValue(true);
        body.saveOverwrite(m,conflicts,e->finish(e==null?"Lưu thành công":null,"Lỗi lưu dữ liệu!"));
    }
    public void saveSymptom(Symptom s) {
        busy.setValue(true);
        WellnessRepository.IO.execute(()-> {
            try {
                symptomRepo.save(s);
                finish("Lưu thành công",null);
            }
            catch(Exception e) {
                finish(null,"Lỗi lưu dữ liệu!");
            }
        }
        );
    }
    public void deleteSymptom(Symptom s) {
        WellnessRepository.IO.execute(()-> {
            try {
                symptomRepo.delete(s);
                message.postValue("Đã xóa triệu chứng");
            }
            catch(Exception e) {
                message.postValue("Lỗi lưu dữ liệu!");
            }
        }
        );
    }
    public void delete(BodyMeasurement m) {
        body.delete(m,e->message.postValue(e==null?"Đã xóa số đo":"Không thể xóa: "+e.getMessage()));
    }
}

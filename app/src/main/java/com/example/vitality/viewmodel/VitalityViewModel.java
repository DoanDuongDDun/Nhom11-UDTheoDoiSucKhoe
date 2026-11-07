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
    public final WellnessRepository wellness;
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
        wellness=new WellnessRepository(app);
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
    public void run(Runnable action,boolean notify) {
        busy.setValue(true);
        WellnessRepository.IO.execute(()-> {
            try {
                action.run();new Handler(Looper.getMainLooper()).post(()-> {
                    busy.setValue(false);if(notify) {
                        saved.setValue(saved.getValue()+1);message.setValue("Đã lưu");
                    }
                }
                );
            }
            catch(Exception e) {
                new Handler(Looper.getMainLooper()).post(()-> {
                    busy.setValue(false);message.setValue("Không thể lưu: "+e.getMessage());
                }
                );
            }
        }
        );
    }
    public void save(BodyMeasurement m) {
        busy.setValue(true);
        body.save(m,e->new Handler(Looper.getMainLooper()).post(()-> {
            busy.setValue(false);if(e==null) {
                saved.setValue(saved.getValue()+1);message.setValue("Đã lưu số đo");
            }
            else message.setValue("Không thể lưu: "+e.getMessage());
        }
        ));
    }
    public void delete(BodyMeasurement m) {
        body.delete(m,e->message.postValue(e==null?"Đã xóa số đo":"Không thể xóa: "+e.getMessage()));
    }
}

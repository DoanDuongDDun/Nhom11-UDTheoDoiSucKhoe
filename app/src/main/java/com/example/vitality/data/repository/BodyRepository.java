package com.example.vitality.data.repository;
import android.content.Context;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.example.vitality.data.database.VitalityDatabase;
import com.example.vitality.data.dao.BodyMeasurementDao;
import com.example.vitality.data.entity.BodyMeasurement;
public class BodyRepository  {
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    private final BodyMeasurementDao dao;
    public BodyRepository(Context context) {
        dao=VitalityDatabase.get(context).measurements();
    }
    public LiveData<List<BodyMeasurement>> observeAll() {
        return dao.observeAll();
    }
    public interface Result  {
        void complete(Exception error);
    }
    public void save(BodyMeasurement value, Result result) {
        IO.execute(()-> {
            try {
                value.updatedAt=System.currentTimeMillis();if(value.id==0)dao.insert(value);else if(dao.update(value)!=1)throw new IllegalStateException("Bản ghi không còn tồn tại");result.complete(null);
            }
            catch(Exception e) {
                result.complete(e);
            }
        }
        );
    }
    public void delete(BodyMeasurement value,Result result) {
        IO.execute(()-> {
            try {
                if(dao.delete(value)!=1)throw new IllegalStateException("Bản ghi không còn tồn tại");result.complete(null);
            }
            catch(Exception e) {
                result.complete(e);
            }
        }
        );
    }
}

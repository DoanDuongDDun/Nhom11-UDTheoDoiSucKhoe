package com.example.vitality.data.repository;
import android.content.Context;
import androidx.lifecycle.LiveData;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import com.example.vitality.data.database.VitalityDatabase;
import com.example.vitality.data.dao.BodyMeasurementDao;
import com.example.vitality.data.entity.BodyMeasurement;
public class BodyRepository  {
    private static final ExecutorService IO=WellnessRepository.IO;
    private final VitalityDatabase db;
    private final BodyMeasurementDao dao;
    public BodyRepository(Context context) {
        db=VitalityDatabase.get(context);
        dao=db.measurements();
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
    /** Ghi đè các số đo cùng ngày: số đo mới thay thế bản ghi trùng (giữ id cũ), các bản trùng còn lại bị xóa. Cả quá trình nằm trong một transaction. */
    public void saveOverwrite(BodyMeasurement value,List<BodyMeasurement> conflicts,Result result) {
        IO.execute(()-> {
            try {
                db.runInTransaction(()-> {
                    value.updatedAt=System.currentTimeMillis();List<BodyMeasurement> toDelete=new ArrayList<>(conflicts);if(value.id==0&&!toDelete.isEmpty()) {
                        BodyMeasurement target=toDelete.remove(0);value.id=target.id;value.createdAt=target.createdAt;
                    }
                    for(BodyMeasurement c:toDelete)dao.delete(c);if(value.id==0)dao.insert(value);else if(dao.update(value)!=1)throw new IllegalStateException("Bản ghi không còn tồn tại");
                }
                );result.complete(null);
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

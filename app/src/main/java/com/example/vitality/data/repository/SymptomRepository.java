package com.example.vitality.data.repository;
import android.content.Context;
import androidx.lifecycle.LiveData;
import java.util.List;
import com.example.vitality.data.dao.SymptomDao;
import com.example.vitality.data.database.VitalityDatabase;
import com.example.vitality.data.entity.Symptom;
/** Các hàm ghi/xóa chạy trên luồng nền (xem VitalityViewModel). */
public class SymptomRepository  {
    private final SymptomDao dao;
    public SymptomRepository(Context context) {
        dao=VitalityDatabase.get(context).symptoms();
    }
    public LiveData<List<Symptom>> observeAll() {
        return dao.observeAll();
    }
    public void save(Symptom s) {
        if(s.id==0)dao.insert(s);
        else if(dao.update(s)!=1)throw new IllegalStateException("Bản ghi không còn tồn tại");
    }
    public void delete(Symptom s) {
        if(dao.delete(s)!=1)throw new IllegalStateException("Bản ghi không còn tồn tại");
    }
}

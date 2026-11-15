package com.example.vitality.data.dao;
import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;
import com.example.vitality.data.entity.Symptom;
@Dao public interface SymptomDao  {
    @Query("SELECT * FROM symptoms ORDER BY occurredAt DESC, id DESC") LiveData<List<Symptom>> observeAll();
    @Query("SELECT * FROM symptoms WHERE occurredAt>=:start AND occurredAt<:end ORDER BY occurredAt DESC, id DESC")
    List<Symptom> between(long start, long end);
    @Insert long insert(Symptom symptom);
    @Update int update(Symptom symptom);
    @Delete int delete(Symptom symptom);
}

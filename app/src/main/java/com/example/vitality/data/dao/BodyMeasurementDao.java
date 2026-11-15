package com.example.vitality.data.dao;
import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;
import com.example.vitality.data.entity.BodyMeasurement;
@Dao public interface BodyMeasurementDao  {
    @Query("SELECT * FROM body_measurements ORDER BY measuredAt DESC, id DESC") LiveData<List<BodyMeasurement>> observeAll();
    @Query("SELECT * FROM body_measurements ORDER BY measuredAt DESC, id DESC LIMIT 1")
    BodyMeasurement latest();
    @Query("SELECT * FROM body_measurements WHERE measuredAt>=:start AND measuredAt<:end ORDER BY measuredAt DESC, id DESC")
    List<BodyMeasurement> between(long start, long end);
    @Insert long insert(BodyMeasurement measurement);
    @Update int update(BodyMeasurement measurement);
    @Delete int delete(BodyMeasurement measurement);
}

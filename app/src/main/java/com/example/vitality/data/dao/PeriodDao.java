package com.example.vitality.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;
import com.example.vitality.data.entity.PeriodEntry;

@Dao public interface PeriodDao {
    @Query("SELECT * FROM periods ORDER BY startDay DESC") LiveData<List<PeriodEntry>> observeAll();
    @Query("SELECT * FROM periods ORDER BY startDay DESC") List<PeriodEntry> all();
    @Query("SELECT * FROM periods WHERE startDay < :until AND endDay >= :from ORDER BY startDay DESC, id DESC")
    List<PeriodEntry> between(long from, long until);
    @Query("SELECT COUNT(*) FROM periods WHERE id != :id AND startDay <= :end AND endDay >= :start") int overlaps(long id, long start, long end);
    @Insert long insert(PeriodEntry entry);
    @Update void update(PeriodEntry entry);
    @Delete void delete(PeriodEntry entry);
}

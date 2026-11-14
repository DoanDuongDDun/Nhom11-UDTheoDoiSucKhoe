package com.example.vitality.data.dao;
import androidx.room.*;
import androidx.lifecycle.LiveData;
import java.util.List;
import com.example.vitality.data.entity.*;
@Dao public interface WellnessDao  {
    @Query("SELECT * FROM journal ORDER BY startedAt DESC,id DESC") LiveData<List<JournalEntry>> journals();
    @Query("SELECT COUNT(*) FROM journal WHERE kind='sleep' AND endedAt IS NULL AND id!=:id") int activeSleepsExcept(long id);
    @Query("SELECT COUNT(*) FROM journal WHERE kind='sleep' AND id!=:id AND startedAt<:end AND COALESCE(endedAt,9223372036854775807)>:start") int overlappingSleeps(long id,long start,long end);
    @Insert long insert(JournalEntry e);
    @Update void update(JournalEntry e);
    @Delete void delete(JournalEntry e);
    @Query("SELECT * FROM profile WHERE id=1") LiveData<UserProfile> profile();
    @Insert(onConflict=OnConflictStrategy.REPLACE) void save(UserProfile p);
    @Query("SELECT * FROM habits WHERE deleted=0 ORDER BY hour,minute") LiveData<List<HabitSchedule>> habits();
    @Query("SELECT * FROM habits") List<HabitSchedule> allHabits();
    @Insert long insert(HabitSchedule h);
    @Update void update(HabitSchedule h);
    @Query("SELECT * FROM occurrences ORDER BY dueAt DESC") LiveData<List<HabitOccurrence>> occurrences();
    @Query("SELECT * FROM occurrences WHERE dueAt>:now AND dueAt<=:end AND status='pending' ORDER BY dueAt") List<HabitOccurrence> upcoming(long now,long end);
    @Insert(onConflict=OnConflictStrategy.IGNORE) void insert(HabitOccurrence o);
    @Update void update(HabitOccurrence o);
    @Query("DELETE FROM occurrences WHERE scheduleId=:id AND dueAt>:now") void removeFuture(long id,long now);
    @Query("UPDATE occurrences SET status='missed' WHERE status='pending' AND dueAt+3600000<:now") void markMissed(long now);
}

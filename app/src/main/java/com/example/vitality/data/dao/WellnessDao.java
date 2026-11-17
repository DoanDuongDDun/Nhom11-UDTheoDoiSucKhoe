package com.example.vitality.data.dao;
import androidx.room.*;
import androidx.lifecycle.LiveData;
import java.util.List;
import com.example.vitality.data.entity.*;
@Dao public interface WellnessDao  {
    @Query("SELECT * FROM journal ORDER BY startedAt DESC,id DESC") LiveData<List<JournalEntry>> journals();
    @Query("SELECT * FROM journal WHERE id=:id") JournalEntry journal(long id);
    @Query("SELECT * FROM journal WHERE (kind=:kind OR :kind IS NULL) AND "
            + "((kind='sleep' AND endedAt>=:start AND endedAt<:end) "
            + "OR (kind!='sleep' AND startedAt>=:start AND startedAt<:end)) "
            + "ORDER BY CASE WHEN kind='sleep' THEN endedAt ELSE startedAt END DESC, id DESC")
    List<JournalEntry> journalBetween(String kind, long start, long end);
    @Query("SELECT * FROM journal WHERE kind IN ('activity', 'steps') "
            + "AND startedAt>=:start AND startedAt<:end ORDER BY startedAt DESC, id DESC")
    List<JournalEntry> activityBetween(long start, long end);
    @Query("SELECT COUNT(*) FROM journal WHERE kind='sleep' AND endedAt IS NULL AND id!=:id") int activeSleepsExcept(long id);
    @Query("SELECT COUNT(*) FROM journal WHERE kind='sleep' AND id!=:id AND startedAt<:end AND COALESCE(endedAt,9223372036854775807)>:start") int overlappingSleeps(long id,long start,long end);
    @Insert long insert(JournalEntry e);
    @Update void update(JournalEntry e);
    @Delete void delete(JournalEntry e);
    @Query("SELECT * FROM profile WHERE id=1") LiveData<UserProfile> profile();
    @Query("SELECT * FROM profile WHERE id=1") UserProfile savedProfile();
    @Insert(onConflict=OnConflictStrategy.REPLACE) void save(UserProfile p);
    @Query("SELECT * FROM habits WHERE deleted=0 ORDER BY hour,minute") LiveData<List<HabitSchedule>> habits();
    @Query("SELECT * FROM habits") List<HabitSchedule> allHabits();
    @Insert long insert(HabitSchedule h);
    @Update void update(HabitSchedule h);
    @Query("SELECT * FROM occurrences ORDER BY dueAt DESC") LiveData<List<HabitOccurrence>> occurrences();
    @Query("SELECT * FROM occurrences WHERE dueAt>:now AND dueAt<=:end AND status='pending' ORDER BY dueAt") List<HabitOccurrence> upcoming(long now,long end);
    @Query("SELECT o.* FROM occurrences o JOIN habits h ON h.id=o.scheduleId "
            + "WHERE o.dueAt>:since AND o.dueAt<=:now AND o.status='pending' "
            + "AND h.paused=0 AND h.deleted=0 ORDER BY o.dueAt")
    List<HabitOccurrence> dueReminders(long since, long now);
    @Query("SELECT o.* FROM occurrences o JOIN habits h ON h.id=o.scheduleId "
            + "WHERE o.id=:id AND h.deleted=0")
    HabitOccurrence findActiveOccurrence(long id);
    @Query("SELECT * FROM occurrences WHERE dueAt>=:start AND dueAt<:end ORDER BY dueAt DESC, id DESC")
    List<HabitOccurrence> occurrencesBetween(long start, long end);
    @Insert(onConflict=OnConflictStrategy.IGNORE) void insert(HabitOccurrence o);
    @Update void update(HabitOccurrence o);
    @Query("DELETE FROM occurrences WHERE scheduleId=:id AND dueAt>:now") void removeFuture(long id,long now);
    @Query("UPDATE occurrences SET status='missed' WHERE status='pending' AND dueAt+3600000<:now") void markMissed(long now);
}

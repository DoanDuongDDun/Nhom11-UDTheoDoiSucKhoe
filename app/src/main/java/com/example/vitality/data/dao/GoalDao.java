package com.example.vitality.data.dao;
import androidx.room.*;import androidx.lifecycle.LiveData;import java.util.List;import com.example.vitality.data.entity.Goal;
@Dao public interface GoalDao {
 @Query("SELECT * FROM goals ORDER BY id") LiveData<List<Goal>> observe();
 @Query("SELECT * FROM goals WHERE kind=:kind AND keyword=:keyword ORDER BY id DESC LIMIT 1") Goal find(String kind,String keyword);
 @Update void update(Goal g);
 @Insert long insert(Goal g);
 @Delete void delete(Goal g);
}

package com.example.vitality.data.repository;
import android.content.Context;import androidx.lifecycle.LiveData;import java.util.List;import com.example.vitality.data.entity.Goal;import com.example.vitality.data.dao.GoalDao;import com.example.vitality.data.database.VitalityDatabase;
public class GoalRepository {
 private final GoalDao dao;private final VitalityDatabase db;
 public GoalRepository(Context c){db=VitalityDatabase.get(c);dao=db.goals();}
 public LiveData<List<Goal>> observe(){return dao.observe();}
 public void save(Goal g){db.runInTransaction(()->{Goal old=dao.find(g.kind,g.keyword);if(old!=null){g.id=old.id;g.baseline=old.baseline;g.createdAt=old.createdAt;dao.update(g);}else dao.insert(g);});}
 public void delete(Goal g){dao.delete(g);}
}

package com.example.vitality.data.entity;
import androidx.room.*;
@Entity(tableName="goals") public class Goal {
 @PrimaryKey(autoGenerate=true) public long id;
 public String kind="activity", title="", keyword="";
 public double target=1, baseline=0;
 public long createdAt=System.currentTimeMillis();
}

package com.example.vitality.data.entity;
import androidx.room.*;
@Entity(tableName="habits") public class HabitSchedule  {
    @PrimaryKey(autoGenerate=true) public long id;
    public String title="",note="";
    public int hour=8,minute=0,weekdays=127;
    public long startsAt=System.currentTimeMillis(),generatedThrough=0;
    public Long endsAt;
    public boolean paused=false,deleted=false;
}

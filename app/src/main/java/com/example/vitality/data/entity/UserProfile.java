package com.example.vitality.data.entity;
import androidx.room.*;
@Entity(tableName="profile") public class UserProfile  {
    @PrimaryKey public long id=1;
    public String name="",birthday="",gender="Không cung cấp";
    public int bodyTemplate=0;
    public Double targetWeight,targetWaist;
    public double waterGoal=2000,activityGoal=30,sleepGoal=8;
}

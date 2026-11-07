package com.example.vitality.data.entity;
import androidx.room.*;
@Entity(tableName="occurrences",foreignKeys=@ForeignKey(entity=HabitSchedule.class,parentColumns="id",childColumns="scheduleId",onDelete=ForeignKey.RESTRICT),indices= {
    @Index(value= {
        "scheduleId","dueAt"
    }
    ,unique=true)
}
) public class HabitOccurrence  {
    @PrimaryKey(autoGenerate=true) public long id;
    public long scheduleId,dueAt;
    public String title="",status="pending";
}

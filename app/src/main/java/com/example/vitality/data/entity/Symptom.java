package com.example.vitality.data.entity;
import androidx.room.*;
/** Một lần ghi nhận triệu chứng. severity: 1 = nhẹ, 2 = vừa, 3 = nặng. */
@Entity(tableName="symptoms",indices= {
    @Index("occurredAt")
}
) public class Symptom  {
    @PrimaryKey(autoGenerate=true) public long id;
    public String name="",note="";
    public int severity=1;
    public long occurredAt=System.currentTimeMillis();
    public long createdAt=System.currentTimeMillis();
}

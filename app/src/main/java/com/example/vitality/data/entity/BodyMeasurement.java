package com.example.vitality.data.entity;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Index;
@Entity(tableName="body_measurements", indices= {
    @Index("measuredAt")
}
) public class BodyMeasurement  {
    @PrimaryKey(autoGenerate=true) public long id;
    public long profileId=1;
    public long measuredAt=System.currentTimeMillis();
    public double heightCm, weightKg, chestCm, waistCm, hipCm;
    public Double upperArmCm, thighCm, calfCm;
    public int bodyTemplate=0, rendererVersion=1;
    public String note="";
    public long createdAt=System.currentTimeMillis(), updatedAt=System.currentTimeMillis();
}

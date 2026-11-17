package com.example.vitality.data.entity;
import androidx.room.*;
@Entity(tableName="journal",indices= {
    @Index("startedAt"),@Index("kind")
}
) public class JournalEntry  {
    public static final String KIND_STEPS = "steps";

    @PrimaryKey(autoGenerate=true) public long id;
    public String kind="water",title="",category="",portion="",note="";
    public long startedAt=System.currentTimeMillis();
    public Long endedAt;
    public double amount=0,awakeMinutes=0;
    public Double calories,distanceKm;
    public int quality=3;

    public String nameOr(String fallback) {
        if (title != null && !title.trim().isEmpty()) return title.trim();
        if (note != null && !note.trim().isEmpty()) return note.trim();
        return fallback;
    }
}

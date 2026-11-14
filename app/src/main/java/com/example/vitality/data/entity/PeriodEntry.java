package com.example.vitality.data.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "periods", indices = {@Index("startDay")})
public class PeriodEntry {
    @PrimaryKey(autoGenerate = true) public long id;
    public long startDay;
    public long endDay;
    public String symptoms = "";
    public String note = "";
}

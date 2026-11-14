package com.example.vitality.data.database;
import android.content.Context;
import androidx.room.*;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.example.vitality.data.entity.*;
import com.example.vitality.data.dao.*;
@Database(entities= {
    BodyMeasurement.class,JournalEntry.class,UserProfile.class,HabitSchedule.class,HabitOccurrence.class,Symptom.class
}
,version=3,exportSchema=true) public abstract class VitalityDatabase extends RoomDatabase  {
    public abstract BodyMeasurementDao measurements();
    public abstract WellnessDao wellness();
    public abstract SymptomDao symptoms();
    public static final Migration MIGRATION_1_2=new Migration(1,2) {
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS journal (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,kind TEXT,title TEXT,category TEXT,portion TEXT,note TEXT,startedAt INTEGER NOT NULL,endedAt INTEGER,amount REAL NOT NULL,awakeMinutes REAL NOT NULL,calories REAL,distanceKm REAL,quality INTEGER NOT NULL)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_journal_startedAt ON journal(startedAt)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_journal_kind ON journal(kind)");
            db.execSQL("CREATE TABLE IF NOT EXISTS profile (id INTEGER NOT NULL PRIMARY KEY,name TEXT,birthday TEXT,gender TEXT,bodyTemplate INTEGER NOT NULL,targetWeight REAL,targetWaist REAL,waterGoal REAL NOT NULL,activityGoal REAL NOT NULL,sleepGoal REAL NOT NULL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS habits (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,title TEXT,note TEXT,hour INTEGER NOT NULL,minute INTEGER NOT NULL,weekdays INTEGER NOT NULL,startsAt INTEGER NOT NULL,generatedThrough INTEGER NOT NULL,endsAt INTEGER,paused INTEGER NOT NULL,deleted INTEGER NOT NULL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS occurrences (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,scheduleId INTEGER NOT NULL,dueAt INTEGER NOT NULL,title TEXT,status TEXT,FOREIGN KEY(scheduleId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE RESTRICT)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_occurrences_scheduleId_dueAt ON occurrences(scheduleId,dueAt)");
        }
    }
    ;
    public static final Migration MIGRATION_2_3=new Migration(2,3) {
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS symptoms (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,name TEXT,note TEXT,severity INTEGER NOT NULL,occurredAt INTEGER NOT NULL,createdAt INTEGER NOT NULL)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_symptoms_occurredAt ON symptoms(occurredAt)");
        }
    }
    ;
    private static volatile VitalityDatabase instance;
    public static VitalityDatabase get(Context c) {
        if(instance==null)synchronized(VitalityDatabase.class) {
            if(instance==null)instance=Room.databaseBuilder(c.getApplicationContext(),VitalityDatabase.class,"vitality.db").addMigrations(MIGRATION_1_2,MIGRATION_2_3).build();
        }
        return instance;
    }
}

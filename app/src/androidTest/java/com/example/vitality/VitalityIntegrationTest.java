package com.example.vitality;
import android.content.Context;
import android.content.Intent;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.room.Room;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import com.example.vitality.data.database.*;
import com.example.vitality.data.entity.*;
import java.util.*;
@RunWith(AndroidJUnit4.class) public class VitalityIntegrationTest  {
    @Test public void roomPersistsAndUpdatesMeasurement() {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="integration-test.db";
        c.deleteDatabase(name);
        VitalityDatabase db=Room.databaseBuilder(c,VitalityDatabase.class,name).build();
        BodyMeasurement m=new BodyMeasurement();
        m.heightCm=170;
        m.weightKg=65;
        m.chestCm=90;
        m.waistCm=75;
        m.hipCm=95;
        long id=db.measurements().insert(m);
        db.close();
        db=Room.databaseBuilder(c,VitalityDatabase.class,name).build();
        android.database.Cursor cursor=db.getOpenHelper().getReadableDatabase().query("SELECT waistCm FROM body_measurements WHERE id="+id);
        assertTrue(cursor.moveToFirst());
        assertEquals(75,cursor.getDouble(0),0);
        cursor.close();
        m.id=id;
        m.waistCm=80;
        assertEquals(1,db.measurements().update(m));
        assertEquals(1,db.measurements().delete(m));
        db.close();
        c.deleteDatabase(name);
    }
    @Test public void launchesAndNavigates() {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent i=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        InstrumentationRegistry.getInstrumentation().startActivitySync(i);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        onView(withId(R.id.body)).perform(click());
        onView(withText("Cơ thể của bạn")).check(matches(isDisplayed()));
        onView(withText("+ Nhập số đo mới")).perform(click());
        onView(withText("Thêm lần đo")).check(matches(isDisplayed()));
        androidx.test.espresso.Espresso.pressBack();
        onView(withId(R.id.diary)).perform(click());
        onView(withText("Nhật ký sức khỏe")).check(matches(isDisplayed()));
    }
    @Test public void measurementEntryAndComparison() throws Exception  {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        try {
            for(int k=0;k<2;k++) {
                onView(withId(R.id.body)).perform(click());
                onView(withText("+ Nhập số đo mới")).perform(click());
                String[] labels= {
                    "Chiều cao (cm)","Cân nặng (kg)","Vòng ngực (cm)","Vòng eo (cm)","Vòng hông (cm)"
                }
                ;
                String[] values= {
                    "170","65","90",k==0?"75":"85","95"
                }
                ;
                for(int n=0;n<labels.length;n++)onView(withHint(labels[n])).perform(scrollTo(),replaceText(values[n]),closeSoftKeyboard());
                onView(withHint("Ghi chú")).perform(scrollTo(),replaceText("integration-ui"),closeSoftKeyboard());
                onView(withText("Lưu số đo")).perform(scrollTo(),click());
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                Thread.sleep(800);
            }
            onView(withText("So sánh hai giai đoạn")).perform(scrollTo(),click());
            onView(withText("So sánh giai đoạn")).check(matches(isDisplayed()));
            onView(isAssignableFrom(com.example.vitality.graphics.BodyComparisonView.class)).perform(scrollTo());
            Thread.sleep(2200);
            android.graphics.Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir",c.getExternalFilesDir(null).getAbsolutePath()),"comparison-test.png"))) {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);
            }
        }
        finally {
            VitalityDatabase.get(c).getOpenHelper().getWritableDatabase().execSQL("DELETE FROM body_measurements WHERE note='integration-ui'");
        }
    }
    @Test public void migratesVersionOneWithoutLosingMeasurements() {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="migration-test.db";
        c.deleteDatabase(name);
        android.database.sqlite.SQLiteDatabase raw=c.openOrCreateDatabase(name,0,null);
        raw.execSQL("CREATE TABLE IF NOT EXISTS `body_measurements` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileId` INTEGER NOT NULL, `measuredAt` INTEGER NOT NULL, `heightCm` REAL NOT NULL, `weightKg` REAL NOT NULL, `chestCm` REAL NOT NULL, `waistCm` REAL NOT NULL, `hipCm` REAL NOT NULL, `upperArmCm` REAL, `thighCm` REAL, `calfCm` REAL, `bodyTemplate` INTEGER NOT NULL, `rendererVersion` INTEGER NOT NULL, `note` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)");
        raw.execSQL("CREATE INDEX IF NOT EXISTS `index_body_measurements_measuredAt` ON `body_measurements` (`measuredAt`)");
        raw.execSQL("INSERT INTO body_measurements (id,profileId,measuredAt,heightCm,weightKg,chestCm,waistCm,hipCm,bodyTemplate,rendererVersion,note,createdAt,updatedAt) VALUES(1,1,1000,170,65,90,75,95,0,1,'preserved',1000,1000)");
        raw.setVersion(1);
        raw.close();
        VitalityDatabase db=Room.databaseBuilder(c,VitalityDatabase.class,name).addMigrations(VitalityDatabase.MIGRATION_1_2,VitalityDatabase.MIGRATION_2_3,VitalityDatabase.MIGRATION_3_4).build();
        android.database.Cursor cursor=db.getOpenHelper().getReadableDatabase().query("SELECT note FROM body_measurements WHERE id=1");
        assertTrue(cursor.moveToFirst());
        assertEquals("preserved",cursor.getString(0));
        cursor.close();
        db.wellness().save(new UserProfile());
        db.close();
        c.deleteDatabase(name);
    }
}

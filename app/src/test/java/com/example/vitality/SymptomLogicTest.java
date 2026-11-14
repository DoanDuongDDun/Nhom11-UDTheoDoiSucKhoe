package com.example.vitality;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import com.example.vitality.data.entity.Symptom;
import com.example.vitality.utils.*;
public class SymptomLogicTest  {
    private static final long DAY=86400000L;
    private Symptom symptom(String name,int severity,long at) {
        Symptom s=new Symptom();
        s.name=name;
        s.severity=severity;
        s.occurredAt=at;
        return s;
    }
    @Test public void normalizeRemovesVietnameseDiacritics() {
        assertEquals("dau dau",SymptomLogic.normalize("Đau đầu"));
        assertEquals("buon non",SymptomLogic.normalize("  Buồn   NÔN "));
        assertEquals("chong mat",SymptomLogic.normalize("Chóng mặt"));
    }
    @Test public void searchWorksWithoutDiacritics() {
        assertTrue(SymptomLogic.matches("Đau đầu","dau dau"));
        assertTrue(SymptomLogic.matches("Đau đầu","ĐAU ĐẦU"));
        assertTrue(SymptomLogic.matches("Mệt mỏi","met"));
        assertTrue(SymptomLogic.matches("Ho",""));
        assertFalse(SymptomLogic.matches("Sốt","dau"));
    }
    @Test public void futureTimeRejected() {
        assertTrue(SymptomLogic.validTime(1000,1000));
        assertFalse(SymptomLogic.validTime(1001,1000));
    }
    @Test public void severeSymptomNeedsAdvice() {
        assertTrue(SymptomLogic.needsMedicalAdvice(SymptomLogic.SEVERE,1));
        assertFalse(SymptomLogic.needsMedicalAdvice(SymptomLogic.MODERATE,2));
        assertTrue(SymptomLogic.needsMedicalAdvice(SymptomLogic.MILD,3));
    }
    @Test public void consecutiveDaysCounted() {
        long today=System.currentTimeMillis();
        List<Symptom> existing=new ArrayList<>();
        existing.add(symptom("Đau đầu",1,today-DAY));
        existing.add(symptom("dau dau",1,today-2*DAY));
        existing.add(symptom("Ho",1,today-DAY));
        assertEquals(3,SymptomLogic.streakDays(existing,symptom("Đau đầu",1,today)));
        assertEquals(2,SymptomLogic.streakDays(existing,symptom("Ho",1,today)));
    }
    @Test public void gapBreaksStreak() {
        long today=System.currentTimeMillis();
        List<Symptom> existing=new ArrayList<>();
        existing.add(symptom("Sốt",1,today-3*DAY));
        assertEquals(1,SymptomLogic.streakDays(existing,symptom("Sốt",1,today)));
    }
    @Test public void editingIgnoresOldVersionOfSameRecord() {
        long today=System.currentTimeMillis();
        Symptom old=symptom("Ho",1,today-DAY);
        old.id=5;
        List<Symptom> existing=new ArrayList<>();
        existing.add(old);
        Symptom edited=symptom("Ho",1,today);
        edited.id=5;
        assertEquals(1,SymptomLogic.streakDays(existing,edited));
    }
    @Test public void adviceMentionsDisclaimer() {
        long now=System.currentTimeMillis();
        List<Symptom> list=new ArrayList<>();
        list.add(symptom("Đau đầu",3,now-1000));
        String text=SymptomLogic.advice(list,now);
        assertTrue(text.contains("Đau đầu"));
        assertTrue(text.contains("không thay thế chẩn đoán y tế"));
        assertTrue(text.contains("đi khám"));
        assertTrue(SymptomLogic.advice(new ArrayList<>(),now).contains("không thay thế chẩn đoán y tế"));
    }
    @Test public void bmiWarningOnlyOutsideNormalRange() {
        assertNull(HealthMath.bmiWarning(22));
        assertNull(HealthMath.bmiWarning(18.5));
        assertNotNull(HealthMath.bmiWarning(17));
        assertNotNull(HealthMath.bmiWarning(25));
        assertNull(HealthMath.bmiWarning(Double.NaN));
    }
    @Test(expected=IllegalArgumentException.class) public void weightAbove300Rejected() {
        Validation.number("301",Validation.BODY_RANGES[1][0],Validation.BODY_RANGES[1][1]);
    }
    @Test public void progressScoreAveragesAndCaps() {
        assertEquals(50,HealthMath.progressScore(1,0),0);
        assertEquals(100,HealthMath.progressScore(2,3),0);
        assertEquals(0,HealthMath.progressScore(),0);
        assertEquals(50,HealthMath.progressScore(Double.NaN,.5),0);
        assertEquals(0,HealthMath.progressScore(-1),0);
    }
    @Test public void streakCountsConsecutiveDaysEndingTodayOrYesterday() {
        long today=Formats.dayStart(System.currentTimeMillis());
        java.util.Set<Long> days=new java.util.HashSet<>();
        days.add(today);
        long yesterday=Formats.dayStart(today-3600000L);
        days.add(yesterday);
        days.add(Formats.dayStart(yesterday-3600000L));
        assertEquals(3,HealthMath.currentStreak(days,today),0);
        days.remove(today);
        assertEquals(2,HealthMath.currentStreak(days,today),0);
        assertEquals(0,HealthMath.currentStreak(new java.util.HashSet<Long>(),today),0);
    }
    @Test public void initialsFromName() {
        assertEquals("AN",Formats.initials("An Nhiên"));
        assertEquals("A",Formats.initials("an"));
        assertEquals("VT",Formats.initials("  "));
    }
}

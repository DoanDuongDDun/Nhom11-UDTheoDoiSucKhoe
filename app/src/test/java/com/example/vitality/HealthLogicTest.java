package com.example.vitality;
import org.junit.Test;
import static org.junit.Assert.*;
import com.example.vitality.data.entity.BodyMeasurement;
import com.example.vitality.graphics.*;
import com.example.vitality.utils.*;
public class HealthLogicTest  {
    private BodyMeasurement measurement() {
        BodyMeasurement m=new BodyMeasurement();
        m.heightCm=170;
        m.weightKg=65;
        m.chestCm=90;
        m.waistCm=75;
        m.hipCm=95;
        return m;
    }
    @Test public void waistChangesOnlyWaist() {
        BodyMeasurement m=measurement();
        BodyShapeParameters a=BodyShapeMapper.map(m);
        m.waistCm=100;
        BodyShapeParameters b=BodyShapeMapper.map(m);
        assertTrue(b.waist>a.waist);
        assertEquals(a.chest,b.chest,0);
        assertEquals(a.hip,b.hip,0);
        assertEquals(a.height,b.height,0);
    }
    @Test public void hipChangesOnlyHip() {
        BodyMeasurement m=measurement();
        BodyShapeParameters a=BodyShapeMapper.map(m);
        m.hipCm=120;
        BodyShapeParameters b=BodyShapeMapper.map(m);
        assertTrue(b.hip>a.hip);
        assertEquals(a.waist,b.waist,0);
    }
    @Test public void weightDoesNotChangeShape() {
        BodyMeasurement m=measurement();
        BodyShapeParameters a=BodyShapeMapper.map(m);
        m.weightKg=100;
        BodyShapeParameters b=BodyShapeMapper.map(m);
        assertEquals(a.waist,b.waist,0);
        assertEquals(a.hip,b.hip,0);
        assertEquals(a.chest,b.chest,0);
    }
    @Test public void missingOptionalMeasurementsHaveDefaults() {
        BodyShapeParameters a=BodyShapeMapper.map(measurement());
        assertTrue(a.arm>0);
        assertTrue(a.thigh>0);
        assertTrue(a.calf>0);
    }
    @Test public void drawingLimitsDoNotMutateRawData() {
        BodyMeasurement m=measurement();
        m.waistCm=250;
        assertEquals(52,BodyShapeMapper.map(m).waist,0);
        assertEquals(250,m.waistCm,0);
    }
    @Test public void differenceUsesRawValues() {
        assertEquals(-2.2,HealthMath.change(70.25,68.05),.000001);
        assertEquals(-10,HealthMath.percent(80,72),.000001);
    }
    @Test public void sleepAcrossMidnight() {
        assertEquals(450,HealthMath.sleepMinutes(23*3600000L,32*3600000L,90),0);
    }
    @Test(expected=IllegalArgumentException.class) public void invalidSleepRejected() {
        HealthMath.sleepMinutes(10,9,0);
    }
    @Test public void completionHandlesNoDueOccurrences() {
        assertNull(HealthMath.completion(0,0));
        assertEquals(75,HealthMath.completion(3,4),0);
    }
    @Test public void adjacentSleepDoesNotOverlap() {
        assertFalse(HealthMath.overlaps(0,10,10,20));
        assertTrue(HealthMath.overlaps(0,11,10,20));
    }
    @Test(expected=IllegalArgumentException.class) public void nonFiniteRejected() {
        Validation.number("NaN",1,100);
    }
}

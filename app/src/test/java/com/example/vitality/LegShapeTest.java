package com.example.vitality;
import org.junit.Test;
import static org.junit.Assert.*;
import com.example.vitality.data.entity.BodyMeasurement;
import com.example.vitality.graphics.*;
public class LegShapeTest {
 @Test public void largeLegMeasurementsRemainDistinct(){
  BodyMeasurement m=new BodyMeasurement();m.heightCm=170;m.chestCm=90;m.waistCm=70;m.hipCm=90;m.thighCm=50.0;m.calfCm=34.0;
  BodyShapeParameters a=BodyShapeMapper.map(m);
  m.thighCm=100.0;m.calfCm=100.0;BodyShapeParameters b=BodyShapeMapper.map(m);
  assertEquals(36,b.thigh,.001);assertEquals(36,b.calf,.001);
  assertTrue(b.thigh>a.thigh);assertTrue(b.calf>a.calf);
 }
}

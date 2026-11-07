package com.example.vitality.graphics;
import com.example.vitality.data.entity.BodyMeasurement;
/** Version 1: illustrative front widths, not anatomical reconstruction. Never modifies stored measurements. */
public final class BodyShapeMapper  {
    public static final float FRONT_WIDTH_FACTOR=0.36f;
    private static float width(double circumference,float min,float max) {
        return Math.max(min,Math.min(max,(float)circumference*FRONT_WIDTH_FACTOR));
    }
    public static BodyShapeParameters map(BodyMeasurement m) {
        if(m.rendererVersion!=1)throw new IllegalArgumentException("Phiên bản mô hình chưa được hỗ trợ");
        return new BodyShapeParameters((float)m.heightCm,width(m.chestCm,22,52)+(m.bodyTemplate==1?3:0),width(m.waistCm,16,52),width(m.hipCm,24,56),width(m.upperArmCm==null?28:m.upperArmCm,6,16),width(m.thighCm==null?50:m.thighCm,12,25),width(m.calfCm==null?34:m.calfCm,7,17));
    }
    private BodyShapeMapper() {
    }
}

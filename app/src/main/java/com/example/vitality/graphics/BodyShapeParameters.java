package com.example.vitality.graphics;
public final class BodyShapeParameters  {
    public final float height,chest,waist,hip,arm,thigh,calf;
    public BodyShapeParameters(float height,float chest,float waist,float hip,float arm,float thigh,float calf) {
        this.height=height;
        this.chest=chest;
        this.waist=waist;
        this.hip=hip;
        this.arm=arm;
        this.thigh=thigh;
        this.calf=calf;
    }
    @Override public boolean equals(Object o) {
        if(this==o)return true;
        if(!(o instanceof BodyShapeParameters))return false;
        BodyShapeParameters b=(BodyShapeParameters)o;
        return Float.compare(height,b.height)==0&&Float.compare(chest,b.chest)==0&&Float.compare(waist,b.waist)==0&&Float.compare(hip,b.hip)==0&&Float.compare(arm,b.arm)==0&&Float.compare(thigh,b.thigh)==0&&Float.compare(calf,b.calf)==0;
    }
    @Override public int hashCode() {
        return java.util.Arrays.hashCode(new float[] {
            height,chest,waist,hip,arm,thigh,calf
        });
    }
}

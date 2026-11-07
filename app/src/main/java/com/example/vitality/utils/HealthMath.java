package com.example.vitality.utils;
public final class HealthMath  {
    public static double bmi(double kg,double cm) {
        return kg/Math.pow(cm/100,2);
    }
    public static double change(double before,double after) {
        return after-before;
    }
    public static double percent(double before,double after) {
        return (after-before)/before*100;
    }
    public static double sleepMinutes(long start,long end,double awake) {
        if(end<=start||awake<0||awake>=(end-start)/60000d)throw new IllegalArgumentException("Thời gian ngủ hoặc thức giữa giấc không hợp lệ");
        return (end-start)/60000d-awake;
    }
    public static Double completion(int done,int due) {
        return due==0?null:done*100d/due;
    }
    public static boolean overlaps(long a,long b,long c,long d) {
        return a<d&&c<b;
    }
    private HealthMath() {
    }
}

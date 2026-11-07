package com.example.vitality.utils;
import java.text.*;
import java.util.*;
public final class Formats  {
    public static String number(double n) {
        return String.format(Locale.getDefault(),"%.1f",n);
    }
    public static String date(long t) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(new Date(t));
    }
    public static String day(long t) {
        return new SimpleDateFormat("dd/MM/yyyy",Locale.getDefault()).format(new Date(t));
    }
    public static long dayStart(long t) {
        Calendar c=Calendar.getInstance();
        c.setTimeInMillis(t);
        c.set(Calendar.HOUR_OF_DAY,0);
        c.set(Calendar.MINUTE,0);
        c.set(Calendar.SECOND,0);
        c.set(Calendar.MILLISECOND,0);
        return c.getTimeInMillis();
    }
    public static String delta(double a,double b) {
        double d=b-a;
        return (d>0?"Tăng ":d<0?"Giảm ":"Không đổi ")+number(Math.abs(d))+" ("+number(HealthMath.percent(a,b))+"%)";
    }
}

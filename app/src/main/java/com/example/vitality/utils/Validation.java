package com.example.vitality.utils;
import java.text.*;
import java.util.*;
public final class Validation  {
    public static final double[][] BODY_RANGES= {
        {
            50,250
        }
        , {
            2,300
        }
        , {
            20,250
        }
        , {
            20,250
        }
        , {
            20,250
        }
        , {
            5,100
        }
        , {
            10,150
        }
        , {
            5,100
        }
    }
    ;
    public static double number(String text,double min,double max) {
        String s=text.trim();
        NumberFormat f=NumberFormat.getNumberInstance();
        f.setGroupingUsed(false);
        ParsePosition p=new ParsePosition(0);
        Number n=f.parse(s,p);
        if(n==null||p.getIndex()!=s.length()) {
            try {
                n=Double.valueOf(s.replace(',','.'));
            }
            catch(Exception e) {
                throw new IllegalArgumentException("Nhập số hợp lệ");
            }
        }
        double v=n.doubleValue();
        if(!Double.isFinite(v)||v<min||v>max)throw new IllegalArgumentException("Giá trị từ "+min+" đến "+max);
        return v;
    }
    public static long date(String text) {
        SimpleDateFormat f=new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault());
        f.setLenient(false);
        ParsePosition p=new ParsePosition(0);
        Date d=f.parse(text.trim(),p);
        if(d==null||p.getIndex()!=text.trim().length())throw new IllegalArgumentException("Định dạng dd/MM/yyyy HH:mm");
        return d.getTime();
    }
}

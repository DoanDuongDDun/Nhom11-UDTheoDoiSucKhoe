package com.example.vitality.utils;
import com.example.vitality.data.entity.JournalEntry;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
public final class Formats  {
    public static final long DAY_MS=86400000L;
    /** SimpleDateFormat không an toàn đa luồng và tốn kém khi tạo mới: giữ một bản cho mỗi luồng, tạo lại nếu đổi ngôn ngữ. */
    private static final class Cached  {
        private static final class Slot  {
            final SimpleDateFormat format;
            final Locale locale;
            Slot(String pattern,Locale locale) {
                this.format=new SimpleDateFormat(pattern,locale);
                this.locale=locale;
            }
        }
        private final String pattern;
        private final ThreadLocal<Slot> slot=new ThreadLocal<>();
        Cached(String pattern) {
            this.pattern=pattern;
        }
        String format(long t) {
            Locale now=Locale.getDefault();
            Slot s=slot.get();
            if(s==null||!now.equals(s.locale)) {
                s=new Slot(pattern,now);
                slot.set(s);
            }
            return s.format.format(new Date(t));
        }
    }
    private static final Cached DATE_TIME=new Cached("dd/MM/yyyy HH:mm"),DAY=new Cached("dd/MM/yyyy"),CLOCK=new Cached("HH:mm"),SHORT=new Cached("dd/MM HH:mm");
    public static String number(double n) {
        return String.format(Locale.getDefault(),"%.1f",n);
    }
    public static String activityAmount(JournalEntry entry) {
        if (JournalEntry.KIND_STEPS.equals(entry.kind)) {
            return String.format(Locale.getDefault(), "%,d bước", Math.round(entry.amount));
        }
        return number(entry.amount) + " phút";
    }
    public static String date(long t) {
        return DATE_TIME.format(t);
    }
    public static String day(long t) {
        return DAY.format(t);
    }
    /** Chỉ giờ phút ("HH:mm"). */
    public static String clock(long t) {
        return CLOCK.format(t);
    }
    /** "HH:mm" nếu t thuộc hôm nay (từ mốc todayStart), ngược lại "dd/MM HH:mm". */
    public static String shortTime(long t,long todayStart) {
        return t>=todayStart?CLOCK.format(t):SHORT.format(t);
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
    /** Cộng/trừ ngày theo lịch (đúng cả ngày chuyển giờ mùa hè) và trả về 00:00 của ngày kết quả. */
    public static long addDays(long dayStart,int days) {
        Calendar c=Calendar.getInstance();
        c.setTimeInMillis(dayStart);
        c.add(Calendar.DATE,days);
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
    /** Chữ cái đầu của tên (tối đa 2 ký tự) để làm ảnh đại diện; trống thì trả về "VT". */
    public static String initials(String name) {
        String t=name==null?"":name.trim();
        if(t.isEmpty())return "VT";
        String[] parts=t.split("\\s+");
        String first=parts[0].substring(0,1),last=parts[parts.length-1].substring(0,1);
        return (parts.length==1?first:first+last).toUpperCase(Locale.getDefault());
    }
    private Formats() {
    }
}

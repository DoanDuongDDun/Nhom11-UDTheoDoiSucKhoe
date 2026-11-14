package com.example.vitality.utils;
public final class HealthMath  {
    public static double bmi(double kg,double cm) {
        double m=cm/100;
        return kg/(m*m);
    }
    public static final double BMI_LOW=18.5,BMI_HIGH=25;
    /** Trả về lời nhắc tham khảo khi BMI ngoài khoảng 18,5–24,9; trả về null nếu bình thường hoặc không hợp lệ. */
    public static String bmiWarning(double bmi) {
        if(!Double.isFinite(bmi)||bmi<=0)return null;
        String where=bmi<BMI_LOW?"thấp hơn":bmi>=BMI_HIGH?"cao hơn":null;
        if(where==null)return null;
        return "Lưu ý: BMI "+String.format(java.util.Locale.getDefault(),"%.1f",bmi)+" "+where+" mức thông thường (18,5–24,9). Chỉ mang tính tham khảo, không thay thế chẩn đoán y tế.";
    }
    /** Điểm tiến độ trong ngày (0–100): trung bình tỷ lệ đạt từng mục tiêu, mỗi mục tối đa 100%. */
    public static int progressScore(double... ratios) {
        if(ratios==null)return 0;
        double sum=0;
        int count=0;
        for(double r:ratios) {
            if(!Double.isFinite(r))continue;
            sum+=Math.max(0,Math.min(1,r));
            count++;
        }
        return count==0?0:(int)Math.round(sum/count*100);
    }
    /** Số ngày liên tiếp (tính lùi từ hôm nay; nếu hôm nay chưa có thì tính từ hôm qua) có ít nhất một bản ghi. */
    public static int currentStreak(java.util.Set<Long> dayStarts,long todayStart) {
        if(dayStarts==null||dayStarts.isEmpty())return 0;
        long day=todayStart;
        if(!dayStarts.contains(day))day=Formats.addDays(day,-1);
        int count=0;
        while(dayStarts.contains(day)) {
            count++;
            day=Formats.addDays(day,-1);
        }
        return count;
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
    /** Như sleepMinutes nhưng trả 0 cho phiên đang ngủ (end=null) hoặc dữ liệu không hợp lệ thay vì ném lỗi. */
    public static double sleepMinutesOrZero(long start,Long end,double awake) {
        if(end==null||end<=start||awake<0||awake>=(end-start)/60000d)return 0;
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

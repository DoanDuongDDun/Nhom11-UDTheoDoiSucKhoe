package com.example.vitality.utils;
import com.example.vitality.data.entity.Symptom;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
/** Nghiệp vụ thuần Java của chức năng theo dõi triệu chứng (dễ kiểm thử, không phụ thuộc Android). */
public final class SymptomLogic  {
    public static final String DISCLAIMER="Gợi ý chỉ mang tính tham khảo, không thay thế chẩn đoán y tế. Nếu triệu chứng nặng hoặc kéo dài, hãy đến cơ sở y tế.";
    public static final String[] PRESETS= {
        "Đau đầu","Mệt mỏi","Buồn nôn","Đau bụng","Sốt","Ho","Đau họng","Chóng mặt","Khó thở","Đau cơ"
    }
    ;
    public static final int MILD=1,MODERATE=2,SEVERE=3;
    /** Số ngày liên tiếp (cùng một triệu chứng) từ đó trở lên thì khuyến nghị đi khám. */
    public static final int STREAK_LIMIT=3;
    private static final long DAY=Formats.DAY_MS;
    private static final Pattern MARKS=Pattern.compile("\\p{M}+"),SPACES=Pattern.compile("\\s+");
    /** Bỏ dấu tiếng Việt, đổi đ thành d, viết thường, cắt khoảng trắng thừa. */
    public static String normalize(String s) {
        if(s==null)return "";
        String t=MARKS.matcher(Normalizer.normalize(s.trim(),Normalizer.Form.NFD)).replaceAll("");
        return SPACES.matcher(t.replace('đ','d').replace('Đ','D').toLowerCase(Locale.ROOT)).replaceAll(" ");
    }
    /** Tìm kiếm không phân biệt hoa thường và dấu; truy vấn rỗng khớp mọi dòng. */
    public static boolean matches(String text,String query) {
        return matchesNormalized(text,normalize(query));
    }
    /** Như matches nhưng truy vấn đã được normalize sẵn, để lọc danh sách không phải chuẩn hóa lại truy vấn cho từng dòng. */
    public static boolean matchesNormalized(String text,String normalizedQuery) {
        return normalizedQuery.isEmpty()||normalize(text).contains(normalizedQuery);
    }
    public static String severityLabel(int severity) {
        return severity==SEVERE?"Nặng":severity==MODERATE?"Vừa":"Nhẹ";
    }
    /** Thời điểm xuất hiện không được nằm ở tương lai. */
    public static boolean validTime(long occurredAt,long now) {
        return occurredAt<=now;
    }
    /** Số ngày liên tiếp có cùng triệu chứng, tính cả bản ghi sắp lưu (bản cũ cùng id bị bỏ qua khi sửa). */
    public static int streakDays(List<Symptom> existing,Symptom candidate) {
        String key=normalize(candidate.name);
        Set<Long> days=new HashSet<>();
        if(existing!=null)for(Symptom s:existing)if(s.id!=candidate.id||candidate.id==0)if(normalize(s.name).equals(key))days.add(Formats.dayStart(s.occurredAt));
        return streakAround(days,Formats.dayStart(candidate.occurredAt));
    }
    /** Đếm chuỗi ngày liên tiếp chứa {@code day} (day được thêm vào tập nếu chưa có). */
    private static int streakAround(Set<Long> days,long day) {
        days.add(day);
        int count=1;
        for(long d=Formats.addDays(day,-1);days.contains(d);d=Formats.addDays(d,-1))count++;
        for(long d=Formats.addDays(day,1);days.contains(d);d=Formats.addDays(d,1))count++;
        return count;
    }
    public static boolean needsMedicalAdvice(int severity,int streakDays) {
        return severity>=SEVERE||streakDays>=STREAK_LIMIT;
    }
    public static String warningText(Symptom s,int streakDays) {
        StringBuilder b=new StringBuilder();
        if(s.severity>=SEVERE)b.append("Triệu chứng \"").append(s.name).append("\" ở mức nặng.");
        if(streakDays>=STREAK_LIMIT) {
            if(b.length()>0)b.append("\n");
            b.append("Triệu chứng \"").append(s.name).append("\" đã xuất hiện ").append(streakDays).append(" ngày liên tiếp.");
        }
        b.append("\n\nBạn nên cân nhắc đi khám hoặc liên hệ nhân viên y tế. ").append(DISCLAIMER);
        return b.toString();
    }
    private static String tip(String normalizedName) {
        switch(normalizedName) {
            case "dau dau":return "Nghỉ ngơi ở nơi yên tĩnh, hạn chế màn hình sáng, uống đủ nước.";
            case "met moi":return "Ngủ đủ giấc, ăn uống cân đối, vận động nhẹ nhàng.";
            case "buon non":return "Ăn từng ít một, uống nước từng ngụm nhỏ, tránh đồ nhiều dầu mỡ.";
            case "dau bung":return "Nghỉ ngơi, tránh đồ cay hoặc nhiều dầu mỡ, theo dõi vị trí và mức độ đau.";
            case "sot":return "Uống đủ nước, nghỉ ngơi, theo dõi nhiệt độ cơ thể.";
            case "ho":return "Giữ ấm, uống nước ấm, tránh khói bụi.";
            case "dau hong":return "Uống nước ấm, hạn chế nói nhiều, giữ ấm vùng cổ.";
            case "chong mat":return "Ngồi hoặc nằm nghỉ, đứng dậy từ từ, uống đủ nước.";
            case "kho tho":return "Ngồi thẳng, giữ bình tĩnh và nên liên hệ cơ sở y tế sớm.";
            case "dau co":return "Nghỉ ngơi, giãn cơ nhẹ nhàng, giữ ấm.";
            default:return "Nghỉ ngơi, uống đủ nước và tiếp tục theo dõi diễn biến.";
        }
    }
    /** Gợi ý chăm sóc tham khảo dựa trên các triệu chứng trong 7 ngày gần nhất (chạy hoàn toàn trên thiết bị). */
    public static String advice(List<Symptom> all,long now) {
        LinkedHashMap<String,Symptom> worst=new LinkedHashMap<>();
        // Chuẩn hóa mỗi tên đúng một lần và gom các ngày theo từng triệu chứng để tính chuỗi ngày không phải quét lại toàn bộ danh sách.
        Map<String,Set<Long>> daysByKey=new HashMap<>();
        if(all!=null)for(Symptom s:all) {
            String key=normalize(s.name);
            daysByKey.computeIfAbsent(key,k->new HashSet<>()).add(Formats.dayStart(s.occurredAt));
            if(s.occurredAt<now-7*DAY||s.occurredAt>now)continue;
            Symptom old=worst.get(key);
            if(old==null||s.severity>old.severity)worst.put(key,s);
        }
        if(worst.isEmpty())return "Chưa có triệu chứng nào trong 7 ngày gần đây.\n\n"+DISCLAIMER;
        StringBuilder b=new StringBuilder("Các triệu chứng trong 7 ngày gần đây:\n");
        boolean warn=false;
        for(Map.Entry<String,Symptom> e:worst.entrySet()) {
            Symptom s=e.getValue();
            int streak=streakAround(daysByKey.get(e.getKey()),Formats.dayStart(s.occurredAt));
            b.append("\n• ").append(s.name).append(" (mức cao nhất: ").append(severityLabel(s.severity).toLowerCase(Locale.ROOT)).append(")\n  ").append(tip(e.getKey()));
            if(needsMedicalAdvice(s.severity,streak))warn=true;
        }
        if(warn)b.append("\n\nCó triệu chứng ở mức nặng hoặc kéo dài nhiều ngày liên tiếp: bạn nên đi khám.");
        b.append("\n\n").append(DISCLAIMER);
        return b.toString();
    }
    private SymptomLogic() {
    }
}

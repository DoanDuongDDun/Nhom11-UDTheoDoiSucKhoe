package com.example.vitality.utils;
import com.example.vitality.data.entity.BodyMeasurement;
import com.example.vitality.data.export.HealthDataRecord;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
public final class CsvExporter  {
    public static String escape(String s) {
        return "\""+s.replace("\"","\"\"")+"\"";
    }
    public static void writeHealthData(Writer writer,List<HealthDataRecord> records)throws IOException {
        writer.write("\uFEFFLoại dữ liệu,ID bản ghi,Chỉ số,Giá trị,Đơn vị,Thời điểm ghi nhận,Ghi chú\r\n");
        SimpleDateFormat time=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX",Locale.ROOT);
        for(HealthDataRecord row:records) {
            writer.write(escape(row.category)+","+row.id+","+escape(row.metric)+","+escape(row.value)
                    +","+escape(row.unit)+","+escape(time.format(new Date(row.time)))+","+escape(row.note)+"\r\n");
        }
        writer.flush();
    }
    public static void write(Writer w,List<BodyMeasurement> rows)throws IOException {
        w.write("\uFEFFID,Ngày giờ,Chiều cao (cm),Cân nặng (kg),Ngực (cm),Eo (cm),Hông (cm),Tay (cm),Đùi (cm),Bắp chân (cm),Mẫu,Renderer,Ghi chú\r\n");
        for(BodyMeasurement m:rows) {
            w.write(m.id+","+escape(Formats.date(m.measuredAt))+","+m.heightCm+","+m.weightKg+","+m.chestCm+","+m.waistCm+","+m.hipCm+","+(m.upperArmCm==null?"":m.upperArmCm)+","+(m.thighCm==null?"":m.thighCm)+","+(m.calfCm==null?"":m.calfCm)+","+m.bodyTemplate+","+m.rendererVersion+","+escape(m.note==null?"":m.note)+"\r\n");
        }
        w.flush();
    }
}

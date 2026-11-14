package com.example.vitality.utils;
import com.example.vitality.data.entity.BodyMeasurement;
import java.io.*;
import java.util.*;
public final class CsvExporter  {
    public static String escape(String s) {
        return "\""+s.replace("\"","\"\"")+"\"";
    }
    public static void write(Writer w,List<BodyMeasurement> rows)throws IOException {
        w.write("\uFEFFID,Ngày giờ,Chiều cao (cm),Cân nặng (kg),Ngực (cm),Eo (cm),Hông (cm),Tay (cm),Đùi (cm),Bắp chân (cm),Mẫu,Renderer,Ghi chú\r\n");
        for(BodyMeasurement m:rows) {
            w.write(m.id+","+escape(Formats.date(m.measuredAt))+","+m.heightCm+","+m.weightKg+","+m.chestCm+","+m.waistCm+","+m.hipCm+","+(m.upperArmCm==null?"":m.upperArmCm)+","+(m.thighCm==null?"":m.thighCm)+","+(m.calfCm==null?"":m.calfCm)+","+m.bodyTemplate+","+m.rendererVersion+","+escape(m.note==null?"":m.note)+"\r\n");
        }
        w.flush();
    }
}

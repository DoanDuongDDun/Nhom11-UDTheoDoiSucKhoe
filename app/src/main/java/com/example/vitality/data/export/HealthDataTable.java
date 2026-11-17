package com.example.vitality.data.export;

import com.example.vitality.data.export.HealthDataFilter.Type;

enum HealthDataTable {
    WATER(Type.WATER, date("Ngày"), time("Giờ"), number("Lượng nước (ml)"), text("Ghi chú")),
    ACTIVITY(Type.ACTIVITY, date("Ngày"), time("Giờ"), text("Hoạt động"), number("Thời lượng (phút)"),
            number("Quãng đường (km)"), number("Tiêu hao (kcal)"), text("Ghi chú"), number("Số bước")),
    SLEEP(Type.SLEEP, timestamp("Bắt đầu"), timestamp("Kết thúc"), number("Thức giữa giấc (phút)"),
            number("Ngủ thực tế (phút)"), number("Chất lượng (/5)"), text("Ghi chú")),
    CYCLE(Type.CYCLE, date("Ngày"), time("Giờ đến hạn"), text("Tên lịch"), text("Trạng thái")),
    BODY(Type.BODY, date("Ngày"), time("Giờ"), number("Cân nặng (kg)"), number("Chiều cao (cm)"),
            number("Vòng eo (cm)"), number("Vòng ngực (cm)"), number("Vòng hông (cm)"),
            number("Vòng tay (cm)"), number("Vòng đùi (cm)"), number("Vòng bắp chân (cm)"), text("Ghi chú")),
    NUTRITION(Type.NUTRITION, date("Ngày"), time("Giờ"), text("Bữa"), text("Món ăn"), text("Khẩu phần"),
            number("Năng lượng (kcal)"), text("Ghi chú")),
    SYMPTOM(Type.SYMPTOM, date("Ngày"), time("Giờ"), text("Triệu chứng"), text("Mức độ"), text("Ghi chú")),
    PERIOD(Type.PERIOD, date("Ngày bắt đầu"), date("Ngày kết thúc"), number("Số ngày"),
            text("Triệu chứng"), text("Ghi chú"));

    enum CellType { TEXT, NUMBER, DATE, TIME, TIMESTAMP }

    final Type type;
    final Column[] columns;

    HealthDataTable(Type type, Column... columns) {
        this.type = type;
        this.columns = columns;
    }

    private static Column text(String header) { return new Column(header, CellType.TEXT); }
    private static Column number(String header) { return new Column(header, CellType.NUMBER); }
    private static Column date(String header) { return new Column(header, CellType.DATE); }
    private static Column time(String header) { return new Column(header, CellType.TIME); }
    private static Column timestamp(String header) { return new Column(header, CellType.TIMESTAMP); }

    static final class Column {
        final String header;
        final CellType cellType;

        private Column(String header, CellType cellType) {
            this.header = header;
            this.cellType = cellType;
        }
    }
}

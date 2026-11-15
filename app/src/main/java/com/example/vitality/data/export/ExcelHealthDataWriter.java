package com.example.vitality.data.export;

import com.example.vitality.data.export.HealthDataTable.CellType;
import com.example.vitality.data.export.HealthDataTable.Column;
import com.example.vitality.utils.Formats;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Writes a single-sheet XLSX using standard SpreadsheetML parts. */
final class ExcelHealthDataWriter {
    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
    private static final String CONTENT_TYPES = "http://schemas.openxmlformats.org/package/2006/content-types";
    private static final String PACKAGE_RELS = "http://schemas.openxmlformats.org/package/2006/relationships";
    private static final String OFFICE_RELS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String SPREADSHEET = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final Pattern LITERAL_ESCAPE = Pattern.compile("_(x[0-9A-Fa-f]{4}_)");
    private static final int MAX_ROWS = 1_048_576;
    private static final int MAX_CELL_LENGTH = 32_767;
    private static final int TITLE_STYLE = 1;
    private static final int SECTION_STYLE = 2;
    private static final int HEADER_STYLE = 3;
    private static final int TEXT_STYLE = 4;
    private static final int DATE_STYLE = 5;
    private static final int TIME_STYLE = 6;
    private static final int TIMESTAMP_STYLE = 7;
    private static final int NUMBER_STYLE = 8;

    private ExcelHealthDataWriter() {}

    /** Takes ownership of the destination stream. */
    static void write(OutputStream output, HealthDataReport report) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            writePart(zip, "[Content_Types].xml", "<Types xmlns=\"" + CONTENT_TYPES + "\">"
                    + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                    + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                    + contentType("/xl/workbook.xml", "sheet.main")
                    + contentType("/xl/styles.xml", "styles")
                    + contentType("/xl/worksheets/sheet1.xml", "worksheet") + "</Types>");
            writePart(zip, "_rels/.rels", relationships(relationship(1, "officeDocument", "xl/workbook.xml")));
            writePart(zip, "xl/workbook.xml", "<workbook xmlns=\"" + SPREADSHEET + "\" xmlns:r=\"" + OFFICE_RELS
                    + "\"><bookViews><workbookView/></bookViews>"
                    + "<sheets><sheet name=\"Dữ liệu sức khỏe\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            writePart(zip, "xl/_rels/workbook.xml.rels", relationships(
                    relationship(1, "worksheet", "worksheets/sheet1.xml")
                            + relationship(2, "styles", "styles.xml")));
            writePart(zip, "xl/styles.xml", styles());
            zip.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
            Writer xml = new BufferedWriter(new OutputStreamWriter(zip, StandardCharsets.UTF_8));
            writeSheet(xml, report);
            xml.flush();
            zip.closeEntry();
        }
    }

    private static void writeSheet(Writer xml, HealthDataReport report) throws IOException {
        int width = 0;
        for (HealthDataTable table : report.tables.keySet()) {
            width = Math.max(width, table.columns.length);
        }
        if (width == 0 || report.tableRecordCount() == 0) throw new IOException("No Excel records to write");

        xml.write(XML + "<worksheet xmlns=\"" + SPREADSHEET + "\">"
                + "<sheetViews><sheetView showGridLines=\"0\" workbookViewId=\"0\"/></sheetViews>"
                + "<sheetFormatPr defaultRowHeight=\"19\"/><cols>");
        for (int i = 1; i <= width; i++) {
            int columnWidth = i <= 2 ? 20 : 22;
            xml.write("<col min=\"" + i + "\" max=\"" + i + "\" width=\"" + columnWidth + "\" customWidth=\"1\"/>");
        }
        xml.write("</cols><sheetData>");

        List<String> merged = new ArrayList<>();
        int row = 1;
        startRow(xml, row, 31);
        textCell(xml, cellRef(row, 0), TITLE_STYLE, "Vitality · Dữ liệu sức khỏe");
        xml.write("</row>");
        merged.add(merge(row, width));

        TimeZone timezone = TimeZone.getDefault();
        for (Map.Entry<HealthDataTable, List<List<Object>>> section : report.tables.entrySet()) {
            HealthDataTable table = section.getKey();
            List<List<Object>> rows = section.getValue();
            row += 2;
            startRow(xml, row, 24);
            textCell(xml, cellRef(row, 0), SECTION_STYLE, table.type.label);
            xml.write("</row>");
            merged.add(merge(row, table.columns.length));
            startRow(xml, ++row, 32);
            for (int i = 0; i < table.columns.length; i++) {
                textCell(xml, cellRef(row, i), HEADER_STYLE, table.columns[i].header);
            }
            xml.write("</row>");
            if (rows.isEmpty()) {
                startRow(xml, ++row, 0);
                textCell(xml, cellRef(row, 0), TEXT_STYLE, "Không có dữ liệu trong khoảng đã chọn");
                xml.write("</row>");
                merged.add(merge(row, table.columns.length));
            }
            for (List<Object> record : rows) {
                startRow(xml, ++row, 0);
                for (int i = 0; i < table.columns.length; i++) {
                    Column column = table.columns[i];
                    writeCell(xml, cellRef(row, i), column.cellType, record.get(i), timezone);
                }
                xml.write("</row>");
            }
        }
        xml.write("</sheetData><mergeCells count=\"" + merged.size() + "\">");
        for (String reference : merged) xml.write("<mergeCell ref=\"" + reference + "\"/>");
        xml.write("</mergeCells></worksheet>");
    }

    private static void startRow(Writer xml, int row, int height) throws IOException {
        if (row > MAX_ROWS) throw new IOException("The data exceeds Excel's row limit");
        xml.write("<row r=\"" + row + "\"");
        if (height > 0) xml.write(" ht=\"" + height + "\" customHeight=\"1\"");
        xml.write(">");
    }

    private static String cellRef(int row, int column) {
        StringBuilder name = new StringBuilder();
        for (int index = column; index >= 0; index = index / 26 - 1) {
            name.append((char) ('A' + index % 26));
        }
        return name.reverse().append(row).toString();
    }

    private static String merge(int row, int width) {
        return cellRef(row, 0) + ":" + cellRef(row, width - 1);
    }

    private static void writeCell(Writer xml, String address, CellType type, Object value, TimeZone timezone)
            throws IOException {
        if (value == null) {
            xml.write("<c r=\"" + address + "\" s=\"" + TEXT_STYLE + "\"/>");
            return;
        }
        if (type == CellType.TEXT) {
            textCell(xml, address, TEXT_STYLE, (String) value);
            return;
        }
        Number number = (Number) value;
        if (type == CellType.NUMBER) {
            if (Double.isFinite(number.doubleValue())) numberCell(xml, address, NUMBER_STYLE, number);
            else xml.write("<c r=\"" + address + "\" s=\"" + NUMBER_STYLE + "\"/>");
            return;
        }
        long timestamp = number.longValue();
        long localMillis = timestamp + timezone.getOffset(timestamp);
        double fraction = Math.floorMod(localMillis, Formats.DAY_MS) / (double) Formats.DAY_MS;
        if (type == CellType.TIME) {
            numberCell(xml, address, TIME_STYLE, fraction);
            return;
        }
        long day = Math.floorDiv(localMillis, Formats.DAY_MS) + 25_569;
        if (day <= 60) day--; // Excel's 1900 date system includes the fictitious leap day.
        if (day < 1) {
            textCell(xml, address, TEXT_STYLE, type == CellType.DATE ? Formats.day(timestamp) : Formats.date(timestamp));
        } else if (type == CellType.DATE) {
            numberCell(xml, address, DATE_STYLE, day);
        } else {
            numberCell(xml, address, TIMESTAMP_STYLE, day + fraction);
        }
    }

    private static void numberCell(Writer xml, String address, int style, Number value) throws IOException {
        xml.write("<c r=\"" + address + "\" s=\"" + style + "\"><v>" + value + "</v></c>");
    }

    private static void textCell(Writer xml, String address, int style, String value) throws IOException {
        if (value.length() > MAX_CELL_LENGTH) throw new IOException("A note exceeds Excel's cell length limit");
        xml.write("<c r=\"" + address + "\" s=\"" + style + "\" t=\"inlineStr\"><is><t xml:space=\"preserve\">");
        String text = LITERAL_ESCAPE.matcher(value).replaceAll("_x005F_$1");
        for (int i = 0; i < text.length();) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            switch (cp) {
                case '&': xml.write("&amp;"); break;
                case '<': xml.write("&lt;"); break;
                case '>': xml.write("&gt;"); break;
                case '\r': xml.write("&#13;"); break;
                default:
                    boolean allowed = cp == '\n' || cp == '\t' || (cp >= 0x20 && cp <= 0xD7FF)
                            || (cp >= 0xE000 && cp <= 0xFFFD) || cp >= 0x10000;
                    if (!allowed) xml.write('\uFFFD');
                    else if (cp <= 0xFFFF) xml.write(cp);
                    else xml.write(Character.toChars(cp));
            }
        }
        xml.write("</t></is></c>");
    }

    private static void writePart(ZipOutputStream zip, String name, String xml) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write((XML + xml).getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String contentType(String part, String format) {
        return "<Override PartName=\"" + part + "\" ContentType=\"application/vnd.openxmlformats-officedocument."
                + "spreadsheetml." + format + "+xml\"/>";
    }

    private static String relationships(String items) {
        return "<Relationships xmlns=\"" + PACKAGE_RELS + "\">" + items + "</Relationships>";
    }

    private static String relationship(int id, String kind, String target) {
        return "<Relationship Id=\"rId" + id + "\" Type=\"" + OFFICE_RELS + "/" + kind
                + "\" Target=\"" + target + "\"/>";
    }

    private static String styles() {
        return "<styleSheet xmlns=\"" + SPREADSHEET + "\">"
                + "<numFmts count=\"3\"><numFmt numFmtId=\"164\" formatCode=\"dd/mm/yyyy\"/>"
                + "<numFmt numFmtId=\"165\" formatCode=\"hh:mm:ss\"/>"
                + "<numFmt numFmtId=\"166\" formatCode=\"dd/mm/yyyy hh:mm\"/></numFmts>"
                + "<fonts count=\"4\">" + font(11, "FF20354B", false) + font(16, "FF172F48", true)
                + font(11, "FF172F48", true) + font(11, "FFFFFFFF", true) + "</fonts>"
                + "<fills count=\"4\"><fill><patternFill patternType=\"none\"/></fill>"
                + "<fill><patternFill patternType=\"gray125\"/></fill>" + fill("FFE5F0F7") + fill("FF244865") + "</fills>"
                + "<borders count=\"2\"><border><left/><right/><top/><bottom/><diagonal/></border>"
                + "<border><left/><right/><top/><bottom style=\"thin\"><color rgb=\"FFE0E9F1\"/></bottom><diagonal/></border></borders>"
                + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                + "<cellXfs count=\"9\">" + xf(0, 0, 0, 0, "") + xf(0, 1, 0, 0, "")
                + xf(0, 2, 2, 0, "") + xf(0, 3, 3, 0, " horizontal=\"center\" wrapText=\"1\"")
                + xf(0, 0, 0, 1, " wrapText=\"1\"")
                + xf(164, 0, 0, 1, " horizontal=\"center\"") + xf(165, 0, 0, 1, " horizontal=\"center\"")
                + xf(166, 0, 0, 1, " horizontal=\"center\"") + xf(0, 0, 0, 1, "")
                + "</cellXfs><cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles></styleSheet>";
    }

    private static String font(int size, String color, boolean bold) {
        return "<font>" + (bold ? "<b/>" : "") + "<sz val=\"" + size + "\"/><color rgb=\"" + color
                + "\"/><name val=\"Calibri\"/><family val=\"2\"/></font>";
    }

    private static String fill(String color) {
        return "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"" + color
                + "\"/><bgColor indexed=\"64\"/></patternFill></fill>";
    }

    private static String xf(int number, int font, int fill, int border, String alignment) {
        return "<xf numFmtId=\"" + number + "\" fontId=\"" + font + "\" fillId=\"" + fill + "\" borderId=\"" + border
                + "\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\">"
                + "<alignment vertical=\"center\"" + alignment + "/></xf>";
    }
}

package com.example.vitality.data.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ExcelHealthDataWriterTest {
    @Test public void keepsSectionsInOneSheetAndUsesNumericAndTextCells() throws Exception {
        Calendar clock = Calendar.getInstance();
        clock.clear();
        clock.set(2026, Calendar.NOVEMBER, 12, 8, 15, 0);
        long time = clock.getTimeInMillis();
        String literalNote = "=1+1 & <ghi chú>\nDòng tiếp theo";
        Map<HealthDataTable, List<List<Object>>> sections = new EnumMap<>(HealthDataTable.class);
        sections.put(HealthDataTable.WATER, Collections.singletonList(Arrays.asList(time, time, 250, literalNote)));
        sections.put(HealthDataTable.CYCLE, Collections.singletonList(Arrays.asList(time, time, "Giãn cơ", "Hoàn thành")));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ExcelHealthDataWriter.write(bytes, new HealthDataReport(Collections.emptyList(), sections));

        Document workbook = readPart(bytes, "xl/workbook.xml");
        assertEquals(1, workbook.getElementsByTagName("sheet").getLength());
        Document sheet = readPart(bytes, "xl/worksheets/sheet1.xml");
        assertEquals("Nước uống", cell(sheet, "A3").getTextContent());
        assertEquals("Lượng nước (ml)", cell(sheet, "C4").getTextContent());
        assertEquals("46338", cell(sheet, "A5").getTextContent());
        assertEquals("250", cell(sheet, "C5").getTextContent());
        assertEquals("", cell(sheet, "C5").getAttribute("t"));
        assertEquals("inlineStr", cell(sheet, "D5").getAttribute("t"));
        assertEquals(literalNote, cell(sheet, "D5").getTextContent());
        assertEquals("Chu kỳ sinh hoạt", cell(sheet, "A7").getTextContent());
        assertEquals("Trạng thái", cell(sheet, "D8").getTextContent());
        assertEquals("Hoàn thành", cell(sheet, "D9").getTextContent());
        assertEquals(0, sheet.getElementsByTagName("f").getLength());
    }

    private static Document readPart(ByteArrayOutputStream bytes, String name) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes.toByteArray()), StandardCharsets.UTF_8)) {
            ZipEntry part;
            while ((part = zip.getNextEntry()) != null) {
                if (name.equals(part.getName())) {
                    return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                            .parse(new ByteArrayInputStream(zip.readAllBytes()));
                }
            }
        }
        throw new AssertionError("Missing XLSX part: " + name);
    }

    private static Element cell(Document document, String address) {
        NodeList cells = document.getElementsByTagName("c");
        Element match = null;
        for (int i = 0; i < cells.getLength(); i++) {
            Element cell = (Element) cells.item(i);
            if (address.equals(cell.getAttribute("r"))) match = cell;
        }
        assertNotNull("Missing XLSX cell: " + address, match);
        return match;
    }
}

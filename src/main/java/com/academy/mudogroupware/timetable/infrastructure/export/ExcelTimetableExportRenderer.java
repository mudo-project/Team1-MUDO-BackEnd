package com.academy.mudogroupware.timetable.infrastructure.export;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.academy.mudogroupware.timetable.application.port.TimetableExportRenderer;
import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;

@Component
public class ExcelTimetableExportRenderer implements TimetableExportRenderer {

    @Override
    public boolean supports(TimetableExportFormat format) {
        return format == TimetableExportFormat.EXCEL;
    }

    @Override
    public byte[] render(String timetableSetName, List<TimetableSlotView> sortedSlots,
                          Map<ClassType, Color> colorsByClassType) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(safeSheetName(timetableSetName));
            Map<ClassType, CellStyle> stylesByClassType = buildStyles(workbook, colorsByClassType);

            writeHeader(sheet);
            writeRows(sheet, sortedSlots, stylesByClassType);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("시간표 엑셀 생성에 실패했습니다.", e);
        }
    }

    private String safeSheetName(String name) {
        String trimmed = name.length() > 31 ? name.substring(0, 31) : name;
        return trimmed.replaceAll("[\\\\/*\\[\\]:?]", "_");
    }

    private Map<ClassType, CellStyle> buildStyles(XSSFWorkbook workbook, Map<ClassType, Color> colorsByClassType) {
        Map<ClassType, CellStyle> styles = new EnumMap<>(ClassType.class);
        for (Map.Entry<ClassType, Color> entry : colorsByClassType.entrySet()) {
            CellStyle style = workbook.createCellStyle();
            style.setFillForegroundColor(new XSSFColor(entry.getValue(), null));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            styles.put(entry.getKey(), style);
        }
        return styles;
    }

    private void writeHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < TimetableExportLabels.HEADERS.length; i++) {
            header.createCell(i).setCellValue(TimetableExportLabels.HEADERS[i]);
        }
    }

    private void writeRows(Sheet sheet, List<TimetableSlotView> slots, Map<ClassType, CellStyle> stylesByClassType) {
        int rowIndex = 1;
        for (TimetableSlotView slot : slots) {
            Row row = sheet.createRow(rowIndex++);
            String[] values = TimetableExportLabels.toRow(slot);
            CellStyle style = stylesByClassType.get(slot.classType());
            for (int i = 0; i < values.length; i++) {
                Cell cell = row.createCell(i);
                cell.setCellValue(values[i]);
                if (style != null) {
                    cell.setCellStyle(style);
                }
            }
        }
    }
}

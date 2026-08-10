package com.academy.mudogroupware.timetable.infrastructure.export;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.academy.mudogroupware.timetable.application.port.TimetableExportRenderer;
import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportColor;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportOptions;

@Component
public class ExcelTimetableExportRenderer implements TimetableExportRenderer {

    // 화면 밀도(px 기준)를 엑셀 포인트 단위로 환산하는 근사치(1pt ≈ 1.333px).
    private static final float PX_TO_POINT = 0.75f;

    @Override
    public boolean supports(TimetableExportFormat format) {
        return format == TimetableExportFormat.EXCEL;
    }

    @Override
    public byte[] render(String timetableSetName, List<TimetableSlotView> sortedSlots,
                          TimetableExportOptions options) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(safeSheetName(timetableSetName));
            Font font = buildFont(workbook, options);
            float rowHeightPoints = options.density().rowHeightPx() * PX_TO_POINT;
            Map<String, CellStyle> stylesByColorKey = new HashMap<>();

            writeHeader(sheet, font, rowHeightPoints);
            writeRows(sheet, sortedSlots, options, font, rowHeightPoints, workbook, stylesByColorKey);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("시간표 엑셀 생성에 실패했습니다.", e);
        }
    }

    private String safeSheetName(String name) {
        return WorkbookUtil.createSafeSheetName(name);
    }

    private Font buildFont(XSSFWorkbook workbook, TimetableExportOptions options) {
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) Math.round(options.density().fontSize()));
        return font;
    }

    private void writeHeader(Sheet sheet, Font font, float rowHeightPoints) {
        Row header = sheet.createRow(0);
        header.setHeightInPoints(rowHeightPoints);
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        headerStyle.setFont(font);
        for (int i = 0; i < TimetableExportLabels.HEADERS.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(TimetableExportLabels.HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeRows(Sheet sheet, List<TimetableSlotView> slots, TimetableExportOptions options, Font font,
                            float rowHeightPoints, XSSFWorkbook workbook, Map<String, CellStyle> stylesByColorKey) {
        int rowIndex = 1;
        for (TimetableSlotView slot : slots) {
            Row row = sheet.createRow(rowIndex++);
            row.setHeightInPoints(rowHeightPoints);
            String[] values = TimetableExportLabels.toRow(slot);
            TimetableExportColor color = options.colorFor(slot.classroomCode(), slot.teacherName());
            CellStyle style = stylesByColorKey.computeIfAbsent(
                    color.red() + "," + color.green() + "," + color.blue(),
                    key -> buildStyle(workbook, font, color));
            for (int i = 0; i < values.length; i++) {
                Cell cell = row.createCell(i);
                cell.setCellValue(values[i]);
                cell.setCellStyle(style);
            }
        }
    }

    private CellStyle buildStyle(XSSFWorkbook workbook, Font font, TimetableExportColor color) {
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(toAwtColor(color), null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private Color toAwtColor(TimetableExportColor color) {
        return new Color(color.red(), color.green(), color.blue());
    }
}

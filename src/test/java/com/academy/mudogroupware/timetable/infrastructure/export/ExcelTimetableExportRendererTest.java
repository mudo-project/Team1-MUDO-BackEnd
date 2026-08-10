package com.academy.mudogroupware.timetable.infrastructure.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.Grade;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportColor;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportColorCriterion;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportDensity;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportOptions;

class ExcelTimetableExportRendererTest {

    private final ExcelTimetableExportRenderer renderer = new ExcelTimetableExportRenderer();

    @Test
    void supportsOnlyExcelFormat() {
        assertThat(renderer.supports(TimetableExportFormat.EXCEL)).isTrue();
        assertThat(renderer.supports(TimetableExportFormat.PDF)).isFalse();
    }

    @Test
    void renderProducesReadableWorkbookWithHeaderAndRows() throws Exception {
        List<TimetableSlotView> slots = List.of(new TimetableSlotView(
                100L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분"));
        TimetableExportOptions options = new TimetableExportOptions(
                TimetableExportColorCriterion.CLASSROOM,
                Map.of("601", TimetableExportColor.fromHex("FFCC00")),
                TimetableExportDensity.NORMAL);

        byte[] bytes = renderer.render("2026 여름특강", slots, options);

        assertThat(bytes).isNotEmpty();
        assertThat(bytes[0]).isEqualTo((byte) 'P');
        assertThat(bytes[1]).isEqualTo((byte) 'K');

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("요일");
            Row dataRow = sheet.getRow(1);
            assertThat(dataRow.getCell(2).getStringCellValue()).isEqualTo("601");
        }
    }

    @Test
    void renderPaintsTeacherCriterionColorOnDataRow() throws Exception {
        List<TimetableSlotView> slots = List.of(new TimetableSlotView(
                100L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분"));
        TimetableExportOptions options = new TimetableExportOptions(
                TimetableExportColorCriterion.TEACHER,
                Map.of("정T", TimetableExportColor.fromHex("00AACC")),
                TimetableExportDensity.NORMAL);

        byte[] bytes = renderer.render("2026 여름특강", slots, options);

        assertThat(dataRowFillColor(bytes)).isEqualTo("00AACC");
    }

    private String dataRowFillColor(byte[] bytes) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Row dataRow = workbook.getSheetAt(0).getRow(1);
            Cell cell = dataRow.getCell(0);
            XSSFCellStyle style = (XSSFCellStyle) cell.getCellStyle();
            return style.getFillForegroundColorColor().getARGBHex().substring(2).toUpperCase();
        }
    }
}

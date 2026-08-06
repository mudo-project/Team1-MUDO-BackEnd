package com.academy.mudogroupware.rollcall.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.rollcall.application.query.RosterEntryView;
import com.academy.mudogroupware.rollcall.application.query.RosterSummaryView;
import com.academy.mudogroupware.rollcall.application.query.RosterView;
import com.academy.mudogroupware.rollcall.application.usecase.GetLectureRosterUseCase;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;

class ExportAttendanceSheetServiceTest {

    private static final Long LECTURE_ID = 1L;
    private static final Long ACADEMY_ID = 100L;
    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);

    private final GetLectureRosterUseCase getLectureRosterUseCase = mock(GetLectureRosterUseCase.class);

    private ExportAttendanceSheetService service;

    @BeforeEach
    void setUp() {
        service = new ExportAttendanceSheetService(getLectureRosterUseCase);
    }

    @Test
    void generatesSheetWithHeaderEntriesAndSummaryRowEvenWithUncheckedStudents() throws IOException {
        List<RosterEntryView> entries = List.of(
                new RosterEntryView(10L, "이준호", "MIDDLE_3", "010-1111-1111", AttendanceStatus.ABSENT, null),
                new RosterEntryView(20L, "김서윤", "HIGH_1", "010-2222-2222", null, null));
        when(getLectureRosterUseCase.getRoster(LECTURE_ID, ACADEMY_ID, DATE)).thenReturn(
                new RosterView(LECTURE_ID, "수학 기초반", DATE, entries, new RosterSummaryView(2, 0, 1, 0, 0, 0)));

        byte[] sheet = service.exportSheet(LECTURE_ID, ACADEMY_ID, DATE);

        assertThat(sheet).isNotEmpty();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(sheet))) {
            Sheet firstSheet = workbook.getSheetAt(0);
            Row header = firstSheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("번호");

            Row uncheckedRow = firstSheet.getRow(2);
            assertThat(uncheckedRow.getCell(3).getStringCellValue()).isEmpty();

            Row summaryRow = firstSheet.getRow(4);
            assertThat(summaryRow.getCell(0).getStringCellValue()).contains("총원 2");
        }
    }
}

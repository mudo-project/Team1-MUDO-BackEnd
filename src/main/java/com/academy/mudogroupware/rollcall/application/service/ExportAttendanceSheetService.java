package com.academy.mudogroupware.rollcall.application.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.rollcall.application.query.RosterEntryView;
import com.academy.mudogroupware.rollcall.application.query.RosterSummaryView;
import com.academy.mudogroupware.rollcall.application.query.RosterView;
import com.academy.mudogroupware.rollcall.application.usecase.ExportAttendanceSheetUseCase;
import com.academy.mudogroupware.rollcall.application.usecase.GetLectureRosterUseCase;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportAttendanceSheetService implements ExportAttendanceSheetUseCase {

    private static final String[] HEADERS = {"번호", "학생명", "학년", "출결상태", "비고"};

    private final GetLectureRosterUseCase getLectureRosterUseCase;

    @Override
    public byte[] exportSheet(Long lectureId, Long academyId, LocalDate date) {
        RosterView roster = getLectureRosterUseCase.getRoster(lectureId, academyId, date);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("출결부");
            writeHeader(sheet);
            int rowIndex = writeEntries(sheet, roster.entries());
            writeSummary(sheet, rowIndex + 1, roster.summary());

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("엑셀 생성에 실패했습니다.", e);
        }
    }

    private void writeHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }
    }

    private int writeEntries(Sheet sheet, List<RosterEntryView> entries) {
        int rowIndex = 1;
        int number = 1;
        for (RosterEntryView entry : entries) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(number++);
            row.createCell(1).setCellValue(entry.studentName());
            row.createCell(2).setCellValue(entry.grade());
            row.createCell(3).setCellValue(entry.status() != null ? statusLabel(entry.status()) : "");
            row.createCell(4).setCellValue(entry.status() == AttendanceStatus.ETC && entry.note() != null
                    ? entry.note() : "");
        }
        return rowIndex;
    }

    private void writeSummary(Sheet sheet, int rowIndex, RosterSummaryView summary) {
        Row summaryRow = sheet.createRow(rowIndex);
        summaryRow.createCell(0).setCellValue(String.format(
                "총원 %d 출석 %d 결석 %d 지각 %d 인강 %d 기타 %d",
                summary.total(), summary.present(), summary.absent(), summary.late(), summary.online(),
                summary.etc()));
    }

    private String statusLabel(AttendanceStatus status) {
        return switch (status) {
            case PRESENT -> "출석";
            case ABSENT -> "결석";
            case LATE -> "지각";
            case ONLINE -> "인강";
            case ETC -> "기타";
        };
    }
}

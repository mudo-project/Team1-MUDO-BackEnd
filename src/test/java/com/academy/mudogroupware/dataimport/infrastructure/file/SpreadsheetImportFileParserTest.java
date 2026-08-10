package com.academy.mudogroupware.dataimport.infrastructure.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.dataimport.application.port.ImportFile;
import com.academy.mudogroupware.dataimport.application.port.ImportFileRole;
import com.academy.mudogroupware.dataimport.application.port.ParsedImportSheet;

class SpreadsheetImportFileParserTest {

    private final SpreadsheetImportFileParser parser = new SpreadsheetImportFileParser();

    @Test
    void parsesCsvHeaderAndRows() {
        ImportFile file = ImportFile.student("students.csv",
                "이름,학년,학교,연락처\n김민수,고1,무도고,010-1111-2222"
                        .getBytes(StandardCharsets.UTF_8));

        ParsedImportSheet sheet = parser.parse(file);

        assertThat(sheet.role()).isEqualTo(ImportFileRole.STUDENT);
        assertThat(sheet.rows()).hasSize(1);
        assertThat(sheet.rows().get(0).value("이름")).isEqualTo("김민수");
        assertThat(sheet.rows().get(0).value("학년")).isEqualTo("고1");
    }

    @Test
    void parsesXlsxFirstSheet() throws Exception {
        ImportFile file = ImportFile.lecture("lectures.xlsx", lectureWorkbook());

        ParsedImportSheet sheet = parser.parse(file);

        assertThat(sheet.role()).isEqualTo(ImportFileRole.LECTURE);
        assertThat(sheet.rows()).hasSize(1);
        assertThat(sheet.rows().get(0).value("강의명")).isEqualTo("고1 수학");
        assertThat(sheet.rows().get(0).value("강사ID")).isEqualTo("30");
    }

    private byte[] lectureWorkbook() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("lectures");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("강의명");
            header.createCell(1).setCellValue("학년");
            header.createCell(2).setCellValue("강사ID");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("고1 수학");
            row.createCell(1).setCellValue("고1");
            row.createCell(2).setCellValue(30);

            workbook.write(out);
            return out.toByteArray();
        }
    }
}

package com.academy.mudogroupware.dataimport.infrastructure.file;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.academy.mudogroupware.dataimport.application.port.ImportFile;
import com.academy.mudogroupware.dataimport.application.port.ImportFileParserPort;
import com.academy.mudogroupware.dataimport.application.port.ParsedImportRow;
import com.academy.mudogroupware.dataimport.application.port.ParsedImportSheet;
import com.academy.mudogroupware.dataimport.domain.exception.DataImportErrorCode;
import com.academy.mudogroupware.dataimport.domain.exception.DataImportException;

@Component
public class SpreadsheetImportFileParser implements ImportFileParserPort {

    @Override
    public ParsedImportSheet parse(ImportFile file) {
        String lowerName = file.fileName().toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".csv")) {
            return parseCsv(file);
        }
        if (lowerName.endsWith(".xlsx")) {
            return parseXlsx(file);
        }
        throw new DataImportException(DataImportErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    private ParsedImportSheet parseCsv(ImportFile file) {
        String text = new String(file.content(), StandardCharsets.UTF_8);
        String[] lines = text.split("\\R");
        if (lines.length == 0 || lines[0].isBlank()) {
            return new ParsedImportSheet(file.role(), file.fileName(), List.of());
        }

        List<String> headers = parseCsvLine(lines[0]).stream()
                .map(this::cleanHeader)
                .toList();
        List<ParsedImportRow> rows = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            List<String> cells = parseCsvLine(lines[i]);
            Map<String, String> values = valuesByHeader(headers, cells);
            if (!values.isEmpty()) {
                rows.add(new ParsedImportRow(i + 1, values));
            }
        }
        return new ParsedImportSheet(file.role(), file.fileName(), rows);
    }

    private ParsedImportSheet parseXlsx(ImportFile file) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file.content()))) {
            if (workbook.getNumberOfSheets() == 0) {
                return new ParsedImportSheet(file.role(), file.fileName(), List.of());
            }
            DataFormatter formatter = new DataFormatter();
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return new ParsedImportSheet(file.role(), file.fileName(), List.of());
            }
            List<String> headers = new ArrayList<>();
            for (int cellIndex = 0; cellIndex < headerRow.getLastCellNum(); cellIndex++) {
                headers.add(cleanHeader(formatter.formatCellValue(headerRow.getCell(cellIndex))));
            }

            List<ParsedImportRow> rows = new ArrayList<>();
            for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                List<String> cells = new ArrayList<>();
                for (int cellIndex = 0; cellIndex < headers.size(); cellIndex++) {
                    cells.add(formatter.formatCellValue(row.getCell(cellIndex)));
                }
                Map<String, String> values = valuesByHeader(headers, cells);
                if (!values.isEmpty()) {
                    rows.add(new ParsedImportRow(rowIndex + 1, values));
                }
            }
            return new ParsedImportSheet(file.role(), file.fileName(), rows);
        } catch (IOException e) {
            throw new DataImportException(DataImportErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private String cleanHeader(String header) {
        if (header == null) {
            return "";
        }
        return header.replace("\uFEFF", "").trim();
    }

    private Map<String, String> valuesByHeader(List<String> headers, List<String> cells) {
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            if (header == null || header.isBlank()) {
                continue;
            }
            String value = i < cells.size() ? cells.get(i).trim() : "";
            if (!value.isBlank()) {
                values.put(header, value);
            }
        }
        return values;
    }
}

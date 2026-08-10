package com.academy.mudogroupware.timetable.infrastructure.export;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.timetable.application.port.TimetableExportRenderer;
import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportColor;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportOptions;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Component
public class PdfTimetableExportRenderer implements TimetableExportRenderer {

    // 화면 밀도(px 기준)를 PDF 포인트 단위로 환산하는 근사치(1pt ≈ 1.333px).
    private static final float PX_TO_POINT = 0.75f;

    private static final BaseFont KOREAN_BASE_FONT = loadBaseFont();
    private static final Font TITLE_FONT = new Font(KOREAN_BASE_FONT, 16, Font.BOLD);

    @Override
    public boolean supports(TimetableExportFormat format) {
        return format == TimetableExportFormat.PDF;
    }

    @Override
    public byte[] render(String timetableSetName, List<TimetableSlotView> sortedSlots,
                          TimetableExportOptions options) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A3.rotate());
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph(timetableSetName, TITLE_FONT));
            document.add(buildTable(sortedSlots, options));
        } catch (DocumentException e) {
            throw new IllegalStateException("시간표 PDF 생성에 실패했습니다.", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private PdfPTable buildTable(List<TimetableSlotView> sortedSlots, TimetableExportOptions options) {
        Font headerFont = new Font(KOREAN_BASE_FONT, options.density().fontSize() + 1, Font.BOLD);
        Font bodyFont = new Font(KOREAN_BASE_FONT, options.density().fontSize(), Font.NORMAL);
        float rowHeightPoints = options.density().rowHeightPx() * PX_TO_POINT;

        PdfPTable table = new PdfPTable(TimetableExportLabels.HEADERS.length);
        table.setWidthPercentage(100);
        for (String header : TimetableExportLabels.HEADERS) {
            PdfPCell headerCell = new PdfPCell(new Phrase(header, headerFont));
            headerCell.setMinimumHeight(rowHeightPoints);
            table.addCell(headerCell);
        }
        for (TimetableSlotView slot : sortedSlots) {
            TimetableExportColor color = options.colorFor(slot.classroomCode(), slot.teacherName(), slot.grade());
            for (String value : TimetableExportLabels.toRow(slot)) {
                PdfPCell cell = new PdfPCell(new Phrase(value, bodyFont));
                cell.setMinimumHeight(rowHeightPoints);
                cell.setBackgroundColor(new Color(color.red(), color.green(), color.blue()));
                table.addCell(cell);
            }
        }
        return table;
    }

    private static BaseFont loadBaseFont() {
        try {
            byte[] fontBytes = TimetableExportFonts.readBytes(TimetableExportFonts.REGULAR_RESOURCE);
            return BaseFont.createFont(
                    TimetableExportFonts.REGULAR_RESOURCE, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, false,
                    fontBytes, null);
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("내보내기용 한글 폰트를 PDF에 임베드하는 데 실패했습니다.", e);
        }
    }
}

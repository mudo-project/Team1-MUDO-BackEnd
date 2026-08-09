package com.academy.mudogroupware.timetable.infrastructure.export;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.timetable.application.port.TimetableExportRenderer;
import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;
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

    private static final BaseFont KOREAN_BASE_FONT = loadBaseFont();
    private static final Font HEADER_FONT = new Font(KOREAN_BASE_FONT, 11, Font.BOLD);
    private static final Font BODY_FONT = new Font(KOREAN_BASE_FONT, 10, Font.NORMAL);
    private static final Font TITLE_FONT = new Font(KOREAN_BASE_FONT, 16, Font.BOLD);

    @Override
    public boolean supports(TimetableExportFormat format) {
        return format == TimetableExportFormat.PDF;
    }

    @Override
    public byte[] render(String timetableSetName, List<TimetableSlotView> sortedSlots,
                          Map<ClassType, Color> colorsByClassType) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A3.rotate());
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph(timetableSetName, TITLE_FONT));
            document.add(buildTable(sortedSlots, colorsByClassType));
        } catch (DocumentException e) {
            throw new IllegalStateException("시간표 PDF 생성에 실패했습니다.", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private PdfPTable buildTable(List<TimetableSlotView> sortedSlots, Map<ClassType, Color> colorsByClassType) {
        PdfPTable table = new PdfPTable(TimetableExportLabels.HEADERS.length);
        table.setWidthPercentage(100);
        for (String header : TimetableExportLabels.HEADERS) {
            table.addCell(new Phrase(header, HEADER_FONT));
        }
        for (TimetableSlotView slot : sortedSlots) {
            Color awtColor = colorsByClassType.get(slot.classType());
            for (String value : TimetableExportLabels.toRow(slot)) {
                PdfPCell cell = new PdfPCell(new Phrase(value, BODY_FONT));
                if (awtColor != null) {
                    cell.setBackgroundColor(awtColor);
                }
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

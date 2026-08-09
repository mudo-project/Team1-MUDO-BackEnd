package com.academy.mudogroupware.timetable.infrastructure.export;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.timetable.application.port.TimetableExportRenderer;
import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Component
public class PdfTimetableExportRenderer implements TimetableExportRenderer {

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
            document.add(new Paragraph(timetableSetName));
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
            table.addCell(new Phrase(header));
        }
        for (TimetableSlotView slot : sortedSlots) {
            Color awtColor = colorsByClassType.get(slot.classType());
            for (String value : TimetableExportLabels.toRow(slot)) {
                PdfPCell cell = new PdfPCell(new Phrase(value));
                if (awtColor != null) {
                    cell.setBackgroundColor(awtColor);
                }
                table.addCell(cell);
            }
        }
        return table;
    }
}

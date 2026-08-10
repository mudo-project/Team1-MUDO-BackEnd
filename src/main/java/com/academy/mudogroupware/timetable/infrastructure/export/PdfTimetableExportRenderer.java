package com.academy.mudogroupware.timetable.infrastructure.export;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.lowagie.text.pdf.FontSelector;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Component
public class PdfTimetableExportRenderer implements TimetableExportRenderer {

    // 화면 밀도(px 기준)를 PDF 포인트 단위로 환산하는 근사치(1pt ≈ 1.333px).
    private static final float PX_TO_POINT = 0.75f;

    // Inter는 한글 글리프가 없으므로, Inter가 그릴 수 없는 문자(한글 등)는
    // 나눔고딕으로 자동 대체해서 그린다(FontSelector가 문자 단위로 폰트를 고른다).
    private static final BaseFont INTER_BASE_FONT = loadBaseFont(TimetableExportFonts.INTER_RESOURCE);
    private static final BaseFont KOREAN_BASE_FONT = loadBaseFont(TimetableExportFonts.KOREAN_REGULAR_RESOURCE);

    @Override
    public boolean supports(TimetableExportFormat format) {
        return format == TimetableExportFormat.PDF;
    }

    @Override
    public byte[] render(String timetableSetName, List<TimetableSlotView> sortedSlots,
                          TimetableExportOptions options) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A3.rotate());
        // (size, style) 조합별로 FontSelector를 재사용해, 렌더링 1회당 셀 수만큼
        // FontSelector가 새로 생성되며 폰트가 반복 등록되는 비용을 없앤다.
        Map<String, FontSelector> selectorCache = new HashMap<>();
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph(mixedFontPhrase(selectorCache, timetableSetName, 16, Font.BOLD)));
            document.add(buildTable(sortedSlots, options, selectorCache));
        } catch (DocumentException e) {
            throw new IllegalStateException("시간표 PDF 생성에 실패했습니다.", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private PdfPTable buildTable(List<TimetableSlotView> sortedSlots, TimetableExportOptions options,
                                  Map<String, FontSelector> selectorCache) {
        float headerFontSize = options.density().fontSize() + 1;
        float bodyFontSize = options.density().fontSize();
        float rowHeightPoints = options.density().rowHeightPx() * PX_TO_POINT;

        PdfPTable table = new PdfPTable(TimetableExportLabels.HEADERS.length);
        table.setWidthPercentage(100);
        for (String header : TimetableExportLabels.HEADERS) {
            PdfPCell headerCell = new PdfPCell(mixedFontPhrase(selectorCache, header, headerFontSize, Font.BOLD));
            headerCell.setMinimumHeight(rowHeightPoints);
            table.addCell(headerCell);
        }
        for (TimetableSlotView slot : sortedSlots) {
            TimetableExportColor color = options.colorFor(slot.classroomCode(), slot.teacherName());
            for (String value : TimetableExportLabels.toRow(slot)) {
                PdfPCell cell = new PdfPCell(mixedFontPhrase(selectorCache, value, bodyFontSize, Font.NORMAL));
                cell.setMinimumHeight(rowHeightPoints);
                cell.setBackgroundColor(new Color(color.red(), color.green(), color.blue()));
                table.addCell(cell);
            }
        }
        return table;
    }

    private Phrase mixedFontPhrase(Map<String, FontSelector> selectorCache, String text, float size, int style) {
        FontSelector selector = selectorCache.computeIfAbsent(size + ":" + style, key -> {
            FontSelector newSelector = new FontSelector();
            newSelector.addFont(new Font(INTER_BASE_FONT, size, style));
            newSelector.addFont(new Font(KOREAN_BASE_FONT, size, style));
            return newSelector;
        });
        return selector.process(text);
    }

    private static BaseFont loadBaseFont(String resourcePath) {
        try {
            byte[] fontBytes = TimetableExportFonts.readBytes(resourcePath);
            return BaseFont.createFont(
                    resourcePath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, false, fontBytes, null);
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("내보내기용 폰트를 PDF에 임베드하는 데 실패했습니다: " + resourcePath, e);
        }
    }
}

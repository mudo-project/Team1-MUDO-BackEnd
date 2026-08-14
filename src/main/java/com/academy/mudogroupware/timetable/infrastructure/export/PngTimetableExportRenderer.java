package com.academy.mudogroupware.timetable.infrastructure.export;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.timetable.application.port.TimetableExportRenderer;
import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.domain.exception.ExportImageTooLargeException;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportColor;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportOptions;

@Component
public class PngTimetableExportRenderer implements TimetableExportRenderer {

    private static final int[] COLUMN_WIDTHS = {60, 130, 100, 90, 100, 140, 70};
    private static final int TITLE_HEIGHT = 30;
    private static final long MAX_TOTAL_PIXELS = 20_000_000L;

    @Override
    public boolean supports(TimetableExportFormat format) {
        return format == TimetableExportFormat.PNG;
    }

    @Override
    public byte[] render(String timetableSetName, List<TimetableSlotView> sortedSlots,
                          TimetableExportOptions options) {
        int rowHeight = options.density().rowHeightPx();
        int headerHeight = rowHeight;
        float fontSize = options.density().fontSize();

        long totalWidth = sumWidths();
        long totalHeight = (long) TITLE_HEIGHT + headerHeight + (long) sortedSlots.size() * rowHeight;
        if (totalWidth * totalHeight > MAX_TOTAL_PIXELS) {
            throw new ExportImageTooLargeException();
        }

        int width = (int) totalWidth;
        int height = (int) totalHeight;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);

            g.setColor(Color.BLACK);
            drawMixedFontString(g, timetableSetName, 8, TITLE_HEIGHT - 8, 16f, true);

            int y = TITLE_HEIGHT;
            drawRow(g, y, headerHeight, TimetableExportLabels.HEADERS, new Color(0xE0E0E0), true, fontSize);
            y += headerHeight;

            for (TimetableSlotView slot : sortedSlots) {
                TimetableExportColor color = TimetableExportColor.fromHex(slot.color());
                Color rowColor = new Color(color.red(), color.green(), color.blue());
                drawRow(g, y, rowHeight, TimetableExportLabels.toRow(slot), rowColor, false, fontSize);
                y += rowHeight;
            }
        } finally {
            g.dispose();
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("시간표 이미지 생성에 실패했습니다.", e);
        }
    }

    private int sumWidths() {
        int total = 0;
        for (int width : COLUMN_WIDTHS) {
            total += width;
        }
        return total;
    }

    private void drawRow(Graphics2D g, int y, int height, String[] values, Color background, boolean bold,
                          float fontSize) {
        int x = 0;
        for (int i = 0; i < values.length; i++) {
            g.setColor(background);
            g.fillRect(x, y, COLUMN_WIDTHS[i], height);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, COLUMN_WIDTHS[i], height);
            drawMixedFontString(g, values[i], x + 6, y + height / 2 + 5, fontSize, bold);
            x += COLUMN_WIDTHS[i];
        }
    }

    /**
     * Inter는 한글 글리프가 없으므로, Inter가 그릴 수 없는 문자(한글 등)는
     * 나눔고딕으로 자동 대체해서 그린다.
     */
    private void drawMixedFontString(Graphics2D g, String text, int x, int y, float fontSize, boolean bold) {
        Font interFont = TimetableExportFonts.AWT_INTER.deriveFont(bold ? Font.BOLD : Font.PLAIN, fontSize);
        Font koreanFont = (bold ? TimetableExportFonts.AWT_KOREAN_BOLD : TimetableExportFonts.AWT_KOREAN_REGULAR)
                .deriveFont(fontSize);

        int cursorX = x;
        int i = 0;
        while (i < text.length()) {
            int codePoint = text.codePointAt(i);
            boolean useInter = interFont.canDisplay(codePoint);
            int runStart = i;
            i += Character.charCount(codePoint);
            while (i < text.length()) {
                int nextCodePoint = text.codePointAt(i);
                if (interFont.canDisplay(nextCodePoint) != useInter) {
                    break;
                }
                i += Character.charCount(nextCodePoint);
            }
            String run = text.substring(runStart, i);
            Font runFont = useInter ? interFont : koreanFont;
            g.setFont(runFont);
            g.drawString(run, cursorX, y);
            cursorX += g.getFontMetrics(runFont).stringWidth(run);
        }
    }
}

package com.academy.mudogroupware.timetable.infrastructure.export;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.timetable.application.port.TimetableExportRenderer;
import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;

@Component
public class PngTimetableExportRenderer implements TimetableExportRenderer {

    private static final int[] COLUMN_WIDTHS = {60, 130, 100, 90, 100, 140, 70};
    private static final int ROW_HEIGHT = 32;
    private static final int HEADER_HEIGHT = 36;
    private static final int TITLE_HEIGHT = 30;

    @Override
    public boolean supports(TimetableExportFormat format) {
        return format == TimetableExportFormat.PNG;
    }

    @Override
    public byte[] render(String timetableSetName, List<TimetableSlotView> sortedSlots,
                          Map<ClassType, Color> colorsByClassType) {
        int totalWidth = sumWidths();
        int totalHeight = TITLE_HEIGHT + HEADER_HEIGHT + sortedSlots.size() * ROW_HEIGHT;

        BufferedImage image = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, totalWidth, totalHeight);

            g.setColor(Color.BLACK);
            g.setFont(TimetableExportFonts.AWT_BOLD.deriveFont(16f));
            g.drawString(timetableSetName, 8, TITLE_HEIGHT - 8);

            int y = TITLE_HEIGHT;
            drawRow(g, y, HEADER_HEIGHT, TimetableExportLabels.HEADERS, new Color(0xE0E0E0), true);
            y += HEADER_HEIGHT;

            for (TimetableSlotView slot : sortedSlots) {
                Color rowColor = colorsByClassType.getOrDefault(slot.classType(), Color.WHITE);
                drawRow(g, y, ROW_HEIGHT, TimetableExportLabels.toRow(slot), rowColor, false);
                y += ROW_HEIGHT;
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

    private void drawRow(Graphics2D g, int y, int height, String[] values, Color background, boolean bold) {
        int x = 0;
        Font baseFont = bold ? TimetableExportFonts.AWT_BOLD : TimetableExportFonts.AWT_REGULAR;
        g.setFont(baseFont.deriveFont(13f));
        for (int i = 0; i < values.length; i++) {
            g.setColor(background);
            g.fillRect(x, y, COLUMN_WIDTHS[i], height);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, COLUMN_WIDTHS[i], height);
            g.drawString(values[i], x + 6, y + height / 2 + 5);
            x += COLUMN_WIDTHS[i];
        }
    }
}

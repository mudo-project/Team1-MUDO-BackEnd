package com.academy.mudogroupware.timetable.infrastructure.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportColor;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;

class PdfTimetableExportRendererTest {

    private final PdfTimetableExportRenderer renderer = new PdfTimetableExportRenderer();

    @Test
    void supportsOnlyPdfFormat() {
        assertThat(renderer.supports(TimetableExportFormat.PDF)).isTrue();
        assertThat(renderer.supports(TimetableExportFormat.PNG)).isFalse();
    }

    @Test
    void renderProducesValidPdfBytes() {
        List<TimetableSlotView> slots = List.of(new TimetableSlotView(
                100L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                "고3", "정T", "미적분"));
        Map<ClassType, TimetableExportColor> colors = new EnumMap<>(ClassType.class);
        colors.put(ClassType.CLASS, TimetableExportColor.fromHex("FFCC00"));

        byte[] bytes = renderer.render("2026 여름특강", slots, colors);

        assertThat(bytes).isNotEmpty();
        String header = new String(bytes, 0, 4, StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF");
    }
}

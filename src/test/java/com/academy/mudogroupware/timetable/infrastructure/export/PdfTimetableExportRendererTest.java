package com.academy.mudogroupware.timetable.infrastructure.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.Grade;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportDensity;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportOptions;

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
                Grade.HIGH_3, "정T", "미적분", "FFCC00"));
        TimetableExportOptions options = new TimetableExportOptions(TimetableExportDensity.NORMAL);

        byte[] bytes = renderer.render("2026 여름특강", slots, options);

        assertThat(bytes).isNotEmpty();
        String header = new String(bytes, 0, 4, StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF");
    }

    @Test
    void renderSucceedsWithSlotOwnColor() {
        List<TimetableSlotView> slots = List.of(new TimetableSlotView(
                100L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", "00AACC"));
        TimetableExportOptions options = new TimetableExportOptions(TimetableExportDensity.NORMAL);

        byte[] bytes = renderer.render("2026 여름특강", slots, options);

        assertThat(bytes).isNotEmpty();
        assertThat(new String(bytes, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }
}

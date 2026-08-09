package com.academy.mudogroupware.timetable.infrastructure.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.domain.exception.ExportImageTooLargeException;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportColor;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;

class PngTimetableExportRendererTest {

    private final PngTimetableExportRenderer renderer = new PngTimetableExportRenderer();

    @Test
    void supportsOnlyPngFormat() {
        assertThat(renderer.supports(TimetableExportFormat.PNG)).isTrue();
        assertThat(renderer.supports(TimetableExportFormat.EXCEL)).isFalse();
    }

    @Test
    void renderProducesReadablePngImage() throws Exception {
        List<TimetableSlotView> slots = List.of(new TimetableSlotView(
                100L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                "고3", "정T", "미적분"));
        Map<ClassType, TimetableExportColor> colors = new EnumMap<>(ClassType.class);
        colors.put(ClassType.CLASS, TimetableExportColor.fromHex("FFCC00"));

        byte[] bytes = renderer.render("2026 여름특강", slots, colors);

        assertThat(bytes).isNotEmpty();
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        assertThat(image).isNotNull();
        assertThat(image.getHeight()).isGreaterThan(0);
        assertThat(image.getWidth()).isGreaterThan(0);
    }

    @Test
    void renderPaintsRequestedClassTypeColorOnDataRow() throws Exception {
        List<TimetableSlotView> slots = List.of(new TimetableSlotView(
                100L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                "고3", "정T", "미적분"));
        Map<ClassType, TimetableExportColor> colors = new EnumMap<>(ClassType.class);
        colors.put(ClassType.CLASS, TimetableExportColor.fromHex("FFCC00"));

        byte[] bytes = renderer.render("2026 여름특강", slots, colors);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));

        // 요일 열(0~60px) 안쪽, 텍스트와 테두리를 피한 지점의 배경색을 검사한다.
        int pixel = image.getRGB(45, 82) & 0xFFFFFF;
        assertThat(pixel).isEqualTo(0xFFCC00);
    }

    @Test
    void renderThrowsWhenResultingImageExceedsMaxPixelBudget() {
        List<TimetableSlotView> hugeSlotList = new java.util.ArrayList<>();
        for (int i = 0; i < 700_000; i++) {
            hugeSlotList.add(new TimetableSlotView(
                    (long) i, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                    "고3", "정T", "미적분"));
        }
        Map<ClassType, TimetableExportColor> colors = new EnumMap<>(ClassType.class);
        colors.put(ClassType.CLASS, TimetableExportColor.fromHex("FFCC00"));

        assertThatThrownBy(() -> renderer.render("2026 여름특강", hugeSlotList, colors))
                .isInstanceOf(ExportImageTooLargeException.class);
    }
}

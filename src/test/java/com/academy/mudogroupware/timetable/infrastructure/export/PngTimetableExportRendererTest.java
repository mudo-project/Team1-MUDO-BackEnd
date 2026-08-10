package com.academy.mudogroupware.timetable.infrastructure.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.domain.exception.ExportImageTooLargeException;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportColor;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportColorCriterion;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportDensity;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportOptions;

class PngTimetableExportRendererTest {

    private final PngTimetableExportRenderer renderer = new PngTimetableExportRenderer();

    private TimetableExportOptions options(TimetableExportColorCriterion criterion, String groupKey,
                                            TimetableExportDensity density) {
        return new TimetableExportOptions(
                criterion, Map.of(groupKey, TimetableExportColor.fromHex("FFCC00")), density);
    }

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

        byte[] bytes = renderer.render(
                "2026 여름특강", slots, options(TimetableExportColorCriterion.CLASSROOM, "601", TimetableExportDensity.NORMAL));

        assertThat(bytes).isNotEmpty();
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        assertThat(image).isNotNull();
        assertThat(image.getHeight()).isGreaterThan(0);
        assertThat(image.getWidth()).isGreaterThan(0);
    }

    @Test
    void renderPaintsClassroomCriterionColorOnDataRow() throws Exception {
        List<TimetableSlotView> slots = List.of(new TimetableSlotView(
                100L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                "고3", "정T", "미적분"));

        byte[] bytes = renderer.render(
                "2026 여름특강", slots, options(TimetableExportColorCriterion.CLASSROOM, "601", TimetableExportDensity.NORMAL));
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));

        // 요일 열(0~60px) 안쪽, 텍스트와 테두리를 피한 지점의 배경색을 검사한다.
        // title(30) + header(=NORMAL rowHeight 32) = 62, 데이터 행 중앙 y = 62 + 32/2 = 78.
        int pixel = image.getRGB(45, 78) & 0xFFFFFF;
        assertThat(pixel).isEqualTo(0xFFCC00);
    }

    @Test
    void renderPaintsTeacherCriterionColorOnDataRow() throws Exception {
        List<TimetableSlotView> slots = List.of(new TimetableSlotView(
                100L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                "고3", "정T", "미적분"));

        byte[] bytes = renderer.render(
                "2026 여름특강", slots, options(TimetableExportColorCriterion.TEACHER, "정T", TimetableExportDensity.NORMAL));
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));

        int pixel = image.getRGB(45, 78) & 0xFFFFFF;
        assertThat(pixel).isEqualTo(0xFFCC00);
    }

    @Test
    void renderPaintsGradeCriterionColorOnDataRow() throws Exception {
        List<TimetableSlotView> slots = List.of(new TimetableSlotView(
                100L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                "고3", "정T", "미적분"));

        byte[] bytes = renderer.render(
                "2026 여름특강", slots, options(TimetableExportColorCriterion.GRADE, "고3", TimetableExportDensity.NORMAL));
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));

        int pixel = image.getRGB(45, 78) & 0xFFFFFF;
        assertThat(pixel).isEqualTo(0xFFCC00);
    }

    @Test
    void renderAppliesDensityToHeaderHeightAsWellAsRowHeight() throws Exception {
        List<TimetableSlotView> slots = List.of(new TimetableSlotView(
                100L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                "고3", "정T", "미적분"));

        // title(30) + header(density rowHeight) + 1 row(density rowHeight)
        byte[] compactBytes = renderer.render(
                "2026 여름특강", slots, options(TimetableExportColorCriterion.CLASSROOM, "601", TimetableExportDensity.COMPACT));
        byte[] spaciousBytes = renderer.render(
                "2026 여름특강", slots, options(TimetableExportColorCriterion.CLASSROOM, "601", TimetableExportDensity.SPACIOUS));

        BufferedImage compactImage = ImageIO.read(new ByteArrayInputStream(compactBytes));
        BufferedImage spaciousImage = ImageIO.read(new ByteArrayInputStream(spaciousBytes));

        assertThat(compactImage.getHeight()).isEqualTo(30 + 24 + 24);
        assertThat(spaciousImage.getHeight()).isEqualTo(30 + 44 + 44);
    }

    @Test
    void renderThrowsWhenResultingImageExceedsMaxPixelBudget() {
        List<TimetableSlotView> hugeSlotList = new java.util.ArrayList<>();
        for (int i = 0; i < 700_000; i++) {
            hugeSlotList.add(new TimetableSlotView(
                    (long) i, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                    "고3", "정T", "미적분"));
        }

        assertThatThrownBy(() -> renderer.render(
                "2026 여름특강", hugeSlotList,
                options(TimetableExportColorCriterion.CLASSROOM, "601", TimetableExportDensity.NORMAL)))
                .isInstanceOf(ExportImageTooLargeException.class);
    }
}

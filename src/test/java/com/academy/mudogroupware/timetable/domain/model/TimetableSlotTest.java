package com.academy.mudogroupware.timetable.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.timetable.domain.exception.InvalidTimetableClassroomException;
import com.academy.mudogroupware.timetable.domain.exception.InvalidTimetableSlotException;

class TimetableSlotTest {

    private static final LocalDate FROM = LocalDate.of(2026, 7, 20);
    private static final LocalDate UNTIL = LocalDate.of(2026, 8, 16);

    private TimetableSlot slot(String classroomCode, DayOfWeek day, LocalTime start, LocalTime end) {
        return TimetableSlot.create(
                1L, ClassType.CLASS, day, classroomCode, start, end, Grade.HIGH_3, "정T", "미적분", FROM, UNTIL);
    }

    @Test
    void createBuildsSlotWithDefaultEffectiveRange() {
        TimetableSlot slot = slot("601", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));

        assertThat(slot.getClassroomCode()).isEqualTo("601");
        assertThat(slot.getEffectiveFrom()).isEqualTo(FROM);
        assertThat(slot.getEffectiveUntil()).isEqualTo(UNTIL);
    }

    @Test
    void overlapsReturnsTrueForSameClassroomDayAndOverlappingTime() {
        TimetableSlot a = slot("601", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));
        TimetableSlot b = slot("601", DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0));

        assertThat(a.overlaps(b)).isTrue();
    }

    @Test
    void overlapsReturnsFalseForDifferentClassroom() {
        TimetableSlot a = slot("601", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));
        TimetableSlot b = slot("602", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));

        assertThat(a.overlaps(b)).isFalse();
    }

    @Test
    void overlapsReturnsFalseForDifferentDay() {
        TimetableSlot a = slot("601", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));
        TimetableSlot b = slot("601", DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));

        assertThat(a.overlaps(b)).isFalse();
    }

    @Test
    void overlapsReturnsFalseForNonOverlappingTime() {
        TimetableSlot a = slot("601", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));
        TimetableSlot b = slot("601", DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(12, 0));

        assertThat(a.overlaps(b)).isFalse();
    }

    @Test
    void overlapsReturnsFalseWhenEffectiveRangesDoNotOverlap() {
        TimetableSlot a = TimetableSlot.create(
                1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 31));
        TimetableSlot b = TimetableSlot.create(
                1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 16));

        assertThat(a.overlaps(b)).isFalse();
    }

    @Test
    void closeEffectiveUntilTruncatesRange() {
        TimetableSlot slot = slot("601", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));

        slot.closeEffectiveUntil(LocalDate.of(2026, 8, 1));

        assertThat(slot.getEffectiveUntil()).isEqualTo(LocalDate.of(2026, 8, 1));
    }

    @Test
    void applyFullUpdateThrowsDomainExceptionWhenGradeIsMissing() {
        TimetableSlot slot = slot("601", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));

        assertThatThrownBy(() -> slot.applyFullUpdate(
                ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                null, "정T", "미적분"))
                .isInstanceOf(InvalidTimetableSlotException.class)
                .satisfies(e -> assertThat(((InvalidTimetableSlotException) e).getContext())
                        .containsEntry("field", "grade"));
    }

    @Test
    void restoreKeepsPersistedId() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 1, 0, 0);
        TimetableSlot slot = TimetableSlot.restore(
                10L, 1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", FROM, UNTIL, now, now);

        assertThat(slot.getId()).isEqualTo(10L);
    }

    @Test
    void createThrowsDomainExceptionWhenTimeRangeIsInvalid() {
        assertThatThrownBy(() -> slot(
                "601", DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(10, 0)))
                .isInstanceOf(InvalidTimetableSlotException.class)
                .satisfies(e -> assertThat(((InvalidTimetableSlotException) e).getContext())
                        .containsEntry("field", "timeRange"));
    }

    @Test
    void createThrowsDomainExceptionWhenGradeIsMissing() {
        assertThatThrownBy(() -> TimetableSlot.create(
                1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                null, "정T", "미적분", FROM, UNTIL))
                .isInstanceOf(InvalidTimetableSlotException.class)
                .satisfies(e -> assertThat(((InvalidTimetableSlotException) e).getContext())
                        .containsEntry("field", "grade"));
    }

    @Test
    void classroomThrowsDomainExceptionWhenCodeIsBlank() {
        assertThatThrownBy(() -> new TimetableClassroom("6층", " "))
                .isInstanceOf(InvalidTimetableClassroomException.class)
                .satisfies(e -> assertThat(((InvalidTimetableClassroomException) e).getContext())
                        .containsEntry("field", "code"));
    }
}

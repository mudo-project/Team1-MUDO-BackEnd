package com.academy.mudogroupware.timetable.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.timetable.domain.exception.DuplicateClassroomCodeException;
import com.academy.mudogroupware.timetable.domain.exception.InvalidTimetablePeriodException;
import com.academy.mudogroupware.timetable.domain.exception.InvalidTimetableSetException;
import com.academy.mudogroupware.timetable.domain.exception.TimetableNameRequiredException;

class TimetableSetTest {

    private static final List<TimetableClassroom> CLASSROOMS = List.of(
            new TimetableClassroom("6층", "601"), new TimetableClassroom("6층", "602"));
    private static final Set<DayOfWeek> DAYS = Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);

    @Test
    void createBuildsTimetableSet() {
        TimetableSet set = TimetableSet.create(
                1L, "2026 여름특강", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), DAYS, 30, CLASSROOMS);

        assertThat(set.getAcademyId()).isEqualTo(1L);
        assertThat(set.getName()).isEqualTo("2026 여름특강");
        assertThat(set.getClassrooms()).containsExactlyInAnyOrderElementsOf(CLASSROOMS);
    }

    @Test
    void createThrowsWhenNameIsBlank() {
        assertThatThrownBy(() -> TimetableSet.create(
                1L, "  ", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), DAYS, 30, CLASSROOMS))
                .isInstanceOf(TimetableNameRequiredException.class);
    }

    @Test
    void createThrowsWhenEndDateBeforeStartDate() {
        assertThatThrownBy(() -> TimetableSet.create(
                1L, "이름", LocalDate.of(2026, 8, 16), LocalDate.of(2026, 7, 20),
                LocalTime.of(8, 30), LocalTime.of(22, 0), DAYS, 30, CLASSROOMS))
                .isInstanceOf(InvalidTimetablePeriodException.class);
    }

    @Test
    void createThrowsWhenClassroomCodeDuplicated() {
        List<TimetableClassroom> duplicated = List.of(
                new TimetableClassroom("6층", "601"), new TimetableClassroom("5층", "601"));

        assertThatThrownBy(() -> TimetableSet.create(
                1L, "이름", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), DAYS, 30, duplicated))
                .isInstanceOf(DuplicateClassroomCodeException.class);
    }

    @Test
    void createThrowsDomainExceptionWhenOperatingDaysAreMissing() {
        assertThatThrownBy(() -> TimetableSet.create(
                1L, "이름", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(), 30, CLASSROOMS))
                .isInstanceOf(InvalidTimetableSetException.class)
                .satisfies(e -> assertThat(((InvalidTimetableSetException) e).getContext())
                        .containsEntry("field", "operatingDays"));
    }

    @Test
    void deriveStatusReturnsPlannedBeforeStartDate() {
        TimetableSet set = TimetableSet.create(
                1L, "이름", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), DAYS, 30, CLASSROOMS);

        assertThat(set.deriveStatus(LocalDate.of(2026, 7, 19))).isEqualTo(TimetableSetStatus.PLANNED);
    }

    @Test
    void deriveStatusReturnsActiveWithinPeriod() {
        TimetableSet set = TimetableSet.create(
                1L, "이름", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), DAYS, 30, CLASSROOMS);

        assertThat(set.deriveStatus(LocalDate.of(2026, 8, 1))).isEqualTo(TimetableSetStatus.ACTIVE);
    }

    @Test
    void deriveStatusReturnsEndedAfterEndDate() {
        TimetableSet set = TimetableSet.create(
                1L, "이름", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), DAYS, 30, CLASSROOMS);

        assertThat(set.deriveStatus(LocalDate.of(2026, 8, 17))).isEqualTo(TimetableSetStatus.ENDED);
    }

    @Test
    void updateReplacesFieldsAndRevalidates() {
        TimetableSet set = TimetableSet.create(
                1L, "이름", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), DAYS, 30, CLASSROOMS);

        set.update("새 이름", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31),
                LocalTime.of(9, 0), LocalTime.of(21, 0), Set.of(DayOfWeek.TUESDAY), 10,
                List.of(new TimetableClassroom("3층", "301")));

        assertThat(set.getName()).isEqualTo("새 이름");
        assertThat(set.getSlotUnitMinutes()).isEqualTo(10);
        assertThat(set.getClassrooms()).containsExactly(new TimetableClassroom("3층", "301"));
    }

    @Test
    void restoreKeepsPersistedTimestamps() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 1, 0, 0);
        TimetableSet set = TimetableSet.restore(
                10L, 1L, "이름", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), DAYS, 30, CLASSROOMS, now, now);

        assertThat(set.getId()).isEqualTo(10L);
        assertThat(set.getCreatedAt()).isEqualTo(now);
    }
}

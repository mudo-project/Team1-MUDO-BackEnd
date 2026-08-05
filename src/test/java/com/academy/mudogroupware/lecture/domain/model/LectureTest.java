package com.academy.mudogroupware.lecture.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.lecture.domain.exception.LectureNameRequiredException;
import com.academy.mudogroupware.lecture.domain.exception.LectureScheduleRequiredException;

class LectureTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 9, 0);

    private LectureSchedule schedule(DayOfWeek day, int startHour, int endHour) {
        return LectureSchedule.create(day, LocalTime.of(startHour, 0), LocalTime.of(endHour, 0));
    }

    @Test
    void createsLectureWithValidData() {
        Lecture lecture = Lecture.create(1L, "수학 기초반", Grade.MIDDLE_3, 10L, 20L, 30L, 40L, FeeType.PER_SESSION,
                50000, List.of(schedule(DayOfWeek.MONDAY, 15, 17)), NOW);

        assertThat(lecture.getName()).isEqualTo("수학 기초반");
        assertThat(lecture.getGrade()).isEqualTo(Grade.MIDDLE_3);
        assertThat(lecture.getSchedules()).hasSize(1);
    }

    @Test
    void throwsWhenNameIsBlank() {
        assertThatThrownBy(() -> Lecture.create(1L, "  ", Grade.MIDDLE_3, 10L, 20L, 30L, 40L, null, null,
                List.of(schedule(DayOfWeek.MONDAY, 15, 17)), NOW))
                .isInstanceOf(LectureNameRequiredException.class);
    }

    @Test
    void throwsWhenSchedulesAreEmpty() {
        assertThatThrownBy(() -> Lecture.create(1L, "수학 기초반", Grade.MIDDLE_3, 10L, 20L, 30L, 40L, null, null,
                List.of(), NOW))
                .isInstanceOf(LectureScheduleRequiredException.class);
    }

    @Test
    void conflictsWithReturnsTrueWhenScheduleOverlaps() {
        Lecture lecture = Lecture.create(1L, "수학 기초반", Grade.MIDDLE_3, 10L, 20L, 30L, 40L, null, null,
                List.of(schedule(DayOfWeek.MONDAY, 15, 17)), NOW);

        assertThat(lecture.conflictsWith(schedule(DayOfWeek.MONDAY, 16, 18))).isTrue();
        assertThat(lecture.conflictsWith(schedule(DayOfWeek.MONDAY, 17, 19))).isFalse();
        assertThat(lecture.conflictsWith(schedule(DayOfWeek.TUESDAY, 15, 17))).isFalse();
    }
}

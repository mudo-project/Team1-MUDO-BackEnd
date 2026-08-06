package com.academy.mudogroupware.lecture.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.lecture.domain.exception.InvalidLectureScheduleTimeException;

class LectureScheduleTest {

    @Test
    void throwsWhenStartTimeIsNotBeforeEndTime() {
        assertThatThrownBy(() -> LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(17, 0), LocalTime.of(15, 0)))
                .isInstanceOf(InvalidLectureScheduleTimeException.class);
    }

    @Test
    void overlapsReturnsFalseForDifferentDay() {
        LectureSchedule monday = LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(15, 0), LocalTime.of(17, 0));
        LectureSchedule tuesday = LectureSchedule.create(DayOfWeek.TUESDAY, LocalTime.of(15, 0), LocalTime.of(17, 0));

        assertThat(monday.overlaps(tuesday)).isFalse();
    }

    @Test
    void overlapsReturnsTrueWhenTimeRangesIntersect() {
        LectureSchedule first = LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(15, 0), LocalTime.of(17, 0));
        LectureSchedule second = LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(16, 0), LocalTime.of(18, 0));

        assertThat(first.overlaps(second)).isTrue();
    }

    @Test
    void overlapsReturnsFalseWhenAdjacentButNotOverlapping() {
        LectureSchedule first = LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(15, 0), LocalTime.of(17, 0));
        LectureSchedule second = LectureSchedule.create(DayOfWeek.MONDAY, LocalTime.of(17, 0), LocalTime.of(19, 0));

        assertThat(first.overlaps(second)).isFalse();
    }
}

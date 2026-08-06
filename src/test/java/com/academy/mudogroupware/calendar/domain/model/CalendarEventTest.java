package com.academy.mudogroupware.calendar.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.calendar.domain.exception.CalendarTitleRequiredException;
import com.academy.mudogroupware.calendar.domain.exception.InvalidCalendarPeriodException;

class CalendarEventTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 8, 3, 10, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 8, 3, 11, 30);

    @Test
    void createBuildsEventWithGivenFields() {
        CalendarEvent event = CalendarEvent.create(
                1L, "2학기 수업 준비 회의", "교재 배분 논의", START, END, false, "green", 7L);

        assertThat(event.getAcademyId()).isEqualTo(1L);
        assertThat(event.getTitle()).isEqualTo("2학기 수업 준비 회의");
        assertThat(event.getContent()).isEqualTo("교재 배분 논의");
        assertThat(event.getEventStartAt()).isEqualTo(START);
        assertThat(event.getEventEndAt()).isEqualTo(END);
        assertThat(event.isAllDay()).isFalse();
        assertThat(event.getColor()).isEqualTo("green");
        assertThat(event.getCreatedBy()).isEqualTo(7L);
    }

    @Test
    void createThrowsWhenTitleIsBlank() {
        assertThatThrownBy(() -> CalendarEvent.create(1L, "  ", null, START, END, false, null, 7L))
                .isInstanceOf(CalendarTitleRequiredException.class);
    }

    @Test
    void createThrowsWhenEventEndAtIsBeforeEventStartAt() {
        LocalDateTime invalidEnd = START.minusMinutes(1);

        assertThatThrownBy(() -> CalendarEvent.create(1L, "제목", null, START, invalidEnd, false, null, 7L))
                .isInstanceOf(InvalidCalendarPeriodException.class);
    }

    @Test
    void updateReplacesMutableFields() {
        CalendarEvent event = CalendarEvent.create(1L, "제목", "내용", START, END, false, "green", 7L);
        LocalDateTime newStart = LocalDateTime.of(2026, 8, 4, 12, 30);
        LocalDateTime newEnd = LocalDateTime.of(2026, 8, 4, 15, 30);

        event.update("새 제목", "새 내용", newStart, newEnd, true, "orange");

        assertThat(event.getTitle()).isEqualTo("새 제목");
        assertThat(event.getContent()).isEqualTo("새 내용");
        assertThat(event.getEventStartAt()).isEqualTo(newStart);
        assertThat(event.getEventEndAt()).isEqualTo(newEnd);
        assertThat(event.isAllDay()).isTrue();
        assertThat(event.getColor()).isEqualTo("orange");
    }

    @Test
    void updateThrowsWhenTitleIsBlank() {
        CalendarEvent event = CalendarEvent.create(1L, "제목", "내용", START, END, false, "green", 7L);

        assertThatThrownBy(() -> event.update(" ", "내용", START, END, false, "green"))
                .isInstanceOf(CalendarTitleRequiredException.class);
    }
}

package com.academy.mudogroupware.calendar.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.calendar.domain.exception.CalendarEventNotFoundException;
import com.academy.mudogroupware.calendar.domain.model.CalendarEvent;
import com.academy.mudogroupware.calendar.domain.repository.CalendarEventRepository;

@ExtendWith(MockitoExtension.class)
class GetCalendarEventServiceTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 8, 3, 10, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 8, 3, 11, 30);

    @Mock private CalendarEventRepository calendarEventRepository;

    private GetCalendarEventService getCalendarEventService;

    @BeforeEach
    void setUp() {
        getCalendarEventService = new GetCalendarEventService(calendarEventRepository);
    }

    @Test
    void getEventReturnsEventWhenBelongsToSameAcademy() {
        CalendarEvent event = CalendarEvent.restore(
                101L, 1L, "2학기 수업 준비 회의", "교재 배분 논의", START, END, false, "green", 7L, START, START);
        when(calendarEventRepository.findById(101L)).thenReturn(Optional.of(event));

        CalendarEvent found = getCalendarEventService.getEvent(1L, 101L);

        assertThat(found).isEqualTo(event);
    }

    @Test
    void getEventThrowsWhenEventDoesNotExist() {
        when(calendarEventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getCalendarEventService.getEvent(1L, 999L))
                .isInstanceOf(CalendarEventNotFoundException.class);
    }

    @Test
    void getEventThrowsWhenEventBelongsToDifferentAcademy() {
        CalendarEvent event = CalendarEvent.restore(
                101L, 2L, "다른 학원 일정", null, START, END, false, null, 7L, START, START);
        when(calendarEventRepository.findById(101L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> getCalendarEventService.getEvent(1L, 101L))
                .isInstanceOf(CalendarEventNotFoundException.class);
    }
}

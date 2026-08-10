package com.academy.mudogroupware.calendar.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.calendar.application.command.DeleteCalendarEventCommand;
import com.academy.mudogroupware.calendar.domain.exception.CalendarEventNotFoundException;
import com.academy.mudogroupware.calendar.domain.model.CalendarEvent;
import com.academy.mudogroupware.calendar.domain.repository.CalendarEventRepository;

@ExtendWith(MockitoExtension.class)
class DeleteCalendarEventServiceTest {
    private static final LocalDateTime START = LocalDateTime.of(2026, 8, 3, 10, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 8, 3, 11, 30);

    @Mock private CalendarEventRepository calendarEventRepository;
    private DeleteCalendarEventService service;

    @BeforeEach
    void setUp() {
        service = new DeleteCalendarEventService(calendarEventRepository);
    }

    @Test
    void deleteEventDeletesExistingEvent() {
        CalendarEvent existing = CalendarEvent.restore(
                101L, "제목", "내용", START, END, false, "green", 7L, START, START);
        when(calendarEventRepository.findById(101L)).thenReturn(Optional.of(existing));

        service.deleteEvent(new DeleteCalendarEventCommand(101L));

        verify(calendarEventRepository).deleteById(101L);
    }

    @Test
    void deleteEventThrowsWhenEventDoesNotExist() {
        when(calendarEventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteEvent(new DeleteCalendarEventCommand(999L)))
                .isInstanceOf(CalendarEventNotFoundException.class);
        verify(calendarEventRepository, never()).deleteById(org.mockito.ArgumentMatchers.any());
    }
}

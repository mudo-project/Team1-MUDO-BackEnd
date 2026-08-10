package com.academy.mudogroupware.calendar.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.calendar.application.command.UpdateCalendarEventCommand;
import com.academy.mudogroupware.calendar.domain.exception.CalendarEventNotFoundException;
import com.academy.mudogroupware.calendar.domain.exception.CalendarTitleRequiredException;
import com.academy.mudogroupware.calendar.domain.model.CalendarEvent;
import com.academy.mudogroupware.calendar.domain.repository.CalendarEventRepository;

@ExtendWith(MockitoExtension.class)
class UpdateCalendarEventServiceTest {
    private static final LocalDateTime START = LocalDateTime.of(2026, 8, 3, 10, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 8, 3, 11, 30);

    @Mock private CalendarEventRepository calendarEventRepository;
    private UpdateCalendarEventService service;

    @BeforeEach
    void setUp() {
        service = new UpdateCalendarEventService(calendarEventRepository);
    }

    @Test
    void updateEventAppliesChangesAndSaves() {
        CalendarEvent existing = CalendarEvent.restore(
                101L, "기존 제목", "기존 내용", START, END, false, "green", 7L, START, START);
        when(calendarEventRepository.findById(101L)).thenReturn(Optional.of(existing));
        LocalDateTime newStart = START.plusDays(1);
        LocalDateTime newEnd = END.plusDays(1);
        UpdateCalendarEventCommand command = new UpdateCalendarEventCommand(
                101L, "새 제목", "새 내용", newStart, newEnd, true, "orange");

        service.updateEvent(command);

        ArgumentCaptor<CalendarEvent> captor = ArgumentCaptor.forClass(CalendarEvent.class);
        verify(calendarEventRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("새 제목");
        assertThat(captor.getValue().getColor()).isEqualTo("orange");
    }

    @Test
    void updateEventThrowsWhenEventDoesNotExist() {
        when(calendarEventRepository.findById(999L)).thenReturn(Optional.empty());
        UpdateCalendarEventCommand command = new UpdateCalendarEventCommand(
                999L, "제목", null, START, END, false, null);

        assertThatThrownBy(() -> service.updateEvent(command))
                .isInstanceOf(CalendarEventNotFoundException.class);
        verify(calendarEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateEventThrowsWhenTitleIsBlankWithoutSaving() {
        CalendarEvent existing = CalendarEvent.restore(
                101L, "기존 제목", null, START, END, false, null, 7L, START, START);
        when(calendarEventRepository.findById(101L)).thenReturn(Optional.of(existing));
        UpdateCalendarEventCommand command = new UpdateCalendarEventCommand(
                101L, "   ", null, START, END, false, null);

        assertThatThrownBy(() -> service.updateEvent(command))
                .isInstanceOf(CalendarTitleRequiredException.class);
        verify(calendarEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}

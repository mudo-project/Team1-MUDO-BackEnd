package com.academy.mudogroupware.calendar.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.calendar.application.command.CreateCalendarEventCommand;
import com.academy.mudogroupware.calendar.domain.exception.CalendarTitleRequiredException;
import com.academy.mudogroupware.calendar.domain.exception.InvalidCalendarPeriodException;
import com.academy.mudogroupware.calendar.domain.model.CalendarEvent;
import com.academy.mudogroupware.calendar.domain.repository.CalendarEventRepository;

@ExtendWith(MockitoExtension.class)
class CreateCalendarEventServiceTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 8, 3, 10, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 8, 3, 11, 30);

    @Mock private CalendarEventRepository calendarEventRepository;

    private CreateCalendarEventService createCalendarEventService;

    @BeforeEach
    void setUp() {
        createCalendarEventService = new CreateCalendarEventService(calendarEventRepository);
    }

    @Test
    void createEventPersistsDomainAndReturnsSavedId() {
        CreateCalendarEventCommand command = new CreateCalendarEventCommand(
                1L, "2학기 수업 준비 회의", "교재 배분 논의", START, END, false, "green", 7L);
        when(calendarEventRepository.save(any(CalendarEvent.class)))
                .thenAnswer(invocation -> CalendarEvent.restore(
                        101L, 1L, "2학기 수업 준비 회의", "교재 배분 논의", START, END, false, "green", 7L, null, null));

        Long eventId = createCalendarEventService.createEvent(command);

        assertThat(eventId).isEqualTo(101L);
        ArgumentCaptor<CalendarEvent> captor = ArgumentCaptor.forClass(CalendarEvent.class);
        verify(calendarEventRepository).save(captor.capture());
        CalendarEvent saved = captor.getValue();
        assertThat(saved.getAcademyId()).isEqualTo(1L);
        assertThat(saved.getTitle()).isEqualTo("2학기 수업 준비 회의");
        assertThat(saved.getContent()).isEqualTo("교재 배분 논의");
        assertThat(saved.getEventStartAt()).isEqualTo(START);
        assertThat(saved.getEventEndAt()).isEqualTo(END);
        assertThat(saved.isAllDay()).isFalse();
        assertThat(saved.getColor()).isEqualTo("green");
        assertThat(saved.getCreatedBy()).isEqualTo(7L);
    }

    @Test
    void createEvent_blankTitle_throwsExceptionWithoutSaving() {
        CreateCalendarEventCommand command = new CreateCalendarEventCommand(
                1L, "   ", null, START, END, false, null, 7L);

        assertThatThrownBy(() -> createCalendarEventService.createEvent(command))
                .isInstanceOf(CalendarTitleRequiredException.class);
        verify(calendarEventRepository, never()).save(any());
    }

    @Test
    void createEvent_endBeforeStart_throwsExceptionWithoutSaving() {
        CreateCalendarEventCommand command = new CreateCalendarEventCommand(
                1L, "회의", null, START, START.minusMinutes(1), false, null, 7L);

        assertThatThrownBy(() -> createCalendarEventService.createEvent(command))
                .isInstanceOf(InvalidCalendarPeriodException.class);
        verify(calendarEventRepository, never()).save(any());
    }
}

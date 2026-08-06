package com.academy.mudogroupware.calendar.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.calendar.domain.exception.InvalidCalendarPeriodException;
import com.academy.mudogroupware.calendar.domain.model.CalendarEvent;
import com.academy.mudogroupware.calendar.domain.repository.CalendarEventRepository;

@ExtendWith(MockitoExtension.class)
class GetCalendarEventsServiceTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 8, 1, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 8, 31, 23, 59, 59);

    @Mock private CalendarEventRepository calendarEventRepository;

    private GetCalendarEventsService getCalendarEventsService;

    @BeforeEach
    void setUp() {
        getCalendarEventsService = new GetCalendarEventsService(calendarEventRepository);
    }

    @Test
    void getEventsReturnsEventsFromRepositoryForGivenPeriod() {
        CalendarEvent event = CalendarEvent.restore(
                101L, 1L, "2학기 수업 준비 회의", "교재 배분 논의",
                LocalDateTime.of(2026, 8, 3, 10, 0), LocalDateTime.of(2026, 8, 3, 11, 30),
                false, "green", 7L, LocalDateTime.now(), LocalDateTime.now());
        when(calendarEventRepository.findAllByAcademyIdAndPeriod(1L, FROM, TO))
                .thenReturn(List.of(event));

        List<CalendarEvent> events = getCalendarEventsService.getEvents(1L, FROM, TO);

        assertThat(events).containsExactly(event);
        verify(calendarEventRepository).findAllByAcademyIdAndPeriod(1L, FROM, TO);
    }

    @Test
    void getEventsThrowsWhenToIsBeforeFromWithoutQueryingRepository() {
        LocalDateTime invalidTo = FROM.minusDays(1);

        assertThatThrownBy(() -> getCalendarEventsService.getEvents(1L, FROM, invalidTo))
                .isInstanceOf(InvalidCalendarPeriodException.class);
        verifyNoInteractions(calendarEventRepository);
    }
}

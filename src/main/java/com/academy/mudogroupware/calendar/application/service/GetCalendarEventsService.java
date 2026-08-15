package com.academy.mudogroupware.calendar.application.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.calendar.application.usecase.GetCalendarEventsUseCase;
import com.academy.mudogroupware.calendar.domain.exception.InvalidCalendarPeriodException;
import com.academy.mudogroupware.calendar.domain.model.CalendarEvent;
import com.academy.mudogroupware.calendar.domain.repository.CalendarEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetCalendarEventsService implements GetCalendarEventsUseCase {

    private final CalendarEventRepository calendarEventRepository;

    @Override
    public List<CalendarEvent> getEvents(LocalDateTime from, LocalDateTime to) {
        log.info("event=calendar_event_list_시작 from={}, to={}", from, to);

        if (to.isBefore(from)) {
            throw new InvalidCalendarPeriodException();
        }
        List<CalendarEvent> events = calendarEventRepository.findAllByPeriod(from, to);

        log.info("event=calendar_event_list_완료 from={}, to={}, count={}", from, to, events.size());
        return events;
    }
}

package com.academy.mudogroupware.calendar.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.calendar.application.usecase.GetCalendarEventUseCase;
import com.academy.mudogroupware.calendar.domain.exception.CalendarEventNotFoundException;
import com.academy.mudogroupware.calendar.domain.model.CalendarEvent;
import com.academy.mudogroupware.calendar.domain.repository.CalendarEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetCalendarEventService implements GetCalendarEventUseCase {

    private final CalendarEventRepository calendarEventRepository;

    @Override
    public CalendarEvent getEvent(Long eventId) {
        log.info("event=calendar_event_get_시작 eventId={}", eventId);

        CalendarEvent event = calendarEventRepository.findById(eventId)
                .orElseThrow(() -> new CalendarEventNotFoundException(eventId));

        log.info("event=calendar_event_get_완료 eventId={}", eventId);
        return event;
    }
}

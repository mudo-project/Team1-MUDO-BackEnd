package com.academy.mudogroupware.calendar.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.calendar.application.command.CreateCalendarEventCommand;
import com.academy.mudogroupware.calendar.application.usecase.CreateCalendarEventUseCase;
import com.academy.mudogroupware.calendar.domain.model.CalendarEvent;
import com.academy.mudogroupware.calendar.domain.repository.CalendarEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreateCalendarEventService implements CreateCalendarEventUseCase {

    private final CalendarEventRepository calendarEventRepository;

    @Override
    public Long createEvent(CreateCalendarEventCommand command) {
        log.info("event=calendar_event_create_시작 createdBy={}, title={}", command.createdBy(), command.title());

        CalendarEvent calendarEvent = CalendarEvent.create(
                command.title(), command.content(),
                command.eventStartAt(), command.eventEndAt(), command.allDay(),
                command.color(), command.createdBy());

        Long eventId = calendarEventRepository.save(calendarEvent).getId();
        log.info("event=calendar_event_create_완료 createdBy={}, eventId={}", command.createdBy(), eventId);
        return eventId;
    }
}

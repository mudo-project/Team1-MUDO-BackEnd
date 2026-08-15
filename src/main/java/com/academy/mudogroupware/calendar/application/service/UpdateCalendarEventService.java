package com.academy.mudogroupware.calendar.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.calendar.application.command.UpdateCalendarEventCommand;
import com.academy.mudogroupware.calendar.application.usecase.UpdateCalendarEventUseCase;
import com.academy.mudogroupware.calendar.domain.exception.CalendarEventNotFoundException;
import com.academy.mudogroupware.calendar.domain.model.CalendarEvent;
import com.academy.mudogroupware.calendar.domain.repository.CalendarEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateCalendarEventService implements UpdateCalendarEventUseCase {

    private final CalendarEventRepository calendarEventRepository;

    @Override
    public void updateEvent(UpdateCalendarEventCommand command) {
        log.info("event=calendar_event_update_시작 eventId={}", command.eventId());

        CalendarEvent event = calendarEventRepository.findById(command.eventId())
                .orElseThrow(() -> new CalendarEventNotFoundException(command.eventId()));

        event.update(command.title(), command.content(), command.eventStartAt(),
                command.eventEndAt(), command.allDay(), command.color());

        calendarEventRepository.save(event);
        log.info("event=calendar_event_update_완료 eventId={}", command.eventId());
    }
}

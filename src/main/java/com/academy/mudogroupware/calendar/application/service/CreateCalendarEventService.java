package com.academy.mudogroupware.calendar.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.calendar.application.command.CreateCalendarEventCommand;
import com.academy.mudogroupware.calendar.application.usecase.CreateCalendarEventUseCase;
import com.academy.mudogroupware.calendar.domain.model.CalendarEvent;
import com.academy.mudogroupware.calendar.domain.repository.CalendarEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateCalendarEventService implements CreateCalendarEventUseCase {

    private final CalendarEventRepository calendarEventRepository;

    @Override
    public Long createEvent(CreateCalendarEventCommand command) {
        CalendarEvent calendarEvent = CalendarEvent.create(
                command.academyId(), command.title(), command.content(),
                command.eventStartAt(), command.eventEndAt(), command.allDay(),
                command.color(), command.createdBy());

        return calendarEventRepository.save(calendarEvent).getId();
    }
}

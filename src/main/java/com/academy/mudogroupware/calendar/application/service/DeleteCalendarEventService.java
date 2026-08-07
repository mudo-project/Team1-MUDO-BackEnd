package com.academy.mudogroupware.calendar.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.calendar.application.command.DeleteCalendarEventCommand;
import com.academy.mudogroupware.calendar.application.usecase.DeleteCalendarEventUseCase;
import com.academy.mudogroupware.calendar.domain.exception.CalendarEventNotFoundException;
import com.academy.mudogroupware.calendar.domain.model.CalendarEvent;
import com.academy.mudogroupware.calendar.domain.repository.CalendarEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteCalendarEventService implements DeleteCalendarEventUseCase {

    private final CalendarEventRepository calendarEventRepository;

    @Override
    public void deleteEvent(DeleteCalendarEventCommand command) {
        CalendarEvent event = calendarEventRepository.findById(command.eventId())
                .orElseThrow(() -> new CalendarEventNotFoundException(command.eventId()));
        if (!event.getAcademyId().equals(command.academyId())) {
            throw new CalendarEventNotFoundException(command.eventId());
        }

        calendarEventRepository.deleteById(command.eventId());
    }
}

package com.academy.mudogroupware.calendar.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.calendar.application.usecase.GetCalendarEventUseCase;
import com.academy.mudogroupware.calendar.domain.exception.CalendarEventNotFoundException;
import com.academy.mudogroupware.calendar.domain.model.CalendarEvent;
import com.academy.mudogroupware.calendar.domain.repository.CalendarEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetCalendarEventService implements GetCalendarEventUseCase {

    private final CalendarEventRepository calendarEventRepository;

    @Override
    public CalendarEvent getEvent(Long eventId) {
        return calendarEventRepository.findById(eventId)
                .orElseThrow(() -> new CalendarEventNotFoundException(eventId));
    }
}

package com.academy.mudogroupware.calendar.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.calendar.domain.model.CalendarEvent;

public interface CalendarEventRepository {

    CalendarEvent save(CalendarEvent calendarEvent);

    Optional<CalendarEvent> findById(Long id);

    List<CalendarEvent> findAllByPeriod(LocalDateTime from, LocalDateTime to);

    void deleteById(Long id);
}

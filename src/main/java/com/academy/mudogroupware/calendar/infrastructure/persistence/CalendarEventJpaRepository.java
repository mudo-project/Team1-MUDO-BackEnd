package com.academy.mudogroupware.calendar.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarEventJpaRepository extends JpaRepository<CalendarEventEntity, Long> {

    List<CalendarEventEntity> findAllByEventStartAtBetween(LocalDateTime from, LocalDateTime to);
}

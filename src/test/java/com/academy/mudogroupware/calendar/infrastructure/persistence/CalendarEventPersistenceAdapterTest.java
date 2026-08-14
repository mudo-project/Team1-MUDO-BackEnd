package com.academy.mudogroupware.calendar.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

import com.academy.mudogroupware.calendar.domain.exception.CalendarEventNotFoundException;
import com.academy.mudogroupware.calendar.domain.exception.CalendarEventUpdateConflictException;
import com.academy.mudogroupware.calendar.domain.model.CalendarEvent;

import jakarta.persistence.EntityNotFoundException;

class CalendarEventPersistenceAdapterTest {

    @Test
    void convertsEntityNotFoundOnUpdateToCalendarEventNotFoundException() {
        CalendarEventJpaRepository jpaRepository = mock(CalendarEventJpaRepository.class);
        CalendarEventPersistenceAdapter adapter = new CalendarEventPersistenceAdapter(jpaRepository);
        when(jpaRepository.getReferenceById(1L))
                .thenThrow(new EntityNotFoundException("deleted concurrently"));

        CalendarEvent domain = CalendarEvent.restore(
                1L, "수정된 제목", "내용", LocalDateTime.of(2026, 8, 20, 9, 0),
                LocalDateTime.of(2026, 8, 20, 10, 0), false, "FFCC00", 1L, null, null);

        assertThatThrownBy(() -> adapter.save(domain))
                .isInstanceOf(CalendarEventNotFoundException.class);
    }

    @Test
    void convertsOptimisticLockConflictOnUpdateToCalendarEventUpdateConflictException() {
        CalendarEventJpaRepository jpaRepository = mock(CalendarEventJpaRepository.class);
        CalendarEventPersistenceAdapter adapter = new CalendarEventPersistenceAdapter(jpaRepository);
        CalendarEventEntity entity = CalendarEventEntity.builder()
                .id(1L)
                .title("제목")
                .content("내용")
                .eventStartAt(LocalDateTime.of(2026, 8, 20, 9, 0))
                .eventEndAt(LocalDateTime.of(2026, 8, 20, 10, 0))
                .allDay(false)
                .color("FFCC00")
                .createdBy(1L)
                .build();
        when(jpaRepository.getReferenceById(1L)).thenReturn(entity);
        OptimisticLockingFailureException conflict = new OptimisticLockingFailureException("concurrent update");
        doThrow(conflict).when(jpaRepository).flush();

        CalendarEvent domain = CalendarEvent.restore(
                1L, "수정된 제목", "내용", LocalDateTime.of(2026, 8, 20, 9, 0),
                LocalDateTime.of(2026, 8, 20, 10, 0), false, "FFCC00", 1L, null, null);

        assertThatThrownBy(() -> adapter.save(domain))
                .isInstanceOf(CalendarEventUpdateConflictException.class)
                .hasCause(conflict);
    }
}

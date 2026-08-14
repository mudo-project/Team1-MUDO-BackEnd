package com.academy.mudogroupware.timetable.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

import com.academy.mudogroupware.timetable.domain.exception.TimetableSetNotFoundException;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSetUpdateConflictException;
import com.academy.mudogroupware.timetable.domain.model.TimetableClassroom;
import com.academy.mudogroupware.timetable.domain.model.TimetableSet;

import jakarta.persistence.EntityNotFoundException;

class TimetableSetPersistenceAdapterTest {

    @Test
    void convertsEntityNotFoundOnUpdateToTimetableSetNotFoundException() {
        TimetableSetJpaRepository jpaRepository = mock(TimetableSetJpaRepository.class);
        TimetableSetPersistenceAdapter adapter = new TimetableSetPersistenceAdapter(jpaRepository);
        when(jpaRepository.getReferenceById(1L))
                .thenThrow(new EntityNotFoundException("deleted concurrently"));

        TimetableSet domain = TimetableSet.restore(
                1L, "수정된 이름", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY), 30,
                List.of(new TimetableClassroom("6층", "601")), null, null);

        assertThatThrownBy(() -> adapter.save(domain))
                .isInstanceOf(TimetableSetNotFoundException.class);
    }

    @Test
    void convertsOptimisticLockConflictOnUpdateToTimetableSetUpdateConflictException() {
        TimetableSetJpaRepository jpaRepository = mock(TimetableSetJpaRepository.class);
        TimetableSetPersistenceAdapter adapter = new TimetableSetPersistenceAdapter(jpaRepository);
        TimetableSetEntity entity = TimetableSetEntity.builder()
                .id(1L)
                .name("이름")
                .startDate(LocalDate.of(2026, 7, 20))
                .endDate(LocalDate.of(2026, 8, 16))
                .operatingStartTime(LocalTime.of(8, 30))
                .operatingEndTime(LocalTime.of(22, 0))
                .operatingDays("MONDAY")
                .slotUnitMinutes(30)
                .classrooms(List.of(new TimetableClassroomEmbeddable("6층", "601")))
                .build();
        when(jpaRepository.getReferenceById(1L)).thenReturn(entity);
        OptimisticLockingFailureException conflict = new OptimisticLockingFailureException("concurrent update");
        doThrow(conflict).when(jpaRepository).flush();

        TimetableSet domain = TimetableSet.restore(
                1L, "수정된 이름", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY), 30,
                List.of(new TimetableClassroom("6층", "601")), null, null);

        assertThatThrownBy(() -> adapter.save(domain))
                .isInstanceOf(TimetableSetUpdateConflictException.class)
                .hasCause(conflict);
    }
}

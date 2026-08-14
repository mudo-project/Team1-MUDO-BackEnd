package com.academy.mudogroupware.timetable.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

import com.academy.mudogroupware.timetable.domain.exception.TimetableSlotNotFoundException;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSlotUpdateConflictException;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.Grade;
import com.academy.mudogroupware.timetable.domain.model.TimetableSlot;

import jakarta.persistence.EntityNotFoundException;

class TimetableSlotPersistenceAdapterTest {

    @Test
    void convertsEntityNotFoundOnUpdateToTimetableSlotNotFoundException() {
        TimetableSlotJpaRepository jpaRepository = mock(TimetableSlotJpaRepository.class);
        TimetableSlotPersistenceAdapter adapter = new TimetableSlotPersistenceAdapter(jpaRepository);
        when(jpaRepository.getReferenceById(1L))
                .thenThrow(new EntityNotFoundException("deleted concurrently"));

        TimetableSlot domain = TimetableSlot.restore(
                1L, 1L, ClassType.SPECIAL, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "수정됨", "FFCC00", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16), null,
                null);

        assertThatThrownBy(() -> adapter.save(domain))
                .isInstanceOf(TimetableSlotNotFoundException.class);
    }

    @Test
    void convertsOptimisticLockConflictOnUpdateToTimetableSlotUpdateConflictException() {
        TimetableSlotJpaRepository jpaRepository = mock(TimetableSlotJpaRepository.class);
        TimetableSlotPersistenceAdapter adapter = new TimetableSlotPersistenceAdapter(jpaRepository);
        TimetableSlotEntity entity = TimetableSlotEntity.builder()
                .id(1L)
                .timetableSetId(1L)
                .classType(ClassType.CLASS)
                .dayOfWeek(DayOfWeek.MONDAY)
                .classroomCode("601")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .grade(Grade.HIGH_3)
                .teacherName("정T")
                .subjectName("미적분")
                .effectiveFrom(LocalDate.of(2026, 7, 20))
                .effectiveUntil(LocalDate.of(2026, 8, 16))
                .build();
        when(jpaRepository.getReferenceById(1L)).thenReturn(entity);
        OptimisticLockingFailureException conflict = new OptimisticLockingFailureException("concurrent update");
        doThrow(conflict).when(jpaRepository).flush();

        TimetableSlot domain = TimetableSlot.restore(
                1L, 1L, ClassType.SPECIAL, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "수정됨", "FFCC00", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16), null,
                null);

        assertThatThrownBy(() -> adapter.save(domain))
                .isInstanceOf(TimetableSlotUpdateConflictException.class)
                .hasCause(conflict);
    }
}

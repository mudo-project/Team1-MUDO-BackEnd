package com.academy.mudogroupware.timetable.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.timetable.application.command.UpdateTimetableSetCommand;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSetNotFoundException;
import com.academy.mudogroupware.timetable.domain.model.TimetableClassroom;
import com.academy.mudogroupware.timetable.domain.model.TimetableSet;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSetRepository;

@ExtendWith(MockitoExtension.class)
class UpdateTimetableSetServiceTest {

    @Mock private TimetableSetRepository timetableSetRepository;

    private UpdateTimetableSetService service;

    @BeforeEach
    void setUp() {
        service = new UpdateTimetableSetService(timetableSetRepository);
    }

    @Test
    void updateTimetableSetAppliesNewValues() {
        TimetableSet set = TimetableSet.restore(
                1L, "이름", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY), 30,
                List.of(new TimetableClassroom("6층", "601")), null, null);
        when(timetableSetRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(set));
        UpdateTimetableSetCommand command = new UpdateTimetableSetCommand(
                1L, "새 이름", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31),
                LocalTime.of(9, 0), LocalTime.of(21, 0), Set.of(DayOfWeek.TUESDAY), 10,
                List.of(new TimetableClassroom("3층", "301")));

        service.updateTimetableSet(command);

        verify(timetableSetRepository).save(set);
    }

    @Test
    void updateTimetableSetLooksUpWithPessimisticLock() {
        TimetableSet set = TimetableSet.restore(
                1L, "이름", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY), 30,
                List.of(new TimetableClassroom("6층", "601")), null, null);
        when(timetableSetRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(set));
        UpdateTimetableSetCommand command = new UpdateTimetableSetCommand(
                1L, "새 이름", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31),
                LocalTime.of(9, 0), LocalTime.of(21, 0), Set.of(DayOfWeek.TUESDAY), 10,
                List.of(new TimetableClassroom("3층", "301")));

        service.updateTimetableSet(command);

        verify(timetableSetRepository).findByIdForUpdate(1L);
        verify(timetableSetRepository, never()).findById(1L);
    }

    @Test
    void updateTimetableSetThrowsWhenNotFound() {
        when(timetableSetRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());
        UpdateTimetableSetCommand command = new UpdateTimetableSetCommand(
                999L, "이름", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY), 30,
                List.of(new TimetableClassroom("6층", "601")));

        assertThatThrownBy(() -> service.updateTimetableSet(command))
                .isInstanceOf(TimetableSetNotFoundException.class);
    }

    @Test
    void updateTimetableSetThrowsWhenSetIsMissing() {
        when(timetableSetRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());
        UpdateTimetableSetCommand command = new UpdateTimetableSetCommand(
                1L, "이름", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY), 30,
                List.of(new TimetableClassroom("6층", "601")));

        assertThatThrownBy(() -> service.updateTimetableSet(command))
                .isInstanceOf(TimetableSetNotFoundException.class);
    }
}

package com.academy.mudogroupware.timetable.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
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

import com.academy.mudogroupware.timetable.application.command.DeleteTimetableSetCommand;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSetNotFoundException;
import com.academy.mudogroupware.timetable.domain.model.TimetableClassroom;
import com.academy.mudogroupware.timetable.domain.model.TimetableSet;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSetRepository;

@ExtendWith(MockitoExtension.class)
class DeleteTimetableSetServiceTest {

    @Mock private TimetableSetRepository timetableSetRepository;

    private DeleteTimetableSetService service;

    @BeforeEach
    void setUp() {
        service = new DeleteTimetableSetService(timetableSetRepository);
    }

    @Test
    void deleteTimetableSetDeletesExistingSet() {
        TimetableSet set = TimetableSet.restore(
                1L, "이름", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY), 30,
                List.of(new TimetableClassroom("6층", "601")), null, null);
        when(timetableSetRepository.findById(1L)).thenReturn(Optional.of(set));

        service.deleteTimetableSet(new DeleteTimetableSetCommand(1L));

        verify(timetableSetRepository).deleteById(1L);
    }

    @Test
    void deleteTimetableSetThrowsWhenNotFound() {
        when(timetableSetRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteTimetableSet(new DeleteTimetableSetCommand(999L)))
                .isInstanceOf(TimetableSetNotFoundException.class);
        verify(timetableSetRepository, never()).deleteById(anyLong());
    }
}

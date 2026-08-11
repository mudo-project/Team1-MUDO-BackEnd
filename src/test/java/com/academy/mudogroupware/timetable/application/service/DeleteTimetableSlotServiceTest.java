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

import com.academy.mudogroupware.timetable.application.command.DeleteTimetableSlotCommand;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSetNotFoundException;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSlotNotFoundException;
import com.academy.mudogroupware.timetable.domain.exception.UnsupportedSlotScopeException;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.Grade;
import com.academy.mudogroupware.timetable.domain.model.TimetableClassroom;
import com.academy.mudogroupware.timetable.domain.model.TimetableSet;
import com.academy.mudogroupware.timetable.domain.model.TimetableSlot;
import com.academy.mudogroupware.timetable.domain.model.UpdateScope;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSetRepository;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSlotRepository;

@ExtendWith(MockitoExtension.class)
class DeleteTimetableSlotServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 7, 20);
    private static final LocalDate UNTIL = LocalDate.of(2026, 8, 16);

    @Mock private TimetableSetRepository timetableSetRepository;
    @Mock private TimetableSlotRepository timetableSlotRepository;

    private DeleteTimetableSlotService service;

    @BeforeEach
    void setUp() {
        service = new DeleteTimetableSlotService(timetableSetRepository, timetableSlotRepository);
    }

    private TimetableSet timetableSet() {
        return TimetableSet.restore(
                1L, "이름", FROM, UNTIL, LocalTime.of(8, 30), LocalTime.of(22, 0),
                Set.of(DayOfWeek.MONDAY), 30, List.of(new TimetableClassroom("6층", "601")), null, null);
    }

    @Test
    void deleteSlotDeletesWhenScopeIsAllAndBelongsToSet() {
        when(timetableSetRepository.findById(1L)).thenReturn(Optional.of(timetableSet()));
        TimetableSlot slot = TimetableSlot.restore(
                100L, 1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", FROM, UNTIL, null, null);
        when(timetableSlotRepository.findById(100L)).thenReturn(Optional.of(slot));

        service.deleteSlot(new DeleteTimetableSlotCommand(1L, 100L, UpdateScope.ALL));

        verify(timetableSlotRepository).deleteById(100L);
    }

    @Test
    void deleteSlotThrowsWhenScopeIsNotAll() {
        assertThatThrownBy(() -> service.deleteSlot(
                new DeleteTimetableSlotCommand(1L, 100L, UpdateScope.FROM_NOW)))
                .isInstanceOf(UnsupportedSlotScopeException.class);
        verify(timetableSlotRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteSlotThrowsWhenTimetableSetNotFound() {
        when(timetableSetRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteSlot(new DeleteTimetableSlotCommand(999L, 100L, UpdateScope.ALL)))
                .isInstanceOf(TimetableSetNotFoundException.class);
    }

    @Test
    void deleteSlotThrowsWhenSlotIsMissing() {
        when(timetableSetRepository.findById(1L)).thenReturn(Optional.of(timetableSet()));

        assertThatThrownBy(() -> service.deleteSlot(new DeleteTimetableSlotCommand(1L, 100L, UpdateScope.ALL)))
                .isInstanceOf(TimetableSlotNotFoundException.class);
    }

    @Test
    void deleteSlotThrowsWhenNotFound() {
        when(timetableSetRepository.findById(1L)).thenReturn(Optional.of(timetableSet()));
        when(timetableSlotRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteSlot(new DeleteTimetableSlotCommand(1L, 999L, UpdateScope.ALL)))
                .isInstanceOf(TimetableSlotNotFoundException.class);
    }

    @Test
    void deleteSlotThrowsWhenBelongsToDifferentSet() {
        when(timetableSetRepository.findById(1L)).thenReturn(Optional.of(timetableSet()));
        TimetableSlot slot = TimetableSlot.restore(
                100L, 2L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", FROM, UNTIL, null, null);
        when(timetableSlotRepository.findById(100L)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> service.deleteSlot(new DeleteTimetableSlotCommand(1L, 100L, UpdateScope.ALL)))
                .isInstanceOf(TimetableSlotNotFoundException.class);
    }
}

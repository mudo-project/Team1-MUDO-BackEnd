package com.academy.mudogroupware.timetable.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.timetable.application.command.DeleteTimetableSlotCommand;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSlotNotFoundException;
import com.academy.mudogroupware.timetable.domain.exception.UnsupportedSlotScopeException;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.TimetableSlot;
import com.academy.mudogroupware.timetable.domain.model.UpdateScope;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSlotRepository;

@ExtendWith(MockitoExtension.class)
class DeleteTimetableSlotServiceTest {

    @Mock private TimetableSlotRepository timetableSlotRepository;

    private DeleteTimetableSlotService service;

    @BeforeEach
    void setUp() {
        service = new DeleteTimetableSlotService(timetableSlotRepository);
    }

    @Test
    void deleteSlotDeletesWhenScopeIsAllAndBelongsToSet() {
        TimetableSlot slot = TimetableSlot.restore(
                100L, 1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                "고3", "정T", "미적분", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16), null, null);
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
    void deleteSlotThrowsWhenNotFound() {
        when(timetableSlotRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteSlot(new DeleteTimetableSlotCommand(1L, 999L, UpdateScope.ALL)))
                .isInstanceOf(TimetableSlotNotFoundException.class);
    }

    @Test
    void deleteSlotThrowsWhenBelongsToDifferentSet() {
        TimetableSlot slot = TimetableSlot.restore(
                100L, 2L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                "고3", "정T", "미적분", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16), null, null);
        when(timetableSlotRepository.findById(100L)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> service.deleteSlot(new DeleteTimetableSlotCommand(1L, 100L, UpdateScope.ALL)))
                .isInstanceOf(TimetableSlotNotFoundException.class);
    }
}

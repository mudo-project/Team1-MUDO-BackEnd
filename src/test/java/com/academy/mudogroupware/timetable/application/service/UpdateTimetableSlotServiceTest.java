package com.academy.mudogroupware.timetable.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.timetable.application.command.UpdateTimetableSlotCommand;
import com.academy.mudogroupware.timetable.domain.exception.ClassroomTimeConflictException;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSlotNotFoundException;
import com.academy.mudogroupware.timetable.domain.exception.UnsupportedSlotScopeException;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.TimetableSlot;
import com.academy.mudogroupware.timetable.domain.model.UpdateScope;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSlotRepository;

@ExtendWith(MockitoExtension.class)
class UpdateTimetableSlotServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 7, 20);
    private static final LocalDate UNTIL = LocalDate.of(2026, 8, 16);

    @Mock private TimetableSlotRepository timetableSlotRepository;

    private UpdateTimetableSlotService service;

    @BeforeEach
    void setUp() {
        service = new UpdateTimetableSlotService(timetableSlotRepository);
    }

    private TimetableSlot existingSlot() {
        return TimetableSlot.restore(
                100L, 1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                "고3", "정T", "미적분", FROM, UNTIL, null, null);
    }

    @Test
    void updateSlotAppliesNewValuesWhenScopeIsAll() {
        TimetableSlot slot = existingSlot();
        when(timetableSlotRepository.findById(100L)).thenReturn(Optional.of(slot));
        when(timetableSlotRepository.findAllByTimetableSetIdAndClassroomCode(1L, "602")).thenReturn(List.of());
        UpdateTimetableSlotCommand command = new UpdateTimetableSlotCommand(
                1L, 100L, UpdateScope.ALL, ClassType.SPECIAL, DayOfWeek.TUESDAY, "602",
                LocalTime.of(13, 0), LocalTime.of(15, 0), "고2", "오T", "물리");

        service.updateSlot(command);

        verify(timetableSlotRepository).save(slot);
    }

    @Test
    void updateSlotThrowsWhenScopeIsNotAll() {
        UpdateTimetableSlotCommand command = new UpdateTimetableSlotCommand(
                1L, 100L, UpdateScope.THIS_OCCURRENCE, ClassType.CLASS, DayOfWeek.MONDAY, "601",
                LocalTime.of(9, 0), LocalTime.of(11, 0), "고3", "정T", "미적분");

        assertThatThrownBy(() -> service.updateSlot(command))
                .isInstanceOf(UnsupportedSlotScopeException.class);
    }

    @Test
    void updateSlotThrowsWhenNotFound() {
        when(timetableSlotRepository.findById(999L)).thenReturn(Optional.empty());
        UpdateTimetableSlotCommand command = new UpdateTimetableSlotCommand(
                1L, 999L, UpdateScope.ALL, ClassType.CLASS, DayOfWeek.MONDAY, "601",
                LocalTime.of(9, 0), LocalTime.of(11, 0), "고3", "정T", "미적분");

        assertThatThrownBy(() -> service.updateSlot(command))
                .isInstanceOf(TimetableSlotNotFoundException.class);
    }

    @Test
    void updateSlotThrowsWhenNewTimeConflictsWithAnotherSlot() {
        TimetableSlot slot = existingSlot();
        when(timetableSlotRepository.findById(100L)).thenReturn(Optional.of(slot));
        TimetableSlot other = TimetableSlot.restore(
                200L, 1L, ClassType.CLASS, DayOfWeek.TUESDAY, "602", LocalTime.of(13, 0), LocalTime.of(15, 0),
                "고2", "오T", "물리", FROM, UNTIL, null, null);
        when(timetableSlotRepository.findAllByTimetableSetIdAndClassroomCode(1L, "602")).thenReturn(List.of(other));

        UpdateTimetableSlotCommand command = new UpdateTimetableSlotCommand(
                1L, 100L, UpdateScope.ALL, ClassType.SPECIAL, DayOfWeek.TUESDAY, "602",
                LocalTime.of(14, 0), LocalTime.of(16, 0), "고2", "오T", "물리");

        assertThatThrownBy(() -> service.updateSlot(command))
                .isInstanceOf(ClassroomTimeConflictException.class);
    }
}

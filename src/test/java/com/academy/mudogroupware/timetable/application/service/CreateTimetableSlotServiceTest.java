package com.academy.mudogroupware.timetable.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.academy.mudogroupware.timetable.application.command.CreateTimetableSlotCommand;
import com.academy.mudogroupware.timetable.domain.exception.ClassroomTimeConflictException;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSetNotFoundException;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.Grade;
import com.academy.mudogroupware.timetable.domain.model.TimetableClassroom;
import com.academy.mudogroupware.timetable.domain.model.TimetableSet;
import com.academy.mudogroupware.timetable.domain.model.TimetableSlot;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSetRepository;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSlotRepository;

@ExtendWith(MockitoExtension.class)
class CreateTimetableSlotServiceTest {

    private static final LocalDate SET_START = LocalDate.of(2026, 7, 20);
    private static final LocalDate SET_END = LocalDate.of(2026, 8, 16);

    @Mock private TimetableSetRepository timetableSetRepository;
    @Mock private TimetableSlotRepository timetableSlotRepository;

    private CreateTimetableSlotService service;

    @BeforeEach
    void setUp() {
        service = new CreateTimetableSlotService(timetableSetRepository, timetableSlotRepository);
    }

    private TimetableSet timetableSet() {
        return TimetableSet.restore(
                1L, "2026 여름특강", SET_START, SET_END, LocalTime.of(8, 30), LocalTime.of(22, 0),
                Set.of(DayOfWeek.MONDAY), 30, List.of(new TimetableClassroom("6층", "601")), null, null);
    }

    @Test
    void createSlotSavesAndReturnsIdWhenNoConflict() {
        when(timetableSetRepository.findById(1L)).thenReturn(Optional.of(timetableSet()));
        when(timetableSlotRepository.findAllByTimetableSetIdAndClassroomCode(1L, "601")).thenReturn(List.of());
        TimetableSlot saved = TimetableSlot.restore(
                100L, 1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", SET_START, SET_END, null, null);
        when(timetableSlotRepository.save(any(TimetableSlot.class))).thenReturn(saved);

        CreateTimetableSlotCommand command = new CreateTimetableSlotCommand(
                1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분");

        Long id = service.createSlot(command);

        assertThat(id).isEqualTo(100L);
    }

    @Test
    void createSlotThrowsWhenTimetableSetNotFound() {
        when(timetableSetRepository.findById(999L)).thenReturn(Optional.empty());
        CreateTimetableSlotCommand command = new CreateTimetableSlotCommand(
                999L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분");

        assertThatThrownBy(() -> service.createSlot(command))
                .isInstanceOf(TimetableSetNotFoundException.class);
    }

    @Test
    void createSlotThrowsWhenClassroomTimeConflicts() {
        when(timetableSetRepository.findById(1L)).thenReturn(Optional.of(timetableSet()));
        TimetableSlot existing = TimetableSlot.restore(
                50L, 1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", SET_START, SET_END, null, null);
        when(timetableSlotRepository.findAllByTimetableSetIdAndClassroomCode(1L, "601"))
                .thenReturn(List.of(existing));

        CreateTimetableSlotCommand command = new CreateTimetableSlotCommand(
                1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(10, 0), LocalTime.of(12, 0),
                Grade.HIGH_3, "정T", "미적분");

        assertThatThrownBy(() -> service.createSlot(command))
                .isInstanceOf(ClassroomTimeConflictException.class);
    }
}

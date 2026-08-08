package com.academy.mudogroupware.timetable.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSetNotFoundException;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSlotNotFoundException;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.TimetableClassroom;
import com.academy.mudogroupware.timetable.domain.model.TimetableSet;
import com.academy.mudogroupware.timetable.domain.model.TimetableSlot;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSetRepository;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSlotRepository;

@ExtendWith(MockitoExtension.class)
class GetTimetableSlotServiceTest {

    @Mock private TimetableSetRepository timetableSetRepository;
    @Mock private TimetableSlotRepository timetableSlotRepository;

    private GetTimetableSlotService service;

    @BeforeEach
    void setUp() {
        service = new GetTimetableSlotService(timetableSetRepository, timetableSlotRepository);
    }

    private TimetableSet timetableSet(Long academyId) {
        return TimetableSet.restore(
                1L, academyId, "이름", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY), 30,
                List.of(new TimetableClassroom("6층", "601")), null, null);
    }

    @Test
    void getSlotReturnsViewWhenBelongsToSet() {
        when(timetableSetRepository.findById(1L)).thenReturn(Optional.of(timetableSet(1L)));
        TimetableSlot slot = TimetableSlot.restore(
                100L, 1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                "고3", "정T", "미적분", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16), null, null);
        when(timetableSlotRepository.findById(100L)).thenReturn(Optional.of(slot));

        TimetableSlotView view = service.getSlot(1L, 1L, 100L);

        assertThat(view.teacherName()).isEqualTo("정T");
    }

    @Test
    void getSlotThrowsWhenTimetableSetNotFound() {
        when(timetableSetRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSlot(1L, 999L, 100L))
                .isInstanceOf(TimetableSetNotFoundException.class);
    }

    @Test
    void getSlotThrowsWhenTimetableSetBelongsToDifferentAcademy() {
        when(timetableSetRepository.findById(1L)).thenReturn(Optional.of(timetableSet(2L)));

        assertThatThrownBy(() -> service.getSlot(1L, 1L, 100L))
                .isInstanceOf(TimetableSetNotFoundException.class);
    }

    @Test
    void getSlotThrowsWhenNotFound() {
        when(timetableSetRepository.findById(1L)).thenReturn(Optional.of(timetableSet(1L)));
        when(timetableSlotRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSlot(1L, 1L, 999L))
                .isInstanceOf(TimetableSlotNotFoundException.class);
    }

    @Test
    void getSlotThrowsWhenBelongsToDifferentSet() {
        when(timetableSetRepository.findById(1L)).thenReturn(Optional.of(timetableSet(1L)));
        TimetableSlot slot = TimetableSlot.restore(
                100L, 2L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                "고3", "정T", "미적분", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16), null, null);
        when(timetableSlotRepository.findById(100L)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> service.getSlot(1L, 1L, 100L))
                .isInstanceOf(TimetableSlotNotFoundException.class);
    }
}

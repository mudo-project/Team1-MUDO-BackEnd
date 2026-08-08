package com.academy.mudogroupware.timetable.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSlotNotFoundException;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.TimetableSlot;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSlotRepository;

@ExtendWith(MockitoExtension.class)
class GetTimetableSlotServiceTest {

    @Mock private TimetableSlotRepository timetableSlotRepository;

    private GetTimetableSlotService service;

    @BeforeEach
    void setUp() {
        service = new GetTimetableSlotService(timetableSlotRepository);
    }

    @Test
    void getSlotReturnsViewWhenBelongsToSet() {
        TimetableSlot slot = TimetableSlot.restore(
                100L, 1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                "고3", "정T", "미적분", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16), null, null);
        when(timetableSlotRepository.findById(100L)).thenReturn(Optional.of(slot));

        TimetableSlotView view = service.getSlot(1L, 100L);

        assertThat(view.teacherName()).isEqualTo("정T");
    }

    @Test
    void getSlotThrowsWhenNotFound() {
        when(timetableSlotRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSlot(1L, 999L))
                .isInstanceOf(TimetableSlotNotFoundException.class);
    }

    @Test
    void getSlotThrowsWhenBelongsToDifferentSet() {
        TimetableSlot slot = TimetableSlot.restore(
                100L, 2L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                "고3", "정T", "미적분", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16), null, null);
        when(timetableSlotRepository.findById(100L)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> service.getSlot(1L, 100L))
                .isInstanceOf(TimetableSlotNotFoundException.class);
    }
}

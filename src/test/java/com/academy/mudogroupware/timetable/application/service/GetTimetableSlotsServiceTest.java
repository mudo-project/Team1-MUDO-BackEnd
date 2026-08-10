package com.academy.mudogroupware.timetable.application.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.Grade;
import com.academy.mudogroupware.timetable.domain.model.TimetableClassroom;
import com.academy.mudogroupware.timetable.domain.model.TimetableSet;
import com.academy.mudogroupware.timetable.domain.model.TimetableSlot;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSetRepository;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSlotRepository;

@ExtendWith(MockitoExtension.class)
class GetTimetableSlotsServiceTest {

    @Mock private TimetableSetRepository timetableSetRepository;
    @Mock private TimetableSlotRepository timetableSlotRepository;

    private GetTimetableSlotsService service;

    @BeforeEach
    void setUp() {
        service = new GetTimetableSlotsService(timetableSetRepository, timetableSlotRepository);
    }

    @Test
    void getSlotsReturnsAllSlotsInSet() {
        TimetableSet set = TimetableSet.restore(
                1L, "이름", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY), 30,
                List.of(new TimetableClassroom("6층", "601")), null, null);
        when(timetableSetRepository.findById(1L)).thenReturn(Optional.of(set));
        TimetableSlot slot = TimetableSlot.restore(
                100L, 1L, ClassType.CLASS, DayOfWeek.MONDAY, "601", LocalTime.of(9, 0), LocalTime.of(11, 0),
                Grade.HIGH_3, "정T", "미적분", set.getStartDate(), set.getEndDate(), null, null);
        when(timetableSlotRepository.findAllByTimetableSetId(1L)).thenReturn(List.of(slot));

        List<TimetableSlotView> views = service.getSlots(1L);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).classroomCode()).isEqualTo("601");
    }
}

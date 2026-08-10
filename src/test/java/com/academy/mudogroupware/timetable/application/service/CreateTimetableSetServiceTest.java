package com.academy.mudogroupware.timetable.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.timetable.application.command.CreateTimetableSetCommand;
import com.academy.mudogroupware.timetable.domain.model.TimetableClassroom;
import com.academy.mudogroupware.timetable.domain.model.TimetableSet;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSetRepository;

@ExtendWith(MockitoExtension.class)
class CreateTimetableSetServiceTest {

    @Mock private TimetableSetRepository timetableSetRepository;

    private CreateTimetableSetService service;

    @BeforeEach
    void setUp() {
        service = new CreateTimetableSetService(timetableSetRepository);
    }

    @Test
    void createTimetableSetSavesAndReturnsId() {
        CreateTimetableSetCommand command = new CreateTimetableSetCommand(
                "2026 여름특강", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY), 30,
                List.of(new TimetableClassroom("6층", "601")));
        TimetableSet saved = TimetableSet.restore(
                10L, "2026 여름특강", command.startDate(), command.endDate(),
                command.operatingStartTime(), command.operatingEndTime(), command.operatingDays(),
                30, command.classrooms(), null, null);
        when(timetableSetRepository.save(any(TimetableSet.class))).thenReturn(saved);

        Long id = service.createTimetableSet(command);

        assertThat(id).isEqualTo(10L);
    }
}

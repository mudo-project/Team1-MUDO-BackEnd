package com.academy.mudogroupware.timetable.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.timetable.application.query.TimetableSetDetailView;
import com.academy.mudogroupware.timetable.domain.exception.TimetableSetNotFoundException;
import com.academy.mudogroupware.timetable.domain.model.TimetableClassroom;
import com.academy.mudogroupware.timetable.domain.model.TimetableSet;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSetRepository;

@ExtendWith(MockitoExtension.class)
class GetTimetableSetServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-01T00:00:00Z");

    @Mock private TimetableSetRepository timetableSetRepository;

    private GetTimetableSetService service;

    @BeforeEach
    void setUp() {
        service = new GetTimetableSetService(timetableSetRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void getTimetableSetReturnsExistingDetail() {
        TimetableSet set = TimetableSet.restore(
                1L, "2026 여름특강", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY), 30,
                List.of(new TimetableClassroom("6층", "601")), null, null);
        when(timetableSetRepository.findById(1L)).thenReturn(Optional.of(set));

        TimetableSetDetailView view = service.getTimetableSet(1L);

        assertThat(view.name()).isEqualTo("2026 여름특강");
        assertThat(view.classrooms()).containsExactly(new TimetableClassroom("6층", "601"));
    }

    @Test
    void getTimetableSetThrowsWhenNotFound() {
        when(timetableSetRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTimetableSet(999L))
                .isInstanceOf(TimetableSetNotFoundException.class);
    }

    @Test
    void getTimetableSetReturnsExistingSet() {
        TimetableSet set = TimetableSet.restore(
                1L, "다른 세트", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY), 30,
                List.of(new TimetableClassroom("6층", "601")), null, null);
        when(timetableSetRepository.findById(1L)).thenReturn(Optional.of(set));

        TimetableSetDetailView view = service.getTimetableSet(1L);

        assertThat(view.timetableSetId()).isEqualTo(1L);
    }
}

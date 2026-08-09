package com.academy.mudogroupware.timetable.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.timetable.application.query.TimetableSetSummaryView;
import com.academy.mudogroupware.timetable.domain.model.TimetableClassroom;
import com.academy.mudogroupware.timetable.domain.model.TimetableSet;
import com.academy.mudogroupware.timetable.domain.model.TimetableSetStatus;
import com.academy.mudogroupware.timetable.domain.repository.TimetableSetRepository;

@ExtendWith(MockitoExtension.class)
class GetTimetableSetsServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-01T00:00:00Z");

    @Mock private TimetableSetRepository timetableSetRepository;

    private GetTimetableSetsService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new GetTimetableSetsService(timetableSetRepository, clock);
    }

    @Test
    void getTimetableSetsReturnsSummariesWithDerivedStatus() {
        TimetableSet active = TimetableSet.restore(
                1L, 1L, "2026 여름특강", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 16),
                LocalTime.of(8, 30), LocalTime.of(22, 0), Set.of(DayOfWeek.MONDAY), 30,
                List.of(new TimetableClassroom("6층", "601")), null, null);
        when(timetableSetRepository.findAllByAcademyId(1L)).thenReturn(List.of(active));

        List<TimetableSetSummaryView> views = service.getTimetableSets(1L);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).status()).isEqualTo(TimetableSetStatus.ACTIVE);
        assertThat(views.get(0).name()).isEqualTo("2026 여름특강");
    }
}

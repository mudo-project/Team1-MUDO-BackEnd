package com.academy.mudogroupware.student.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.student.domain.model.EnrollmentStatus;

class ActiveEnrollmentCountPortAdapterTest {

    private final EnrollmentJpaRepository enrollmentJpaRepository = mock(EnrollmentJpaRepository.class);
    private final ActiveEnrollmentCountPortAdapter adapter =
            new ActiveEnrollmentCountPortAdapter(enrollmentJpaRepository);

    @Test
    void mapsCountsByLectureId() {
        EnrollmentJpaRepository.LectureEnrollmentCount count = mock(
                EnrollmentJpaRepository.LectureEnrollmentCount.class);
        when(count.getLectureId()).thenReturn(1L);
        when(count.getCount()).thenReturn(12L);
        when(enrollmentJpaRepository.countByLectureIdsAndStatus(List.of(1L, 2L), EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(count));

        Map<Long, Long> result = adapter.countActiveByLectureIds(List.of(1L, 2L));

        assertThat(result).containsExactly(Map.entry(1L, 12L));
    }

    @Test
    void returnsEmptyMapForEmptyInput() {
        Map<Long, Long> result = adapter.countActiveByLectureIds(List.of());

        assertThat(result).isEmpty();
        verify(enrollmentJpaRepository, never()).countByLectureIdsAndStatus(any(), any());
    }

    @Test
    void returnsEmptyMapForNullInput() {
        Map<Long, Long> result = adapter.countActiveByLectureIds(null);

        assertThat(result).isEmpty();
        verify(enrollmentJpaRepository, never()).countByLectureIdsAndStatus(any(), any());
    }
}

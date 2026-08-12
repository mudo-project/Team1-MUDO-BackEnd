package com.academy.mudogroupware.student.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.student.domain.model.EnrollmentStatus;

class EnrollmentLectureLookupPortAdapterTest {

    private final EnrollmentJpaRepository enrollmentJpaRepository = mock(EnrollmentJpaRepository.class);
    private final EnrollmentLectureLookupPortAdapter adapter =
            new EnrollmentLectureLookupPortAdapter(enrollmentJpaRepository);

    @Test
    void mapsEnrollmentIdToLectureId() {
        EnrollmentEntity entity = EnrollmentEntity.builder()
                .id(100L).studentId(1L).lectureId(5L)
                .status(EnrollmentStatus.ACTIVE).enrolledAt(LocalDateTime.now())
                .build();
        when(enrollmentJpaRepository.findAllById(List.of(100L))).thenReturn(List.of(entity));

        Map<Long, Long> result = adapter.findLectureIdsByEnrollmentIds(List.of(100L));

        assertThat(result).containsExactly(Map.entry(100L, 5L));
    }

    @Test
    void returnsEmptyMapForEmptyInput() {
        Map<Long, Long> result = adapter.findLectureIdsByEnrollmentIds(List.of());

        assertThat(result).isEmpty();
    }
}

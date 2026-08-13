package com.academy.mudogroupware.student.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EnrollmentLectureLookupPortAdapterTest {

    private final EnrollmentJpaRepository enrollmentJpaRepository = mock(EnrollmentJpaRepository.class);
    private final EnrollmentLectureLookupPortAdapter adapter =
            new EnrollmentLectureLookupPortAdapter(enrollmentJpaRepository);

    private EnrollmentJpaRepository.EnrollmentLectureIdProjection projection(Long id, Long lectureId) {
        EnrollmentJpaRepository.EnrollmentLectureIdProjection row =
                mock(EnrollmentJpaRepository.EnrollmentLectureIdProjection.class);
        when(row.getId()).thenReturn(id);
        when(row.getLectureId()).thenReturn(lectureId);
        return row;
    }

    @Test
    void mapsEnrollmentIdToLectureId() {
        EnrollmentJpaRepository.EnrollmentLectureIdProjection row = projection(100L, 5L);
        when(enrollmentJpaRepository.findLectureIdsByIdIn(List.of(100L))).thenReturn(List.of(row));

        Map<Long, Long> result = adapter.findLectureIdsByEnrollmentIds(List.of(100L));

        assertThat(result).containsExactly(Map.entry(100L, 5L));
    }

    @Test
    void returnsEmptyMapForEmptyInput() {
        Map<Long, Long> result = adapter.findLectureIdsByEnrollmentIds(List.of());

        assertThat(result).isEmpty();
        verify(enrollmentJpaRepository, never()).findLectureIdsByIdIn(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void returnsEmptyMapForNullInput() {
        Map<Long, Long> result = adapter.findLectureIdsByEnrollmentIds(null);

        assertThat(result).isEmpty();
        verify(enrollmentJpaRepository, never()).findLectureIdsByIdIn(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void resultCanHaveFewerEntriesThanRequestedIds() {
        // 삭제된 enrollmentId가 결제에 남아있는 경우 — 그 결제는 조회 결과에서 그냥 빠진다.
        EnrollmentJpaRepository.EnrollmentLectureIdProjection row = projection(100L, 5L);
        when(enrollmentJpaRepository.findLectureIdsByIdIn(List.of(100L, 999L))).thenReturn(List.of(row));

        Map<Long, Long> result = adapter.findLectureIdsByEnrollmentIds(List.of(100L, 999L));

        assertThat(result).containsExactly(Map.entry(100L, 5L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void splitsLargeInputIntoBatchesOf1000() {
        List<Long> ids = LongStream.rangeClosed(1, 1500).boxed().collect(Collectors.toList());
        List<EnrollmentJpaRepository.EnrollmentLectureIdProjection> firstBatchRows = new ArrayList<>();
        for (long i = 1; i <= 1000; i++) {
            firstBatchRows.add(projection(i, 1L));
        }
        List<EnrollmentJpaRepository.EnrollmentLectureIdProjection> secondBatchRows = new ArrayList<>();
        for (long i = 1001; i <= 1500; i++) {
            secondBatchRows.add(projection(i, 2L));
        }
        when(enrollmentJpaRepository.findLectureIdsByIdIn(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(firstBatchRows, secondBatchRows);

        Map<Long, Long> result = adapter.findLectureIdsByEnrollmentIds(ids);

        assertThat(result).hasSize(1500);
        ArgumentCaptor<List<Long>> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(enrollmentJpaRepository, org.mockito.Mockito.times(2)).findLectureIdsByIdIn(batchCaptor.capture());
        assertThat(batchCaptor.getAllValues().get(0)).hasSize(1000);
        assertThat(batchCaptor.getAllValues().get(1)).hasSize(500);
    }
}

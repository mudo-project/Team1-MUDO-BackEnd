package com.academy.mudogroupware.lecture.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.revenuereport.application.port.LectureRevenueInfo;

class LectureRevenuePortAdapterTest {

    private final LectureJpaRepository lectureJpaRepository = mock(LectureJpaRepository.class);
    private final LectureRevenuePortAdapter adapter = new LectureRevenuePortAdapter(lectureJpaRepository);

    @Test
    void mapsProjectionRowsToRevenueInfo() {
        LectureJpaRepository.LectureRevenueProjection row = mock(
                LectureJpaRepository.LectureRevenueProjection.class);
        when(row.getId()).thenReturn(1L);
        when(row.getName()).thenReturn("중등 수학 심화반");
        when(row.getTeacherName()).thenReturn("김강사");
        when(row.getFeeAmount()).thenReturn(300000);
        when(lectureJpaRepository.findAllRevenueProjection()).thenReturn(List.of(row));

        List<LectureRevenueInfo> result = adapter.findAll();

        assertThat(result).containsExactly(new LectureRevenueInfo(1L, "중등 수학 심화반", "김강사", 300000));
    }
}

package com.academy.mudogroupware.attendance.infrastructure.persistence;

import static com.academy.mudogroupware.attendance.domain.model.LeaveRequestStatus.APPROVED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeaveRequestRepositoryImplTest {
    @Mock LeaveRequestJpaRepository jpaRepository;
    @InjectMocks LeaveRequestRepositoryImpl repository;

    @Test
    void 주간_승인휴가를_날짜별_일곱번이_아닌_기간_한번으로_조회한다() {
        LocalDate monday = LocalDate.of(2026, 8, 10);
        LocalDate sunday = monday.plusDays(6);
        LeaveRequestJpaEntity leave = LeaveRequestJpaEntity.builder()
                .id(1L)
                .userId(10L)
                .documentId(20L)
                .startDate(monday.plusDays(1))
                .endDate(monday.plusDays(2))
                .usedDays(2)
                .status(APPROVED)
                .build();
        when(jpaRepository.findAllOverlapping(APPROVED, monday, sunday))
                .thenReturn(List.of(leave));

        var result = repository.findApprovedUserIdsBetween(monday, sunday);

        assertThat(result).hasSize(7);
        assertThat(result.get(monday)).isEmpty();
        assertThat(result.get(monday.plusDays(1))).containsExactly(10L);
        assertThat(result.get(monday.plusDays(2))).containsExactly(10L);
        verify(jpaRepository).findAllOverlapping(APPROVED, monday, sunday);
    }
}

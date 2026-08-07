package com.academy.mudogroupware.attendance.application.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.attendance.domain.model.LeaveGrant;
import com.academy.mudogroupware.attendance.domain.model.LeaveRequestStatus;
import com.academy.mudogroupware.attendance.domain.repository.LeaveGrantRepository;
import com.academy.mudogroupware.attendance.domain.repository.LeaveRequestRepository;

@ExtendWith(MockitoExtension.class)
class MyLeaveSummaryQueryServiceTest {

    @Mock LeaveGrantRepository grantRepository;
    @Mock LeaveRequestRepository requestRepository;

    @Test
    void subtractsApprovedAndPendingDaysFromRemainingDays() {
        Long academyId = 10L;
        Long userId = 2L;
        LocalDate today = LocalDate.of(2026, 8, 7);
        LocalDate grantDate = LocalDate.of(2026, 3, 1);
        LocalDate expirationDate = LocalDate.of(2027, 2, 28);
        var grant = LeaveGrant.restore(
                1L, academyId, userId, grantDate, expirationDate, 15,
                LocalDateTime.of(2026, 3, 1, 0, 0));
        when(grantRepository.findActive(academyId, userId, today))
                .thenReturn(Optional.of(grant));
        when(requestRepository.sumUsedDaysByStatus(
                academyId, userId, grantDate, expirationDate, LeaveRequestStatus.APPROVED))
                .thenReturn(5);
        when(requestRepository.sumUsedDaysByStatus(
                academyId, userId, grantDate, expirationDate, LeaveRequestStatus.PENDING))
                .thenReturn(2);
        var service = new MyLeaveSummaryQueryService(
                grantRepository, requestRepository,
                Clock.fixed(Instant.parse("2026-08-07T03:00:00Z"), ZoneId.of("Asia/Seoul")));

        var result = service.getSummary(userId, academyId);

        assertEquals(15, result.totalDays());
        assertEquals(5, result.usedDays());
        assertEquals(2, result.pendingDays());
        assertEquals(8, result.remainingDays());
        assertEquals(LocalDate.of(2027, 3, 1), result.nextGrantDate());
    }
}

package com.academy.mudogroupware.attendance.application.query;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.attendance.application.usecase.GetMyLeaveSummaryUseCase;
import com.academy.mudogroupware.attendance.domain.model.LeaveRequestStatus;
import com.academy.mudogroupware.attendance.domain.repository.LeaveGrantRepository;
import com.academy.mudogroupware.attendance.domain.repository.LeaveRequestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MyLeaveSummaryQueryService implements GetMyLeaveSummaryUseCase {

    private final LeaveGrantRepository leaveGrantRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final Clock clock;

    @Override
    public MyLeaveSummaryView getSummary(Long userId, Long academyId) {
        log.info("event=attendance_leave_summary_read_시작 userId={}, academyId={}", userId, academyId);
        LocalDate today = LocalDate.now(clock);
        MyLeaveSummaryView result = leaveGrantRepository.findActive(academyId, userId, today)
                .map(grant -> {
                    int usedDays = leaveRequestRepository.sumUsedDaysByStatus(
                            academyId, userId, grant.getGrantDate(), grant.getExpirationDate(),
                            LeaveRequestStatus.APPROVED);
                    int pendingDays = leaveRequestRepository.sumUsedDaysByStatus(
                            academyId, userId, grant.getGrantDate(), grant.getExpirationDate(),
                            LeaveRequestStatus.PENDING);
                    int remainingDays = Math.max(
                            0, grant.getGrantedDays() - usedDays - pendingDays);
                    return new MyLeaveSummaryView(
                            grant.getGrantedDays(), usedDays, pendingDays, remainingDays,
                            grant.getExpirationDate().plusDays(1));
                })
                .orElseGet(() -> new MyLeaveSummaryView(0, 0, 0, 0, null));
        log.info("event=attendance_leave_summary_read_완료 userId={}, academyId={}", userId, academyId);
        return result;
    }
}

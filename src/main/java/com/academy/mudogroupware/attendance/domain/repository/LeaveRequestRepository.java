package com.academy.mudogroupware.attendance.domain.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import java.util.Set;

import com.academy.mudogroupware.attendance.domain.model.LeaveRequest;

public interface LeaveRequestRepository {

    LeaveRequest save(LeaveRequest leaveRequest);

    Optional<LeaveRequest> findByDocumentId(Long documentId);

    /**
     * 오늘 팀 근태 조회에서 직원별로 Port/쿼리를 반복 호출하지 않도록, 해당 학원·날짜에
     * APPROVED 상태인 휴가의 userId를 한 번에 모아서 반환한다.
     */
    Set<Long> findApprovedUserIds(LocalDate date);

    boolean existsOverlapping(Long userId, LocalDate startDate, LocalDate endDate);

    int sumReservedDays(Long userId, LocalDate periodStart, LocalDate periodEnd);

    List<LeaveRequest> findApprovedOverlapping(
            Long userId, LocalDate startDate, LocalDate endDate);

    int sumUsedDaysByStatus(
            Long userId, LocalDate periodStart, LocalDate periodEnd,
            com.academy.mudogroupware.attendance.domain.model.LeaveRequestStatus status);
}

package com.academy.mudogroupware.attendance.domain.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import com.academy.mudogroupware.attendance.domain.model.LeaveRequest;

public interface LeaveRequestRepository {

    LeaveRequest save(LeaveRequest leaveRequest);

    Optional<LeaveRequest> findByDocumentId(Long documentId);

    /**
     * 오늘 팀 근태 조회에서 직원별로 Port/쿼리를 반복 호출하지 않도록, 해당 학원·날짜에
     * CONFIRMED 상태인 휴가의 userId를 한 번에 모아서 반환한다.
     */
    Set<Long> findConfirmedUserIds(Long academyId, LocalDate date);
}

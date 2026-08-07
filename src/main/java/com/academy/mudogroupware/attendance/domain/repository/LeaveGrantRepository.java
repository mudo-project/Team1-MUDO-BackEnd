package com.academy.mudogroupware.attendance.domain.repository;

import java.time.LocalDate;
import java.util.Optional;

import com.academy.mudogroupware.attendance.domain.model.LeaveGrant;

public interface LeaveGrantRepository {

    LeaveGrant save(LeaveGrant leaveGrant);

    boolean existsByAcademyIdAndUserIdAndGrantDate(Long academyId, Long userId, LocalDate grantDate);

    Optional<LeaveGrant> findActiveForUpdate(Long academyId, Long userId, LocalDate date);
}

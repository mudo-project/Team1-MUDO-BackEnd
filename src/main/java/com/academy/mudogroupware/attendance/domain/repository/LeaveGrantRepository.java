package com.academy.mudogroupware.attendance.domain.repository;

import java.time.LocalDate;
import java.util.Optional;

import com.academy.mudogroupware.attendance.domain.model.LeaveGrant;

public interface LeaveGrantRepository {

    LeaveGrant save(LeaveGrant leaveGrant);

    boolean existsByUserIdAndGrantDate(Long userId, LocalDate grantDate);

    Optional<LeaveGrant> findActiveForUpdate(Long userId, LocalDate date);

    Optional<LeaveGrant> findActive(Long userId, LocalDate date);
}

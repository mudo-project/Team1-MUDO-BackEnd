package com.academy.mudogroupware.attendance.domain.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.academy.mudogroupware.attendance.domain.model.AttendanceCorrectionRequest;
import com.academy.mudogroupware.attendance.domain.model.AttendanceCorrectionStatus;
import com.academy.mudogroupware.global.domain.common.page.PagedResult;

public interface AttendanceCorrectionRequestRepository {
    AttendanceCorrectionRequest save(AttendanceCorrectionRequest request);
    boolean existsPending(Long userId, LocalDate workDate);
    Optional<AttendanceCorrectionRequest> findByIdAndOwner(Long id, Long userId);
    List<AttendanceCorrectionRequest> findAllByOwner(Long userId);
    PagedResult<AttendanceCorrectionRequest> findAll(
            AttendanceCorrectionStatus status, int page, int size);
    Optional<AttendanceCorrectionRequest> findByIdForUpdate(Long id);
    Optional<AttendanceCorrectionRequest> findById(Long id);
}

package com.academy.mudogroupware.attendance.domain.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.academy.mudogroupware.attendance.domain.model.AttendanceCorrectionRequest;
import com.academy.mudogroupware.attendance.domain.model.AttendanceCorrectionStatus;
import com.academy.mudogroupware.global.domain.common.page.PageResult;

public interface AttendanceCorrectionRequestRepository {
    AttendanceCorrectionRequest save(AttendanceCorrectionRequest request);
    boolean existsPending(Long academyId, Long userId, LocalDate workDate);
    Optional<AttendanceCorrectionRequest> findByIdAndOwner(Long id, Long academyId, Long userId);
    List<AttendanceCorrectionRequest> findAllByOwner(Long academyId, Long userId);
    PageResult<AttendanceCorrectionRequest> findAllByAcademyId(
            Long academyId, AttendanceCorrectionStatus status, int page, int size);
    Optional<AttendanceCorrectionRequest> findByIdAndAcademyIdForUpdate(Long id, Long academyId);
    Optional<AttendanceCorrectionRequest> findByIdAndAcademyId(Long id, Long academyId);
}

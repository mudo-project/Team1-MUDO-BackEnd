package com.academy.mudogroupware.attendance.domain.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.academy.mudogroupware.attendance.domain.model.AttendanceCorrectionRequest;

public interface AttendanceCorrectionRequestRepository {
    AttendanceCorrectionRequest save(AttendanceCorrectionRequest request);
    boolean existsPending(Long academyId, Long userId, LocalDate workDate);
    Optional<AttendanceCorrectionRequest> findByIdAndOwner(Long id, Long academyId, Long userId);
    List<AttendanceCorrectionRequest> findAllByOwner(Long academyId, Long userId);
}

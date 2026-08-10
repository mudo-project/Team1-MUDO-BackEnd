package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import com.academy.mudogroupware.attendance.domain.exception.*;
import com.academy.mudogroupware.attendance.domain.model.*;
import com.academy.mudogroupware.attendance.domain.repository.AttendanceCorrectionRequestRepository;
import com.academy.mudogroupware.global.domain.common.page.PageResult;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AttendanceCorrectionRequestRepositoryImpl implements AttendanceCorrectionRequestRepository {
    private final AttendanceCorrectionRequestJpaRepository repository;

    @Override public AttendanceCorrectionRequest save(AttendanceCorrectionRequest request) {
        try { return toDomain(repository.saveAndFlush(toEntity(request))); }
        catch (DataIntegrityViolationException e) {
            if (isPendingRequestDuplicate(e)) {
                throw new AttendanceException(
                        AttendanceErrorCode.CORRECTION_REQUEST_ALREADY_PENDING, e);
            }
            throw e;
        }
    }
    @Override public boolean existsPending(Long userId, LocalDate workDate) {
        return repository.existsByUserIdAndWorkDateAndStatus(userId, workDate, AttendanceCorrectionStatus.PENDING);
    }
    @Override public Optional<AttendanceCorrectionRequest> findByIdAndOwner(Long id, Long userId) {
        return repository.findByIdAndUserId(id, userId).map(this::toDomain);
    }
    @Override public List<AttendanceCorrectionRequest> findAllByOwner(Long userId) {
        return repository.findAllByUserIdOrderByRequestedAtDesc(userId).stream().map(this::toDomain).toList();
    }
    @Override public PageResult<AttendanceCorrectionRequest> findAll(
            AttendanceCorrectionStatus status, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "requestedAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        PageRequest pageable = PageRequest.of(page, size, sort);
        Slice<AttendanceCorrectionRequestJpaEntity> slice = status == null
                ? repository.findPage(pageable)
                : repository.findAllByStatus(status, pageable);
        return PageResult.of(slice.getContent().stream().map(this::toDomain).toList(),
                slice.getNumber(), slice.getSize(), slice.hasNext());
    }
    @Override public Optional<AttendanceCorrectionRequest> findByIdForUpdate(
            Long id) {
        return repository.findByIdForUpdate(id).map(this::toDomain);
    }
    @Override public Optional<AttendanceCorrectionRequest> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }
    private AttendanceCorrectionRequestJpaEntity toEntity(AttendanceCorrectionRequest r) {
        return AttendanceCorrectionRequestJpaEntity.builder().id(r.getId()).userId(r.getUserId()).attendanceId(r.getAttendanceId())
                .workDate(r.getWorkDate()).type(r.getType()).status(r.getStatus()).originalClockInAt(r.getOriginalClockInAt()).originalClockOutAt(r.getOriginalClockOutAt())
                .originalClockInNote(r.getOriginalClockInNote()).originalClockOutNote(r.getOriginalClockOutNote()).requestedClockInAt(r.getRequestedClockInAt())
                .requestedClockOutAt(r.getRequestedClockOutAt()).requestedClockInNote(r.getRequestedClockInNote()).requestedClockOutNote(r.getRequestedClockOutNote())
                .reason(r.getReason()).requestedAt(r.getRequestedAt()).processedAt(r.getProcessedAt()).processedBy(r.getProcessedBy()).rejectionReason(r.getRejectionReason()).build();
    }
    private AttendanceCorrectionRequest toDomain(AttendanceCorrectionRequestJpaEntity e) {
        return AttendanceCorrectionRequest.restore(e.getId(), e.getUserId(), e.getAttendanceId(), e.getWorkDate(), e.getType(), e.getStatus(),
                e.getOriginalClockInAt(), e.getOriginalClockOutAt(), e.getOriginalClockInNote(), e.getOriginalClockOutNote(), e.getRequestedClockInAt(),
                e.getRequestedClockOutAt(), e.getRequestedClockInNote(), e.getRequestedClockOutNote(), e.getReason(), e.getRequestedAt(), e.getProcessedAt(), e.getProcessedBy(), e.getRejectionReason());
    }

    private boolean isPendingRequestDuplicate(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT)
                    .contains("uk_attendance_correction_request_pending")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}

package com.academy.mudogroupware.attendance.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.attendance.application.port.AttendanceCorrectionRequesterPort;
import com.academy.mudogroupware.attendance.application.port.AttendanceCorrectionRequesterPort.Requester;
import com.academy.mudogroupware.attendance.application.query.AdminAttendanceCorrectionView;
import com.academy.mudogroupware.attendance.application.query.AttendanceCorrectionView;
import com.academy.mudogroupware.attendance.application.usecase.ManageAttendanceCorrectionUseCase;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AttendanceCorrectionRequest;
import com.academy.mudogroupware.attendance.domain.model.AttendanceCorrectionStatus;
import com.academy.mudogroupware.attendance.domain.model.AttendanceCorrectionType;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicy;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicyWeekday;
import com.academy.mudogroupware.attendance.domain.model.AttendanceRecord;
import com.academy.mudogroupware.attendance.domain.model.AttendanceStatus;
import com.academy.mudogroupware.attendance.domain.repository.AttendanceCorrectionRequestRepository;
import com.academy.mudogroupware.attendance.domain.repository.AttendancePolicyRepository;
import com.academy.mudogroupware.attendance.domain.repository.AttendanceRecordRepository;
import com.academy.mudogroupware.global.domain.common.page.PageResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageAttendanceCorrectionService implements ManageAttendanceCorrectionUseCase {

    private final AttendanceCorrectionRequestRepository correctionRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final AttendancePolicyRepository policyRepository;
    private final AttendanceCorrectionRequesterPort requesterPort;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminAttendanceCorrectionView> getAll(AttendanceCorrectionStatus status,
                                                              int page, int size) {
        log.info("event=attendance_admin_correction_list_read_시작 status={}, page={}, size={}", status, page, size);
        try {
        PageResult<AttendanceCorrectionRequest> result = correctionRepository
                .findAll(status, page, size);
        Map<Long, Requester> requesters = requesterPort.findByUserIds(
                result.content().stream().map(AttendanceCorrectionRequest::getUserId).collect(java.util.stream.Collectors.toSet()));
        PageResult<AdminAttendanceCorrectionView> mapped = result.map(request -> new AdminAttendanceCorrectionView(
                AttendanceCorrectionView.from(request), requesters.get(request.getUserId())));
        log.info("event=attendance_admin_correction_list_read_완료 count={}", mapped.content().size());
        return mapped;
        } catch (RuntimeException e) { log.warn("event=attendance_admin_correction_list_read_실패 errorType={}", e.getClass().getSimpleName()); throw e; }
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAttendanceCorrectionView get(Long requestId) {
        log.info("event=attendance_admin_correction_detail_read_시작 requestId={}", requestId);
        try {
        AttendanceCorrectionRequest request = findRequest(requestId);
        Requester requester = requesterPort.findByUserIds(Set.of(request.getUserId()))
                .get(request.getUserId());
        AdminAttendanceCorrectionView result = new AdminAttendanceCorrectionView(AttendanceCorrectionView.from(request), requester);
        log.info("event=attendance_admin_correction_detail_read_완료 requestId={}", requestId); return result;
        } catch (RuntimeException e) { log.warn("event=attendance_admin_correction_detail_read_실패 requestId={}, errorType={}", requestId, e.getClass().getSimpleName()); throw e; }
    }

    @Override
    @Transactional
    public void approve(Long requestId, Long processorId) {
        log.info("event=attendance_admin_correction_approve_시작 requestId={}, processorId={}", requestId, processorId);
        try {
        AttendanceCorrectionRequest request = findRequestForUpdate(requestId);
        LocalDateTime now = LocalDateTime.now(clock);
        AttendanceRecord current = attendanceRepository.findByUserIdAndWorkDate(
                request.getUserId(), request.getWorkDate()).orElse(null);
        AttendanceRecord corrected = applyCorrection(request, current, now);
        AttendanceRecord saved = attendanceRepository.save(corrected);
        AttendanceCorrectionRequest processed = request.attachAttendanceId(saved.getId())
                .approve(processorId, now);
        correctionRepository.save(processed);
        log.info("event=attendance_admin_correction_approve_완료 requestId={}", requestId);
        } catch (RuntimeException e) { log.warn("event=attendance_admin_correction_approve_실패 requestId={}, errorType={}", requestId, e.getClass().getSimpleName()); throw e; }
    }

    @Override
    @Transactional
    public void reject(Long requestId, Long processorId, String reason) {
        log.info("event=attendance_admin_correction_reject_시작 requestId={}, processorId={}", requestId, processorId);
        try {
        AttendanceCorrectionRequest request = findRequestForUpdate(requestId);
        correctionRepository.save(request.reject(processorId, LocalDateTime.now(clock), reason));
        log.info("event=attendance_admin_correction_reject_완료 requestId={}", requestId);
        } catch (RuntimeException e) { log.warn("event=attendance_admin_correction_reject_실패 requestId={}, errorType={}", requestId, e.getClass().getSimpleName()); throw e; }
    }

    private AttendanceCorrectionRequest findRequest(Long requestId) {
        return correctionRepository.findById(requestId)
                .orElseThrow(() -> new AttendanceException(AttendanceErrorCode.CORRECTION_REQUEST_NOT_FOUND));
    }

    private AttendanceCorrectionRequest findRequestForUpdate(Long requestId) {
        return correctionRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new AttendanceException(AttendanceErrorCode.CORRECTION_REQUEST_NOT_FOUND));
    }

    private AttendanceRecord applyCorrection(AttendanceCorrectionRequest request,
                                              AttendanceRecord current,
                                              LocalDateTime now) {
        AttendanceCorrectionType type = request.getType();
        if (type == AttendanceCorrectionType.MISSING_RECORD) {
            if (current != null) {
                throw new AttendanceException(AttendanceErrorCode.INVALID_CORRECTION_REQUEST);
            }
            return AttendanceRecord.createFromCorrection(request.getUserId(),
                    request.getWorkDate(), request.getRequestedClockInAt(), request.getRequestedClockInNote(),
                    request.getRequestedClockOutAt(), request.getRequestedClockOutNote(),
                    resolveStatus(request.getWorkDate(), request.getRequestedClockInAt()), now);
        }
        if (current == null) {
            throw new AttendanceException(AttendanceErrorCode.CORRECTION_ATTENDANCE_NOT_FOUND);
        }
        LocalDateTime clockInAt = type == AttendanceCorrectionType.CLOCK_IN_TIME
                ? request.getRequestedClockInAt() : current.getClockInAt();
        LocalDateTime clockOutAt = type == AttendanceCorrectionType.CLOCK_OUT_TIME
                ? request.getRequestedClockOutAt() : current.getClockOutAt();
        String clockInNote = type == AttendanceCorrectionType.CLOCK_IN_NOTE
                ? request.getRequestedClockInNote() : current.getClockInNote();
        String clockOutNote = type == AttendanceCorrectionType.CLOCK_OUT_NOTE
                ? request.getRequestedClockOutNote() : current.getClockOutNote();
        AttendanceStatus status = type == AttendanceCorrectionType.CLOCK_IN_TIME
                ? resolveStatus(request.getWorkDate(), clockInAt)
                : current.getStatus();
        return current.applyCorrection(clockInAt, clockInNote, clockOutAt, clockOutNote, status, now);
    }

    private AttendanceStatus resolveStatus(java.time.LocalDate workDate,
                                            LocalDateTime clockInAt) {
        AttendancePolicy policy = policyRepository.findCurrent()
                .orElseThrow(() -> new AttendanceException(AttendanceErrorCode.ATTENDANCE_POLICY_NOT_FOUND));
        if (!policy.isWorkday(workDate.getDayOfWeek().getValue())) {
            throw new AttendanceException(AttendanceErrorCode.ATTENDANCE_NON_WORKDAY);
        }
        LocalTime start = policy.getDefaultStartTime();
        if (policy.isWeekdayExceptionEnabled()) {
            start = policy.getWeekdays().stream()
                    .filter(day -> day.dayOfWeek() == workDate.getDayOfWeek().getValue())
                    .findFirst().map(AttendancePolicyWeekday::startTime).orElse(start);
            if (start == null) {
                start = policy.getDefaultStartTime();
            }
        }
        return clockInAt.isAfter(workDate.atTime(start).plusMinutes(policy.getLateGraceMinutes()))
                ? AttendanceStatus.LATE : AttendanceStatus.NORMAL;
    }
}

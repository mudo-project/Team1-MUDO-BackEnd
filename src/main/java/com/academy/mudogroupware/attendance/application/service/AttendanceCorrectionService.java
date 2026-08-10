package com.academy.mudogroupware.attendance.application.service;
import java.time.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.academy.mudogroupware.attendance.application.command.CreateAttendanceCorrectionCommand;
import com.academy.mudogroupware.attendance.application.query.*;
import com.academy.mudogroupware.attendance.application.usecase.*;
import com.academy.mudogroupware.attendance.domain.exception.*;
import com.academy.mudogroupware.attendance.domain.model.*;
import com.academy.mudogroupware.attendance.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Service @RequiredArgsConstructor @Slf4j @Transactional(readOnly = true)
public class AttendanceCorrectionService implements CreateAttendanceCorrectionUseCase,
        GetMyAttendanceCorrectionUseCase, GetMyAttendanceDayUseCase {
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceCorrectionRequestRepository correctionRepository;
    private final Clock clock;
    @Override @Transactional
    public AttendanceCorrectionView create(CreateAttendanceCorrectionCommand c) {
        log.info("event=attendance_correction_create_시작 userId={}, date={}", c.userId(), c.date());
        try {
        validateDate(c.date());
        if (correctionRepository.existsPending(c.userId(), c.date()))
            throw new AttendanceException(AttendanceErrorCode.CORRECTION_REQUEST_ALREADY_PENDING);
        AttendanceRecord attendance = attendanceRecordRepository.findByUserIdAndWorkDate(
                c.userId(), c.date()).orElse(null);
        AttendanceCorrectionRequest request = AttendanceCorrectionRequest.submit(c.userId(),
                attendance, c.date(), c.type(), atDate(c.date(), c.requestedClockInTime()),
                atDate(c.date(), c.requestedClockOutTime()), c.requestedClockInNote(),
                c.requestedClockOutNote(), c.reason(), LocalDateTime.now(clock));
        AttendanceCorrectionView result = AttendanceCorrectionView.from(correctionRepository.save(request));
        log.info("event=attendance_correction_create_완료 userId={}, date={}", c.userId(), c.date());
        return result;
        } catch (RuntimeException e) { log.warn("event=attendance_correction_create_실패 userId={}, reason={}", c.userId(), e.getMessage()); throw e; }
    }
    @Override public List<AttendanceCorrectionView> getAll(Long userId) {
        log.info("event=attendance_correction_list_read_시작 userId={}", userId);
        try { List<AttendanceCorrectionView> result = correctionRepository.findAllByOwner(userId).stream().map(AttendanceCorrectionView::from).toList();
        log.info("event=attendance_correction_list_read_완료 userId={}, count={}", userId, result.size()); return result;
        } catch (RuntimeException e) { log.warn("event=attendance_correction_list_read_실패 userId={}, reason={}", userId, e.getMessage()); throw e; }
    }
    @Override public AttendanceCorrectionView get(Long requestId, Long userId) {
        log.info("event=attendance_correction_detail_read_시작 userId={}, requestId={}", userId, requestId);
        try { AttendanceCorrectionView result = correctionRepository.findByIdAndOwner(requestId, userId).map(AttendanceCorrectionView::from)
                .orElseThrow(() -> new AttendanceException(AttendanceErrorCode.CORRECTION_REQUEST_NOT_FOUND));
        log.info("event=attendance_correction_detail_read_완료 requestId={}", requestId); return result;
        } catch (RuntimeException e) { log.warn("event=attendance_correction_detail_read_실패 requestId={}, reason={}", requestId, e.getMessage()); throw e; }
    }
    @Override public MyAttendanceDayView get(Long userId, LocalDate date) {
        log.info("event=attendance_day_detail_read_시작 userId={}, date={}", userId, date);
        try { validateDate(date);
        AttendanceRecord r = attendanceRecordRepository.findByUserIdAndWorkDate(userId, date).orElse(null);
        MyAttendanceDayView result = new MyAttendanceDayView(date, r == null ? null : r.getClockInAt(), r == null ? null : r.getClockOutAt(),
                r == null ? null : r.getClockInNote(), r == null ? null : r.getClockOutNote(),
                correctionRepository.existsPending(userId, date));
        log.info("event=attendance_day_detail_read_완료 userId={}, date={}", userId, date); return result;
        } catch (RuntimeException e) { log.warn("event=attendance_day_detail_read_실패 userId={}, date={}, reason={}", userId, date, e.getMessage()); throw e; }
    }
    private void validateDate(LocalDate date) {
        if (date == null || date.isAfter(LocalDate.now(clock)))
            throw new AttendanceException(AttendanceErrorCode.FUTURE_CORRECTION_DATE);
    }
    private LocalDateTime atDate(LocalDate date, LocalTime time) { return time == null ? null : date.atTime(time); }
}

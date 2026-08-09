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
        log.info("event=attendance_correction_create_시작 academyId={}, userId={}, date={}", c.academyId(), c.userId(), c.date());
        try {
        validateDate(c.date());
        if (correctionRepository.existsPending(c.academyId(), c.userId(), c.date()))
            throw new AttendanceException(AttendanceErrorCode.CORRECTION_REQUEST_ALREADY_PENDING);
        AttendanceRecord attendance = attendanceRecordRepository.findByAcademyIdAndUserIdAndWorkDate(
                c.academyId(), c.userId(), c.date()).orElse(null);
        AttendanceCorrectionRequest request = AttendanceCorrectionRequest.submit(c.academyId(), c.userId(),
                attendance, c.date(), c.type(), atDate(c.date(), c.requestedClockInTime()),
                atDate(c.date(), c.requestedClockOutTime()), c.requestedClockInNote(),
                c.requestedClockOutNote(), c.reason(), LocalDateTime.now(clock));
        AttendanceCorrectionView result = AttendanceCorrectionView.from(correctionRepository.save(request));
        log.info("event=attendance_correction_create_완료 academyId={}, userId={}, date={}", c.academyId(), c.userId(), c.date());
        return result;
        } catch (RuntimeException e) { log.warn("event=attendance_correction_create_실패 academyId={}, userId={}, reason={}", c.academyId(), c.userId(), e.getMessage()); throw e; }
    }
    @Override public List<AttendanceCorrectionView> getAll(Long academyId, Long userId) {
        log.info("event=attendance_correction_list_read_시작 academyId={}, userId={}", academyId, userId);
        try { List<AttendanceCorrectionView> result = correctionRepository.findAllByOwner(academyId, userId).stream().map(AttendanceCorrectionView::from).toList();
        log.info("event=attendance_correction_list_read_완료 academyId={}, userId={}, count={}", academyId, userId, result.size()); return result;
        } catch (RuntimeException e) { log.warn("event=attendance_correction_list_read_실패 academyId={}, userId={}, reason={}", academyId, userId, e.getMessage()); throw e; }
    }
    @Override public AttendanceCorrectionView get(Long requestId, Long academyId, Long userId) {
        log.info("event=attendance_correction_detail_read_시작 academyId={}, userId={}, requestId={}", academyId, userId, requestId);
        try { AttendanceCorrectionView result = correctionRepository.findByIdAndOwner(requestId, academyId, userId).map(AttendanceCorrectionView::from)
                .orElseThrow(() -> new AttendanceException(AttendanceErrorCode.CORRECTION_REQUEST_NOT_FOUND));
        log.info("event=attendance_correction_detail_read_완료 academyId={}, requestId={}", academyId, requestId); return result;
        } catch (RuntimeException e) { log.warn("event=attendance_correction_detail_read_실패 academyId={}, requestId={}, reason={}", academyId, requestId, e.getMessage()); throw e; }
    }
    @Override public MyAttendanceDayView get(Long academyId, Long userId, LocalDate date) {
        log.info("event=attendance_day_detail_read_시작 academyId={}, userId={}, date={}", academyId, userId, date);
        try { validateDate(date);
        AttendanceRecord r = attendanceRecordRepository.findByAcademyIdAndUserIdAndWorkDate(academyId, userId, date).orElse(null);
        MyAttendanceDayView result = new MyAttendanceDayView(date, r == null ? null : r.getClockInAt(), r == null ? null : r.getClockOutAt(),
                r == null ? null : r.getClockInNote(), r == null ? null : r.getClockOutNote(),
                correctionRepository.existsPending(academyId, userId, date));
        log.info("event=attendance_day_detail_read_완료 academyId={}, userId={}, date={}", academyId, userId, date); return result;
        } catch (RuntimeException e) { log.warn("event=attendance_day_detail_read_실패 academyId={}, userId={}, date={}, reason={}", academyId, userId, date, e.getMessage()); throw e; }
    }
    private void validateDate(LocalDate date) {
        if (date == null || date.isAfter(LocalDate.now(clock)))
            throw new AttendanceException(AttendanceErrorCode.FUTURE_CORRECTION_DATE);
    }
    private LocalDateTime atDate(LocalDate date, LocalTime time) { return time == null ? null : date.atTime(time); }
}

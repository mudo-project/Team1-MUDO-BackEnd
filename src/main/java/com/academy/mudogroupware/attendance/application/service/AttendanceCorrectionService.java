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
@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class AttendanceCorrectionService implements CreateAttendanceCorrectionUseCase,
        GetMyAttendanceCorrectionUseCase, GetMyAttendanceDayUseCase {
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceCorrectionRequestRepository correctionRepository;
    private final Clock clock;
    @Override @Transactional
    public AttendanceCorrectionView create(CreateAttendanceCorrectionCommand c) {
        validateDate(c.date());
        if (correctionRepository.existsPending(c.academyId(), c.userId(), c.date()))
            throw new AttendanceException(AttendanceErrorCode.CORRECTION_REQUEST_ALREADY_PENDING);
        AttendanceRecord attendance = attendanceRecordRepository.findByAcademyIdAndUserIdAndWorkDate(
                c.academyId(), c.userId(), c.date()).orElse(null);
        AttendanceCorrectionRequest request = AttendanceCorrectionRequest.submit(c.academyId(), c.userId(),
                attendance, c.date(), c.type(), atDate(c.date(), c.requestedClockInTime()),
                atDate(c.date(), c.requestedClockOutTime()), c.requestedClockInNote(),
                c.requestedClockOutNote(), c.reason(), LocalDateTime.now(clock));
        return AttendanceCorrectionView.from(correctionRepository.save(request));
    }
    @Override public List<AttendanceCorrectionView> getAll(Long academyId, Long userId) {
        return correctionRepository.findAllByOwner(academyId, userId).stream().map(AttendanceCorrectionView::from).toList();
    }
    @Override public AttendanceCorrectionView get(Long requestId, Long academyId, Long userId) {
        return correctionRepository.findByIdAndOwner(requestId, academyId, userId).map(AttendanceCorrectionView::from)
                .orElseThrow(() -> new AttendanceException(AttendanceErrorCode.CORRECTION_REQUEST_NOT_FOUND));
    }
    @Override public MyAttendanceDayView get(Long academyId, Long userId, LocalDate date) {
        validateDate(date);
        AttendanceRecord r = attendanceRecordRepository.findByAcademyIdAndUserIdAndWorkDate(academyId, userId, date).orElse(null);
        return new MyAttendanceDayView(date, r == null ? null : r.getClockInAt(), r == null ? null : r.getClockOutAt(),
                r == null ? null : r.getClockInNote(), r == null ? null : r.getClockOutNote(),
                correctionRepository.existsPending(academyId, userId, date));
    }
    private void validateDate(LocalDate date) {
        if (date == null || date.isAfter(LocalDate.now(clock)))
            throw new AttendanceException(AttendanceErrorCode.FUTURE_CORRECTION_DATE);
    }
    private LocalDateTime atDate(LocalDate date, LocalTime time) { return time == null ? null : date.atTime(time); }
}

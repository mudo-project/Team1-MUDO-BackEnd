package com.academy.mudogroupware.attendance.presentation.api.response;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.academy.mudogroupware.attendance.application.query.AttendanceCorrectionView;
import com.academy.mudogroupware.attendance.domain.model.*;
public record AttendanceCorrectionResponse(Long requestId, LocalDate date, AttendanceCorrectionType type,
        AttendanceCorrectionStatus status, LocalDateTime originalClockInAt, LocalDateTime originalClockOutAt,
        String originalClockInNote, String originalClockOutNote, LocalDateTime requestedClockInAt,
        LocalDateTime requestedClockOutAt, String requestedClockInNote, String requestedClockOutNote,
        String reason, LocalDateTime requestedAt, LocalDateTime processedAt, String rejectionReason) {
    public static AttendanceCorrectionResponse from(AttendanceCorrectionView v) {
        return new AttendanceCorrectionResponse(v.requestId(), v.date(), v.type(), v.status(), v.originalClockInAt(),
                v.originalClockOutAt(), v.originalClockInNote(), v.originalClockOutNote(), v.requestedClockInAt(),
                v.requestedClockOutAt(), v.requestedClockInNote(), v.requestedClockOutNote(), v.reason(),
                v.requestedAt(), v.processedAt(), v.rejectionReason());
    }
}

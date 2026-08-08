package com.academy.mudogroupware.attendance.application.query;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.academy.mudogroupware.attendance.domain.model.*;
public record AttendanceCorrectionView(Long requestId, LocalDate date, AttendanceCorrectionType type,
        AttendanceCorrectionStatus status, LocalDateTime originalClockInAt, LocalDateTime originalClockOutAt,
        String originalClockInNote, String originalClockOutNote, LocalDateTime requestedClockInAt,
        LocalDateTime requestedClockOutAt, String requestedClockInNote, String requestedClockOutNote,
        String reason, LocalDateTime requestedAt, LocalDateTime processedAt, Long processedBy, String rejectionReason) {
    public static AttendanceCorrectionView from(AttendanceCorrectionRequest r) {
        return new AttendanceCorrectionView(r.getId(), r.getWorkDate(), r.getType(), r.getStatus(),
                r.getOriginalClockInAt(), r.getOriginalClockOutAt(), r.getOriginalClockInNote(), r.getOriginalClockOutNote(),
                r.getRequestedClockInAt(), r.getRequestedClockOutAt(), r.getRequestedClockInNote(), r.getRequestedClockOutNote(),
                r.getReason(), r.getRequestedAt(), r.getProcessedAt(), r.getProcessedBy(), r.getRejectionReason());
    }
}

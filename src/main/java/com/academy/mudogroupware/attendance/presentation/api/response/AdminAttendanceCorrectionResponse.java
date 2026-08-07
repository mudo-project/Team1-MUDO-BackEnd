package com.academy.mudogroupware.attendance.presentation.api.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.academy.mudogroupware.attendance.application.port.AttendanceCorrectionRequesterPort.Requester;
import com.academy.mudogroupware.attendance.application.query.AdminAttendanceCorrectionView;
import com.academy.mudogroupware.attendance.domain.model.AttendanceCorrectionStatus;
import com.academy.mudogroupware.attendance.domain.model.AttendanceCorrectionType;

public record AdminAttendanceCorrectionResponse(
        Long requestId,
        RequesterResponse requester,
        LocalDate workDate,
        AttendanceCorrectionType type,
        AttendanceCorrectionStatus status,
        LocalDateTime originalClockInAt,
        LocalDateTime originalClockOutAt,
        String originalClockInNote,
        String originalClockOutNote,
        LocalDateTime requestedClockInAt,
        LocalDateTime requestedClockOutAt,
        String requestedClockInNote,
        String requestedClockOutNote,
        String reason,
        LocalDateTime requestedAt,
        LocalDateTime processedAt,
        Long processedBy,
        String rejectionReason) {

    public static AdminAttendanceCorrectionResponse from(AdminAttendanceCorrectionView view) {
        var c = view.correction();
        return new AdminAttendanceCorrectionResponse(c.requestId(), RequesterResponse.from(view.requester()),
                c.date(), c.type(), c.status(), c.originalClockInAt(), c.originalClockOutAt(),
                c.originalClockInNote(), c.originalClockOutNote(), c.requestedClockInAt(),
                c.requestedClockOutAt(), c.requestedClockInNote(), c.requestedClockOutNote(),
                c.reason(), c.requestedAt(), c.processedAt(), c.processedBy(), c.rejectionReason());
    }

    public record RequesterResponse(Long userId, String name, String position) {
        static RequesterResponse from(Requester requester) {
            return requester == null ? null : new RequesterResponse(
                    requester.userId(), requester.name(), requester.position());
        }
    }
}

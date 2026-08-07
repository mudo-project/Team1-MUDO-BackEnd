package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.academy.mudogroupware.attendance.domain.model.AttendanceCorrectionStatus;
import com.academy.mudogroupware.attendance.domain.model.AttendanceCorrectionType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "attendance_correction_request")
@Getter @Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AttendanceCorrectionRequestJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "request_id") private Long id;
    @Column(name = "academy_id", nullable = false) private Long academyId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "attendance_id") private Long attendanceId;
    @Column(name = "work_date", nullable = false) private LocalDate workDate;
    @Enumerated(EnumType.STRING) @Column(name = "request_type", nullable = false) private AttendanceCorrectionType type;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false) private AttendanceCorrectionStatus status;
    @Column(name = "original_clock_in_at") private LocalDateTime originalClockInAt;
    @Column(name = "original_clock_out_at") private LocalDateTime originalClockOutAt;
    @Column(name = "original_clock_in_note") private String originalClockInNote;
    @Column(name = "original_clock_out_note") private String originalClockOutNote;
    @Column(name = "requested_clock_in_at") private LocalDateTime requestedClockInAt;
    @Column(name = "requested_clock_out_at") private LocalDateTime requestedClockOutAt;
    @Column(name = "requested_clock_in_note") private String requestedClockInNote;
    @Column(name = "requested_clock_out_note") private String requestedClockOutNote;
    @Column(name = "reason", nullable = false) private String reason;
    @Column(name = "requested_at", nullable = false) private LocalDateTime requestedAt;
    @Column(name = "processed_at") private LocalDateTime processedAt;
    @Column(name = "processed_by") private Long processedBy;
    @Column(name = "rejection_reason") private String rejectionReason;
}

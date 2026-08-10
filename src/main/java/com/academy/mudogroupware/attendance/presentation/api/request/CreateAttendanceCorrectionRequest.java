package com.academy.mudogroupware.attendance.presentation.api.request;
import java.time.LocalDate;
import java.time.LocalTime;
import com.academy.mudogroupware.attendance.application.command.CreateAttendanceCorrectionCommand;
import com.academy.mudogroupware.attendance.domain.model.AttendanceCorrectionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
public record CreateAttendanceCorrectionRequest(
        @NotNull @Schema(example = "2026-08-05") LocalDate date,
        @NotNull @Schema(example = "CLOCK_IN_TIME") AttendanceCorrectionType type,
        @Schema(example = "09:00") LocalTime requestedClockInTime,
        @Schema(example = "18:00") LocalTime requestedClockOutTime,
        @Size(max = 255) String requestedClockInNote,
        @Size(max = 255) String requestedClockOutNote,
        @NotBlank @Size(max = 500) @Schema(example = "출근 버튼을 늦게 눌렀습니다.") String reason) {
    public CreateAttendanceCorrectionCommand toCommand(Long userId) {
        return new CreateAttendanceCorrectionCommand(userId, date, type, requestedClockInTime,
                requestedClockOutTime, requestedClockInNote, requestedClockOutNote, reason);
    }
}

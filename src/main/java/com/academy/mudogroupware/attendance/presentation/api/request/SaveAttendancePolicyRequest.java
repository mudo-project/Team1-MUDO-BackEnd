package com.academy.mudogroupware.attendance.presentation.api.request;

import java.time.LocalTime;
import java.util.List;

import com.academy.mudogroupware.attendance.application.command.SaveAttendancePolicyCommand;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicyWeekday;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaveAttendancePolicyRequest(
        @Schema(description = "기본 출근 시간", example = "09:00:00")
        @NotNull(message = "기본 출근 시간은 필수입니다.")
        LocalTime defaultStartTime,

        @Schema(description = "기본 퇴근 시간", example = "18:00:00")
        @NotNull(message = "기본 퇴근 시간은 필수입니다.")
        LocalTime defaultEndTime,

        @Schema(description = "지각 유예 시간(분)", example = "10", minimum = "0", maximum = "180")
        @Min(value = 0, message = "지각 유예 시간은 0분 이상이어야 합니다.")
        @Max(value = 180, message = "지각 유예 시간은 180분 이하여야 합니다.")
        int lateGraceMinutes,

        @Schema(description = "요일별 근무 설정 사용 여부", example = "true")
        @NotNull(message = "요일별 설정 활성화 여부는 필수입니다.")
        Boolean weekdayExceptionEnabled,

        @ArraySchema(
                arraySchema = @Schema(description = "요일별 근무 설정"),
                schema = @Schema(implementation = WeekdayRequest.class))
        @Valid
        List<@NotNull WeekdayRequest> weekdays
) {
    public SaveAttendancePolicyCommand toCommand(Long requesterId) {
        List<AttendancePolicyWeekday> weekdayCommands = weekdays == null ? null
                : weekdays.stream().map(WeekdayRequest::toDomain).toList();
        return new SaveAttendancePolicyCommand(
                requesterId, defaultStartTime, defaultEndTime, lateGraceMinutes,
                weekdayExceptionEnabled, weekdayCommands);
    }

    public record WeekdayRequest(
            @Schema(description = "요일(1: 월요일, 7: 일요일)", example = "1")
            @NotNull(message = "요일은 필수입니다.")
            @Min(value = 1, message = "요일은 1 이상이어야 합니다.")
            @Max(value = 7, message = "요일은 7 이하여야 합니다.")
            Integer dayOfWeek,

            @Schema(description = "근무일 여부", example = "true")
            @NotNull(message = "근무일 여부는 필수입니다.")
            Boolean isWorkday,

            @Schema(description = "요일별 출근 시간. 미입력 시 기본 출근 시간을 사용합니다.", example = "10:00:00")
            LocalTime startTime,

            @Schema(description = "요일별 퇴근 시간. 미입력 시 기본 퇴근 시간을 사용합니다.", example = "19:00:00")
            LocalTime endTime
    ) {
        private AttendancePolicyWeekday toDomain() {
            return new AttendancePolicyWeekday(
                    dayOfWeek, isWorkday, startTime, endTime);
        }
    }
}

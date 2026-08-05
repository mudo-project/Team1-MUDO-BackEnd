package com.academy.mudogroupware.attendance.presentation.api.request;

import java.time.LocalTime;
import java.util.List;

import com.academy.mudogroupware.attendance.application.command.SaveAttendancePolicyCommand;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicyWeekday;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaveAttendancePolicyRequest(
        @NotNull(message = "기본 출근 시간은 필수입니다.")
        LocalTime defaultStartTime,

        @NotNull(message = "기본 퇴근 시간은 필수입니다.")
        LocalTime defaultEndTime,

        @Min(value = 0, message = "지각 유예 시간은 0분 이상이어야 합니다.")
        @Max(value = 180, message = "지각 유예 시간은 180분 이하여야 합니다.")
        int lateGraceMinutes,

        @NotNull(message = "요일별 설정 활성화 여부는 필수입니다.")
        Boolean weekdayExceptionEnabled,

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
            @NotNull(message = "요일은 필수입니다.")
            @Min(value = 1, message = "요일은 1 이상이어야 합니다.")
            @Max(value = 7, message = "요일은 7 이하여야 합니다.")
            Integer dayOfWeek,

            @NotNull(message = "근무일 여부는 필수입니다.")
            Boolean isWorkday,

            LocalTime startTime,
            LocalTime endTime
    ) {
        private AttendancePolicyWeekday toDomain() {
            return new AttendancePolicyWeekday(
                    dayOfWeek, isWorkday, startTime, endTime);
        }
    }
}

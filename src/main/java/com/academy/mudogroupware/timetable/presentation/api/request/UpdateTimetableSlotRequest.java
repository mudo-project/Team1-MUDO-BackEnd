package com.academy.mudogroupware.timetable.presentation.api.request;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.academy.mudogroupware.timetable.application.command.UpdateTimetableSlotCommand;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.Grade;
import com.academy.mudogroupware.timetable.domain.model.UpdateScope;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateTimetableSlotRequest(
        @Schema(description = "적용 범위. 현재는 ALL만 지원") @NotNull UpdateScope scope,
        @Schema(description = "수업 종류") @NotNull ClassType classType,
        @Schema(description = "요일") @NotNull DayOfWeek dayOfWeek,
        @Schema(description = "강의실 코드") @NotBlank String classroomCode,
        @Schema(description = "시작 시각") @NotNull LocalTime startTime,
        @Schema(description = "종료 시각") @NotNull LocalTime endTime,
        @Schema(description = "학년(초1~고3 또는 공통 중 하나)") @NotNull Grade grade,
        @Schema(description = "강사명") String teacherName,
        @Schema(description = "과목") String subjectName,
        @Schema(description = "색상(6자리 16진수, RRGGBB)", example = "FFCC00")
        @NotBlank @Pattern(regexp = "^[0-9A-Fa-f]{6}$") String color
) {

    public UpdateTimetableSlotCommand toCommand(Long timetableSetId, Long timetableSlotId) {
        return new UpdateTimetableSlotCommand(timetableSetId, timetableSlotId, scope, classType,
                dayOfWeek, classroomCode, startTime, endTime, grade, teacherName, subjectName, color);
    }
}

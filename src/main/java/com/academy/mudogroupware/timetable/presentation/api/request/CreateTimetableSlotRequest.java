package com.academy.mudogroupware.timetable.presentation.api.request;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.academy.mudogroupware.timetable.application.command.CreateTimetableSlotCommand;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.Grade;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTimetableSlotRequest(
        @Schema(description = "수업 종류") @NotNull ClassType classType,
        @Schema(description = "요일") @NotNull DayOfWeek dayOfWeek,
        @Schema(description = "강의실 코드", example = "601") @NotBlank String classroomCode,
        @Schema(description = "시작 시각", example = "09:00") @NotNull LocalTime startTime,
        @Schema(description = "종료 시각", example = "11:00") @NotNull LocalTime endTime,
        @Schema(description = "학년(초1~고3 중 하나)", example = "HIGH_3") @NotNull Grade grade,
        @Schema(description = "강사명", example = "정T") String teacherName,
        @Schema(description = "과목", example = "미적분") String subjectName
) {

    public CreateTimetableSlotCommand toCommand(Long academyId, Long timetableSetId) {
        return new CreateTimetableSlotCommand(academyId, timetableSetId, classType, dayOfWeek, classroomCode,
                startTime, endTime, grade, teacherName, subjectName);
    }
}

package com.academy.mudogroupware.lecture.presentation.api.request;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.academy.mudogroupware.lecture.application.command.ScheduleInput;
import com.academy.mudogroupware.lecture.application.command.UpdateLectureCommand;
import com.academy.mudogroupware.lecture.domain.model.ClassType;
import com.academy.mudogroupware.lecture.domain.model.FeeType;
import com.academy.mudogroupware.lecture.domain.model.Grade;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateLectureRequest(
        @NotBlank String name,
        @NotNull ClassType classType,
        DayOfWeek dayOfWeek,
        @NotBlank String classroomCode,
        LocalTime startTime,
        LocalTime endTime,
        Grade grade,
        String teacherName,
        String subjectName,
        String termName,
        FeeType feeType,
        Integer feeAmount,
        List<@NotNull @Valid ScheduleRequest> schedules
) {

    public UpdateLectureCommand toCommand(Long lectureId, Long requesterId) {
        return new UpdateLectureCommand(lectureId, name, classType, classroomCode, grade, teacherName,
                subjectName, termName, feeType, feeAmount, toScheduleInputs(), requesterId, null);
    }

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "schedules 또는 dayOfWeek/startTime/endTime 입력이 필요합니다.")
    public boolean isScheduleInputPresent() {
        return hasSchedules() || hasLegacySchedule();
    }

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "dayOfWeek, startTime, endTime은 함께 입력해야 합니다.")
    public boolean isLegacyScheduleComplete() {
        boolean hasAnyLegacyField = dayOfWeek != null || startTime != null || endTime != null;
        return !hasAnyLegacyField || hasLegacySchedule();
    }

    private List<ScheduleInput> toScheduleInputs() {
        if (hasSchedules()) {
            return schedules.stream().map(ScheduleRequest::toInput).toList();
        }
        return List.of(new ScheduleInput(dayOfWeek, startTime, endTime));
    }

    private boolean hasSchedules() {
        return schedules != null && !schedules.isEmpty();
    }

    private boolean hasLegacySchedule() {
        return dayOfWeek != null && startTime != null && endTime != null;
    }
}

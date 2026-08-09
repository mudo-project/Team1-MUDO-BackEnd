package com.academy.mudogroupware.timetable.presentation.api.request;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import com.academy.mudogroupware.timetable.application.command.CreateTimetableSetCommand;
import com.academy.mudogroupware.timetable.domain.model.TimetableClassroom;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateTimetableSetRequest(
        @Schema(description = "시간표 세트 이름", example = "2026 여름특강") @NotBlank String name,
        @Schema(description = "시작일", example = "2026-07-20") @NotNull LocalDate startDate,
        @Schema(description = "종료일", example = "2026-08-16") @NotNull LocalDate endDate,
        @Schema(description = "운영 시작 시각", example = "08:30") @NotNull LocalTime operatingStartTime,
        @Schema(description = "운영 종료 시각", example = "22:00") @NotNull LocalTime operatingEndTime,
        @Schema(description = "운영 요일", example = "[\"MONDAY\",\"WEDNESDAY\"]") @NotEmpty Set<DayOfWeek> operatingDays,
        @Schema(description = "슬롯 단위(분)", example = "30") @Positive int slotUnitMinutes,
        @Schema(description = "층별 강의실 구성") @NotEmpty List<ClassroomGroupRequest> classrooms
) {

    public CreateTimetableSetCommand toCommand(Long academyId) {
        List<TimetableClassroom> flattened = classrooms.stream()
                .flatMap(group -> group.codes().stream().map(code -> new TimetableClassroom(group.floor(), code)))
                .toList();
        return new CreateTimetableSetCommand(academyId, name, startDate, endDate, operatingStartTime,
                operatingEndTime, operatingDays, slotUnitMinutes, flattened);
    }
}

package com.academy.mudogroupware.timetable.presentation.api.response;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import com.academy.mudogroupware.timetable.application.query.TimetableSetDetailView;
import com.academy.mudogroupware.timetable.domain.model.TimetableSetStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record TimetableSetDetailResponse(
        @Schema(description = "시간표 세트 번호") Long timetableSetId,
        @Schema(description = "이름") String name,
        @Schema(description = "시작일") LocalDate startDate,
        @Schema(description = "종료일") LocalDate endDate,
        @Schema(description = "운영 시작 시각") LocalTime operatingStartTime,
        @Schema(description = "운영 종료 시각") LocalTime operatingEndTime,
        @Schema(description = "운영 요일") Set<DayOfWeek> operatingDays,
        @Schema(description = "슬롯 단위(분)") int slotUnitMinutes,
        @Schema(description = "층별 강의실 구성") List<ClassroomGroupResponse> classrooms,
        @Schema(description = "상태(PLANNED/ACTIVE/ENDED)") TimetableSetStatus status
) {

    public static TimetableSetDetailResponse from(TimetableSetDetailView view) {
        return new TimetableSetDetailResponse(
                view.timetableSetId(), view.name(), view.startDate(), view.endDate(),
                view.operatingStartTime(), view.operatingEndTime(), view.operatingDays(), view.slotUnitMinutes(),
                ClassroomGroupResponse.fromGroupedByFloor(view.classrooms()), view.status());
    }
}

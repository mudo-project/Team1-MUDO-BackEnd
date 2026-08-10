package com.academy.mudogroupware.timetable.presentation.api.response;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.Grade;

import io.swagger.v3.oas.annotations.media.Schema;

public record TimetableSlotResponse(
        @Schema(description = "수업 슬롯 번호") Long timetableSlotId,
        @Schema(description = "수업 종류") ClassType classType,
        @Schema(description = "요일") DayOfWeek dayOfWeek,
        @Schema(description = "강의실 코드") String classroomCode,
        @Schema(description = "시작 시각") LocalTime startTime,
        @Schema(description = "종료 시각") LocalTime endTime,
        @Schema(description = "학년(초1~고3 중 하나)") Grade grade,
        @Schema(description = "강사명") String teacherName,
        @Schema(description = "과목") String subjectName
) {

    public static TimetableSlotResponse from(TimetableSlotView view) {
        return new TimetableSlotResponse(
                view.timetableSlotId(), view.classType(), view.dayOfWeek(), view.classroomCode(),
                view.startTime(), view.endTime(), view.grade(), view.teacherName(), view.subjectName());
    }
}

package com.academy.mudogroupware.timetable.infrastructure.export;

import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;

import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;
import com.academy.mudogroupware.timetable.domain.model.ClassType;

final class TimetableExportLabels {

    static final String[] HEADERS = {"요일", "시간", "강의실", "수업종류", "강사", "과목", "학년"};

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private TimetableExportLabels() {
    }

    static String dayOfWeek(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };
    }

    static String classType(ClassType classType) {
        return switch (classType) {
            case CLASS -> "수업";
            case SPECIAL -> "특강";
            case CLINIC -> "클리닉";
            case STANDING -> "상시";
            case EXAM -> "시험";
        };
    }

    static String[] toRow(TimetableSlotView slot) {
        return new String[] {
                dayOfWeek(slot.dayOfWeek()),
                slot.startTime().format(TIME_FORMAT) + "~" + slot.endTime().format(TIME_FORMAT),
                slot.classroomCode(),
                classType(slot.classType()),
                slot.teacherName() != null ? slot.teacherName() : "",
                slot.subjectName() != null ? slot.subjectName() : "",
                slot.grade().label()
        };
    }
}

package com.academy.mudogroupware.timetable.application.query;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.academy.mudogroupware.timetable.domain.model.ClassType;

public record TimetableSlotView(
        Long timetableSlotId, ClassType classType, DayOfWeek dayOfWeek, String classroomCode,
        LocalTime startTime, LocalTime endTime, String grade, String teacherName, String subjectName) {
}

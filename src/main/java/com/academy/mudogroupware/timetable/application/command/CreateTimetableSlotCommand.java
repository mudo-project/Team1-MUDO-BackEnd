package com.academy.mudogroupware.timetable.application.command;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.academy.mudogroupware.timetable.domain.model.ClassType;

public record CreateTimetableSlotCommand(
        Long academyId, Long timetableSetId, ClassType classType, DayOfWeek dayOfWeek, String classroomCode,
        LocalTime startTime, LocalTime endTime, String grade, String teacherName, String subjectName) {
}

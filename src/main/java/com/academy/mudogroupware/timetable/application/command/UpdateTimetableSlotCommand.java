package com.academy.mudogroupware.timetable.application.command;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.UpdateScope;

public record UpdateTimetableSlotCommand(
        Long timetableSetId, Long timetableSlotId, UpdateScope scope, ClassType classType, DayOfWeek dayOfWeek,
        String classroomCode, LocalTime startTime, LocalTime endTime, String grade, String teacherName,
        String subjectName) {
}

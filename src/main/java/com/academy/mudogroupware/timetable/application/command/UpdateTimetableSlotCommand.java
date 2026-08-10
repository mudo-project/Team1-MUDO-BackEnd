package com.academy.mudogroupware.timetable.application.command;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.Grade;
import com.academy.mudogroupware.timetable.domain.model.UpdateScope;

public record UpdateTimetableSlotCommand(
        Long academyId, Long timetableSetId, Long timetableSlotId, UpdateScope scope, ClassType classType,
        DayOfWeek dayOfWeek, String classroomCode, LocalTime startTime, LocalTime endTime, Grade grade,
        String teacherName, String subjectName) {
}

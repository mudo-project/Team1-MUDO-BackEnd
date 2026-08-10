package com.academy.mudogroupware.timetable.domain.model;

import com.academy.mudogroupware.timetable.domain.exception.InvalidTimetableClassroomException;

public record TimetableClassroom(String floor, String code) {

    public TimetableClassroom {
        if (floor == null || floor.isBlank()) {
            throw new InvalidTimetableClassroomException("floor");
        }
        if (code == null || code.isBlank()) {
            throw new InvalidTimetableClassroomException("code");
        }
    }
}

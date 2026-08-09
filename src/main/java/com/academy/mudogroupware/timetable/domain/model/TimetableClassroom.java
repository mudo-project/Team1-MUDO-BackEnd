package com.academy.mudogroupware.timetable.domain.model;

public record TimetableClassroom(String floor, String code) {

    public TimetableClassroom {
        if (floor == null || floor.isBlank()) {
            throw new IllegalArgumentException("floor must not be blank");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
    }
}

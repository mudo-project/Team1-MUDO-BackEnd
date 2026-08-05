package com.academy.mudogroupware.lecture.application.command;

public record EnrollStudentCommand(
        Long lectureId,
        Long studentId
) {
}

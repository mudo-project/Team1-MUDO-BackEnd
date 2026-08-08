package com.academy.mudogroupware.student.application.command;

public record DeleteStudentCommand(
        Long academyId,
        Long studentId
) {
}

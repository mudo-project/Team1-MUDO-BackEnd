package com.academy.mudogroupware.student.application.command;

public record EnrollStudentCommand(Long academyId, Long studentId, Long lectureId) {
}

package com.academy.mudogroupware.student.application.command;

public record EndEnrollmentCommand(Long academyId, Long studentId, Long enrollmentId) {
}

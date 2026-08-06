package com.academy.mudogroupware.student.application.query;

import java.time.LocalDateTime;
import java.util.List;

import com.academy.mudogroupware.student.domain.model.StudentGrade;

public record StudentDetail(
        Long id,
        String name,
        StudentGrade grade,
        String school,
        String phone,
        String parentPhone,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<EnrollmentSummary> enrollments
) {
}

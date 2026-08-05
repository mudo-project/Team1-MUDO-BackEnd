package com.academy.mudogroupware.student.application.query;

import java.time.LocalDateTime;

public record EnrollmentSummary(
        Long enrollmentId,
        Long lectureId,
        String lectureName,
        String teacherName,
        String scheduleText,
        String priceType,
        Integer priceAmount,
        LocalDateTime enrolledAt
) {
}

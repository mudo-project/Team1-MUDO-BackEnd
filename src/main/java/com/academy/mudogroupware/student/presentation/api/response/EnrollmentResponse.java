package com.academy.mudogroupware.student.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.student.application.query.EnrollmentSummary;

import io.swagger.v3.oas.annotations.media.Schema;

public record EnrollmentResponse(
        @Schema(description = "수강 등록 ID", example = "1")
        Long enrollmentId,

        @Schema(description = "강의 ID", example = "100")
        Long lectureId,

        @Schema(description = "강의명", example = "고1 수학")
        String lectureName,

        @Schema(description = "선생님명", example = "박선생")
        String teacherName,

        @Schema(description = "강의 시간", example = "월 19:00")
        String scheduleText,

        @Schema(description = "가격 방식", example = "MONTHLY")
        String priceType,

        @Schema(description = "가격", example = "300000")
        Integer priceAmount,

        @Schema(description = "수강 등록 시각", example = "2026-08-05T10:00:00")
        LocalDateTime enrolledAt
) {

    public static EnrollmentResponse from(EnrollmentSummary summary) {
        return new EnrollmentResponse(summary.enrollmentId(), summary.lectureId(), summary.lectureName(),
                summary.teacherName(), summary.scheduleText(), summary.priceType(), summary.priceAmount(),
                summary.enrolledAt());
    }
}

package com.academy.mudogroupware.student.presentation.api.response;

import com.academy.mudogroupware.student.application.query.StudentSummary;
import com.academy.mudogroupware.student.domain.model.StudentGrade;

import io.swagger.v3.oas.annotations.media.Schema;

public record StudentSummaryResponse(
        @Schema(description = "학생 ID", example = "1")
        Long studentId,

        @Schema(description = "학생 이름", example = "김민수")
        String name,

        @Schema(description = "학생 학년", example = "HIGH_1")
        StudentGrade grade,

        @Schema(description = "학교", example = "무도고")
        String school,

        @Schema(description = "학생 전화번호", example = "010-1111-2222")
        String phone,

        @Schema(description = "학부모 전화번호", example = "010-3333-4444")
        String parentPhone,

        @Schema(description = "현재 수강 중인 강의 수", example = "2")
        int activeEnrollmentCount
) {

    public static StudentSummaryResponse from(StudentSummary summary) {
        return new StudentSummaryResponse(summary.id(), summary.name(), summary.grade(), summary.school(),
                summary.phone(), summary.parentPhone(), summary.activeEnrollmentCount());
    }
}

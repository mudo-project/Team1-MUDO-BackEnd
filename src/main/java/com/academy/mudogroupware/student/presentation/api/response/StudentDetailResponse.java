package com.academy.mudogroupware.student.presentation.api.response;

import java.time.LocalDateTime;
import java.util.List;

import com.academy.mudogroupware.student.application.query.StudentDetail;
import com.academy.mudogroupware.student.domain.model.StudentGrade;

import io.swagger.v3.oas.annotations.media.Schema;

public record StudentDetailResponse(
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

        @Schema(description = "특이사항", example = "수학 선행 중")
        String note,

        @Schema(description = "생성 시각", example = "2026-08-05T10:00:00")
        LocalDateTime createdAt,

        @Schema(description = "수정 시각", example = "2026-08-05T10:00:00")
        LocalDateTime updatedAt,

        @Schema(description = "현재 수강 중인 강의 목록")
        List<EnrollmentResponse> enrollments
) {

    public static StudentDetailResponse from(StudentDetail detail) {
        return new StudentDetailResponse(detail.id(), detail.name(), detail.grade(), detail.school(), detail.phone(),
                detail.parentPhone(), detail.note(), detail.createdAt(), detail.updatedAt(),
                detail.enrollments().stream().map(EnrollmentResponse::from).toList());
    }
}

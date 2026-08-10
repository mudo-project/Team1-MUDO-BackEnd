package com.academy.mudogroupware.student.presentation.api.request;

import com.academy.mudogroupware.student.application.command.CreateStudentCommand;
import com.academy.mudogroupware.student.domain.model.StudentGrade;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateStudentRequest(
        @Schema(description = "학생 이름", example = "김민수")
        @NotBlank
        @Size(max = 50)
        String name,

        @Schema(description = "학생 학년", example = "HIGH_1")
        @NotNull
        StudentGrade grade,

        @Schema(description = "학교", example = "무도고")
        @Size(max = 100)
        String school,

        @Schema(description = "학생 전화번호", example = "010-1111-2222")
        @Size(max = 30)
        String phone,

        @Schema(description = "학부모 전화번호", example = "010-3333-4444")
        @Size(max = 30)
        String parentPhone,

        @Schema(description = "특이사항", example = "수학 선행 중")
        @Size(max = 500)
        String note
) {

    public CreateStudentCommand toCommand(Long academyId) {
        return new CreateStudentCommand(academyId, name, grade, school, phone, parentPhone, note);
    }
}

package com.academy.mudogroupware.users.presentation.api.request;

import com.academy.mudogroupware.users.application.command.SubmitAcademyApplicationCommand;
import com.academy.mudogroupware.users.domain.model.Plan;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitAcademyApplicationRequest(
        @Schema(description = "원장이 요청하는 로그인 아이디", example = "academy01")
        @NotBlank @Size(max = 50) String requestedLoginId,

        @Schema(description = "학원명", example = "우리학원")
        @NotBlank @Size(max = 100) String academyName,

        @Schema(description = "대표자(원장) 이름", example = "홍길동")
        @NotBlank @Size(max = 50) String representativeName,

        @Schema(description = "대표자 이메일", example = "hong@example.com")
        @NotBlank @Email @Size(max = 100) String representativeEmail,

        @Schema(description = "대표자 전화번호", example = "010-0000-0000")
        @NotBlank @Size(max = 20) String representativePhone,

        @Schema(description = "신청 플랜(FREE/PAID)", example = "FREE")
        @NotNull Plan plan) {

    public SubmitAcademyApplicationCommand toCommand() {
        return new SubmitAcademyApplicationCommand(
                requestedLoginId, academyName, representativeName, representativeEmail, representativePhone, plan);
    }
}

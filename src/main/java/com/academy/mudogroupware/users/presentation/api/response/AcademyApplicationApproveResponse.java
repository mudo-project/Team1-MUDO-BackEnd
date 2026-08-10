package com.academy.mudogroupware.users.presentation.api.response;

import com.academy.mudogroupware.users.application.result.ApproveAcademyApplicationResult;

import io.swagger.v3.oas.annotations.media.Schema;

public record AcademyApplicationApproveResponse(
        @Schema(description = "새로 생성된 학원 ID", example = "10") Long academyId,
        @Schema(description = "새로 생성된 최초 관리자(원장) 계정의 사용자 ID", example = "20") Long userId,
        @Schema(description = "비밀번호 설정 링크. 발급자가 계정 주인에게 수동으로 전달한다",
                example = "http://localhost:3000/password-setup?username=academy01&tempPassword=Xk9%23mQ2pRt7") String passwordSetupLink) {

    public static AcademyApplicationApproveResponse from(ApproveAcademyApplicationResult result) {
        return new AcademyApplicationApproveResponse(result.academyId(), result.userId(), result.passwordSetupLink());
    }
}

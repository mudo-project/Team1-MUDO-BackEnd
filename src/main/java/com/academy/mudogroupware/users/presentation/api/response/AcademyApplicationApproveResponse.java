package com.academy.mudogroupware.users.presentation.api.response;

import com.academy.mudogroupware.users.application.result.ApproveAcademyApplicationResult;

import io.swagger.v3.oas.annotations.media.Schema;

public record AcademyApplicationApproveResponse(
        @Schema(description = "새로 생성된 학원 ID", example = "10") Long academyId,
        @Schema(description = "새로 생성된 최초 관리자(원장) 계정의 사용자 ID", example = "20") Long userId,
        @Schema(description = "발급된 임시 비밀번호. 이 응답에서 평문으로 1회만 내려가며 서버에 저장되지 않음",
                example = "Xk9#mQ2pRt7$") String temporaryPassword) {

    public static AcademyApplicationApproveResponse from(ApproveAcademyApplicationResult result) {
        return new AcademyApplicationApproveResponse(result.academyId(), result.userId(), result.temporaryPassword());
    }
}

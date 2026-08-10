package com.academy.mudogroupware.users.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.users.domain.model.AcademyApplication;

import io.swagger.v3.oas.annotations.media.Schema;

public record AcademyApplicationResponse(
        @Schema(description = "신청서 ID", example = "1") Long applicationId,
        @Schema(description = "신청 시 원장이 요청한 로그인 아이디", example = "academy01") String requestedLoginId,
        @Schema(description = "학원명", example = "우리학원") String academyName,
        @Schema(description = "사업자등록번호", example = "123-45-67890") String businessNo,
        @Schema(description = "대표자(원장) 이름", example = "홍길동") String representativeName,
        @Schema(description = "대표자 이메일", example = "hong@example.com") String representativeEmail,
        @Schema(description = "대표자 전화번호", example = "010-0000-0000") String representativePhone,
        @Schema(description = "신청 상태(PENDING/APPROVED/REJECTED)", example = "PENDING") String status,
        @Schema(description = "반려 사유. 반려되지 않았으면 null", example = "null") String rejectReason,
        @Schema(description = "신청 접수 시각") LocalDateTime createdAt) {

    public static AcademyApplicationResponse from(AcademyApplication application) {
        return new AcademyApplicationResponse(application.getId(), application.getRequestedLoginId(),
                application.getAcademyName(), application.getBusinessNo(), application.getRepresentativeName(),
                application.getRepresentativeEmail(), application.getRepresentativePhone(),
                application.getStatus().name(), application.getRejectReason(), application.getCreatedAt());
    }
}

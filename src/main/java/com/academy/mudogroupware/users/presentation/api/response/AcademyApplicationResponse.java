package com.academy.mudogroupware.users.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.users.domain.model.AcademyApplication;

public record AcademyApplicationResponse(
        Long applicationId, String requestedLoginId, String academyName, String businessNo,
        String representativeName, String representativeEmail, String representativePhone,
        String status, String rejectReason, LocalDateTime createdAt) {

    public static AcademyApplicationResponse from(AcademyApplication application) {
        return new AcademyApplicationResponse(application.getId(), application.getRequestedLoginId(),
                application.getAcademyName(), application.getBusinessNo(), application.getRepresentativeName(),
                application.getRepresentativeEmail(), application.getRepresentativePhone(),
                application.getStatus().name(), application.getRejectReason(), application.getCreatedAt());
    }
}

package com.academy.mudogroupware.users.presentation.api.response;

import com.academy.mudogroupware.users.application.result.ApproveAcademyApplicationResult;

public record AcademyApplicationApproveResponse(Long academyId, Long userId, String temporaryPassword) {

    public static AcademyApplicationApproveResponse from(ApproveAcademyApplicationResult result) {
        return new AcademyApplicationApproveResponse(result.academyId(), result.userId(), result.temporaryPassword());
    }
}

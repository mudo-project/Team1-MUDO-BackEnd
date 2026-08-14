package com.academy.mudogroupware.corporatecard.presentation.api.response;

import com.academy.mudogroupware.corporatecard.application.query.ApprovalLineView;

public record ApprovalLineResponse(
        Long approverId,
        String approverName,
        String positionName,
        int stepOrder) {

    public static ApprovalLineResponse from(ApprovalLineView view) {
        return new ApprovalLineResponse(
                view.approverId(), view.approverName(), view.positionName(), view.stepOrder());
    }
}

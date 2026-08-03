package com.academy.mudogroupware.approval.presentation.api.response;

public record ApprovalPendingCountResponse(
        long count
) {

    public static ApprovalPendingCountResponse from(long count) {
        return new ApprovalPendingCountResponse(count);
    }
}

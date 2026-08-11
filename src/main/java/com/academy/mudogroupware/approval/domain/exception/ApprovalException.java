package com.academy.mudogroupware.approval.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BusinessException;

public class ApprovalException extends BusinessException {

    public ApprovalException(ApprovalErrorCode errorCode) {
        super(errorCode);
    }

    public ApprovalException(ApprovalErrorCode errorCode, Throwable cause) {
        super(errorCode, errorCode.getMessage(), cause);
    }
}

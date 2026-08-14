package com.academy.mudogroupware.approval.domain.exception;

import java.util.Map;

import com.academy.mudogroupware.global.domain.common.exception.BusinessException;

public class ApprovalException extends BusinessException {

    public ApprovalException(ApprovalErrorCode errorCode) {
        super(errorCode);
    }

    public ApprovalException(ApprovalErrorCode errorCode, Throwable cause) {
        super(errorCode, errorCode.getMessage(), cause);
    }

    public ApprovalException(ApprovalErrorCode errorCode, Throwable cause, Map<String, Object> context) {
        super(errorCode, errorCode.getMessage(), cause, context);
    }
}

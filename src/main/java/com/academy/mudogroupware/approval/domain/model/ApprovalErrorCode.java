package com.academy.mudogroupware.approval.domain.model;

import org.springframework.http.HttpStatus;

import com.academy.mudogroupware.global.error.ErrorCode;

public enum ApprovalErrorCode implements ErrorCode {

    INVALID_TITLE(HttpStatus.BAD_REQUEST, "APPROVAL-400-1", "결재 제목은 비어 있을 수 없습니다."),
    INVALID_CONTENT(HttpStatus.BAD_REQUEST, "APPROVAL-400-2", "결재 내용이 올바르지 않습니다."),
    NO_APPROVAL_LINES(HttpStatus.BAD_REQUEST, "APPROVAL-400-3", "결재선은 최소 1명 이상 지정해야 합니다."),
    TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "APPROVAL-404-1", "결재 문서를 찾을 수 없습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "APPROVAL-403-1", "해당 결재를 조회할 권한이 없습니다."),
    NOT_YOUR_TURN(HttpStatus.CONFLICT, "APPROVAL-409-1", "본인 차례의 결재가 아닙니다."),
    ALREADY_DECIDED(HttpStatus.CONFLICT, "APPROVAL-409-2", "이미 처리가 완료된 결재입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ApprovalErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

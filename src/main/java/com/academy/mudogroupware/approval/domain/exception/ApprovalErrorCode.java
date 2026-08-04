package com.academy.mudogroupware.approval.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ApprovalErrorCode implements ErrorCode {

    INVALID_CONTENT(HttpStatus.BAD_REQUEST, "APPROVAL_400_1", "결재 내용(텍스트)이 올바르지 않습니다."),
    TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "APPROVAL_400_2", "결재 제목은 비어 있을 수 없습니다."),
    LINES_REQUIRED(HttpStatus.BAD_REQUEST, "APPROVAL_400_3", "결재선은 최소 1명 이상 지정해야 합니다."),
    TEMPLATE_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "APPROVAL_400_4", "템플릿 이름은 비어 있을 수 없습니다."),

    DOCUMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "APPROVAL_403_1", "해당 결재를 조회할 권한이 없습니다."),
    CROSS_ACADEMY_TEMPLATE(HttpStatus.FORBIDDEN, "APPROVAL_403_2", "다른 학원의 템플릿으로는 결재를 신청할 수 없습니다."),
    NOT_DOCUMENT_OWNER_RESUBMIT(HttpStatus.FORBIDDEN, "APPROVAL_403_3", "본인이 신청한 결재만 재상신할 수 있습니다."),
    NOT_DOCUMENT_OWNER_LINES(HttpStatus.FORBIDDEN, "APPROVAL_403_4", "본인이 신청한 결재만 결재선을 수정할 수 있습니다."),

    TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "APPROVAL_404_1", "결재 템플릿을 찾을 수 없습니다."),
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "APPROVAL_404_2", "결재 문서를 찾을 수 없습니다."),
    APPROVER_NOT_FOUND(HttpStatus.NOT_FOUND, "APPROVAL_404_3", "사용자를 찾을 수 없습니다."),

    RESUBMIT_NOT_REJECTED(HttpStatus.CONFLICT, "APPROVAL_409_1", "반려된 결재만 재상신할 수 있습니다."),
    ALREADY_RESUBMITTED(HttpStatus.CONFLICT, "APPROVAL_409_2", "이미 재상신된 결재입니다."),
    DOCUMENT_ALREADY_DECIDED(HttpStatus.CONFLICT, "APPROVAL_409_3", "이미 처리가 완료된 결재입니다."),
    NOT_YOUR_TURN(HttpStatus.CONFLICT, "APPROVAL_409_4", "본인 차례의 결재가 아닙니다."),
    LINES_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "APPROVAL_409_5", "이미 결재가 진행된 건은 결재선을 수정할 수 없습니다."),
    LINE_ALREADY_DECIDED(HttpStatus.CONFLICT, "APPROVAL_409_6", "이미 처리가 완료된 결재선입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

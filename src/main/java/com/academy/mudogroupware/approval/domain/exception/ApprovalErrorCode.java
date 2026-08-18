package com.academy.mudogroupware.approval.domain.exception;

import org.springframework.http.HttpStatus;

import com.academy.mudogroupware.global.domain.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApprovalErrorCode implements ErrorCode {

    INVALID_CONTENT(HttpStatus.BAD_REQUEST, "APPROVAL_400_1", "결재 내용 형식이 올바르지 않습니다."),
    TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "APPROVAL_400_2", "결재 제목은 비어 있을 수 없습니다."),
    LINES_REQUIRED(HttpStatus.BAD_REQUEST, "APPROVAL_400_3", "결재선은 최소 1명 이상 지정해야 합니다."),
    TEMPLATE_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "APPROVAL_400_4", "템플릿 이름은 비어 있을 수 없습니다."),
    INVALID_LEAVE_PERIOD(HttpStatus.BAD_REQUEST, "APPROVAL_400_5",
            "휴가 기간은 시작일과 종료일을 함께 입력해야 하며, 종료일은 시작일보다 빠를 수 없습니다."),
    APPROVER_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "APPROVAL_400_6", "신청자는 본인을 결재자로 지정할 수 없습니다."),
    DUPLICATE_APPROVER(HttpStatus.BAD_REQUEST, "APPROVAL_400_7", "같은 결재자를 결재선에 중복 지정할 수 없습니다."),

    DOCUMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "APPROVAL_403_1", "해당 결재를 조회할 권한이 없습니다."),
    NOT_DOCUMENT_OWNER_RESUBMIT(HttpStatus.FORBIDDEN, "APPROVAL_403_3", "본인이 신청한 결재만 재상신할 수 있습니다."),
    NOT_DOCUMENT_OWNER_LINES(HttpStatus.FORBIDDEN, "APPROVAL_403_4", "본인이 신청한 결재만 결재선을 수정할 수 있습니다."),
    NOT_DOCUMENT_OWNER_CANCEL(HttpStatus.FORBIDDEN, "APPROVAL_403_7", "본인이 신청한 결재만 취소할 수 있습니다."),

    TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "APPROVAL_404_1", "결재 템플릿을 찾을 수 없습니다."),
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "APPROVAL_404_2", "결재 문서를 찾을 수 없습니다."),
    APPROVER_NOT_FOUND(HttpStatus.NOT_FOUND, "APPROVAL_404_3", "사용자를 찾을 수 없습니다."),
    ATTACHMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "APPROVAL_404_4", "첨부파일을 찾을 수 없습니다."),

    RESUBMIT_NOT_REJECTED(HttpStatus.CONFLICT, "APPROVAL_409_1", "반려된 결재만 재상신할 수 있습니다."),
    ALREADY_RESUBMITTED(HttpStatus.CONFLICT, "APPROVAL_409_2", "이미 재상신된 결재입니다."),
    DOCUMENT_ALREADY_DECIDED(HttpStatus.CONFLICT, "APPROVAL_409_3", "이미 처리가 완료된 결재입니다."),
    NOT_YOUR_TURN(HttpStatus.CONFLICT, "APPROVAL_409_4", "본인 차례의 결재가 아닙니다."),
    LINES_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "APPROVAL_409_5", "이미 결재가 진행된 건이라 결재선을 수정할 수 없습니다."),
    LINE_ALREADY_DECIDED(HttpStatus.CONFLICT, "APPROVAL_409_6", "이미 처리가 완료된 결재선입니다."),
    ATTACHMENT_CONTENT_UNAVAILABLE(HttpStatus.CONFLICT, "APPROVAL_409_7", "첨부파일 원문 조회 기능이 없어 요약할 수 없습니다."),
    CANCEL_NOT_ALLOWED(HttpStatus.CONFLICT, "APPROVAL_409_8", "이미 처리가 시작된 결재는 취소할 수 없습니다."),
    HISTORY_HIDE_NOT_ALLOWED(HttpStatus.CONFLICT, "APPROVAL_409_9", "처리가 완료된 본인 결재 이력만 삭제할 수 있습니다."),
    DOCUMENT_VERSION_CONFLICT(HttpStatus.CONFLICT, "APPROVAL_409_10",
            "다른 요청이 먼저 이 결재를 처리했습니다. 새로고침 후 다시 시도해주세요."),

    SUMMARY_GENERATION_FAILED(HttpStatus.BAD_GATEWAY, "APPROVAL_502_1", "첨부파일 요약 생성에 실패했습니다."),
    FIELD_EXTRACTION_FAILED(HttpStatus.BAD_GATEWAY, "APPROVAL_502_2", "첨부파일 필드 추출에 실패했습니다."),
    ATTACHMENT_DOWNLOAD_URL_FAILED(HttpStatus.BAD_GATEWAY, "APPROVAL_502_3", "첨부파일 다운로드 URL 생성에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

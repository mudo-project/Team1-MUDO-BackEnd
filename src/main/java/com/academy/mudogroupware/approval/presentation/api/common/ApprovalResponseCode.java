package com.academy.mudogroupware.approval.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApprovalResponseCode implements ResponseCode {

    TEMPLATE_CREATED("APPROVAL_201_1", "결재 템플릿 생성에 성공했습니다."),
    TEMPLATE_LIST_RETRIEVED("APPROVAL_200_1", "결재 템플릿 목록 조회에 성공했습니다."),
    TEMPLATE_DETAIL_RETRIEVED("APPROVAL_200_2", "결재 템플릿 상세 조회에 성공했습니다."),
    DOCUMENT_CREATED("APPROVAL_201_2", "결재 신청에 성공했습니다."),
    MY_APPROVALS_RETRIEVED("APPROVAL_200_3", "내게 온 결재 목록 조회에 성공했습니다."),
    MY_SUBMITTED_APPROVALS_RETRIEVED("APPROVAL_200_4", "내가 신청한 결재 목록 조회에 성공했습니다."),
    DOCUMENT_DETAIL_RETRIEVED("APPROVAL_200_5", "결재 상세 조회에 성공했습니다."),
    PENDING_COUNT_RETRIEVED("APPROVAL_200_6", "결재 대기 건수 조회에 성공했습니다."),
    DOCUMENT_RESUBMITTED("APPROVAL_201_3", "결재 재상신에 성공했습니다.");

    private final String code;
    private final String message;
}

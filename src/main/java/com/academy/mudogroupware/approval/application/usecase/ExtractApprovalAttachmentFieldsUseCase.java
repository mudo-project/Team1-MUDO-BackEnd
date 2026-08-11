package com.academy.mudogroupware.approval.application.usecase;

import com.academy.mudogroupware.approval.application.query.ApprovalAttachmentFieldsView;

/**
 * 다른 도메인(예: corporatecard)이 결재 문서의 첨부파일에서 금액/일자/가맹점 같은 구조화 필드를
 * 가져오기 위해 직접 주입해서 쓰는 공개 UseCase다. approval 내부 저장 구조는 노출하지 않는다.
 */
public interface ExtractApprovalAttachmentFieldsUseCase {

    /**
     * 결재 문서의 첨부파일(여러 개면 첫 번째)에서 금액/일자/가맹점을 추출한다.
     * 문서를 찾을 수 없거나 첨부파일이 없거나 원문을 읽을 수 없으면 ApprovalException을 던진다.
     */
    ApprovalAttachmentFieldsView extractFields(Long documentId);
}

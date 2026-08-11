package com.academy.mudogroupware.corporatecard.application.port;

import java.time.LocalDate;

public interface ApprovalAttachmentFieldsPort {

    /**
     * 결재 문서에 첨부된 영수증에서 금액/일자/가맹점명을 추출한다. 값을 찾지 못한 필드는 null이다.
     */
    ExtractedReceiptFields extractFields(Long approvalDocumentId);

    record ExtractedReceiptFields(Long amount, LocalDate date, String merchant) {
    }
}

package com.academy.mudogroupware.corporatecard.infrastructure.approval;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.approval.application.query.ApprovalAttachmentFieldsView;
import com.academy.mudogroupware.approval.application.usecase.ExtractApprovalAttachmentFieldsUseCase;
import com.academy.mudogroupware.corporatecard.application.port.ApprovalAttachmentFieldsPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ApprovalAttachmentFieldsAdapter implements ApprovalAttachmentFieldsPort {

    private final ExtractApprovalAttachmentFieldsUseCase extractApprovalAttachmentFieldsUseCase;

    @Override
    public ExtractedReceiptFields extractFields(Long approvalDocumentId) {
        ApprovalAttachmentFieldsView view = extractApprovalAttachmentFieldsUseCase.extractFields(approvalDocumentId);
        return new ExtractedReceiptFields(view.amount(), view.date(), view.merchant());
    }
}

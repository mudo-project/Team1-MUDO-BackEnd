package com.academy.mudogroupware.corporatecard.application.port;

public interface ApprovalSubmissionPort {
    Long submit(Long templateId, Long creatorId, String title, String content);
    ApprovalStatusView findStatus(Long documentId);

    record ApprovalStatusView(String code, String name) { }
}

package com.academy.mudogroupware.corporatecard.application.port;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ApprovalSubmissionPort {
    Long submit(Long templateId, Long creatorId, String title, String content, List<Long> approverIds);
    List<Long> findDefaultApproverIds(Long templateId);
    void saveDefaultApproverIdsIfEmpty(Long templateId, List<Long> approverIds);
    ApprovalStatusView findStatus(Long documentId);
    Map<Long, ApprovalStatusView> findStatuses(Set<Long> documentIds);
    List<ApprovalLineInfo> findApprovalLines(Long documentId);

    record ApprovalStatusView(String code, String name) { }
    record ApprovalLineInfo(Long approverId, int stepOrder) { }
}

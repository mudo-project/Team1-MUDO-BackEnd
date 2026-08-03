package com.academy.mudogroupware.approval.application.usecase;

import java.util.List;

import com.academy.mudogroupware.approval.application.query.ApprovalDetailView;
import com.academy.mudogroupware.approval.application.query.ApprovalSubmittedSummaryView;
import com.academy.mudogroupware.approval.application.query.ApprovalSummaryView;

public interface ApprovalQueryUseCase {

    List<ApprovalSummaryView> getMyApprovals(Long userId);

    List<ApprovalSubmittedSummaryView> getMySubmittedApprovals(Long userId);

    long getMyPendingCount(Long userId);

    ApprovalDetailView getApprovalDetail(Long documentId, Long requesterId);
}

package com.academy.mudogroupware.approval.application.usecase;

import com.academy.mudogroupware.approval.application.query.ApprovalDetailView;
import com.academy.mudogroupware.approval.application.query.ApprovalSubmittedSummaryView;
import com.academy.mudogroupware.approval.application.query.ApprovalSummaryView;
import com.academy.mudogroupware.global.domain.common.page.PageResult;

public interface ApprovalQueryUseCase {

    PageResult<ApprovalSummaryView> getMyApprovals(Long userId, int page, int size);

    PageResult<ApprovalSubmittedSummaryView> getAllApprovals(Long academyId, int page, int size);

    PageResult<ApprovalSummaryView> getMyApprovalHistory(Long userId, int page, int size);

    PageResult<ApprovalSubmittedSummaryView> getMySubmittedApprovals(Long userId, int page, int size);

    long getMyPendingCount(Long userId);

    ApprovalDetailView getApprovalDetail(Long documentId, Long requesterId, Long requesterAcademyId,
                                         boolean canReadAll);
}

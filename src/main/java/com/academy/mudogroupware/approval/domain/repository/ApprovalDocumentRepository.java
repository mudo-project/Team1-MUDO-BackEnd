package com.academy.mudogroupware.approval.domain.repository;

import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.global.domain.common.page.PageResult;

public interface ApprovalDocumentRepository {

    ApprovalDocument save(ApprovalDocument approvalDocument);

    Optional<ApprovalDocument> findById(Long id);

    List<ApprovalDocument> findAllByApproverId(Long approverId);

    long countPendingByApproverId(Long approverId);

    PageResult<ApprovalDocument> findAllByApproverId(Long approverId, int page, int size);

    PageResult<ApprovalDocument> findAllByCreatorId(Long creatorId, int page, int size);

    PageResult<ApprovalDocument> findAll(int page, int size);

    PageResult<ApprovalDocument> findHistoryByApproverId(Long approverId, int page, int size);
}

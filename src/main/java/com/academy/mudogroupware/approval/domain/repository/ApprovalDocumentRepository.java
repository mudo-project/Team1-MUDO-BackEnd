package com.academy.mudogroupware.approval.domain.repository;

import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;

public interface ApprovalDocumentRepository {

    ApprovalDocument save(ApprovalDocument approvalDocument);

    Optional<ApprovalDocument> findById(Long id);

    List<ApprovalDocument> findAllByApproverId(Long approverId);

    List<ApprovalDocument> findAllByCreatorId(Long creatorId);
}

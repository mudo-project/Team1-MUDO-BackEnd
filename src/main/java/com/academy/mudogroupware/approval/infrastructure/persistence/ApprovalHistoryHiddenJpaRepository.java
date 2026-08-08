package com.academy.mudogroupware.approval.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalHistoryHiddenJpaRepository extends JpaRepository<ApprovalHistoryHiddenEntity, Long> {

    boolean existsByApprovalDocumentIdAndUserId(Long approvalDocumentId, Long userId);
}

package com.academy.mudogroupware.approval.domain.repository;

import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.approval.domain.model.ApprovalTemplate;

public interface ApprovalTemplateRepository {

    ApprovalTemplate save(ApprovalTemplate approvalTemplate);

    Optional<ApprovalTemplate> findById(Long id);

    List<ApprovalTemplate> findAllByApproverId(Long approverId);
}

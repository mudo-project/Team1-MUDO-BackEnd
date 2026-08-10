package com.academy.mudogroupware.approval.domain.repository;

import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.approval.domain.model.ApprovalTemplate;
import com.academy.mudogroupware.global.domain.common.page.PageResult;

public interface ApprovalTemplateRepository {

    ApprovalTemplate save(ApprovalTemplate approvalTemplate);

    Optional<ApprovalTemplate> findById(Long id);

    List<ApprovalTemplate> findAllById(List<Long> ids);

    PageResult<ApprovalTemplate> findAll(Long academyId, int page, int size);

    void deleteById(Long id);
}

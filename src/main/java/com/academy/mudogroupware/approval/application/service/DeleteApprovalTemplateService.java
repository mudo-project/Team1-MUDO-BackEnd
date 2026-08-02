package com.academy.mudogroupware.approval.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.usecase.DeleteApprovalTemplateUseCase;
import com.academy.mudogroupware.approval.domain.repository.ApprovalTemplateRepository;
import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteApprovalTemplateService implements DeleteApprovalTemplateUseCase {

    private final ApprovalTemplateRepository approvalTemplateRepository;

    @Override
    public void deleteTemplate(Long templateId) {
        approvalTemplateRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("결재 템플릿을 찾을 수 없습니다."));

        approvalTemplateRepository.deleteById(templateId);
    }
}

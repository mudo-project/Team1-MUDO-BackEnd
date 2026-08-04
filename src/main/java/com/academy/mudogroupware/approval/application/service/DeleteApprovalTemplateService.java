package com.academy.mudogroupware.approval.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.port.ApproverDirectoryPort;
import com.academy.mudogroupware.approval.application.port.ApproverInfo;
import com.academy.mudogroupware.approval.application.usecase.DeleteApprovalTemplateUseCase;
import com.academy.mudogroupware.approval.domain.model.ApprovalTemplate;
import com.academy.mudogroupware.approval.domain.repository.ApprovalTemplateRepository;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteApprovalTemplateService implements DeleteApprovalTemplateUseCase {

    private final ApprovalTemplateRepository approvalTemplateRepository;
    private final ApproverDirectoryPort approverDirectoryPort;

    @Override
    public void deleteTemplate(Long templateId, Long requesterId) {
        ApprovalTemplate approvalTemplate = approvalTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.TEMPLATE_NOT_FOUND));

        ApproverInfo requester = approverDirectoryPort.getApprover(requesterId);
        if (!approvalTemplate.getAcademyId().equals(requester.academyId())) {
            throw new ApprovalException(ApprovalErrorCode.TEMPLATE_ACCESS_DENIED);
        }

        approvalTemplateRepository.deleteById(templateId);
    }
}

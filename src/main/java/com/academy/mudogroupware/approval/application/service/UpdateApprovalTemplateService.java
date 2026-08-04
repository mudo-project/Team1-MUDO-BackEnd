package com.academy.mudogroupware.approval.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.command.UpdateApprovalTemplateCommand;
import com.academy.mudogroupware.approval.application.port.ApproverDirectoryPort;
import com.academy.mudogroupware.approval.application.port.ApproverInfo;
import com.academy.mudogroupware.approval.application.usecase.UpdateApprovalTemplateUseCase;
import com.academy.mudogroupware.approval.domain.model.ApprovalTemplate;
import com.academy.mudogroupware.approval.domain.repository.ApprovalTemplateRepository;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateApprovalTemplateService implements UpdateApprovalTemplateUseCase {

    private final ApprovalTemplateRepository approvalTemplateRepository;
    private final ApproverDirectoryPort approverDirectoryPort;
    private final ApproverValidator approverValidator;
    private final Clock clock;

    @Override
    public void updateTemplate(UpdateApprovalTemplateCommand command) {
        ApprovalTemplate approvalTemplate = approvalTemplateRepository.findById(command.templateId())
                .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.TEMPLATE_NOT_FOUND));

        ApproverInfo requester = approverDirectoryPort.getApprover(command.requesterId());
        if (!approvalTemplate.getAcademyId().equals(requester.academyId())) {
            throw new ApprovalException(ApprovalErrorCode.TEMPLATE_ACCESS_DENIED);
        }

        approverValidator.validate(command.approverIds(), approvalTemplate.getAcademyId());

        approvalTemplate.update(command.name(), command.approverIds(), LocalDateTime.now(clock));

        approvalTemplateRepository.save(approvalTemplate);
    }
}

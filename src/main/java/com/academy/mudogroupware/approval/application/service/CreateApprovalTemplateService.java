package com.academy.mudogroupware.approval.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.command.CreateApprovalTemplateCommand;
import com.academy.mudogroupware.approval.application.usecase.CreateApprovalTemplateUseCase;
import com.academy.mudogroupware.approval.domain.model.ApprovalContent;
import com.academy.mudogroupware.approval.domain.model.ApprovalTemplate;
import com.academy.mudogroupware.approval.domain.repository.ApprovalTemplateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateApprovalTemplateService implements CreateApprovalTemplateUseCase {

    private final ApprovalTemplateRepository approvalTemplateRepository;

    @Override
    public Long createTemplate(CreateApprovalTemplateCommand command) {
        ApprovalContent content = ApprovalContent.create(command.contentType(), command.text(), command.fileUrl());
        ApprovalTemplate approvalTemplate = ApprovalTemplate.create(
                command.title(), content, command.creatorId(), command.approverIds());

        return approvalTemplateRepository.save(approvalTemplate).getId();
    }
}

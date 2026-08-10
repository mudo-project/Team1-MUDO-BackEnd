package com.academy.mudogroupware.approval.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.command.CreateApprovalTemplateCommand;
import com.academy.mudogroupware.approval.application.port.ApproverDirectoryPort;
import com.academy.mudogroupware.approval.application.port.ApproverInfo;
import com.academy.mudogroupware.approval.application.usecase.CreateApprovalTemplateUseCase;
import com.academy.mudogroupware.approval.domain.model.ApprovalTemplate;
import com.academy.mudogroupware.approval.domain.repository.ApprovalTemplateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateApprovalTemplateService implements CreateApprovalTemplateUseCase {

    private final ApprovalTemplateRepository approvalTemplateRepository;
    private final ApproverDirectoryPort approverDirectoryPort;
    private final ApproverValidator approverValidator;
    private final Clock clock;

    @Override
    public Long createTemplate(CreateApprovalTemplateCommand command) {
        ApproverInfo creator = approverDirectoryPort.getApprover(command.creatorId());
        approverValidator.validate(command.approverIds(), creator.academyId());
        ApprovalTemplate approvalTemplate = ApprovalTemplate.create(
                creator.academyId(), command.name(), command.creatorId(), command.approverIds(),
                LocalDateTime.now(clock));

        return approvalTemplateRepository.save(approvalTemplate).getId();
    }
}

package com.academy.mudogroupware.approval.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.command.UpdateApprovalTemplateCommand;
import com.academy.mudogroupware.approval.application.usecase.UpdateApprovalTemplateUseCase;
import com.academy.mudogroupware.approval.domain.model.ApprovalTemplate;
import com.academy.mudogroupware.approval.domain.repository.ApprovalTemplateRepository;
import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateApprovalTemplateService implements UpdateApprovalTemplateUseCase {

    private final ApprovalTemplateRepository approvalTemplateRepository;
    private final Clock clock;

    @Override
    public void updateTemplate(UpdateApprovalTemplateCommand command) {
        ApprovalTemplate approvalTemplate = approvalTemplateRepository.findById(command.templateId())
                .orElseThrow(() -> new NotFoundException("결재 템플릿을 찾을 수 없습니다."));

        approvalTemplate.update(command.name(), command.approverIds(), LocalDateTime.now(clock));

        approvalTemplateRepository.save(approvalTemplate);
    }
}

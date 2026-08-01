package com.academy.mudogroupware.approval.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.command.DecideApprovalLineCommand;
import com.academy.mudogroupware.approval.application.usecase.DecideApprovalLineUseCase;
import com.academy.mudogroupware.approval.domain.model.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.model.ApprovalTemplate;
import com.academy.mudogroupware.approval.domain.repository.ApprovalTemplateRepository;
import com.academy.mudogroupware.global.error.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DecideApprovalLineService implements DecideApprovalLineUseCase {

    private final ApprovalTemplateRepository approvalTemplateRepository;

    @Override
    public void decide(DecideApprovalLineCommand command) {
        ApprovalTemplate approvalTemplate = approvalTemplateRepository.findById(command.templateId())
                .orElseThrow(() -> new BusinessException(ApprovalErrorCode.TEMPLATE_NOT_FOUND));

        approvalTemplate.decide(command.approverId(), command.decision(), command.comment());

        approvalTemplateRepository.save(approvalTemplate);
    }
}

package com.academy.mudogroupware.approval.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.command.CreateApprovalDocumentCommand;
import com.academy.mudogroupware.approval.application.usecase.CreateApprovalDocumentUseCase;
import com.academy.mudogroupware.approval.domain.model.ApprovalContent;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.model.ApprovalTemplate;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;
import com.academy.mudogroupware.approval.domain.repository.ApprovalTemplateRepository;
import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateApprovalDocumentService implements CreateApprovalDocumentUseCase {

    private final ApprovalTemplateRepository approvalTemplateRepository;
    private final ApprovalDocumentRepository approvalDocumentRepository;

    @Override
    public Long createDocument(CreateApprovalDocumentCommand command) {
        ApprovalTemplate approvalTemplate = approvalTemplateRepository.findById(command.templateId())
                .orElseThrow(() -> new NotFoundException("결재 템플릿을 찾을 수 없습니다."));

        List<Long> approverIds = (command.approverIds() != null && !command.approverIds().isEmpty())
                ? command.approverIds()
                : approvalTemplate.approverIdsInOrder();

        ApprovalContent content = ApprovalContent.create(command.contentType(), command.text());
        ApprovalDocument approvalDocument = ApprovalDocument.create(
                approvalTemplate.getAcademyId(), approvalTemplate.getId(), command.title(), content,
                command.creatorId(), approverIds, command.fileIds());

        return approvalDocumentRepository.save(approvalDocument).getId();
    }
}

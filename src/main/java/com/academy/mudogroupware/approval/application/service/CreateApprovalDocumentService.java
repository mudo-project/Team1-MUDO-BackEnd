package com.academy.mudogroupware.approval.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.command.CreateApprovalDocumentCommand;
import com.academy.mudogroupware.approval.application.port.ApproverDirectoryPort;
import com.academy.mudogroupware.approval.application.port.ApproverInfo;
import com.academy.mudogroupware.approval.application.usecase.CreateApprovalDocumentUseCase;
import com.academy.mudogroupware.approval.domain.model.ApprovalContent;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.model.ApprovalTemplate;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;
import com.academy.mudogroupware.approval.domain.repository.ApprovalTemplateRepository;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateApprovalDocumentService implements CreateApprovalDocumentUseCase {

    private final ApprovalTemplateRepository approvalTemplateRepository;
    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final ApproverDirectoryPort approverDirectoryPort;
    private final Clock clock;

    @Override
    public Long createDocument(CreateApprovalDocumentCommand command) {
        ApprovalTemplate approvalTemplate = approvalTemplateRepository.findById(command.templateId())
                .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.TEMPLATE_NOT_FOUND));

        ApproverInfo creator = approverDirectoryPort.getApprover(command.creatorId());
        if (!approvalTemplate.getAcademyId().equals(creator.academyId())) {
            throw new ApprovalException(ApprovalErrorCode.CROSS_ACADEMY_TEMPLATE);
        }

        List<Long> approverIds = (command.approverIds() != null && !command.approverIds().isEmpty())
                ? command.approverIds()
                : approvalTemplate.approverIdsInOrder();

        ApprovalContent content = ApprovalContent.create(command.contentType(), command.text());
        ApprovalDocument approvalDocument = ApprovalDocument.create(
                approvalTemplate.getAcademyId(), approvalTemplate.getId(), command.title(), content,
                command.creatorId(), approverIds, command.fileIds(), LocalDateTime.now(clock));

        return approvalDocumentRepository.save(approvalDocument).getId();
    }
}

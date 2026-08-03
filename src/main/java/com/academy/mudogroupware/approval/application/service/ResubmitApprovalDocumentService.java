package com.academy.mudogroupware.approval.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.command.ResubmitApprovalDocumentCommand;
import com.academy.mudogroupware.approval.application.usecase.ResubmitApprovalDocumentUseCase;
import com.academy.mudogroupware.approval.domain.model.ApprovalAttachment;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocumentLine;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;
import com.academy.mudogroupware.global.domain.common.exception.ForbiddenException;
import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ResubmitApprovalDocumentService implements ResubmitApprovalDocumentUseCase {

    private final ApprovalDocumentRepository approvalDocumentRepository;

    @Override
    public Long resubmit(ResubmitApprovalDocumentCommand command) {
        ApprovalDocument original = approvalDocumentRepository.findById(command.documentId())
                .orElseThrow(() -> new NotFoundException("결재 문서를 찾을 수 없습니다."));

        if (!original.getCreatorId().equals(command.requesterId())) {
            throw new ForbiddenException("본인이 신청한 결재만 재상신할 수 있습니다.");
        }
        original.markResubmitted();

        List<Long> approverIds = original.getLines().stream()
                .map(ApprovalDocumentLine::getApproverId)
                .toList();
        List<Long> fileIds = original.getAttachments().stream()
                .map(ApprovalAttachment::getFileId)
                .toList();

        ApprovalDocument resubmitted = ApprovalDocument.create(
                original.getAcademyId(), original.getTemplateId(), original.getTitle(), original.getContent(),
                original.getCreatorId(), approverIds, fileIds);

        Long newDocumentId = approvalDocumentRepository.save(resubmitted).getId();
        approvalDocumentRepository.save(original);
        return newDocumentId;
    }
}

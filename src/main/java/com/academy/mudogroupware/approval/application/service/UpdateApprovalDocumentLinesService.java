package com.academy.mudogroupware.approval.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.command.UpdateApprovalDocumentLinesCommand;
import com.academy.mudogroupware.approval.application.usecase.UpdateApprovalDocumentLinesUseCase;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;
import com.academy.mudogroupware.global.domain.common.exception.ForbiddenException;
import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateApprovalDocumentLinesService implements UpdateApprovalDocumentLinesUseCase {

    private final ApprovalDocumentRepository approvalDocumentRepository;

    @Override
    public void updateLines(UpdateApprovalDocumentLinesCommand command) {
        ApprovalDocument approvalDocument = approvalDocumentRepository.findById(command.documentId())
                .orElseThrow(() -> new NotFoundException("결재 문서를 찾을 수 없습니다."));

        if (!approvalDocument.getCreatorId().equals(command.requesterId())) {
            throw new ForbiddenException("본인이 신청한 결재만 결재선을 수정할 수 있습니다.");
        }

        approvalDocument.updateLines(command.approverIds());

        approvalDocumentRepository.save(approvalDocument);
    }
}

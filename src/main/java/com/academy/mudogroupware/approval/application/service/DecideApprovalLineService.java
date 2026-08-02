package com.academy.mudogroupware.approval.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.command.DecideApprovalLineCommand;
import com.academy.mudogroupware.approval.application.usecase.DecideApprovalLineUseCase;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;
import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DecideApprovalLineService implements DecideApprovalLineUseCase {

    private final ApprovalDocumentRepository approvalDocumentRepository;

    @Override
    public void decide(DecideApprovalLineCommand command) {
        ApprovalDocument approvalDocument = approvalDocumentRepository.findById(command.documentId())
                .orElseThrow(() -> new NotFoundException("결재 문서를 찾을 수 없습니다."));

        approvalDocument.decide(command.approverId(), command.decision(), command.comment());

        approvalDocumentRepository.save(approvalDocument);
    }
}

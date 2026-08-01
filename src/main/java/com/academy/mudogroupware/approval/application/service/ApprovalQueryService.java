package com.academy.mudogroupware.approval.application.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.port.ApproverDirectoryPort;
import com.academy.mudogroupware.approval.application.port.ApproverInfo;
import com.academy.mudogroupware.approval.application.query.ApprovalDetailView;
import com.academy.mudogroupware.approval.application.query.ApprovalLineView;
import com.academy.mudogroupware.approval.application.query.ApprovalSummaryView;
import com.academy.mudogroupware.approval.application.usecase.ApprovalQueryUseCase;
import com.academy.mudogroupware.approval.domain.model.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.model.ApprovalLine;
import com.academy.mudogroupware.approval.domain.model.ApprovalTemplate;
import com.academy.mudogroupware.approval.domain.repository.ApprovalTemplateRepository;
import com.academy.mudogroupware.global.error.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalQueryService implements ApprovalQueryUseCase {

    private final ApprovalTemplateRepository approvalTemplateRepository;
    private final ApproverDirectoryPort approverDirectoryPort;

    @Override
    public List<ApprovalSummaryView> getMyApprovals(Long userId) {
        return approvalTemplateRepository.findAllByApproverId(userId).stream()
                .map(approvalTemplate -> toSummaryView(approvalTemplate, userId))
                .toList();
    }

    @Override
    public ApprovalDetailView getApprovalDetail(Long templateId, Long requesterId) {
        ApprovalTemplate approvalTemplate = approvalTemplateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ApprovalErrorCode.TEMPLATE_NOT_FOUND));

        if (!approvalTemplate.isApprover(requesterId) && !approvalTemplate.getCreatorId().equals(requesterId)) {
            throw new BusinessException(ApprovalErrorCode.ACCESS_DENIED);
        }

        List<Long> approverIds = approvalTemplate.getApprovalLines().stream()
                .map(ApprovalLine::getApproverId)
                .toList();
        Map<Long, ApproverInfo> approvers = approverDirectoryPort.getApprovers(approverIds);

        List<ApprovalLineView> lines = approvalTemplate.getApprovalLines().stream()
                .map(line -> toLineView(line, approvers))
                .toList();

        return new ApprovalDetailView(
                approvalTemplate.getId(),
                approvalTemplate.getTitle(),
                approvalTemplate.getContent().getType(),
                approvalTemplate.getContent().getText(),
                approvalTemplate.getContent().getFileUrl(),
                approvalTemplate.getCreatorId(),
                approvalTemplate.getStatus(),
                approvalTemplate.getCreatedAt(),
                lines
        );
    }

    private ApprovalSummaryView toSummaryView(ApprovalTemplate approvalTemplate, Long userId) {
        ApprovalLine myLine = approvalTemplate.getApprovalLines().stream()
                .filter(line -> line.getApproverId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ApprovalErrorCode.ACCESS_DENIED));

        return new ApprovalSummaryView(
                approvalTemplate.getId(),
                approvalTemplate.getTitle(),
                approvalTemplate.getStatus(),
                myLine.getStepOrder(),
                myLine.getStatus(),
                approvalTemplate.getCreatedAt()
        );
    }

    private ApprovalLineView toLineView(ApprovalLine line, Map<Long, ApproverInfo> approvers) {
        ApproverInfo approverInfo = approvers.get(line.getApproverId());
        String approverName = approverInfo != null ? approverInfo.name() : null;

        return new ApprovalLineView(
                line.getId(),
                line.getStepOrder(),
                line.getApproverId(),
                approverName,
                line.getStatus(),
                line.getComment(),
                line.getDecidedAt()
        );
    }
}

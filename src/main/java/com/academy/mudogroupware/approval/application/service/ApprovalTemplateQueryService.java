package com.academy.mudogroupware.approval.application.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.port.ApproverDirectoryPort;
import com.academy.mudogroupware.approval.application.port.ApproverInfo;
import com.academy.mudogroupware.approval.application.query.ApprovalTemplateDetailView;
import com.academy.mudogroupware.approval.application.query.ApprovalTemplateLineView;
import com.academy.mudogroupware.approval.application.query.ApprovalTemplateSummaryView;
import com.academy.mudogroupware.approval.application.usecase.ApprovalTemplateQueryUseCase;
import com.academy.mudogroupware.approval.domain.model.ApprovalTemplate;
import com.academy.mudogroupware.approval.domain.model.ApprovalTemplateLine;
import com.academy.mudogroupware.approval.domain.repository.ApprovalTemplateRepository;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;
import com.academy.mudogroupware.global.domain.common.page.PageResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalTemplateQueryService implements ApprovalTemplateQueryUseCase {

    private final ApprovalTemplateRepository approvalTemplateRepository;
    private final ApproverDirectoryPort approverDirectoryPort;

    @Override
    public PageResult<ApprovalTemplateSummaryView> getTemplates(Long requesterId, int page, int size) {
        ApproverInfo requester = approverDirectoryPort.getApprover(requesterId);
        return approvalTemplateRepository.findAll(requester.academyId(), page, size)
                .map(this::toSummaryView);
    }

    private ApprovalTemplateSummaryView toSummaryView(ApprovalTemplate template) {
        List<Long> approverIds = template.getLines().stream()
                .map(ApprovalTemplateLine::getApproverId)
                .toList();
        Map<Long, ApproverInfo> approvers = approverDirectoryPort.getApprovers(approverIds);

        List<ApprovalTemplateLineView> lines = template.getLines().stream()
                .map(line -> toLineView(line, approvers))
                .toList();

        return new ApprovalTemplateSummaryView(template.getId(), template.getName(), template.getCreatedAt(), lines);
    }

    @Override
    public ApprovalTemplateDetailView getTemplateDetail(Long templateId, Long requesterId) {
        ApprovalTemplate approvalTemplate = approvalTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.TEMPLATE_NOT_FOUND));

        ApproverInfo requester = approverDirectoryPort.getApprover(requesterId);
        if (!approvalTemplate.getAcademyId().equals(requester.academyId())) {
            throw new ApprovalException(ApprovalErrorCode.TEMPLATE_ACCESS_DENIED);
        }

        List<Long> approverIds = approvalTemplate.getLines().stream()
                .map(ApprovalTemplateLine::getApproverId)
                .toList();
        Map<Long, ApproverInfo> approvers = approverDirectoryPort.getApprovers(approverIds);

        List<ApprovalTemplateLineView> lines = approvalTemplate.getLines().stream()
                .map(line -> toLineView(line, approvers))
                .toList();

        return new ApprovalTemplateDetailView(
                approvalTemplate.getId(),
                approvalTemplate.getName(),
                approvalTemplate.getCreatorId(),
                approvalTemplate.getCreatedAt(),
                lines
        );
    }

    private ApprovalTemplateLineView toLineView(ApprovalTemplateLine line, Map<Long, ApproverInfo> approvers) {
        ApproverInfo approverInfo = approvers.get(line.getApproverId());
        String approverName = approverInfo != null ? approverInfo.name() : null;
        return new ApprovalTemplateLineView(line.getStepOrder(), line.getApproverId(), approverName);
    }
}

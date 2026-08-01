package com.academy.mudogroupware.approval.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.academy.mudogroupware.global.error.BusinessException;

public final class ApprovalTemplate {

    private final Long id;
    private final String title;
    private final ApprovalContent content;
    private final Long creatorId;
    private final List<ApprovalLine> approvalLines;
    private ApprovalStatus status;
    private final LocalDateTime createdAt;

    private ApprovalTemplate(Long id, String title, ApprovalContent content, Long creatorId,
                              List<ApprovalLine> approvalLines, ApprovalStatus status, LocalDateTime createdAt) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(ApprovalErrorCode.INVALID_TITLE);
        }
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        if (creatorId == null) {
            throw new IllegalArgumentException("creatorId must not be null");
        }
        if (approvalLines == null || approvalLines.isEmpty()) {
            throw new BusinessException(ApprovalErrorCode.NO_APPROVAL_LINES);
        }
        this.id = id;
        this.title = title;
        this.content = content;
        this.creatorId = creatorId;
        this.approvalLines = new ArrayList<>(approvalLines);
        this.status = status;
        this.createdAt = createdAt;
    }

    public static ApprovalTemplate create(String title, ApprovalContent content, Long creatorId, List<Long> approverIds) {
        List<ApprovalLine> lines = new ArrayList<>();
        int order = 1;
        if (approverIds != null) {
            for (Long approverId : approverIds) {
                lines.add(ApprovalLine.create(order++, approverId));
            }
        }
        return new ApprovalTemplate(null, title, content, creatorId, lines, ApprovalStatus.IN_PROGRESS, LocalDateTime.now());
    }

    public static ApprovalTemplate restore(Long id, String title, ApprovalContent content, Long creatorId,
                                            List<ApprovalLine> approvalLines, ApprovalStatus status, LocalDateTime createdAt) {
        return new ApprovalTemplate(id, title, content, creatorId, approvalLines, status, createdAt);
    }

    public void decide(Long approverId, ApprovalDecision decision, String comment) {
        if (this.status != ApprovalStatus.IN_PROGRESS) {
            throw new BusinessException(ApprovalErrorCode.ALREADY_DECIDED);
        }
        ApprovalLine currentLine = currentPendingLine();
        if (currentLine == null) {
            throw new BusinessException(ApprovalErrorCode.ALREADY_DECIDED);
        }
        if (!currentLine.getApproverId().equals(approverId)) {
            throw new BusinessException(ApprovalErrorCode.NOT_YOUR_TURN);
        }

        if (decision == ApprovalDecision.APPROVE) {
            currentLine.approve(comment);
            activateNextLine(currentLine.getStepOrder());
            if (isAllApproved()) {
                this.status = ApprovalStatus.APPROVED;
            }
        } else {
            currentLine.reject(comment);
            this.status = ApprovalStatus.REJECTED;
        }
    }

    public boolean isApprover(Long userId) {
        return approvalLines.stream().anyMatch(line -> line.getApproverId().equals(userId));
    }

    private ApprovalLine currentPendingLine() {
        return approvalLines.stream()
                .filter(line -> line.getStatus() == ApprovalLineStatus.PENDING)
                .findFirst()
                .orElse(null);
    }

    private void activateNextLine(int currentStepOrder) {
        approvalLines.stream()
                .filter(line -> line.getStepOrder() == currentStepOrder + 1)
                .findFirst()
                .ifPresent(ApprovalLine::activate);
    }

    private boolean isAllApproved() {
        return approvalLines.stream().allMatch(line -> line.getStatus() == ApprovalLineStatus.APPROVED);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public ApprovalContent getContent() {
        return content;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public List<ApprovalLine> getApprovalLines() {
        return Collections.unmodifiableList(approvalLines);
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

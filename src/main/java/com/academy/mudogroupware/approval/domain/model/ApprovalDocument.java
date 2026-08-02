package com.academy.mudogroupware.approval.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;
import com.academy.mudogroupware.global.domain.common.exception.ConflictException;

public final class ApprovalDocument {

    private final Long id;
    private final Long templateId;
    private final String title;
    private final ApprovalContent content;
    private final Long creatorId;
    private final List<ApprovalDocumentLine> lines;
    private ApprovalStatus status;
    private final LocalDateTime createdAt;

    private ApprovalDocument(Long id, Long templateId, String title, ApprovalContent content, Long creatorId,
                              List<ApprovalDocumentLine> lines, ApprovalStatus status, LocalDateTime createdAt) {
        if (title == null || title.isBlank()) {
            throw new BadRequestException("결재 제목은 비어 있을 수 없습니다.");
        }
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        if (creatorId == null) {
            throw new IllegalArgumentException("creatorId must not be null");
        }
        if (lines == null || lines.isEmpty()) {
            throw new BadRequestException("결재선은 최소 1명 이상 지정해야 합니다.");
        }
        this.id = id;
        this.templateId = templateId;
        this.title = title;
        this.content = content;
        this.creatorId = creatorId;
        this.lines = new ArrayList<>(lines);
        this.status = status;
        this.createdAt = createdAt;
    }

    public static ApprovalDocument create(Long templateId, String title, ApprovalContent content, Long creatorId,
                                           List<Long> approverIds) {
        return new ApprovalDocument(null, templateId, title, content, creatorId, buildLines(approverIds),
                ApprovalStatus.IN_PROGRESS, LocalDateTime.now());
    }

    public static ApprovalDocument restore(Long id, Long templateId, String title, ApprovalContent content,
                                            Long creatorId, List<ApprovalDocumentLine> lines, ApprovalStatus status,
                                            LocalDateTime createdAt) {
        return new ApprovalDocument(id, templateId, title, content, creatorId, lines, status, createdAt);
    }

    public void decide(Long approverId, ApprovalDecision decision, String comment) {
        if (this.status != ApprovalStatus.IN_PROGRESS) {
            throw new ConflictException("이미 처리가 완료된 결재입니다.");
        }
        ApprovalDocumentLine currentLine = currentPendingLine();
        if (currentLine == null) {
            throw new ConflictException("이미 처리가 완료된 결재입니다.");
        }
        if (!currentLine.getApproverId().equals(approverId)) {
            throw new ConflictException("본인 차례의 결재가 아닙니다.");
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

    public void updateLines(List<Long> approverIds) {
        if (this.status != ApprovalStatus.IN_PROGRESS) {
            throw new ConflictException("이미 결재가 진행된 건은 결재선을 수정할 수 없습니다.");
        }
        boolean anyDecided = lines.stream().anyMatch(line -> line.getStatus() == ApprovalLineStatus.APPROVED
                || line.getStatus() == ApprovalLineStatus.REJECTED);
        if (anyDecided) {
            throw new ConflictException("이미 결재가 진행된 건은 결재선을 수정할 수 없습니다.");
        }
        List<ApprovalDocumentLine> newLines = buildLines(approverIds);
        if (newLines.isEmpty()) {
            throw new BadRequestException("결재선은 최소 1명 이상 지정해야 합니다.");
        }
        this.lines.clear();
        this.lines.addAll(newLines);
    }

    public boolean isApprover(Long userId) {
        return lines.stream().anyMatch(line -> line.getApproverId().equals(userId));
    }

    private static List<ApprovalDocumentLine> buildLines(List<Long> approverIds) {
        List<ApprovalDocumentLine> lines = new ArrayList<>();
        int order = 1;
        if (approverIds != null) {
            for (Long approverId : approverIds) {
                lines.add(ApprovalDocumentLine.create(order++, approverId));
            }
        }
        return lines;
    }

    private ApprovalDocumentLine currentPendingLine() {
        return lines.stream()
                .filter(line -> line.getStatus() == ApprovalLineStatus.PENDING)
                .findFirst()
                .orElse(null);
    }

    private void activateNextLine(int currentStepOrder) {
        lines.stream()
                .filter(line -> line.getStepOrder() == currentStepOrder + 1)
                .findFirst()
                .ifPresent(ApprovalDocumentLine::activate);
    }

    private boolean isAllApproved() {
        return lines.stream().allMatch(line -> line.getStatus() == ApprovalLineStatus.APPROVED);
    }

    public Long getId() {
        return id;
    }

    public Long getTemplateId() {
        return templateId;
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

    public List<ApprovalDocumentLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

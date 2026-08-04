package com.academy.mudogroupware.approval.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;
import com.academy.mudogroupware.global.domain.common.exception.ConflictException;

public final class ApprovalDocument {

    private final Long id;
    private final Long academyId;
    private final Long templateId;
    private final String title;
    private final ApprovalContent content;
    private final Long creatorId;
    private final List<ApprovalDocumentLine> lines;
    private final List<ApprovalAttachment> attachments;
    private ApprovalStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime resubmittedAt;

    private ApprovalDocument(Long id, Long academyId, Long templateId, String title, ApprovalContent content,
                              Long creatorId, List<ApprovalDocumentLine> lines, List<ApprovalAttachment> attachments,
                              ApprovalStatus status, LocalDateTime createdAt, LocalDateTime resubmittedAt) {
        if (academyId == null) {
            throw new IllegalArgumentException("academyId must not be null");
        }
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
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        this.id = id;
        this.academyId = academyId;
        this.templateId = templateId;
        this.title = title;
        this.content = content;
        this.creatorId = creatorId;
        this.lines = new ArrayList<>(lines);
        this.attachments = attachments != null ? new ArrayList<>(attachments) : new ArrayList<>();
        this.status = status;
        this.createdAt = createdAt;
        this.resubmittedAt = resubmittedAt;
    }

    public static ApprovalDocument create(Long academyId, Long templateId, String title, ApprovalContent content,
                                           Long creatorId, List<Long> approverIds, List<Long> fileIds,
                                           LocalDateTime now) {
        List<ApprovalAttachment> attachments = fileIds != null
                ? fileIds.stream().map(ApprovalAttachment::create).toList()
                : List.of();
        return new ApprovalDocument(null, academyId, templateId, title, content, creatorId, buildLines(approverIds),
                attachments, ApprovalStatus.IN_PROGRESS, now, null);
    }

    public static ApprovalDocument restore(Long id, Long academyId, Long templateId, String title,
                                            ApprovalContent content, Long creatorId, List<ApprovalDocumentLine> lines,
                                            List<ApprovalAttachment> attachments, ApprovalStatus status,
                                            LocalDateTime createdAt, LocalDateTime resubmittedAt) {
        return new ApprovalDocument(id, academyId, templateId, title, content, creatorId, lines, attachments,
                status, createdAt, resubmittedAt);
    }

    public void markResubmitted(LocalDateTime now) {
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        if (this.status != ApprovalStatus.REJECTED) {
            throw new ConflictException("반려된 결재만 재상신할 수 있습니다.");
        }
        if (this.resubmittedAt != null) {
            throw new ConflictException("이미 재상신된 결재입니다.");
        }
        this.resubmittedAt = now;
    }

    public void decide(Long approverId, ApprovalDecision decision, String comment, LocalDateTime now) {
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
            currentLine.approve(comment, now);
            activateNextLine(currentLine.getStepOrder());
            if (isAllApproved()) {
                this.status = ApprovalStatus.APPROVED;
            }
        } else {
            currentLine.reject(comment, now);
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

    public Long getAcademyId() {
        return academyId;
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

    public List<ApprovalAttachment> getAttachments() {
        return Collections.unmodifiableList(attachments);
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getResubmittedAt() {
        return resubmittedAt;
    }
}

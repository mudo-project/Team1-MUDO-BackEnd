package com.academy.mudogroupware.approval.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public final class ApprovalTemplate {

    private final Long id;
    private final Long academyId;
    private String name;
    private final Long creatorId;
    private final List<ApprovalTemplateLine> lines;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private ApprovalTemplate(Long id, Long academyId, String name, Long creatorId, List<ApprovalTemplateLine> lines,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (academyId == null) {
            throw new IllegalArgumentException("academyId must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new BadRequestException("템플릿 이름은 비어 있을 수 없습니다.");
        }
        if (creatorId == null) {
            throw new IllegalArgumentException("creatorId must not be null");
        }
        if (lines == null || lines.isEmpty()) {
            throw new BadRequestException("결재선은 최소 1명 이상 지정해야 합니다.");
        }
        this.id = id;
        this.academyId = academyId;
        this.name = name;
        this.creatorId = creatorId;
        this.lines = new ArrayList<>(lines);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ApprovalTemplate create(Long academyId, String name, Long creatorId, List<Long> approverIds) {
        LocalDateTime now = LocalDateTime.now();
        return new ApprovalTemplate(null, academyId, name, creatorId, buildLines(approverIds), now, now);
    }

    public static ApprovalTemplate restore(Long id, Long academyId, String name, Long creatorId,
                                            List<ApprovalTemplateLine> lines, LocalDateTime createdAt,
                                            LocalDateTime updatedAt) {
        return new ApprovalTemplate(id, academyId, name, creatorId, lines, createdAt, updatedAt);
    }

    public void update(String name, List<Long> approverIds) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("템플릿 이름은 비어 있을 수 없습니다.");
        }
        List<ApprovalTemplateLine> newLines = buildLines(approverIds);
        if (newLines.isEmpty()) {
            throw new BadRequestException("결재선은 최소 1명 이상 지정해야 합니다.");
        }
        this.name = name;
        this.lines.clear();
        this.lines.addAll(newLines);
        this.updatedAt = LocalDateTime.now();
    }

    private static List<ApprovalTemplateLine> buildLines(List<Long> approverIds) {
        List<ApprovalTemplateLine> lines = new ArrayList<>();
        int order = 1;
        if (approverIds != null) {
            for (Long approverId : approverIds) {
                lines.add(ApprovalTemplateLine.create(order++, approverId));
            }
        }
        return lines;
    }

    public List<Long> approverIdsInOrder() {
        return lines.stream()
                .sorted((a, b) -> Integer.compare(a.getStepOrder(), b.getStepOrder()))
                .map(ApprovalTemplateLine::getApproverId)
                .toList();
    }

    public Long getId() {
        return id;
    }

    public Long getAcademyId() {
        return academyId;
    }

    public String getName() {
        return name;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public List<ApprovalTemplateLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

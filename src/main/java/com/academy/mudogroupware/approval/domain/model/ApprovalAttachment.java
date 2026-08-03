package com.academy.mudogroupware.approval.domain.model;

import java.time.LocalDateTime;

public final class ApprovalAttachment {

    private final Long id;
    private final Long fileId;
    private String aiSummary;
    private AttachmentSummaryStatus summaryStatus;
    private LocalDateTime summarizedAt;

    private ApprovalAttachment(Long id, Long fileId, String aiSummary, AttachmentSummaryStatus summaryStatus,
                                LocalDateTime summarizedAt) {
        if (fileId == null) {
            throw new IllegalArgumentException("fileId must not be null");
        }
        this.id = id;
        this.fileId = fileId;
        this.aiSummary = aiSummary;
        this.summaryStatus = summaryStatus;
        this.summarizedAt = summarizedAt;
    }

    public static ApprovalAttachment create(Long fileId) {
        return new ApprovalAttachment(null, fileId, null, AttachmentSummaryStatus.PENDING, null);
    }

    public static ApprovalAttachment restore(Long id, Long fileId, String aiSummary,
                                              AttachmentSummaryStatus summaryStatus, LocalDateTime summarizedAt) {
        return new ApprovalAttachment(id, fileId, aiSummary, summaryStatus, summarizedAt);
    }

    public Long getId() {
        return id;
    }

    public Long getFileId() {
        return fileId;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public AttachmentSummaryStatus getSummaryStatus() {
        return summaryStatus;
    }

    public LocalDateTime getSummarizedAt() {
        return summarizedAt;
    }
}

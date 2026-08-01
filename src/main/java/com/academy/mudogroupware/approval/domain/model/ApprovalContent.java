package com.academy.mudogroupware.approval.domain.model;

import com.academy.mudogroupware.global.error.BusinessException;

public final class ApprovalContent {

    private final ApprovalContentType type;
    private final String text;
    private final String fileUrl;

    private ApprovalContent(ApprovalContentType type, String text, String fileUrl) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (type == ApprovalContentType.TEXT && (text == null || text.isBlank())) {
            throw new BusinessException(ApprovalErrorCode.INVALID_CONTENT);
        }
        if (type == ApprovalContentType.FILE && (fileUrl == null || fileUrl.isBlank())) {
            throw new BusinessException(ApprovalErrorCode.INVALID_CONTENT);
        }
        this.type = type;
        this.text = text;
        this.fileUrl = fileUrl;
    }

    public static ApprovalContent create(ApprovalContentType type, String text, String fileUrl) {
        return new ApprovalContent(type, text, fileUrl);
    }

    public static ApprovalContent restore(ApprovalContentType type, String text, String fileUrl) {
        return new ApprovalContent(type, text, fileUrl);
    }

    public ApprovalContentType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public String getFileUrl() {
        return fileUrl;
    }
}

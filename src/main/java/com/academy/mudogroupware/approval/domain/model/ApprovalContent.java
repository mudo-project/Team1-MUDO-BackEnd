package com.academy.mudogroupware.approval.domain.model;

import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;

public final class ApprovalContent {

    private final ApprovalContentType type;
    private final String text;

    private ApprovalContent(ApprovalContentType type, String text) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (type == ApprovalContentType.TEXT && (text == null || text.isBlank())) {
            throw new ApprovalException(ApprovalErrorCode.INVALID_CONTENT);
        }
        this.type = type;
        this.text = text;
    }

    public static ApprovalContent create(ApprovalContentType type, String text) {
        return new ApprovalContent(type, text);
    }

    public static ApprovalContent restore(ApprovalContentType type, String text) {
        return new ApprovalContent(type, text);
    }

    public ApprovalContentType getType() {
        return type;
    }

    public String getText() {
        return text;
    }
}

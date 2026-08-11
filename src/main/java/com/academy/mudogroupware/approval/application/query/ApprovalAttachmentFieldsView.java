package com.academy.mudogroupware.approval.application.query;

import java.time.LocalDate;

public record ApprovalAttachmentFieldsView(Long fileId, Long amount, LocalDate date, String merchant) {
}

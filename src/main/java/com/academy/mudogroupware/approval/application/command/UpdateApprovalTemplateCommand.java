package com.academy.mudogroupware.approval.application.command;

import java.util.List;

public record UpdateApprovalTemplateCommand(
        Long templateId,
        String name,
        List<Long> approverIds,
        Long requesterId
) {
}

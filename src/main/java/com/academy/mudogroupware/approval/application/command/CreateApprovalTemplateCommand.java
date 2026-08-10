package com.academy.mudogroupware.approval.application.command;

import java.util.List;

public record CreateApprovalTemplateCommand(
        String name,
        Long creatorId,
        List<Long> approverIds
) {
}

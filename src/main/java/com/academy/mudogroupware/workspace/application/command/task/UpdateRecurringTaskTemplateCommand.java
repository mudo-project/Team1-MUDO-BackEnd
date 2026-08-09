package com.academy.mudogroupware.workspace.application.command.task;

import com.academy.mudogroupware.workspace.domain.model.task.RecurrenceType;
import java.util.Map;

// title은 단독으로 선택. recurrenceType·recurrenceRule은 항상 세트로 오거나 둘 다 null이다
// (Presentation 계층에서 검증). 셋 다 null인 요청은 Presentation 계층에서 400으로 걸러진다.
public record UpdateRecurringTaskTemplateCommand(
    Long workspaceId,
    Long templateId,
    Long requesterId,
    String title,
    RecurrenceType recurrenceType,
    Map<String, Object> recurrenceRule) {}

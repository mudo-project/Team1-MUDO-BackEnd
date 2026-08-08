package com.academy.mudogroupware.workspace.presentation.api.request.task;

import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.workspace.application.command.task.UpdateRecurringTaskTemplateCommand;
import com.academy.mudogroupware.workspace.domain.model.task.RecurrenceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record UpdateRecurringTaskTemplateRequest(
    @Schema(description = "새 템플릿 제목", example = "주간 출결 현황 정리(수정)")
        @Size(max = 200, message = "템플릿 제목은 200자 이하여야 합니다.")
        String title,
    @Schema(description = "새 반복 주기 타입. 변경하려면 recurrenceRule과 함께 보낸다.", example = "MONTHLY")
        RecurrenceType recurrenceType,
    @Schema(
            description = "새 주기별 부가 정보. recurrenceType과 함께 보낸다.",
            example = "{\"dayOfMonth\":1}")
        Map<String, Object> recurrenceRule) {

  public UpdateRecurringTaskTemplateRequest {
    title = title == null ? null : title.trim();
  }

  @AssertTrue(message = "제목 또는 반복 주기 중 최소 하나는 입력해야 합니다.")
  public boolean isAtLeastOneFieldPresent() {
    return title != null || recurrenceType != null;
  }

  @AssertTrue(message = "반복 주기 타입과 부가 정보는 함께 입력해야 합니다.")
  public boolean isRecurrencePairComplete() {
    return (recurrenceType == null) == (recurrenceRule == null);
  }

  @AssertTrue(message = "제목은 공백일 수 없습니다.")
  public boolean isTitleNotBlank() {
    return title == null || !title.isEmpty();
  }

  public UpdateRecurringTaskTemplateCommand toCommand(
      AuthUser authUser, Long workspaceId, Long templateId) {
    return new UpdateRecurringTaskTemplateCommand(
        workspaceId, templateId, authUser.userId(), title, recurrenceType, recurrenceRule);
  }
}

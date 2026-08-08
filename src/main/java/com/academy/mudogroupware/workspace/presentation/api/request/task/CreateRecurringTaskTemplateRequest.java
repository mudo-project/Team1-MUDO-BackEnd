package com.academy.mudogroupware.workspace.presentation.api.request.task;

import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.workspace.application.command.task.CreateRecurringTaskTemplateCommand;
import com.academy.mudogroupware.workspace.domain.model.task.RecurrenceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record CreateRecurringTaskTemplateRequest(
    @Schema(description = "템플릿 제목", example = "주간 출결 현황 정리")
        @NotBlank(message = "템플릿 제목은 필수입니다.")
        @Size(max = 200, message = "템플릿 제목은 200자 이하여야 합니다.")
        String title,
    @Schema(description = "반복 주기 타입", example = "WEEKLY")
        @NotNull(message = "반복 주기 타입은 필수입니다.")
        RecurrenceType recurrenceType,
    @Schema(
            description =
                "주기별 부가 정보. DAILY={}, WEEKLY={\"daysOfWeek\":[1,3,5]}, MONTHLY={\"dayOfMonth\":1}",
            example = "{\"daysOfWeek\":[1]}")
        @NotNull(message = "반복 주기 설정은 필수입니다.")
        Map<String, Object> recurrenceRule) {

  public CreateRecurringTaskTemplateRequest {
    title = title == null ? null : title.trim();
  }

  public CreateRecurringTaskTemplateCommand toCommand(AuthUser authUser, Long workspaceId) {
    return new CreateRecurringTaskTemplateCommand(
        workspaceId, authUser.userId(), title, recurrenceType, recurrenceRule);
  }
}

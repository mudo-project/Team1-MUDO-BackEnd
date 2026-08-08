package com.academy.mudogroupware.workspace.presentation.api.request.task;

import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.workspace.application.command.task.CreateTaskCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateTaskRequest(
    @Schema(description = "업무 제목", example = "8월 원생 청구서 발송")
        @NotBlank(message = "업무 제목은 필수입니다.")
        @Size(max = 200, message = "업무 제목은 200자 이하여야 합니다.")
        String title,
    @Schema(description = "마감일. 과거 날짜를 지정하면 지연 상태로 생성됩니다.", example = "2026-08-10")
        @NotNull(message = "마감일은 필수입니다.")
        LocalDate dueAt
) {

  public CreateTaskRequest {
    // @Size가 trim 전 원본 길이를 검증하지 않도록, 검증 이전에 미리 trim한다.
    // ("trim 후 최대 200자" 계약과 어긋나는 것을 방지)
    title = title == null ? null : title.trim();
  }

  public CreateTaskCommand toCommand(AuthUser authUser, Long workspaceId) {
    return new CreateTaskCommand(workspaceId, authUser.userId(), title, dueAt);
  }
}

package com.academy.mudogroupware.workspace.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record CreateTaskResponse(
    @Schema(description = "생성된 업무 번호", example = "101") Long taskId) {

  public static CreateTaskResponse from(Long taskId) {
    return CreateTaskResponse.builder().taskId(taskId).build();
  }
}

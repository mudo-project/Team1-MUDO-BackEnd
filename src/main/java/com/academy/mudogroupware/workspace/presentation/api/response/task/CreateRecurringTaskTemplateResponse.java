package com.academy.mudogroupware.workspace.presentation.api.response.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record CreateRecurringTaskTemplateResponse(
    @Schema(description = "생성된 템플릿 번호", example = "1") Long templateId) {

  public static CreateRecurringTaskTemplateResponse from(Long templateId) {
    return CreateRecurringTaskTemplateResponse.builder().templateId(templateId).build();
  }
}

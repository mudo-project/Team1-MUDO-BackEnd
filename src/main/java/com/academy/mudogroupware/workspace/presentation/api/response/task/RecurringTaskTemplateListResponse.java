package com.academy.mudogroupware.workspace.presentation.api.response.task;

import com.academy.mudogroupware.workspace.domain.model.task.RecurrenceType;
import com.academy.mudogroupware.workspace.domain.model.task.RecurringTaskTemplate;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Builder;

@Builder
public record RecurringTaskTemplateListResponse(
    @Schema(description = "템플릿 번호", example = "1") Long templateId,
    @Schema(description = "템플릿 제목", example = "주간 출결 현황 정리") String title,
    @Schema(description = "반복 주기 타입") RecurrenceType recurrenceType,
    @Schema(description = "반복 주기 부가 정보") Map<String, Object> recurrenceRule,
    @Schema(description = "생성자 사용자 번호", example = "10") Long createdBy) {

  public static RecurringTaskTemplateListResponse from(RecurringTaskTemplate template) {
    return RecurringTaskTemplateListResponse.builder()
        .templateId(template.getId())
        .title(template.getTitle())
        .recurrenceType(template.getRecurrenceType())
        .recurrenceRule(template.getRecurrenceRule())
        .createdBy(template.getCreatedBy())
        .build();
  }
}

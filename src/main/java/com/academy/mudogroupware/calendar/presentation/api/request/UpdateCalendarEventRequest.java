package com.academy.mudogroupware.calendar.presentation.api.request;

import java.time.LocalDateTime;

import com.academy.mudogroupware.calendar.application.command.UpdateCalendarEventCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCalendarEventRequest(
        @Schema(description = "일정 제목", example = "2학기 수업 준비 회의")
        @NotBlank(message = "일정 제목은 필수입니다.")
        @Size(max = 200, message = "일정 제목은 200자 이하여야 합니다.")
        String title,

        @Schema(description = "일정 내용", example = "2학기 수업 계획 논의 및 교재 배분")
        String content,

        @Schema(description = "일정 시작 일시", example = "2026-08-03T10:00:00+09:00")
        @NotNull(message = "일정 시작 일시는 필수입니다.")
        LocalDateTime eventStartAt,

        @Schema(description = "일정 종료 일시", example = "2026-08-03T11:30:00+09:00")
        LocalDateTime eventEndAt,

        @Schema(description = "종일 일정 여부", example = "false")
        boolean allDay,

        @Schema(description = "일정 표시 색상", example = "green")
        @Size(max = 20, message = "색상 값은 20자 이하여야 합니다.")
        String color
) {

    public UpdateCalendarEventCommand toCommand(Long eventId) {
        return new UpdateCalendarEventCommand(
                eventId, title, content, eventStartAt, eventEndAt, allDay, color);
    }
}

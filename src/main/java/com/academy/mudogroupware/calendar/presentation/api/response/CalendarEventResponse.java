package com.academy.mudogroupware.calendar.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.calendar.domain.model.CalendarEvent;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "캘린더 일정 응답")
public record CalendarEventResponse(
        @Schema(description = "일정 번호", example = "1") Long eventId,
        @Schema(description = "일정 제목", example = "2학기 수업 준비 회의") String title,
        @Schema(description = "일정 내용", example = "2학기 수업 계획 논의 및 교재 배분") String content,
        @Schema(description = "일정 시작 일시", example = "2026-08-03T10:00:00") LocalDateTime eventStartAt,
        @Schema(description = "일정 종료 일시", example = "2026-08-03T11:30:00") LocalDateTime eventEndAt,
        @Schema(description = "종일 일정 여부", example = "false") boolean allDay,
        @Schema(description = "표시 색상 코드", example = "green") String color,
        @Schema(description = "작성자 사용자 번호", example = "7") Long createdBy,
        @Schema(description = "생성 일시") LocalDateTime createdAt,
        @Schema(description = "수정 일시") LocalDateTime updatedAt
) {

    public static CalendarEventResponse from(CalendarEvent calendarEvent) {
        return new CalendarEventResponse(
                calendarEvent.getId(),
                calendarEvent.getTitle(),
                calendarEvent.getContent(),
                calendarEvent.getEventStartAt(),
                calendarEvent.getEventEndAt(),
                calendarEvent.isAllDay(),
                calendarEvent.getColor(),
                calendarEvent.getCreatedBy(),
                calendarEvent.getCreatedAt(),
                calendarEvent.getUpdatedAt());
    }
}

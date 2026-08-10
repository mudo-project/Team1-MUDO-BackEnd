package com.academy.mudogroupware.rollcall.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.rollcall.application.query.MessageTemplateView;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;

public record MessageTemplateResponse(
        Long id,
        String name,
        AttendanceStatus status,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static MessageTemplateResponse from(MessageTemplateView view) {
        return new MessageTemplateResponse(view.id(), view.name(), view.status(), view.content(), view.createdAt(),
                view.updatedAt());
    }
}

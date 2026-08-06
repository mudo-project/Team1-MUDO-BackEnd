package com.academy.mudogroupware.rollcall.application.query;

import java.time.LocalDateTime;

import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;

public record MessageTemplateView(
        Long id,
        String name,
        AttendanceStatus status,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

package com.academy.mudogroupware.memo.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.memo.domain.model.Memo;

public record MemoResponse(
        Long id,
        String title,
        String content,
        String color,
        Integer positionX,
        Integer positionY,
        Integer width,
        Integer height,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static MemoResponse from(Memo memo) {
        return new MemoResponse(memo.getId(), memo.getTitle(), memo.getContent(), memo.getColor(),
                memo.getPositionX(), memo.getPositionY(), memo.getWidth(), memo.getHeight(), memo.getCreatedAt(),
                memo.getUpdatedAt());
    }
}

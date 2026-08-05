package com.academy.mudogroupware.memo.domain.model;

import java.time.LocalDateTime;

import com.academy.mudogroupware.memo.domain.exception.MemoErrorCode;
import com.academy.mudogroupware.memo.domain.exception.MemoException;

public final class Memo {

    private final Long id;
    private final Long userId;
    private final String title;
    private final String content;
    private final MemoColor color;
    private final Integer positionX;
    private final Integer positionY;
    private final Integer width;
    private final Integer height;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private Memo(Long id, Long userId, String title, String content, MemoColor color, Integer positionX,
                 Integer positionY, Integer width, Integer height, LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (title == null || title.isBlank()) {
            throw new MemoException(MemoErrorCode.TITLE_REQUIRED);
        }
        if (color == null) {
            throw new MemoException(MemoErrorCode.COLOR_REQUIRED);
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.color = color;
        this.positionX = positionX;
        this.positionY = positionY;
        this.width = width;
        this.height = height;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Memo create(Long userId, String title, String content, MemoColor color, LocalDateTime now) {
        return new Memo(null, userId, title, content, color, null, null, null, null, now, now);
    }

    public static Memo restore(Long id, Long userId, String title, String content, MemoColor color,
                                Integer positionX, Integer positionY, Integer width, Integer height,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Memo(id, userId, title, content, color, positionX, positionY, width, height, createdAt,
                updatedAt);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public MemoColor getColor() {
        return color;
    }

    public Integer getPositionX() {
        return positionX;
    }

    public Integer getPositionY() {
        return positionY;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

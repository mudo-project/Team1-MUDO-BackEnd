package com.academy.mudogroupware.memo.domain.model;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

import com.academy.mudogroupware.memo.domain.exception.MemoErrorCode;
import com.academy.mudogroupware.memo.domain.exception.MemoException;

public final class Memo {

    private static final int TITLE_MAX_LENGTH = 100;
    private static final Pattern HEX_COLOR = Pattern.compile("^[0-9A-Fa-f]{6}$");

    private final Long id;
    private final Long userId;
    private String title;
    private String content;
    private String color;
    private Integer positionX;
    private Integer positionY;
    private Integer width;
    private Integer height;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Memo(Long id, Long userId, String title, String content, String color, Integer positionX,
                 Integer positionY, Integer width, Integer height, LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (title == null || title.isBlank()) {
            throw new MemoException(MemoErrorCode.TITLE_REQUIRED);
        }
        if (title.length() > TITLE_MAX_LENGTH) {
            throw new MemoException(MemoErrorCode.TITLE_TOO_LONG);
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

    public static Memo create(Long userId, String title, String content, String color, LocalDateTime now) {
        validateColor(color);
        return new Memo(null, userId, title, content, color, null, null, null, null, now, now);
    }

    // 검증하지 않는다: 2026-08-16 이전엔 색상을 12종 enum 이름(예: "MUSTARD")으로 저장했고, 그 실제
    // hex 값을 BE가 가진 적이 없어 변환표를 만들 수 없다. 새로 쓰는 값(create/updateColor)만 hex
    // 형식을 강제하고, 이미 저장된 값은 그대로 신뢰해 조회 시 예외가 나지 않게 한다.
    public static Memo restore(Long id, Long userId, String title, String content, String color,
                                Integer positionX, Integer positionY, Integer width, Integer height,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Memo(id, userId, title, content, color, positionX, positionY, width, height, createdAt,
                updatedAt);
    }

    public void updateContent(String title, String content, LocalDateTime now) {
        if (title == null || title.isBlank()) {
            throw new MemoException(MemoErrorCode.TITLE_REQUIRED);
        }
        if (title.length() > TITLE_MAX_LENGTH) {
            throw new MemoException(MemoErrorCode.TITLE_TOO_LONG);
        }
        this.title = title;
        this.content = content;
        this.updatedAt = now;
    }

    public void updateColor(String color, LocalDateTime now) {
        validateColor(color);
        this.color = color;
        this.updatedAt = now;
    }

    private static void validateColor(String color) {
        if (color == null || color.isBlank()) {
            throw new MemoException(MemoErrorCode.COLOR_REQUIRED);
        }
        if (!HEX_COLOR.matcher(color).matches()) {
            throw new MemoException(MemoErrorCode.COLOR_INVALID_FORMAT);
        }
    }

    public void updatePosition(int positionX, int positionY, int width, int height, LocalDateTime now) {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be positive");
        }
        this.positionX = positionX;
        this.positionY = positionY;
        this.width = width;
        this.height = height;
        this.updatedAt = now;
    }

    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
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

    public String getColor() {
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

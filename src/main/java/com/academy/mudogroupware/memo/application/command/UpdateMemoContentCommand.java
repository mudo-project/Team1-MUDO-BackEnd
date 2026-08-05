package com.academy.mudogroupware.memo.application.command;

public record UpdateMemoContentCommand(
        Long memoId,
        Long userId,
        String title,
        String content
) {
}

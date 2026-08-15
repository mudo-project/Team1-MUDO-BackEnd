package com.academy.mudogroupware.memo.application.command;

public record UpdateMemoColorCommand(
        Long memoId,
        Long userId,
        String color
) {
}

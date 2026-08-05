package com.academy.mudogroupware.memo.application.command;

public record DeleteMemoCommand(
        Long memoId,
        Long userId
) {
}

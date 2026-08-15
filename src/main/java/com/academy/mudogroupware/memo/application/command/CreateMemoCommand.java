package com.academy.mudogroupware.memo.application.command;

public record CreateMemoCommand(
        Long userId,
        String title,
        String content,
        String color
) {
}

package com.academy.mudogroupware.memo.application.command;

import com.academy.mudogroupware.memo.domain.model.MemoColor;

public record CreateMemoCommand(
        Long userId,
        String title,
        String content,
        MemoColor color
) {
}

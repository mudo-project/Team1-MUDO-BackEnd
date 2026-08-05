package com.academy.mudogroupware.memo.application.command;

import com.academy.mudogroupware.memo.domain.model.MemoColor;

public record UpdateMemoColorCommand(
        Long memoId,
        Long userId,
        MemoColor color
) {
}

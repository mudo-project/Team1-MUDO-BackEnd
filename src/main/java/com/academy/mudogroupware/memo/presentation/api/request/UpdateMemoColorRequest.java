package com.academy.mudogroupware.memo.presentation.api.request;

import com.academy.mudogroupware.memo.application.command.UpdateMemoColorCommand;
import com.academy.mudogroupware.memo.domain.model.MemoColor;

import jakarta.validation.constraints.NotNull;

public record UpdateMemoColorRequest(
        @NotNull MemoColor color
) {

    public UpdateMemoColorCommand toCommand(Long memoId, Long userId) {
        return new UpdateMemoColorCommand(memoId, userId, color);
    }
}

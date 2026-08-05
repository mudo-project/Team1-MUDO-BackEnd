package com.academy.mudogroupware.memo.presentation.api.request;

import com.academy.mudogroupware.memo.application.command.UpdateMemoContentCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMemoContentRequest(
        @NotBlank @Size(max = 100) String title,
        String content
) {

    public UpdateMemoContentCommand toCommand(Long memoId, Long userId) {
        return new UpdateMemoContentCommand(memoId, userId, title, content);
    }
}

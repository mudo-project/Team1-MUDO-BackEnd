package com.academy.mudogroupware.memo.presentation.api.request;

import com.academy.mudogroupware.memo.application.command.CreateMemoCommand;
import com.academy.mudogroupware.memo.domain.model.MemoColor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMemoRequest(
        @NotBlank @Size(max = 100) String title,
        String content,
        @NotNull MemoColor color
) {

    public CreateMemoCommand toCommand(Long userId) {
        return new CreateMemoCommand(userId, title, content, color);
    }
}

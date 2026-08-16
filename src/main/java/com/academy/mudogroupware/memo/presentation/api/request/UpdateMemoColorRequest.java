package com.academy.mudogroupware.memo.presentation.api.request;

import com.academy.mudogroupware.memo.application.command.UpdateMemoColorCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateMemoColorRequest(
        @NotBlank @Pattern(regexp = "^[0-9A-Fa-f]{6}$") String color
) {

    public UpdateMemoColorCommand toCommand(Long memoId, Long userId) {
        return new UpdateMemoColorCommand(memoId, userId, color);
    }
}

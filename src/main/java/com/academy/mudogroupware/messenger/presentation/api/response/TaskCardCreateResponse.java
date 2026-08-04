package com.academy.mudogroupware.messenger.presentation.api.response;

public record TaskCardCreateResponse(
        Long cardId
) {

    public static TaskCardCreateResponse from(Long cardId) {
        return new TaskCardCreateResponse(cardId);
    }
}

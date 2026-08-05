package com.academy.mudogroupware.memo.presentation.api.response;

public record MemoCreateResponse(
        Long id
) {

    public static MemoCreateResponse from(Long id) {
        return new MemoCreateResponse(id);
    }
}

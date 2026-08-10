package com.academy.mudogroupware.dataimport.presentation.api.response;

public record ImportCreatedResponse(Long importId) {

    public static ImportCreatedResponse from(Long importId) {
        return new ImportCreatedResponse(importId);
    }
}

package com.academy.mudogroupware.rollcall.presentation.api.response;

public record MessageTemplateCreateResponse(
        Long templateId
) {

    public static MessageTemplateCreateResponse from(Long templateId) {
        return new MessageTemplateCreateResponse(templateId);
    }
}

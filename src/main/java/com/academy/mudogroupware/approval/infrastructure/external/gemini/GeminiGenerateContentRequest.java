package com.academy.mudogroupware.approval.infrastructure.external.gemini;

import java.util.Base64;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

record GeminiGenerateContentRequest(List<Content> contents) {

    record Content(List<Part> parts) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Part(String text, InlineData inlineData) {

        static Part ofText(String text) {
            return new Part(text, null);
        }

        static Part ofInlineData(String mimeType, byte[] data) {
            return new Part(null, new InlineData(mimeType, Base64.getEncoder().encodeToString(data)));
        }
    }

    record InlineData(String mimeType, String data) {
    }

    static GeminiGenerateContentRequest ofText(String prompt) {
        return new GeminiGenerateContentRequest(List.of(new Content(List.of(Part.ofText(prompt)))));
    }

    static GeminiGenerateContentRequest ofInlineBinary(String instruction, String mimeType, byte[] data) {
        return new GeminiGenerateContentRequest(
                List.of(new Content(List.of(Part.ofText(instruction), Part.ofInlineData(mimeType, data)))));
    }
}

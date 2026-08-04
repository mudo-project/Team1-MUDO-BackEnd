package com.academy.mudogroupware.approval.infrastructure.external.gemini;

import java.util.List;

record GeminiGenerateContentRequest(List<Content> contents) {

    record Content(List<Part> parts) {
    }

    record Part(String text) {
    }

    static GeminiGenerateContentRequest of(String prompt) {
        return new GeminiGenerateContentRequest(List.of(new Content(List.of(new Part(prompt)))));
    }
}

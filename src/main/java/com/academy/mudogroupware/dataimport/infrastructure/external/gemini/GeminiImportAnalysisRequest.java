package com.academy.mudogroupware.dataimport.infrastructure.external.gemini;

import java.util.List;

record GeminiImportAnalysisRequest(List<Content> contents) {

    record Content(List<Part> parts) {
    }

    record Part(String text) {
    }

    static GeminiImportAnalysisRequest of(String prompt) {
        return new GeminiImportAnalysisRequest(List.of(new Content(List.of(new Part(prompt)))));
    }
}

package com.academy.mudogroupware.dataimport.infrastructure.external.gemini;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record DataImportGeminiProperties(String apiKey, String model) {

    public DataImportGeminiProperties(
            @Value("${GEMINI_API_KEY:}") String apiKey,
            @Value("${GEMINI_MODEL:gemini-2.0-flash}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }
}

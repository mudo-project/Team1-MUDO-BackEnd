package com.academy.mudogroupware.dataimport.infrastructure.external.gemini;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record DataImportGeminiProperties(String apiKey, String model) {

    // 특정 버전(예: gemini-2.0-flash)을 박아두면 Google이 그 버전을 종료할 때마다 죽는다(실제로 두 번
    // 겪음). gemini-flash-latest는 그 시점의 최신 GA flash 모델을 가리키는 공식 별칭이라 이 문제를 피한다.
    public DataImportGeminiProperties(
            @Value("${GEMINI_API_KEY:}") String apiKey,
            @Value("${GEMINI_MODEL:gemini-flash-latest}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }
}

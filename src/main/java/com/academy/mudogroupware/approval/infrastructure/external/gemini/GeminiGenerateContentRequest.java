package com.academy.mudogroupware.approval.infrastructure.external.gemini;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
record GeminiGenerateContentRequest(List<Content> contents, GenerationConfig generationConfig) {

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

    /**
     * responseSchema를 지정하면 Gemini가 자유 텍스트 대신 그 스키마를 따르는 JSON 문자열을 응답으로 준다
     * (구조화 출력). 요약처럼 자유 텍스트만 필요하면 이 필드 자체를 생략한다.
     */
    record GenerationConfig(String responseMimeType, ResponseSchema responseSchema) {
    }

    record ResponseSchema(String type, Map<String, SchemaProperty> properties, List<String> required) {
    }

    record SchemaProperty(String type, String description) {
    }

    static GeminiGenerateContentRequest ofText(String prompt) {
        return new GeminiGenerateContentRequest(List.of(new Content(List.of(Part.ofText(prompt)))), null);
    }

    static GeminiGenerateContentRequest ofInlineBinary(String instruction, String mimeType, byte[] data) {
        return new GeminiGenerateContentRequest(
                List.of(new Content(List.of(Part.ofText(instruction), Part.ofInlineData(mimeType, data)))), null);
    }

    static GeminiGenerateContentRequest ofTextWithSchema(String prompt, ResponseSchema schema) {
        return new GeminiGenerateContentRequest(List.of(new Content(List.of(Part.ofText(prompt)))),
                new GenerationConfig("application/json", schema));
    }

    static GeminiGenerateContentRequest ofInlineBinaryWithSchema(String instruction, String mimeType, byte[] data,
                                                                   ResponseSchema schema) {
        return new GeminiGenerateContentRequest(
                List.of(new Content(List.of(Part.ofText(instruction), Part.ofInlineData(mimeType, data)))),
                new GenerationConfig("application/json", schema));
    }
}

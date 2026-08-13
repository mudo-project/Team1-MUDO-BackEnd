package com.academy.mudogroupware.approval.infrastructure.external.gemini;

import java.util.List;

record GeminiGenerateContentResponse(List<Candidate> candidates, UsageMetadata usageMetadata) {

    record Candidate(Content content) {
    }

    record Content(List<Part> parts) {
    }

    record Part(String text) {
    }

    // Gemini가 매 응답마다 함께 내려주는 토큰 사용량. 요청 실패 등으로 응답 자체가 비어 있으면 null일 수 있다.
    record UsageMetadata(Integer promptTokenCount, Integer candidatesTokenCount, Integer totalTokenCount) {
    }

    String firstText() {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        Content content = candidates.get(0).content();
        if (content == null || content.parts() == null || content.parts().isEmpty()) {
            return null;
        }
        return content.parts().get(0).text();
    }
}

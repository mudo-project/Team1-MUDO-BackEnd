package com.academy.mudogroupware.approval.infrastructure.external.gemini;

import java.util.List;

record GeminiGenerateContentResponse(List<Candidate> candidates) {

    record Candidate(Content content) {
    }

    record Content(List<Part> parts) {
    }

    record Part(String text) {
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

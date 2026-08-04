package com.academy.mudogroupware.approval.infrastructure.external.gemini;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.academy.mudogroupware.approval.application.port.AttachmentSummarizationException;
import com.academy.mudogroupware.approval.application.port.AttachmentSummarizerPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GeminiSummarizerAdapter implements AttachmentSummarizerPort {

    private final RestClient geminiRestClient;
    private final GeminiProperties geminiProperties;

    @Override
    public String summarize(String content) {
        GeminiGenerateContentResponse response;
        try {
            response = geminiRestClient.post()
                    .uri("/v1beta/models/{model}:generateContent", geminiProperties.model())
                    .header("x-goog-api-key", geminiProperties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(GeminiGenerateContentRequest.of(content))
                    .retrieve()
                    .body(GeminiGenerateContentResponse.class);
        } catch (RestClientException e) {
            throw new AttachmentSummarizationException("Gemini API 호출에 실패했습니다.", e);
        }

        String text = response != null ? response.firstText() : null;
        if (text == null || text.isBlank()) {
            throw new AttachmentSummarizationException("Gemini 응답에 요약 텍스트가 없습니다.");
        }
        return text;
    }
}

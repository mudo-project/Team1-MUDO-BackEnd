package com.academy.mudogroupware.approval.infrastructure.external.gemini;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.academy.mudogroupware.approval.application.port.AttachmentContent;
import com.academy.mudogroupware.approval.application.port.AttachmentSummarizationException;
import com.academy.mudogroupware.approval.application.port.AttachmentSummarizerPort;
import com.academy.mudogroupware.global.infrastructure.observability.ai.GeminiTokenUsageTracker;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GeminiSummarizerAdapter implements AttachmentSummarizerPort {

    private static final String FEATURE = "approval-attachment-summary";

    private static final String SUMMARY_INSTRUCTION = """
            다음은 사내 전자결재에 첨부된 문서입니다. 결재자가 승인 여부를 빠르게 판단할 수 있도록
            핵심 내용을 3~5문장의 한국어 평문으로 요약해 주세요. 금액, 기간, 대상자 등 숫자·고유명사는
            빠뜨리지 말고 그대로 포함하고, 요약 외의 부연 설명이나 마크다운 서식은 넣지 마세요.
            """;

    private final RestClient geminiRestClient;
    private final GeminiProperties geminiProperties;
    private final GeminiTokenUsageTracker tokenUsageTracker;

    @Override
    public String summarize(AttachmentContent content) {
        GeminiGenerateContentRequest request = switch (content.kind()) {
            case TEXT -> GeminiGenerateContentRequest.ofText(SUMMARY_INSTRUCTION + "\n\n" + content.text());
            case BINARY -> GeminiGenerateContentRequest.ofInlineBinary(
                    SUMMARY_INSTRUCTION, content.mimeType(), content.binaryData());
        };

        GeminiGenerateContentResponse response;
        try {
            response = geminiRestClient.post()
                    .uri("/v1beta/models/{model}:generateContent", geminiProperties.model())
                    .header("x-goog-api-key", geminiProperties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiGenerateContentResponse.class);
        } catch (RestClientException e) {
            throw new AttachmentSummarizationException("Gemini API 호출에 실패했습니다.", e);
        }

        if (response != null && response.usageMetadata() != null) {
            GeminiGenerateContentResponse.UsageMetadata usage = response.usageMetadata();
            tokenUsageTracker.record(FEATURE, geminiProperties.model(), usage.promptTokenCount(),
                    usage.candidatesTokenCount(), usage.totalTokenCount());
        }

        String text = response != null ? response.firstText() : null;
        if (text == null || text.isBlank()) {
            throw new AttachmentSummarizationException("Gemini 응답에 요약 텍스트가 없습니다.");
        }
        return text;
    }
}

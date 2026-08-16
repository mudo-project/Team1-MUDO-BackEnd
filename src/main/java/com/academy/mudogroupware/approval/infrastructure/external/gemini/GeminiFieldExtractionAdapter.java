package com.academy.mudogroupware.approval.infrastructure.external.gemini;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.academy.mudogroupware.approval.application.port.AttachmentContent;
import com.academy.mudogroupware.approval.application.port.AttachmentFieldExtractionException;
import com.academy.mudogroupware.approval.application.port.AttachmentFieldExtractorPort;
import com.academy.mudogroupware.approval.application.port.ExtractedReceiptFields;
import com.academy.mudogroupware.global.infrastructure.observability.ai.GeminiTokenUsageTracker;
import com.academy.mudogroupware.planquota.application.service.CurrentPlanProvider;
import com.academy.mudogroupware.planquota.domain.exception.PlanLimitErrorCode;
import com.academy.mudogroupware.planquota.domain.exception.PlanLimitExceededException;
import com.academy.mudogroupware.resourceusage.application.port.ResourceUsageQueryPort;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageType;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * Gemini의 structured output(responseSchema) 기능으로 영수증류 첨부파일에서 금액/일자/가맹점을
 * JSON으로 강제 추출한다. 자유 텍스트 요약(GeminiSummarizerAdapter)과 달리 응답 자체가 JSON 문자열이라
 * 후처리(파싱)가 필요하다.
 */
@Component
@RequiredArgsConstructor
public class GeminiFieldExtractionAdapter implements AttachmentFieldExtractorPort {

    private static final String FEATURE = "approval-attachment-field-extraction";

    private static final String EXTRACTION_INSTRUCTION = """
            다음은 사내 법인카드 경비 정산 결재에 첨부된 영수증입니다. 영수증에서 결제 금액, 결제 일자,
            가맹점명을 추출해 주세요. 금액은 원 단위 정수로, 일자는 YYYY-MM-DD 형식으로 답하세요.
            해당 정보를 문서에서 찾을 수 없으면 그 필드는 null로 두세요.
            """;

    private static final GeminiGenerateContentRequest.ResponseSchema RECEIPT_FIELDS_SCHEMA =
            new GeminiGenerateContentRequest.ResponseSchema("OBJECT", Map.of(
                    "amount", new GeminiGenerateContentRequest.SchemaProperty("INTEGER", "결제 금액(원 단위 정수)"),
                    "date", new GeminiGenerateContentRequest.SchemaProperty("STRING", "결제 일자(YYYY-MM-DD)"),
                    "merchant", new GeminiGenerateContentRequest.SchemaProperty("STRING", "가맹점명")),
                    List.of());

    private final RestClient geminiRestClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;
    private final GeminiTokenUsageTracker tokenUsageTracker;
    private final ResourceUsageQueryPort resourceUsageQueryPort;
    private final CurrentPlanProvider currentPlanProvider;

    @Override
    public ExtractedReceiptFields extract(AttachmentContent content) {
        checkAiTokenLimit();

        GeminiGenerateContentRequest request = switch (content.kind()) {
            case TEXT -> GeminiGenerateContentRequest.ofTextWithSchema(
                    EXTRACTION_INSTRUCTION + "\n\n" + content.text(), RECEIPT_FIELDS_SCHEMA);
            case BINARY -> GeminiGenerateContentRequest.ofInlineBinaryWithSchema(
                    EXTRACTION_INSTRUCTION, content.mimeType(), content.binaryData(), RECEIPT_FIELDS_SCHEMA);
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
            throw new AttachmentFieldExtractionException("Gemini API 호출에 실패했습니다.", e);
        }

        if (response != null && response.usageMetadata() != null) {
            GeminiGenerateContentResponse.UsageMetadata usage = response.usageMetadata();
            tokenUsageTracker.record(FEATURE, geminiProperties.model(), usage.promptTokenCount(),
                    usage.candidatesTokenCount(), usage.totalTokenCount());
        }

        String json = response != null ? response.firstText() : null;
        if (json == null || json.isBlank()) {
            throw new AttachmentFieldExtractionException("Gemini 응답에 추출 결과가 없습니다.");
        }

        RawFields raw;
        try {
            raw = objectMapper.readValue(json, RawFields.class);
        } catch (Exception e) {
            throw new AttachmentFieldExtractionException("Gemini 응답을 파싱하지 못했습니다.", e);
        }
        return new ExtractedReceiptFields(raw.amount(), parseDate(raw.date()), raw.merchant());
    }

    private void checkAiTokenLimit() {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime to = from.plusMonths(1);
        long current = resourceUsageQueryPort.sumByTypeAndPeriod(ResourceUsageType.AI_TOKEN, from, to);
        long limit = currentPlanProvider.currentLimits().aiTokenMonthlyLimit();
        if (current >= limit) {
            throw new PlanLimitExceededException(PlanLimitErrorCode.AI_TOKEN_LIMIT_EXCEEDED,
                    currentPlanProvider.currentPlan(), limit, current);
        }
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private record RawFields(Long amount, String date, String merchant) {
    }
}

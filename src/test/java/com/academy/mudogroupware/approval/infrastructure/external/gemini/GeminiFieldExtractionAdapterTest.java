package com.academy.mudogroupware.approval.infrastructure.external.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.academy.mudogroupware.approval.application.port.AttachmentContent;
import com.academy.mudogroupware.approval.application.port.AttachmentFieldExtractionException;
import com.academy.mudogroupware.approval.application.port.ExtractedReceiptFields;
import com.academy.mudogroupware.global.infrastructure.observability.ai.GeminiTokenUsageTracker;
import com.academy.mudogroupware.planquota.application.service.CurrentPlanProvider;
import com.academy.mudogroupware.planquota.domain.exception.PlanLimitExceededException;
import com.academy.mudogroupware.planquota.domain.model.Plan;
import com.academy.mudogroupware.planquota.domain.model.PlanLimits;
import com.academy.mudogroupware.resourceusage.application.port.ResourceUsageQueryPort;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageType;
import com.fasterxml.jackson.databind.ObjectMapper;

class GeminiFieldExtractionAdapterTest {

    private static final String GENERATE_CONTENT_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-test:generateContent";

    private final GeminiTokenUsageTracker tokenUsageTracker = new GeminiTokenUsageTracker();
    private final ResourceUsageQueryPort resourceUsageQueryPort = unlimitedUsage();
    private final CurrentPlanProvider currentPlanProvider = unlimitedPlan();

    private RestClient.Builder builder() {
        return RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");
    }

    private GeminiFieldExtractionAdapter adapter(RestClient.Builder builder) {
        return new GeminiFieldExtractionAdapter(builder.build(), new GeminiProperties("test-key", "gemini-test"),
                new ObjectMapper(), tokenUsageTracker, resourceUsageQueryPort, currentPlanProvider);
    }

    private static ResourceUsageQueryPort unlimitedUsage() {
        ResourceUsageQueryPort stub = mock(ResourceUsageQueryPort.class);
        when(stub.sumByTypeAndPeriod(eq(ResourceUsageType.AI_TOKEN), any(), any())).thenReturn(0L);
        return stub;
    }

    private static CurrentPlanProvider unlimitedPlan() {
        CurrentPlanProvider stub = mock(CurrentPlanProvider.class);
        when(stub.currentLimits()).thenReturn(PlanLimits.of(Plan.PAID));
        return stub;
    }

    @Test
    void throwsWhenMonthlyAiTokenLimitReachedWithoutCallingGemini() {
        ResourceUsageQueryPort exhaustedUsage = mock(ResourceUsageQueryPort.class);
        when(exhaustedUsage.sumByTypeAndPeriod(eq(ResourceUsageType.AI_TOKEN), any(), any())).thenReturn(100_000L);
        CurrentPlanProvider freePlan = mock(CurrentPlanProvider.class);
        when(freePlan.currentPlan()).thenReturn(Plan.FREE);
        when(freePlan.currentLimits()).thenReturn(PlanLimits.of(Plan.FREE));
        RestClient.Builder builder = builder();
        MockRestServiceServer.bindTo(builder).build(); // 요청이 하나도 등록되지 않았으므로, 실제로
        // Gemini를 호출하려 하면 즉시 실패한다 — 한도 체크가 호출 전에 막는지 검증하는 안전장치.
        GeminiFieldExtractionAdapter adapter = new GeminiFieldExtractionAdapter(builder.build(),
                new GeminiProperties("test-key", "gemini-test"), new ObjectMapper(), tokenUsageTracker,
                exhaustedUsage, freePlan);

        assertThatThrownBy(() -> adapter.extract(AttachmentContent.text("영수증 내용")))
                .isInstanceOf(PlanLimitExceededException.class);
    }

    @Test
    void parsesStructuredJsonResponseIntoFields() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(GENERATE_CONTENT_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"{\\"amount\\":45000,\\"date\\":\\"2026-08-05\\",\\"merchant\\":\\"스타벅스 강남점\\"}"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        ExtractedReceiptFields fields = adapter(builder).extract(AttachmentContent.text("영수증 내용"));

        assertThat(fields.amount()).isEqualTo(45000L);
        assertThat(fields.date()).isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(fields.merchant()).isEqualTo("스타벅스 강남점");
        server.verify();
    }

    @Test
    void recordsTokenUsageWhenGeminiReturnsUsageMetadata() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(GENERATE_CONTENT_URL))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"{\\"amount\\":1000,\\"date\\":null,\\"merchant\\":null}"}]}}],
                         "usageMetadata":{"promptTokenCount":120,"candidatesTokenCount":30,"totalTokenCount":150}}
                        """, MediaType.APPLICATION_JSON));

        adapter(builder).extract(AttachmentContent.text("영수증 내용"));

        assertThat(tokenUsageTracker.snapshot()).hasSize(1);
        assertThat(tokenUsageTracker.snapshot().get(0).feature()).isEqualTo("approval-attachment-field-extraction");
        assertThat(tokenUsageTracker.snapshot().get(0).totalTokens()).isEqualTo(150);
        server.verify();
    }

    @Test
    void treatsMissingFieldsAsNullWithoutFailing() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(GENERATE_CONTENT_URL))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"{\\"amount\\":null,\\"date\\":null,\\"merchant\\":\\"이름 모를 가게\\"}"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        ExtractedReceiptFields fields = adapter(builder).extract(AttachmentContent.binary(
                new byte[] {0x25, 0x50, 0x44, 0x46}, "application/pdf"));

        assertThat(fields.amount()).isNull();
        assertThat(fields.date()).isNull();
        assertThat(fields.merchant()).isEqualTo("이름 모를 가게");
        server.verify();
    }

    @Test
    void treatsUnparseableDateAsNullWithoutFailing() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(GENERATE_CONTENT_URL))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"{\\"amount\\":1000,\\"date\\":\\"모름\\",\\"merchant\\":\\"가게\\"}"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        ExtractedReceiptFields fields = adapter(builder).extract(AttachmentContent.text("영수증 내용"));

        assertThat(fields.date()).isNull();
        server.verify();
    }

    @Test
    void throwsWhenGeminiCallFails() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(GENERATE_CONTENT_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> adapter(builder).extract(AttachmentContent.text("내용")))
                .isInstanceOf(AttachmentFieldExtractionException.class);
        server.verify();
    }

    @Test
    void throwsWhenResponseHasNoText() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(GENERATE_CONTENT_URL))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter(builder).extract(AttachmentContent.text("내용")))
                .isInstanceOf(AttachmentFieldExtractionException.class);
        server.verify();
    }

    @Test
    void throwsWhenResponseTextIsNotValidJson() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(GENERATE_CONTENT_URL))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"이건 JSON이 아닙니다"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter(builder).extract(AttachmentContent.text("내용")))
                .isInstanceOf(AttachmentFieldExtractionException.class);
        server.verify();
    }
}

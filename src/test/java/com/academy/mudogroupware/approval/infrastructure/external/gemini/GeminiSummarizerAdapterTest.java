package com.academy.mudogroupware.approval.infrastructure.external.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.academy.mudogroupware.approval.application.port.AttachmentContent;
import com.academy.mudogroupware.approval.application.port.AttachmentSummarizationException;
import com.academy.mudogroupware.global.infrastructure.observability.ai.GeminiTokenUsageTracker;
import com.academy.mudogroupware.planquota.application.service.CurrentPlanProvider;
import com.academy.mudogroupware.planquota.domain.exception.PlanLimitExceededException;
import com.academy.mudogroupware.planquota.domain.model.Plan;
import com.academy.mudogroupware.planquota.domain.model.PlanLimits;
import com.academy.mudogroupware.resourceusage.application.port.ResourceUsageQueryPort;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageType;

class GeminiSummarizerAdapterTest {

    private static final String GENERATE_CONTENT_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-test:generateContent";

    private final GeminiTokenUsageTracker tokenUsageTracker = new GeminiTokenUsageTracker();
    private final ResourceUsageQueryPort resourceUsageQueryPort = unlimitedUsage();
    private final CurrentPlanProvider currentPlanProvider = unlimitedPlan();

    private RestClient.Builder builder() {
        return RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");
    }

    private GeminiSummarizerAdapter adapter(RestClient.Builder builder) {
        return new GeminiSummarizerAdapter(builder.build(), new GeminiProperties("test-key", "gemini-test"),
                tokenUsageTracker, resourceUsageQueryPort, currentPlanProvider);
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
        MockRestServiceServer.bindTo(builder).build();
        GeminiSummarizerAdapter adapter = new GeminiSummarizerAdapter(builder.build(),
                new GeminiProperties("test-key", "gemini-test"), tokenUsageTracker, exhaustedUsage, freePlan);

        assertThatThrownBy(() -> adapter.summarize(AttachmentContent.text("문서 내용")))
                .isInstanceOf(PlanLimitExceededException.class);
    }

    @Test
    void sendsTextPromptAndReturnsSummaryForTextContent() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(GENERATE_CONTENT_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("휴가 신청 사유")))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"요약: 휴가 신청입니다."}]}}]}
                        """, MediaType.APPLICATION_JSON));

        String summary = adapter(builder).summarize(AttachmentContent.text("휴가 신청 사유는 개인 사정입니다."));

        assertThat(summary).isEqualTo("요약: 휴가 신청입니다.");
        server.verify();
    }

    @Test
    void recordsTokenUsageWhenGeminiReturnsUsageMetadata() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(GENERATE_CONTENT_URL))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"요약입니다."}]}}],
                         "usageMetadata":{"promptTokenCount":200,"candidatesTokenCount":50,"totalTokenCount":250}}
                        """, MediaType.APPLICATION_JSON));

        adapter(builder).summarize(AttachmentContent.text("내용"));

        assertThat(tokenUsageTracker.snapshot()).hasSize(1);
        assertThat(tokenUsageTracker.snapshot().get(0).feature()).isEqualTo("approval-attachment-summary");
        assertThat(tokenUsageTracker.snapshot().get(0).totalTokens()).isEqualTo(250);
        server.verify();
    }

    @Test
    void sendsInlineBase64DataForBinaryContentLikePdf() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        byte[] pdfBytes = "%PDF-1.4 fake".getBytes(StandardCharsets.UTF_8);
        String expectedBase64 = Base64.getEncoder().encodeToString(pdfBytes);
        server.expect(requestTo(GENERATE_CONTENT_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(expectedBase64)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"mimeType\":\"application/pdf\"")))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"PDF 요약 결과"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        String summary = adapter(builder).summarize(AttachmentContent.binary(pdfBytes, "application/pdf"));

        assertThat(summary).isEqualTo("PDF 요약 결과");
        server.verify();
    }

    @Test
    void throwsWhenGeminiCallFails() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(GENERATE_CONTENT_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> adapter(builder).summarize(AttachmentContent.text("내용")))
                .isInstanceOf(AttachmentSummarizationException.class);
        server.verify();
    }

    @Test
    void throwsWhenResponseHasNoSummaryText() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(GENERATE_CONTENT_URL))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter(builder).summarize(AttachmentContent.text("내용")))
                .isInstanceOf(AttachmentSummarizationException.class);
        server.verify();
    }
}

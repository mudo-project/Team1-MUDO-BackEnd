package com.academy.mudogroupware.approval.infrastructure.external.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

class GeminiSummarizerAdapterTest {

    private static final String GENERATE_CONTENT_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-test:generateContent";

    private RestClient.Builder builder() {
        return RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");
    }

    private GeminiSummarizerAdapter adapter(RestClient.Builder builder) {
        return new GeminiSummarizerAdapter(builder.build(), new GeminiProperties("test-key", "gemini-test"));
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

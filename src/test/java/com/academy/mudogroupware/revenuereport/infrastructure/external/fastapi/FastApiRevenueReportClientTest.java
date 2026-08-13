package com.academy.mudogroupware.revenuereport.infrastructure.external.fastapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import com.academy.mudogroupware.revenuereport.domain.exception.RevenueReportAiException;

import com.academy.mudogroupware.revenuereport.application.port.RevenueSnapshot;

class FastApiRevenueReportClientTest {

    private RevenueSnapshot sampleSnapshot() {
        return new RevenueSnapshot(LocalDate.of(2026, 8, 1),
                new RevenueSnapshot.Revenue(5000000, 4200000),
                new RevenueSnapshot.Expense(1200000, List.of()),
                new RevenueSnapshot.Profit(3000000, 3800000),
                RevenueSnapshot.PreviousMonth.unavailable(),
                List.of(), List.of());
    }

    @Test
    void throwsWhenBaseUrlIsMissing() {
        FastApiRevenueReportClient client = new FastApiRevenueReportClient(RestClient.builder().build(),
                new RevenueReportAiEngineProperties("", "/api/revenue-report/generate", "", 2000, 15000));

        assertThatThrownBy(() -> client.generateReport(sampleSnapshot()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void callsFastApiAndReturnsReportText() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FastApiRevenueReportClient client = new FastApiRevenueReportClient(builder.build(),
                new RevenueReportAiEngineProperties("http://localhost:8000", "/api/revenue-report/generate",
                        "secret", 2000, 15000));
        server.expect(requestTo("http://localhost:8000/api/revenue-report/generate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Revenue-Report-Ai-Key", "secret"))
                // 요청 바디는 RevenueSnapshot 필드가 최상위에 그대로 와야 한다(FastAPI가 그 형태를
                // 기대함) — {"snapshot": {...}}처럼 감싸면 FastAPI에서 422가 난다(실제로 났었음).
                .andExpect(jsonPath("$.targetMonth").exists())
                .andExpect(jsonPath("$.snapshot").doesNotExist())
                .andRespond(withSuccess("""
                        {"report":"8월 매출은 420만원으로 전월 대비 안정적입니다."}
                        """, MediaType.APPLICATION_JSON));

        String result = client.generateReport(sampleSnapshot());

        assertThat(result).isEqualTo("8월 매출은 420만원으로 전월 대비 안정적입니다.");
        server.verify();
    }

    @Test
    void retriesOnServerErrorAndSucceedsOnLaterAttempt() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FastApiRevenueReportClient client = new FastApiRevenueReportClient(builder.build(),
                new RevenueReportAiEngineProperties("http://localhost:8000", "/api/revenue-report/generate",
                        "secret", 2000, 15000));
        // 5xx가 두 번 발생해도 세 번째 시도에서 성공하면 재시도로 살아나야 한다.
        server.expect(requestTo("http://localhost:8000/api/revenue-report/generate"))
                .andRespond(withServerError());
        server.expect(requestTo("http://localhost:8000/api/revenue-report/generate"))
                .andRespond(withServerError());
        server.expect(requestTo("http://localhost:8000/api/revenue-report/generate"))
                .andRespond(withSuccess("""
                        {"report":"재시도 끝에 성공한 리포트"}
                        """, MediaType.APPLICATION_JSON));

        String result = client.generateReport(sampleSnapshot());

        assertThat(result).isEqualTo("재시도 끝에 성공한 리포트");
        server.verify();
    }

    @Test
    void doesNotRetryOnClientError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FastApiRevenueReportClient client = new FastApiRevenueReportClient(builder.build(),
                new RevenueReportAiEngineProperties("http://localhost:8000", "/api/revenue-report/generate",
                        "secret", 2000, 15000));
        // 4xx는 요청 자체가 잘못됐다는 뜻이므로 재시도하지 않는다 — 두 번째 요청이 오면
        // 서버에 등록해둔 기대치가 없어서 테스트가 실패한다.
        server.expect(requestTo("http://localhost:8000/api/revenue-report/generate"))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.generateReport(sampleSnapshot()))
                .isInstanceOf(RevenueReportAiException.class);
        server.verify();
    }
}

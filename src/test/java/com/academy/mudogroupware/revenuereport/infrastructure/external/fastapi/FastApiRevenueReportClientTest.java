package com.academy.mudogroupware.revenuereport.infrastructure.external.fastapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.academy.mudogroupware.revenuereport.application.service.RevenueSnapshot;

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
                .andRespond(withSuccess("""
                        {"report":"8월 매출은 420만원으로 전월 대비 안정적입니다."}
                        """, MediaType.APPLICATION_JSON));

        String result = client.generateReport(sampleSnapshot());

        assertThat(result).isEqualTo("8월 매출은 420만원으로 전월 대비 안정적입니다.");
        server.verify();
    }
}

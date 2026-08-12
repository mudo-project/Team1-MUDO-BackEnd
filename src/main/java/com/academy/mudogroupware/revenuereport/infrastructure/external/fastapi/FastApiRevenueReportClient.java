package com.academy.mudogroupware.revenuereport.infrastructure.external.fastapi;

import java.net.URI;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.academy.mudogroupware.revenuereport.application.port.RevenueReportAiPort;
import com.academy.mudogroupware.revenuereport.application.service.RevenueSnapshot;
import com.academy.mudogroupware.revenuereport.domain.exception.RevenueReportAiException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class FastApiRevenueReportClient implements RevenueReportAiPort {

    private static final String API_KEY_HEADER = "X-Revenue-Report-Ai-Key";

    private final RestClient restClient;
    private final RevenueReportAiEngineProperties properties;

    /**
     * Spring이 자동 구성한 ObjectMapper(날짜를 [2026,7,1] 배열이 아니라 "2026-07-01"
     * ISO 문자열로 직렬화)를 그대로 주입받아 쓴다. RestClient.builder()를 아무 설정 없이
     * 쓰면 별도의 기본 ObjectMapper가 생겨서 FastAPI(Pydantic)가 날짜 필드를 거부한다
     * (직접 겪은 버그 — 로컬 통합 테스트에서 422로 드러났다).
     */
    @Autowired
    public FastApiRevenueReportClient(RevenueReportAiEngineProperties properties, ObjectMapper objectMapper) {
        this(RestClient.builder()
                        .requestFactory(requestFactory(properties))
                        .messageConverters(converters -> converters.add(0,
                                new MappingJackson2HttpMessageConverter(objectMapper)))
                        .build(),
                properties);
    }

    FastApiRevenueReportClient(RestClient restClient, RevenueReportAiEngineProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public String generateReport(RevenueSnapshot snapshot) {
        if (!properties.enabled()) {
            throw new IllegalStateException("REVENUE_REPORT_AI_BASE_URL is not configured.");
        }

        URI uri = properties.generateUri();
        properties.validateCredentialFor(uri);

        RestClient.RequestBodySpec request = restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
        if (!properties.apiKey().isBlank()) {
            request.header(API_KEY_HEADER, properties.apiKey());
        }

        FastApiRevenueReportResponse response;
        try {
            response = request
                    .body(snapshot)
                    .retrieve()
                    .body(FastApiRevenueReportResponse.class);
        } catch (org.springframework.web.client.RestClientException e) {
            throw new RevenueReportAiException("매출 리포트 AI 서버 호출에 실패했습니다.", e);
        }

        if (response == null || response.report() == null || response.report().isBlank()) {
            throw new RevenueReportAiException("매출 리포트 AI 응답에 텍스트가 없습니다.");
        }
        return response.report();
    }

    private static SimpleClientHttpRequestFactory requestFactory(RevenueReportAiEngineProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.connectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));
        return requestFactory;
    }
}

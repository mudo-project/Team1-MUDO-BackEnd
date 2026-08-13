package com.academy.mudogroupware.revenuereport.infrastructure.external.fastapi;

import java.net.URI;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.academy.mudogroupware.revenuereport.application.port.RevenueReportAiPort;
import com.academy.mudogroupware.revenuereport.application.port.RevenueSnapshot;
import com.academy.mudogroupware.revenuereport.domain.exception.RevenueReportAiException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class FastApiRevenueReportClient implements RevenueReportAiPort {

    private static final String API_KEY_HEADER = "X-Revenue-Report-Ai-Key";
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration INITIAL_BACKOFF = Duration.ofMillis(200);

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

        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return callOnce(uri, snapshot);
            } catch (HttpServerErrorException | ResourceAccessException e) {
                // 월 1회 배치의 단일 경로다 — 일시적인 5xx/timeout으로 그 달 리포트가 아예
                // 생성되지 않으면 다음 시도는 한 달 뒤다. 4xx는 요청 자체가 잘못됐다는 뜻이라
                // 재시도해도 같은 결과이므로 대상에서 제외한다(아래 RestClientException으로 처리).
                lastFailure = e;
                if (attempt < MAX_ATTEMPTS) {
                    sleep(INITIAL_BACKOFF.multipliedBy(1L << (attempt - 1)));
                }
            } catch (RestClientException e) {
                throw new RevenueReportAiException("매출 리포트 AI 서버 호출에 실패했습니다.", e);
            }
        }
        throw new RevenueReportAiException(
                "매출 리포트 AI 서버 호출에 실패했습니다(" + MAX_ATTEMPTS + "회 재시도 소진).", lastFailure);
    }

    private String callOnce(URI uri, RevenueSnapshot snapshot) {
        RestClient.RequestBodySpec request = restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
        if (!properties.apiKey().isBlank()) {
            request.header(API_KEY_HEADER, properties.apiKey());
        }

        FastApiRevenueReportResponse response = request
                .body(snapshot)
                .retrieve()
                .body(FastApiRevenueReportResponse.class);

        if (response == null || response.report() == null || response.report().isBlank()) {
            throw new RevenueReportAiException("매출 리포트 AI 응답에 텍스트가 없습니다.");
        }
        return response.report();
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RevenueReportAiException("매출 리포트 AI 재시도 대기 중 인터럽트되었습니다.", e);
        }
    }

    private static SimpleClientHttpRequestFactory requestFactory(RevenueReportAiEngineProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.connectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));
        return requestFactory;
    }
}

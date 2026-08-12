package com.academy.mudogroupware.revenuereport.infrastructure.external.fastapi;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RevenueReportAiEnginePropertiesTest {

    @Test
    void skipsValidationWhenDisabled() {
        RevenueReportAiEngineProperties properties =
                new RevenueReportAiEngineProperties("", "/api/revenue-report/generate", "", 2000, 15000);

        // baseUrl이 비어있으면(enabled() == false) 검증 없이 그냥 넘어가야 한다 —
        // 로컬처럼 AI 연동을 아예 안 쓰는 환경에서 부팅 자체가 실패하면 안 된다.
        assertThatCode(properties::validateAtStartup).doesNotThrowAnyException();
    }

    @Test
    void failsAtStartupWhenNonLocalUrlUsesHttp() {
        RevenueReportAiEngineProperties properties = new RevenueReportAiEngineProperties(
                "http://revenue-report-ai.example.com", "/api/revenue-report/generate", "secret", 2000, 15000);

        // 배치 실행 시점(한 달 뒤)이 아니라 부팅 시점에 바로 잡혀야 한다.
        assertThatThrownBy(properties::validateAtStartup).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void failsAtStartupWhenNonLocalUrlMissingApiKey() {
        RevenueReportAiEngineProperties properties = new RevenueReportAiEngineProperties(
                "https://revenue-report-ai.example.com", "/api/revenue-report/generate", "", 2000, 15000);

        assertThatThrownBy(properties::validateAtStartup).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void passesValidationForLocalHttpUrlWithoutApiKey() {
        RevenueReportAiEngineProperties properties = new RevenueReportAiEngineProperties(
                "http://localhost:8000", "/api/revenue-report/generate", "", 2000, 15000);

        assertThatCode(properties::validateAtStartup).doesNotThrowAnyException();
    }
}

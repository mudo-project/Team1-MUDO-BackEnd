package com.academy.mudogroupware.revenuereport.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.academy.mudogroupware.revenuereport.application.port.ExpenseSummary;
import com.academy.mudogroupware.revenuereport.application.port.LectureRevenueInfo;
import com.academy.mudogroupware.revenuereport.application.port.RevenueReportAiPort;
import com.academy.mudogroupware.revenuereport.domain.exception.RevenueReportAiException;
import com.academy.mudogroupware.revenuereport.domain.model.RevenueReport;
import com.academy.mudogroupware.revenuereport.domain.repository.RevenueReportRepository;

class GenerateRevenueReportServiceTest {

    private final RevenueReportAggregationReader aggregationReader = mock(RevenueReportAggregationReader.class);
    private final RevenueReportAiPort revenueReportAiPort = mock(RevenueReportAiPort.class);
    private final RevenueReportRepository revenueReportRepository = mock(RevenueReportRepository.class);
    private final RevenueSnapshotCalculator calculator = new RevenueSnapshotCalculator();
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-01T00:30:00Z"), ZoneId.of("Asia/Seoul"));
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper =
            new com.fasterxml.jackson.databind.ObjectMapper()
                    .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                    .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final GenerateRevenueReportService service = new GenerateRevenueReportService(
            aggregationReader, revenueReportAiPort, revenueReportRepository, calculator, clock, objectMapper);

    private RevenueReportAggregation sampleAggregation() {
        return new RevenueReportAggregation(
                List.of(new LectureRevenueInfo(1L, "중등 수학 심화반", "김강사", 300000)),
                Map.of(1L, 10L),
                List.of(),
                new ExpenseSummary(0L, List.of()),
                Map.of());
    }

    @Test
    void skipsWhenReportAlreadyExistsForTargetMonth() {
        LocalDate targetMonth = LocalDate.of(2026, 8, 1);
        when(revenueReportRepository.findByTargetMonth(targetMonth))
                .thenReturn(Optional.of(RevenueReport.create(targetMonth, "이미 있음", "{}", LocalDateTime.now())));

        service.generate(targetMonth);

        verify(revenueReportAiPort, never()).generateReport(any());
        verify(revenueReportRepository, never()).save(any());
    }

    @Test
    void aggregatesCallsAiAndSavesWhenNoExistingReport() {
        LocalDate targetMonth = LocalDate.of(2026, 8, 1);
        when(revenueReportRepository.findByTargetMonth(targetMonth)).thenReturn(Optional.empty());
        when(revenueReportRepository.findByTargetMonth(LocalDate.of(2026, 7, 1))).thenReturn(Optional.empty());
        when(aggregationReader.read(any(), any())).thenReturn(sampleAggregation());
        when(revenueReportAiPort.generateReport(any())).thenReturn("8월 매출 리포트 텍스트");
        when(revenueReportRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.generate(targetMonth);

        ArgumentCaptor<RevenueReport> savedReport = ArgumentCaptor.forClass(RevenueReport.class);
        verify(revenueReportRepository).save(savedReport.capture());
        verify(revenueReportAiPort).generateReport(any());
        // targetMonth가 [2026,8,1] 배열이 아니라 ISO 문자열로 저장돼야 한다 — FastAPI(Pydantic)와
        // 프론트가 이 JSON을 그대로 읽는데, 배열로 나가면 날짜 검증에서 거부당한다(실제로 겪은 버그).
        assertThat(savedReport.getValue().getDataSnapshot()).contains("\"targetMonth\":\"2026-08-01\"");
    }

    @Test
    void propagatesAiFailureAndDoesNotSaveReport() {
        LocalDate targetMonth = LocalDate.of(2026, 8, 1);
        when(revenueReportRepository.findByTargetMonth(targetMonth)).thenReturn(Optional.empty());
        when(revenueReportRepository.findByTargetMonth(LocalDate.of(2026, 7, 1))).thenReturn(Optional.empty());
        when(aggregationReader.read(any(), any())).thenReturn(sampleAggregation());
        when(revenueReportAiPort.generateReport(any()))
                .thenThrow(new RevenueReportAiException("매출 리포트 AI 서버 호출에 실패했습니다."));

        // 이 기능은 명시적으로 fallback이 없다 — AI 실패는 그대로 호출자(배치 스케줄러)에게 전파되어야
        // 잘못된(비어있는) 리포트가 저장되는 일이 없다.
        assertThatThrownBy(() -> service.generate(targetMonth)).isInstanceOf(RevenueReportAiException.class);

        verify(revenueReportRepository, never()).save(any());
    }

    @Test
    void tolerantOfUnknownPropertiesWhenParsingPreviousSnapshot() {
        LocalDate targetMonth = LocalDate.of(2026, 8, 1);
        LocalDate previousMonth = LocalDate.of(2026, 7, 1);
        when(revenueReportRepository.findByTargetMonth(targetMonth)).thenReturn(Optional.empty());
        // 이전 달 스냅샷에 이 서비스가 모르는 필드("newField")가 섞여 있어도(스키마 변경 시나리오)
        // 파싱이 실패해서 전월비교가 조용히 사라지면 안 된다.
        String previousSnapshotJson = "{\"targetMonth\":\"2026-07-01\","
                + "\"revenue\":{\"expected\":1000000,\"actual\":900000},"
                + "\"expense\":{\"actual\":100000,\"byCategory\":[]},"
                + "\"profit\":{\"actual\":800000,\"expected\":900000},"
                + "\"previousMonth\":{\"available\":false},"
                + "\"byLecture\":[],\"byTeacher\":[],"
                + "\"newField\":\"이 서비스가 모르는 필드\"}";
        when(revenueReportRepository.findByTargetMonth(previousMonth)).thenReturn(
                Optional.of(RevenueReport.create(previousMonth, "7월 리포트", previousSnapshotJson, LocalDateTime.now())));
        when(aggregationReader.read(any(), any())).thenReturn(sampleAggregation());
        when(revenueReportAiPort.generateReport(any())).thenReturn("8월 매출 리포트 텍스트");
        when(revenueReportRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.generate(targetMonth);

        ArgumentCaptor<RevenueReport> savedReport = ArgumentCaptor.forClass(RevenueReport.class);
        verify(revenueReportRepository).save(savedReport.capture());
        assertThat(savedReport.getValue().getDataSnapshot()).contains("\"previousMonth\":{\"available\":true");
    }

    @Test
    void computesPreviousMonthDeltaWhenPreviousReportAvailable() {
        LocalDate targetMonth = LocalDate.of(2026, 8, 1);
        LocalDate previousMonth = LocalDate.of(2026, 7, 1);
        when(revenueReportRepository.findByTargetMonth(targetMonth)).thenReturn(Optional.empty());
        String previousSnapshotJson = "{\"targetMonth\":\"2026-07-01\","
                + "\"revenue\":{\"expected\":2500000,\"actual\":2000000},"
                + "\"expense\":{\"actual\":300000,\"byCategory\":[]},"
                + "\"profit\":{\"actual\":1700000,\"expected\":2200000},"
                + "\"previousMonth\":{\"available\":false},"
                + "\"byLecture\":[],\"byTeacher\":[]}";
        when(revenueReportRepository.findByTargetMonth(previousMonth)).thenReturn(
                Optional.of(RevenueReport.create(previousMonth, "7월 리포트", previousSnapshotJson, LocalDateTime.now())));
        when(aggregationReader.read(any(), any())).thenReturn(sampleAggregation());
        when(revenueReportAiPort.generateReport(any())).thenReturn("8월 매출 리포트 텍스트");
        when(revenueReportRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.generate(targetMonth);

        ArgumentCaptor<RevenueReport> savedReport = ArgumentCaptor.forClass(RevenueReport.class);
        verify(revenueReportRepository).save(savedReport.capture());
        // sampleAggregation()의 강의1(300000원*10명=3000000 예상매출, 결제 없음=0 실매출, 지출 0)
        // 기준 계산: actual revenue 0 - 2000000(전월 실매출) = -2000000
        assertThat(savedReport.getValue().getDataSnapshot())
                .contains("\"previousMonth\":{\"available\":true")
                .contains("\"revenueActualDelta\":-2000000");
    }

    @Test
    void treatsCorruptedPreviousSnapshotAsUnavailable() {
        // 이전 달 스냅샷 JSON 자체가 손상돼 있어도(파싱 예외) available=false로 조용히
        // 넘어가야 한다 — 리포트 생성 자체를 막지 않는다.
        LocalDate targetMonth = LocalDate.of(2026, 8, 1);
        LocalDate previousMonth = LocalDate.of(2026, 7, 1);
        when(revenueReportRepository.findByTargetMonth(targetMonth)).thenReturn(Optional.empty());
        when(revenueReportRepository.findByTargetMonth(previousMonth)).thenReturn(
                Optional.of(RevenueReport.create(previousMonth, "7월 리포트", "{이건 유효한 JSON이 아님", LocalDateTime.now())));
        when(aggregationReader.read(any(), any())).thenReturn(sampleAggregation());
        when(revenueReportAiPort.generateReport(any())).thenReturn("8월 매출 리포트 텍스트");
        when(revenueReportRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.generate(targetMonth);

        ArgumentCaptor<RevenueReport> savedReport = ArgumentCaptor.forClass(RevenueReport.class);
        verify(revenueReportRepository).save(savedReport.capture());
        assertThat(savedReport.getValue().getDataSnapshot()).contains("\"previousMonth\":{\"available\":false}");
    }
}

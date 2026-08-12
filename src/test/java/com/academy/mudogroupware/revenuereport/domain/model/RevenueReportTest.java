package com.academy.mudogroupware.revenuereport.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class RevenueReportTest {

    private static final LocalDate TARGET_MONTH = LocalDate.of(2026, 8, 1);

    @Test
    void createStartsUnread() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 1, 0, 30);

        RevenueReport report = RevenueReport.create(TARGET_MONTH, "8월 매출 요약입니다.", "{\"revenue\":{}}", now);

        assertThat(report.getTargetMonth()).isEqualTo(TARGET_MONTH);
        assertThat(report.getReport()).isEqualTo("8월 매출 요약입니다.");
        assertThat(report.isRead()).isFalse();
    }

    @Test
    void markReadSetsReadAtOnlyOnce() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 1, 0, 30);
        RevenueReport report = RevenueReport.restore(
                1L, TARGET_MONTH, "리포트", "{}", null, now, now);

        LocalDateTime firstRead = LocalDateTime.of(2026, 9, 2, 9, 0);
        report.markRead(firstRead);
        LocalDateTime secondRead = LocalDateTime.of(2026, 9, 3, 9, 0);
        report.markRead(secondRead);

        assertThat(report.getReadAt()).isEqualTo(firstRead);
        assertThat(report.isRead()).isTrue();
    }
}

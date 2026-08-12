package com.academy.mudogroupware.revenuereport.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.revenuereport.domain.model.RevenueReport;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({TimeConfig.class, RevenueReportRepositoryImpl.class})
class RevenueReportRepositoryImplDataJpaTest {

    @Autowired
    private RevenueReportRepositoryImpl revenueReportRepositoryImpl;

    @Test
    void savesAndFindsByTargetMonth() {
        RevenueReport report = RevenueReport.create(
                LocalDate.of(2026, 8, 1), "8월 리포트", "{}", LocalDateTime.of(2026, 9, 1, 0, 30));

        RevenueReport saved = revenueReportRepositoryImpl.save(report);

        assertThat(saved.getId()).isNotNull();
        assertThat(revenueReportRepositoryImpl.findByTargetMonth(LocalDate.of(2026, 8, 1))).isPresent();
        assertThat(revenueReportRepositoryImpl.countUnread()).isEqualTo(1);
    }

    @Test
    void markReadDecreasesUnreadCount() {
        RevenueReport report = RevenueReport.create(
                LocalDate.of(2026, 7, 1), "7월 리포트", "{}", LocalDateTime.of(2026, 8, 1, 0, 30));
        RevenueReport saved = revenueReportRepositoryImpl.save(report);

        revenueReportRepositoryImpl.markRead(saved.getId(), LocalDateTime.of(2026, 8, 2, 9, 0));

        assertThat(revenueReportRepositoryImpl.countUnread()).isEqualTo(0);
        assertThat(revenueReportRepositoryImpl.findById(saved.getId()).get().isRead()).isTrue();
    }
}

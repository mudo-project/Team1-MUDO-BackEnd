package com.academy.mudogroupware.payroll.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class PayrollStatementDeliveryPersistenceAdapterTest {
  private JdbcTemplate jdbc;
  private PayrollStatementDeliveryPersistenceAdapter adapter;

  @BeforeEach
  void setUp() {
    var dataSource = new DriverManagerDataSource(
        "jdbc:h2:mem:payroll-email-schedule;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
    jdbc = new JdbcTemplate(dataSource);
    jdbc.execute("drop table if exists payroll_statement_delivery");
    jdbc.execute("create table payroll_statement_delivery ("
        + "status varchar(20) not null, requested_at timestamp not null, "
        + "next_attempt_at timestamp null, sending_started_at timestamp null, "
        + "sent_at timestamp null, failed_at timestamp null, last_reconciled_at timestamp null)");
    adapter = new PayrollStatementDeliveryPersistenceAdapter(jdbc,
        new NamedParameterJdbcTemplate(dataSource));
  }

  @Test
  void 모든_미완료_상태에서_가장_빠른_다음_실행시각을_찾는다() {
    insert("RETRY_WAIT", "2026-08-15 10:00:00", "2026-08-15 10:01:00", null, null,
        null, null);
    insert("SENDING", "2026-08-15 09:40:00", null, "2026-08-15 09:50:00", null,
        null, null);
    insert("SENT", "2026-08-15 09:50:00", null, null, "2026-08-15 09:55:00", null,
        null);
    insert("UNKNOWN", "2026-08-15 09:45:00", null, null, null,
        "2026-08-15 09:50:00", "2026-08-15 09:58:00");

    assertThat(adapter.findNextWakeupAt(Duration.ofMinutes(15), Duration.ofMinutes(10),
        Duration.ofMinutes(5))).contains(LocalDateTime.of(2026, 8, 15, 10, 1));
  }

  private void insert(String status, String requestedAt, String nextAttemptAt,
      String sendingStartedAt, String sentAt, String failedAt, String lastReconciledAt) {
    jdbc.update("insert into payroll_statement_delivery values (?, ?, ?, ?, ?, ?, ?)", status,
        requestedAt, nextAttemptAt, sendingStartedAt, sentAt, failedAt, lastReconciledAt);
  }
}

package com.academy.mudogroupware.platform.infrastructure.database;

import com.academy.mudogroupware.platform.application.port.CurrentTenantDatabaseUsagePort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentTenantDatabaseUsageAdapter implements CurrentTenantDatabaseUsagePort {
  private static final String DATABASE_BYTES_QUERY = """
      SELECT COALESCE(SUM(data_length + index_length), 0)
      FROM information_schema.tables
      WHERE table_schema = DATABASE()
      """;

  private final JdbcTemplate jdbcTemplate;

  @Override
  public long databaseBytes() {
    Long result = jdbcTemplate.queryForObject(DATABASE_BYTES_QUERY, Long.class);
    return result == null ? 0 : result;
  }
}

package com.academy.mudogroupware.global.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/**
 * db/migration 전체가 빈 MySQL에서 버전 순서대로 처음부터 재현 가능한지 검증한다.
 *
 * <p>dev/prod DB는 이미 데이터가 쌓여 있어 마이그레이션 버전이 뒤바뀌어도 out-of-order 옵션 덕분에 조용히 통과할 수 있다. 하지만 신규
 * 환경(로컬 최초 구축, 스테이징 재생성, 장애 복구)에서는 Flyway가 버전 오름차순으로 처음부터 실행하므로, 나중 버전 파일이 이전 버전 파일이
 * 만든 테이블을 전제로 하면 그 지점에서 실패한다. 이 테스트는 그 상황을 CI에서 미리 재현한다.
 */
@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationFromScratchTest {

  @Container
  static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0");

  @Test
  void migratesAllVersionsFromEmptyDatabase() {
    Flyway flyway =
        Flyway.configure()
            .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
            .locations("classpath:db/migration")
            .outOfOrder(true)
            .load();

    assertThatCode(flyway::migrate)
        .as("db/migration의 모든 버전이 빈 DB에서 순서대로 재현 가능해야 한다")
        .doesNotThrowAnyException();
  }
}

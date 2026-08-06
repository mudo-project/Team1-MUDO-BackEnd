package com.academy.mudogroupware;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/**
 * 완전히 빈 MySQL 스키마에서 db/migration 전체가 버전 순서대로 실행되는지 검증한다.
 * 담당자별 폴더(be1~be7)의 버전 번호가 실제 작성 시점과 어긋나 다른 담당자의 테이블
 * 생성보다 먼저 실행되는 의존성 순서 역전(2026-08-06 V1.4.3/V1.4.6/V2.1.8 사례)이
 * 재발하면 이 테스트가 머지 전에 잡아낸다.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class FlywayFreshDatabaseMigrationTest {

    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void allMigrationsApplySuccessfullyFromEmptySchema() {
        List<Map<String, Object>> failed = jdbcTemplate.queryForList(
                "SELECT version, description FROM flyway_schema_history WHERE success = 0");

        assertThat(failed).isEmpty();
    }
}

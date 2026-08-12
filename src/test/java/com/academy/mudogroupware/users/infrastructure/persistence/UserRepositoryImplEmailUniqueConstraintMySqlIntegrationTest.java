package com.academy.mudogroupware.users.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import com.academy.mudogroupware.users.domain.exception.EmailDuplicateException;
import com.academy.mudogroupware.users.domain.model.UserStatus;

/**
 * {@code uk_users_email} 유니크 제약은 Flyway 마이그레이션에만 정의돼 있고 {@code UserEntity}에는
 * {@code @Column(unique = true)}가 없다. 그래서 H2 create-drop 스키마를 쓰는
 * {@link UserRepositoryImplDataJpaTest}/{@link UserRepositoryImplTest}는 실제 제약 위반을
 * 재현하지 못하고, 문자열을 흉내 낸 {@code DataIntegrityViolationException}으로만 변환 로직을
 * 검증한다. 이 테스트는 실제 MySQL + Flyway 마이그레이션으로 진짜 제약 위반이
 * {@link EmailDuplicateException}으로 변환되는지 확인한다.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackageClasses = UserEntity.class)
@EnableJpaRepositories(basePackageClasses = UserJpaRepository.class)
@Import(UserRepositoryImpl.class)
@Testcontainers(disabledWithoutDocker = true)
class UserRepositoryImplEmailUniqueConstraintMySqlIntegrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepositoryImpl userRepository;

    @Test
    void convertsRealEmailUniqueConstraintViolationToEmailDuplicateException() {
        insertUser(1L, "existing", "already-hash", false, "existing@example.com");
        insertUser(2L, "pending", "old-hash", true, "pending@example.com");

        assertThatThrownBy(() -> userRepository.completePasswordSetup(
                2L, "new-hash", "010-1234-5678", "existing@example.com"))
                .isInstanceOf(EmailDuplicateException.class);
    }

    private void insertUser(long id, String suffix, String password, boolean mustChangePw, String email) {
        jdbcTemplate.update("""
                insert into users (
                    id, role_id, username, password, name, phone_number, email, status,
                    must_change_pw, account_type, admin_scope, version, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """, id, null, "user-" + suffix, password, "사용자-" + suffix,
                "010-0000-0000", email, UserStatus.ACTIVE.name(), mustChangePw, "MEMBER", null, 0L);
    }
}

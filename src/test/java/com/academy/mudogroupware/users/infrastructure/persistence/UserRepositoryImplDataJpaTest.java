package com.academy.mudogroupware.users.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.academy.mudogroupware.users.domain.model.UserStatus;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(UserRepositoryImpl.class)
class UserRepositoryImplDataJpaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepositoryImpl userRepository;

    @Test
    void returnsOnlyRequestedActiveUserIdsInTheSameAcademy() {
        long includedId = 10L;
        long otherAcademyId = 20L;
        long inactiveId = 30L;
        long resignedId = 40L;
        long notRequestedId = 50L;
        insertUser(includedId, 1L, "included", UserStatus.ACTIVE);
        insertUser(otherAcademyId, 2L, "other-academy", UserStatus.ACTIVE);
        insertUser(inactiveId, 1L, "inactive", UserStatus.INACTIVE);
        insertUser(resignedId, 1L, "resigned", UserStatus.RESIGNED);
        insertUser(notRequestedId, 1L, "not-requested", UserStatus.ACTIVE);

        Set<Long> activeUserIds = userRepository.findActiveUserIds(
                1L, Set.of(includedId, otherAcademyId, inactiveId, resignedId));

        assertThat(activeUserIds).containsExactly(includedId);
        assertThat(activeUserIds).doesNotContain(notRequestedId);
    }

    private void insertUser(long id, long academyId, String suffix, UserStatus status) {
        jdbcTemplate.update("""
                insert into users (
                    id, academy_id, username, password, name, phone_number, email, status,
                    must_change_pw, is_platform_admin, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """, id, academyId, "user-" + suffix, "password", "사용자-" + suffix,
                "010-0000-0000", suffix + "@example.com", status.name(), false, false);
    }
}

package com.academy.mudogroupware.users.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.academy.mudogroupware.users.domain.model.User;
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

    @Test
    void findAllByIdReturnsOnlyExistingUsersAndIgnoresUnknownIds() {
        insertUser(1L, 1L, "one", UserStatus.ACTIVE);
        insertUser(2L, 1L, "two", UserStatus.ACTIVE);

        List<User> result = userRepository.findAllById(Set.of(1L, 2L, 999L));

        assertThat(result).extracting(User::getId, User::getName)
                .containsExactlyInAnyOrder(tuple(1L, "사용자-one"), tuple(2L, "사용자-two"));
    }

    @Test
    void findAllByIdReturnsEmptyListForEmptyIds() {
        assertThat(userRepository.findAllById(Set.of())).isEmpty();
    }

    @Test
    void countActiveByRoleIdsCountsOnlyActiveUsersGroupedByRole() {
        insertUserWithRole(1L, 1L, "role5-active-1", UserStatus.ACTIVE, 5L);
        insertUserWithRole(2L, 1L, "role5-active-2", UserStatus.ACTIVE, 5L);
        insertUserWithRole(3L, 1L, "role5-resigned", UserStatus.RESIGNED, 5L);
        insertUserWithRole(6L, 1L, "role5-inactive", UserStatus.INACTIVE, 5L);
        insertUserWithRole(4L, 1L, "role7-active", UserStatus.ACTIVE, 7L);
        insertUserWithRole(5L, 1L, "role9-active-not-requested", UserStatus.ACTIVE, 9L);

        Map<Long, Long> result = userRepository.countActiveByRoleIds(Set.of(5L, 7L));

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(5L, 2L, 7L, 1L));
    }

    @Test
    void countActiveByRoleIdsReturnsEmptyMapWhenNoActiveUsersForRequestedRoles() {
        insertUserWithRole(1L, 1L, "role5-resigned", UserStatus.RESIGNED, 5L);
        insertUserWithRole(2L, 1L, "role5-inactive", UserStatus.INACTIVE, 5L);

        Map<Long, Long> result = userRepository.countActiveByRoleIds(Set.of(5L));

        assertThat(result).isEmpty();
    }

    @Test
    void completePasswordSetupReplacesPasswordAndClearsMustChangePw() {
        insertUserWithPasswordAndMustChangePw(1L, 1L, "pending", "old-hash", true);

        userRepository.completePasswordSetup(1L, "new-hash");

        User found = userRepository.findById(1L).orElseThrow();
        assertThat(found.getPassword()).isEqualTo("new-hash");
        assertThat(found.isMustChangePw()).isFalse();
    }

    private void insertUserWithPasswordAndMustChangePw(long id, long academyId, String suffix, String password,
                                                         boolean mustChangePw) {
        jdbcTemplate.update("""
                insert into users (
                    id, academy_id, role_id, username, password, name, phone_number, email, status,
                    must_change_pw, account_type, admin_scope, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """, id, academyId, null, "user-" + suffix, password, "사용자-" + suffix,
                "010-0000-0000", suffix + "@example.com", UserStatus.ACTIVE.name(), mustChangePw, "MEMBER", null);
    }

    private void insertUser(long id, long academyId, String suffix, UserStatus status) {
        insertUserWithRole(id, academyId, suffix, status, null);
    }

    private void insertUserWithRole(long id, long academyId, String suffix, UserStatus status, Long roleId) {
        jdbcTemplate.update("""
                insert into users (
                    id, academy_id, role_id, username, password, name, phone_number, email, status,
                    must_change_pw, account_type, admin_scope, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """, id, academyId, roleId, "user-" + suffix, "password", "사용자-" + suffix,
                "010-0000-0000", suffix + "@example.com", status.name(), false, "MEMBER", null);
    }
}

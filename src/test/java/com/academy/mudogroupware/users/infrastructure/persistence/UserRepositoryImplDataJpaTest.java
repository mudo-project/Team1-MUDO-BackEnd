package com.academy.mudogroupware.users.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.model.UserStatus;

/**
 * {@code notice.UserInfoEntity}/{@code messenger.ChatMemberInfoEntity}가 같은 "users" 테이블에
 * 매핑된 임시 shim이라(README.md 참고), 기본 전체 엔티티 스캔으로 {@code @DataJpaTest}를 띄우면
 * Hibernate가 여러 엔티티의 매핑을 하나의 "users" 테이블 DDL로 합치면서 {@code id} 컬럼의
 * IDENTITY(자동증가) 속성이 사라진다({@code save()}로 신규 저장 시 PK가 NULL로 들어가 실패).
 * users 도메인 엔티티/리포지토리만 스캔하도록 좁혀서 이 충돌을 피한다.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@EntityScan(basePackageClasses = UserEntity.class)
@EnableJpaRepositories(basePackageClasses = UserJpaRepository.class)
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

        boolean updated = userRepository.completePasswordSetup(1L, "new-hash");

        assertThat(updated).isTrue();
        User found = userRepository.findById(1L).orElseThrow();
        assertThat(found.getPassword()).isEqualTo("new-hash");
        assertThat(found.isMustChangePw()).isFalse();
    }

    @Test
    void completePasswordSetupReturnsFalseWhenAlreadyCompleted() {
        insertUserWithPasswordAndMustChangePw(2L, 1L, "done", "already-hash", false);

        boolean updated = userRepository.completePasswordSetup(2L, "new-hash");

        assertThat(updated).isFalse();
        User found = userRepository.findById(2L).orElseThrow();
        assertThat(found.getPassword()).isEqualTo("already-hash");
        assertThat(found.isMustChangePw()).isFalse();
    }

    @Test
    void findAllByAcademyIdReturnsAllStatusesIncludingResignedButExcludesOtherAcademies() {
        insertUserWithRole(1L, 1L, "active", UserStatus.ACTIVE, 5L);
        insertUserWithRole(2L, 1L, "resigned", UserStatus.RESIGNED, 5L);
        insertUserWithRole(3L, 1L, "inactive", UserStatus.INACTIVE, 7L);
        insertUserWithRole(4L, 2L, "other-academy", UserStatus.ACTIVE, 5L);

        List<User> result = userRepository.findAllByAcademyId(1L);

        assertThat(result).extracting(User::getId)
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void savesUserWithNullPhoneAndEmail() {
        User user = User.create(1L, "no-contact", "hashed", "연락처없음", null, null,
                null, com.academy.mudogroupware.global.domain.auth.AccountType.MEMBER, null,
                java.time.LocalDateTime.now());

        User saved = userRepository.save(user);

        assertThat(saved.getPhone()).isNull();
        assertThat(saved.getEmail()).isNull();
    }

    @Test
    void updateProfileReplacesNameContactAndJoinedAt() {
        insertUserWithRole(1L, 1L, "before", UserStatus.ACTIVE, null);
        java.time.LocalDateTime newJoinedAt = java.time.LocalDateTime.of(2026, 1, 1, 0, 0);

        userRepository.updateProfile(1L, "새이름", "010-9999-0000", "new@example.com", newJoinedAt);

        User found = userRepository.findById(1L).orElseThrow();
        assertThat(found.getName()).isEqualTo("새이름");
        assertThat(found.getPhone()).isEqualTo("010-9999-0000");
        assertThat(found.getEmail()).isEqualTo("new@example.com");
        assertThat(found.getJoinedAt()).isEqualTo(newJoinedAt);
    }

    @Test
    void changePasswordReplacesPasswordHash() {
        insertUserWithRole(1L, 1L, "before", UserStatus.ACTIVE, null);

        userRepository.changePassword(1L, "new-encoded-hash");

        assertThat(userRepository.findById(1L).orElseThrow().getPassword()).isEqualTo("new-encoded-hash");
    }

    @Test
    void changesStatusBidirectionally() {
        insertUserWithRole(1L, 1L, "before", UserStatus.ACTIVE, null);

        userRepository.changeStatus(1L, UserStatus.RESIGNED);
        assertThat(userRepository.findById(1L).orElseThrow().getStatus()).isEqualTo(UserStatus.RESIGNED);

        userRepository.changeStatus(1L, UserStatus.ACTIVE);
        assertThat(userRepository.findById(1L).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
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

package com.academy.mudogroupware.users.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.users.domain.exception.RoleNotFoundException;
import com.academy.mudogroupware.users.domain.exception.UsernameDuplicateException;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.model.UserStatus;

class UserRepositoryImplTest {

    @Test
    void convertsUsersRoleForeignKeyViolationOnChangeRoleToRoleNotFoundException() {
        UserJpaRepository jpaRepository = mock(UserJpaRepository.class);
        UserRepositoryImpl adapter = new UserRepositoryImpl(jpaRepository);
        when(jpaRepository.findById(1L)).thenReturn(Optional.of(userEntity()));
        DataIntegrityViolationException violation = new DataIntegrityViolationException(
                "Cannot add or update a child row: a foreign key constraint fails (`fk_users_role`)");
        doThrow(violation).when(jpaRepository).flush();

        assertThatThrownBy(() -> adapter.changeRole(1L, 5L))
                .isInstanceOf(RoleNotFoundException.class)
                .hasCause(violation);
    }

    @Test
    void preservesUnrelatedDataIntegrityViolationOnChangeRole() {
        UserJpaRepository jpaRepository = mock(UserJpaRepository.class);
        UserRepositoryImpl adapter = new UserRepositoryImpl(jpaRepository);
        when(jpaRepository.findById(1L)).thenReturn(Optional.of(userEntity()));
        DataIntegrityViolationException violation = new DataIntegrityViolationException("some unrelated constraint");
        doThrow(violation).when(jpaRepository).flush();

        assertThatThrownBy(() -> adapter.changeRole(1L, 5L)).isSameAs(violation);
    }

    @Test
    void countActiveByRoleIdsReturnsCountsGroupedByRoleId() {
        UserJpaRepository jpaRepository = mock(UserJpaRepository.class);
        UserRepositoryImpl adapter = new UserRepositoryImpl(jpaRepository);
        RoleMemberCountRow row1 = mock(RoleMemberCountRow.class);
        when(row1.getRoleId()).thenReturn(5L);
        when(row1.getCount()).thenReturn(3L);
        RoleMemberCountRow row2 = mock(RoleMemberCountRow.class);
        when(row2.getRoleId()).thenReturn(7L);
        when(row2.getCount()).thenReturn(1L);
        when(jpaRepository.countActiveByRoleIdIn(Set.of(5L, 7L))).thenReturn(List.of(row1, row2));

        Map<Long, Long> result = adapter.countActiveByRoleIds(Set.of(5L, 7L));

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(5L, 3L, 7L, 1L));
    }

    @Test
    void countActiveByRoleIdsReturnsEmptyMapWhenRoleIdsEmpty() {
        UserJpaRepository jpaRepository = mock(UserJpaRepository.class);
        UserRepositoryImpl adapter = new UserRepositoryImpl(jpaRepository);

        Map<Long, Long> result = adapter.countActiveByRoleIds(Set.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(jpaRepository);
    }

    @Test
    void convertsUsernameUniqueConstraintViolationOnSaveToUsernameDuplicateException() {
        UserJpaRepository jpaRepository = mock(UserJpaRepository.class);
        UserRepositoryImpl adapter = new UserRepositoryImpl(jpaRepository);
        DataIntegrityViolationException violation = new DataIntegrityViolationException(
                "Duplicate entry 'teacher01' for key 'users.uk_users_username'");
        when(jpaRepository.saveAndFlush(any(UserEntity.class))).thenThrow(violation);
        User newUser = User.create(1L, "teacher01", "hashed", "김강사", "010-1111-2222", "teacher01@example.com",
                5L, AccountType.MEMBER, null, LocalDateTime.now());

        assertThatThrownBy(() -> adapter.save(newUser))
                .isInstanceOf(UsernameDuplicateException.class)
                .hasCause(violation);
    }

    @Test
    void preservesUnrelatedDataIntegrityViolationOnSave() {
        UserJpaRepository jpaRepository = mock(UserJpaRepository.class);
        UserRepositoryImpl adapter = new UserRepositoryImpl(jpaRepository);
        DataIntegrityViolationException violation = new DataIntegrityViolationException("some unrelated constraint");
        when(jpaRepository.saveAndFlush(any(UserEntity.class))).thenThrow(violation);
        User newUser = User.create(1L, "teacher01", "hashed", "김강사", "010-1111-2222", "teacher01@example.com",
                5L, AccountType.MEMBER, null, LocalDateTime.now());

        assertThatThrownBy(() -> adapter.save(newUser)).isSameAs(violation);
    }

    private UserEntity userEntity() {
        return UserEntity.builder()
                .id(1L).academyId(10L).username("member01").password("hashed").name("구성원")
                .roleId(3L).phone("010-0000-0000").email("member@example.com").status(UserStatus.ACTIVE)
                .mustChangePw(false).accountType(AccountType.MEMBER).adminScope(null)
                .joinedAt(LocalDateTime.now()).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }
}

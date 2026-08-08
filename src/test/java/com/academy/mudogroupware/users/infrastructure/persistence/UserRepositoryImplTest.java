package com.academy.mudogroupware.users.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.users.domain.exception.RoleNotFoundException;
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

    private UserEntity userEntity() {
        return UserEntity.builder()
                .id(1L).academyId(10L).username("member01").password("hashed").name("구성원")
                .roleId(3L).phone("010-0000-0000").email("member@example.com").status(UserStatus.ACTIVE)
                .mustChangePw(false).accountType(AccountType.MEMBER).adminScope(null)
                .joinedAt(LocalDateTime.now()).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }
}

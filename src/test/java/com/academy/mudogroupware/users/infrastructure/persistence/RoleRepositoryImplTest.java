package com.academy.mudogroupware.users.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.academy.mudogroupware.users.domain.exception.RoleNameDuplicateException;
import com.academy.mudogroupware.users.domain.model.Role;

class RoleRepositoryImplTest {

    @Test
    void convertsRoleNameUniqueConstraintViolationToRoleNameDuplicateException() {
        RoleJpaRepository jpaRepository = org.mockito.Mockito.mock(RoleJpaRepository.class);
        RoleRepositoryImpl adapter = new RoleRepositoryImpl(jpaRepository);
        Role role = role();
        DataIntegrityViolationException violation =
                new DataIntegrityViolationException("Duplicate entry for key 'uk_role_academy_name'");
        when(jpaRepository.saveAndFlush(any(RoleEntity.class))).thenThrow(violation);

        assertThatThrownBy(() -> adapter.save(role))
                .isInstanceOf(RoleNameDuplicateException.class)
                .hasCause(violation);
    }

    @Test
    void preservesUnrelatedDataIntegrityViolation() {
        RoleJpaRepository jpaRepository = org.mockito.Mockito.mock(RoleJpaRepository.class);
        RoleRepositoryImpl adapter = new RoleRepositoryImpl(jpaRepository);
        Role role = role();
        DataIntegrityViolationException violation = new DataIntegrityViolationException("foreign key violation");
        when(jpaRepository.saveAndFlush(any(RoleEntity.class))).thenThrow(violation);

        assertThatThrownBy(() -> adapter.save(role)).isSameAs(violation);
    }

    private Role role() {
        return Role.create(1L, "강사", "설명", LocalDateTime.now());
    }
}

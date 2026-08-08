package com.academy.mudogroupware.users.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.academy.mudogroupware.users.domain.exception.RoleInUseException;
import com.academy.mudogroupware.users.domain.exception.RoleNameDuplicateException;
import com.academy.mudogroupware.users.domain.model.Role;

class RoleRepositoryImplTest {

    @Test
    void convertsRoleNameUniqueConstraintViolationToRoleNameDuplicateException() {
        RoleJpaRepository jpaRepository = org.mockito.Mockito.mock(RoleJpaRepository.class);
        PermissionJpaRepository permissionJpaRepository = org.mockito.Mockito.mock(PermissionJpaRepository.class);
        RoleRepositoryImpl adapter = new RoleRepositoryImpl(jpaRepository, permissionJpaRepository);
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
        PermissionJpaRepository permissionJpaRepository = org.mockito.Mockito.mock(PermissionJpaRepository.class);
        RoleRepositoryImpl adapter = new RoleRepositoryImpl(jpaRepository, permissionJpaRepository);
        Role role = role();
        DataIntegrityViolationException violation = new DataIntegrityViolationException("foreign key violation");
        when(jpaRepository.saveAndFlush(any(RoleEntity.class))).thenThrow(violation);

        assertThatThrownBy(() -> adapter.save(role)).isSameAs(violation);
    }

    @Test
    void convertsRoleNameUniqueConstraintViolationOnUpdateToRoleNameDuplicateException() {
        RoleJpaRepository jpaRepository = org.mockito.Mockito.mock(RoleJpaRepository.class);
        PermissionJpaRepository permissionJpaRepository = org.mockito.Mockito.mock(PermissionJpaRepository.class);
        RoleRepositoryImpl adapter = new RoleRepositoryImpl(jpaRepository, permissionJpaRepository);
        when(jpaRepository.findWithPermissionsById(1L)).thenReturn(Optional.of(roleEntity()));
        DataIntegrityViolationException violation =
                new DataIntegrityViolationException("Duplicate entry for key 'uk_role_academy_name'");
        doThrow(violation).when(jpaRepository).flush();

        assertThatThrownBy(() -> adapter.updateNameAndDescription(1L, "조교", "새 설명"))
                .isInstanceOf(RoleNameDuplicateException.class)
                .hasCause(violation);
    }

    @Test
    void preservesUnrelatedDataIntegrityViolationOnUpdate() {
        RoleJpaRepository jpaRepository = org.mockito.Mockito.mock(RoleJpaRepository.class);
        PermissionJpaRepository permissionJpaRepository = org.mockito.Mockito.mock(PermissionJpaRepository.class);
        RoleRepositoryImpl adapter = new RoleRepositoryImpl(jpaRepository, permissionJpaRepository);
        when(jpaRepository.findWithPermissionsById(1L)).thenReturn(Optional.of(roleEntity()));
        DataIntegrityViolationException violation = new DataIntegrityViolationException("some unrelated constraint");
        doThrow(violation).when(jpaRepository).flush();

        assertThatThrownBy(() -> adapter.updateNameAndDescription(1L, "조교", "새 설명")).isSameAs(violation);
    }

    @Test
    void convertsUsersRoleForeignKeyViolationOnDeleteToRoleInUseException() {
        RoleJpaRepository jpaRepository = org.mockito.Mockito.mock(RoleJpaRepository.class);
        PermissionJpaRepository permissionJpaRepository = org.mockito.Mockito.mock(PermissionJpaRepository.class);
        RoleRepositoryImpl adapter = new RoleRepositoryImpl(jpaRepository, permissionJpaRepository);
        DataIntegrityViolationException violation = new DataIntegrityViolationException(
                "Cannot delete or update a parent row: a foreign key constraint fails (`fk_users_role`)");
        doThrow(violation).when(jpaRepository).flush();

        assertThatThrownBy(() -> adapter.deleteById(1L))
                .isInstanceOf(RoleInUseException.class)
                .hasCause(violation);
    }

    @Test
    void preservesUnrelatedDataIntegrityViolationOnDelete() {
        RoleJpaRepository jpaRepository = org.mockito.Mockito.mock(RoleJpaRepository.class);
        PermissionJpaRepository permissionJpaRepository = org.mockito.Mockito.mock(PermissionJpaRepository.class);
        RoleRepositoryImpl adapter = new RoleRepositoryImpl(jpaRepository, permissionJpaRepository);
        DataIntegrityViolationException violation = new DataIntegrityViolationException("some unrelated constraint");
        doThrow(violation).when(jpaRepository).flush();

        assertThatThrownBy(() -> adapter.deleteById(1L)).isSameAs(violation);
    }

    private Role role() {
        return Role.create(1L, "강사", "설명", LocalDateTime.now());
    }

    private RoleEntity roleEntity() {
        return RoleEntity.builder()
                .id(1L).academyId(10L).name("강사").description("설명").createdAt(LocalDateTime.now()).build();
    }
}

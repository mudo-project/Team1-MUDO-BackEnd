package com.academy.mudogroupware.users.infrastructure.persistence;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.users.domain.exception.RoleInUseException;
import com.academy.mudogroupware.users.domain.exception.RoleNameDuplicateException;
import com.academy.mudogroupware.users.domain.exception.RoleNotFoundException;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {

    private static final String ACADEMY_NAME_UNIQUE_CONSTRAINT = "uk_role_academy_name";
    private static final String USERS_ROLE_FK_CONSTRAINT = "fk_users_role";

    private final RoleJpaRepository roleJpaRepository;
    private final PermissionJpaRepository permissionJpaRepository;

    @Override
    public Role save(Role role) {
        if (!role.getPermissionCodes().isEmpty()) {
            throw new IllegalStateException(
                    "save()는 역할 생성 전용이며 권한을 저장하지 않습니다. 권한 조립은 updatePermissions()를 사용하세요.");
        }
        RoleEntity entity = RoleEntity.builder()
                .academyId(role.getAcademyId())
                .name(role.getName())
                .description(role.getDescription())
                .color(role.getColor())
                .createdAt(role.getCreatedAt())
                .build();

        try {
            return toDomain(roleJpaRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            if (containsConstraint(exception, ACADEMY_NAME_UNIQUE_CONSTRAINT)) {
                throw new RoleNameDuplicateException(exception);
            }
            throw exception;
        }
    }

    @Override
    public boolean existsByAcademyIdAndName(Long academyId, String name) {
        return roleJpaRepository.existsByAcademyIdAndName(academyId, name);
    }

    @Override
    public Optional<Role> findById(Long id) {
        return roleJpaRepository.findWithPermissionsById(id).map(this::toDomain);
    }

    @Override
    public void updatePermissions(Long roleId, Set<String> permissionCodes) {
        RoleEntity role = roleJpaRepository.findWithPermissionsByIdForUpdate(roleId)
                .orElseThrow(RoleNotFoundException::new);
        Set<PermissionEntity> permissions = new HashSet<>(permissionJpaRepository.findAllByCodeIn(permissionCodes));
        role.getPermissions().clear();
        role.getPermissions().addAll(permissions);
    }

    @Override
    public List<Role> findAllByAcademyId(Long academyId) {
        return roleJpaRepository.findAllByAcademyIdOrderByIdAsc(academyId).stream()
                .map(this::toDomainWithoutPermissions)
                .toList();
    }

    @Override
    public boolean existsByAcademyIdAndNameAndIdNot(Long academyId, String name, Long excludedRoleId) {
        return roleJpaRepository.existsByAcademyIdAndNameAndIdNot(academyId, name, excludedRoleId);
    }

    @Override
    public void updateNameAndDescription(Long roleId, String name, String description, String color) {
        RoleEntity entity = roleJpaRepository.findWithPermissionsById(roleId)
                .orElseThrow(RoleNotFoundException::new);
        entity.update(name, description, color);
        try {
            roleJpaRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            if (containsConstraint(exception, ACADEMY_NAME_UNIQUE_CONSTRAINT)) {
                throw new RoleNameDuplicateException(exception);
            }
            throw exception;
        }
    }

    @Override
    public void deleteById(Long roleId) {
        try {
            roleJpaRepository.deleteById(roleId);
            roleJpaRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            if (containsConstraint(exception, USERS_ROLE_FK_CONSTRAINT)) {
                throw new RoleInUseException(exception);
            }
            throw exception;
        }
    }

    private Role toDomainWithoutPermissions(RoleEntity entity) {
        return Role.restore(
                entity.getId(), entity.getAcademyId(), entity.getName(), entity.getDescription(), entity.getColor(),
                entity.getCreatedAt(), Set.of());
    }

    private Role toDomain(RoleEntity entity) {
        Set<String> permissionCodes = entity.getPermissions().stream()
                .map(PermissionEntity::getCode)
                .collect(Collectors.toSet());
        return Role.restore(
                entity.getId(), entity.getAcademyId(), entity.getName(), entity.getDescription(), entity.getColor(),
                entity.getCreatedAt(), permissionCodes);
    }

    private boolean containsConstraint(Throwable throwable, String constraintName) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null
                    && message.toLowerCase(Locale.ROOT).contains(constraintName)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}

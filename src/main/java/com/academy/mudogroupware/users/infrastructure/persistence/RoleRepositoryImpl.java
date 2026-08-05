package com.academy.mudogroupware.users.infrastructure.persistence;

import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.users.domain.exception.RoleNameDuplicateException;
import com.academy.mudogroupware.users.domain.exception.RoleNotFoundException;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {

    private static final String ACADEMY_NAME_UNIQUE_CONSTRAINT = "uk_role_academy_name";

    private final RoleJpaRepository roleJpaRepository;
    private final PermissionJpaRepository permissionJpaRepository;

    @Override
    public Role save(Role role) {
        RoleEntity entity = RoleEntity.builder()
                .academyId(role.getAcademyId())
                .name(role.getName())
                .description(role.getDescription())
                .createdAt(role.getCreatedAt())
                .build();

        try {
            return toDomain(roleJpaRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            if (isRoleNameConflict(exception)) {
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
        RoleEntity role = roleJpaRepository.findWithPermissionsById(roleId)
                .orElseThrow(RoleNotFoundException::new);
        Set<PermissionEntity> permissions = new HashSet<>(permissionJpaRepository.findAllByCodeIn(permissionCodes));
        role.getPermissions().clear();
        role.getPermissions().addAll(permissions);
    }

    private Role toDomain(RoleEntity entity) {
        Set<String> permissionCodes = entity.getPermissions().stream()
                .map(PermissionEntity::getCode)
                .collect(Collectors.toSet());
        return Role.restore(
                entity.getId(), entity.getAcademyId(), entity.getName(), entity.getDescription(),
                entity.getCreatedAt(), permissionCodes);
    }

    private boolean isRoleNameConflict(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null
                    && message.toLowerCase(Locale.ROOT).contains(ACADEMY_NAME_UNIQUE_CONSTRAINT)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}

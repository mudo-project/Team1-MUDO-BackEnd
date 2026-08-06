package com.academy.mudogroupware.users.infrastructure.persistence;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.users.domain.model.Permission;
import com.academy.mudogroupware.users.domain.repository.PermissionRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PermissionRepositoryImpl implements PermissionRepository {

    private final PermissionJpaRepository permissionJpaRepository;

    @Override
    public List<Permission> findAll() {
        return permissionJpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Permission> findAllByCodeIn(Set<String> codes) {
        return permissionJpaRepository.findAllByCodeIn(codes).stream()
                .map(this::toDomain)
                .toList();
    }

    private Permission toDomain(PermissionEntity entity) {
        return new Permission(
                entity.getId(), entity.getCode(), entity.getResource(), entity.getAction(), entity.getDescription());
    }
}

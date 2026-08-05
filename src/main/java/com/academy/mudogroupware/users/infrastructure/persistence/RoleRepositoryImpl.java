package com.academy.mudogroupware.users.infrastructure.persistence;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {

    private final RoleJpaRepository roleJpaRepository;

    @Override
    public Role save(Role role) {
        RoleEntity entity = RoleEntity.builder()
                .academyId(role.getAcademyId())
                .name(role.getName())
                .description(role.getDescription())
                .createdAt(role.getCreatedAt())
                .build();
        return toDomain(roleJpaRepository.save(entity));
    }

    @Override
    public boolean existsByAcademyIdAndName(Long academyId, String name) {
        return roleJpaRepository.existsByAcademyIdAndName(academyId, name);
    }

    private Role toDomain(RoleEntity entity) {
        return Role.restore(
                entity.getId(), entity.getAcademyId(), entity.getName(), entity.getDescription(),
                entity.getCreatedAt());
    }
}

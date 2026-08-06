package com.academy.mudogroupware.users.infrastructure.persistence;

import java.util.List;
import java.util.Set;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findByUsername(String username) {
        return userJpaRepository.findByUsername(username).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Set<Long> findActiveUserIds(Long academyId, Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Set.of();
        }
        return userJpaRepository.findActiveIdsByAcademyIdAndIdIn(academyId, userIds);
    }

    @Override
    public List<User> findAllById(Set<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return userJpaRepository.findAllById(ids).stream().map(this::toDomain).toList();
    }

    private User toDomain(UserEntity entity) {
        return User.restore(
                entity.getId(), entity.getAcademyId(), entity.getUsername(), entity.getPassword(), entity.getName(),
                entity.getPhone(), entity.getEmail(), entity.getRoleId(), entity.getStatus(), entity.isMustChangePw(),
                entity.isPlatformAdmin(), entity.getJoinedAt(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}

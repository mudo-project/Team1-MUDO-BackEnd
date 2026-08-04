package com.academy.mudogroupware.users.infrastructure.persistence;

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

    private User toDomain(UserEntity entity) {
        return User.restore(
                entity.getId(), entity.getAcademyId(), entity.getUsername(), entity.getPassword(), entity.getName(),
                entity.getPhone(), entity.getEmail(), entity.getRoleId(), entity.getStatus(), entity.isMustChangePw(),
                entity.isPlatformAdmin(), entity.getJoinedAt(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}

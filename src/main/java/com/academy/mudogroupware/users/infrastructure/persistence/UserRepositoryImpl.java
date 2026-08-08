package com.academy.mudogroupware.users.infrastructure.persistence;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.users.domain.exception.RoleNotFoundException;
import com.academy.mudogroupware.users.domain.exception.UserErrorCode;
import com.academy.mudogroupware.users.domain.exception.UserException;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.model.UserStatus;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private static final String USERS_ROLE_FK_CONSTRAINT = "fk_users_role";

    private final UserJpaRepository userJpaRepository;

    @Override
    public boolean existsActiveByRoleId(Long roleId) {
        return userJpaRepository.existsByRoleIdAndStatus(roleId, UserStatus.ACTIVE);
    }

    @Override
    public void clearRoleId(Long roleId) {
        userJpaRepository.clearRoleId(roleId);
    }

    @Override
    public void changeRole(Long userId, Long roleId) {
        UserEntity entity = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        entity.changeRole(roleId);
        try {
            userJpaRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            if (containsConstraint(exception, USERS_ROLE_FK_CONSTRAINT)) {
                throw new RoleNotFoundException(exception);
            }
            throw exception;
        }
    }

    @Override
    public List<User> searchByAcademyId(Long academyId, String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        List<UserEntity> entities = normalizedKeyword.isEmpty()
                ? userJpaRepository.findAllByAcademyIdAndStatus(academyId, UserStatus.ACTIVE)
                : userJpaRepository.findAllByAcademyIdAndStatusAndNameContainingIgnoreCase(
                        academyId, UserStatus.ACTIVE, normalizedKeyword);
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public Map<Long, Long> countActiveByRoleIds(Set<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        return userJpaRepository.countActiveByRoleIdIn(roleIds).stream()
                .collect(Collectors.toMap(RoleMemberCountRow::getRoleId, RoleMemberCountRow::getCount));
    }

    @Override
    public User save(User user) {
        UserEntity entity = UserEntity.builder()
                .academyId(user.getAcademyId())
                .username(user.getUsername())
                .password(user.getPassword())
                .name(user.getName())
                .roleId(user.getRoleId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .status(user.getStatus())
                .mustChangePw(user.isMustChangePw())
                .accountType(user.getAccountType())
                .adminScope(user.getAdminScope())
                .joinedAt(user.getJoinedAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
        return toDomain(userJpaRepository.save(entity));
    }

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
                entity.getAccountType(), entity.getAdminScope(), entity.getJoinedAt(), entity.getCreatedAt(),
                entity.getUpdatedAt());
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

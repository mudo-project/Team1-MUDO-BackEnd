package com.academy.mudogroupware.users.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.users.domain.exception.EmailDuplicateException;
import com.academy.mudogroupware.users.domain.exception.ProfileUpdateConflictException;
import com.academy.mudogroupware.users.domain.exception.RoleNotFoundException;
import com.academy.mudogroupware.users.domain.exception.UserErrorCode;
import com.academy.mudogroupware.users.domain.exception.UserException;
import com.academy.mudogroupware.users.domain.exception.UsernameDuplicateException;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.model.UserStatus;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private static final String USERS_ROLE_FK_CONSTRAINT = "fk_users_role";
    private static final String USERNAME_UNIQUE_CONSTRAINT = "uk_users_username";
    private static final String EMAIL_UNIQUE_CONSTRAINT = "uk_users_email";

    private final UserJpaRepository userJpaRepository;

    @Override
    public boolean existsActiveByRoleId(Long roleId) {
        return userJpaRepository.existsByRoleIdAndStatus(roleId, UserStatus.ACTIVE);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userJpaRepository.existsByUsername(username);
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
    public void updateProfile(Long userId, String name, String phone, String email, LocalDateTime joinedAt) {
        UserEntity entity = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        entity.updateProfile(name, phone, email, joinedAt);
        try {
            userJpaRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            if (containsConstraint(exception, EMAIL_UNIQUE_CONSTRAINT)) {
                throw new EmailDuplicateException(exception);
            }
            throw exception;
        } catch (OptimisticLockingFailureException exception) {
            throw new ProfileUpdateConflictException(exception);
        }
    }

    @Override
    public void changeStatus(Long userId, UserStatus status) {
        UserEntity entity = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        entity.changeStatus(status);
    }

    @Override
    public void changePassword(Long userId, String encodedPassword) {
        UserEntity entity = userJpaRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        entity.changePassword(encodedPassword);
    }

    @Override
    public boolean completePasswordSetup(Long userId, String newPasswordHash, String phone, String email) {
        try {
            return userJpaRepository.completePasswordSetupIfMustChange(userId, newPasswordHash, phone, email) > 0;
        } catch (DataIntegrityViolationException exception) {
            if (containsConstraint(exception, EMAIL_UNIQUE_CONSTRAINT)) {
                throw new EmailDuplicateException(exception);
            }
            throw exception;
        }
    }

    @Override
    public List<User> search(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        List<UserEntity> entities = normalizedKeyword.isEmpty()
                ? userJpaRepository.findAllByStatus(UserStatus.ACTIVE)
                : userJpaRepository.findAllByStatusAndNameContainingIgnoreCase(
                        UserStatus.ACTIVE, normalizedKeyword);
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public List<User> findAll() {
        return userJpaRepository.findAll().stream().map(this::toDomain).toList();
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
        try {
            return toDomain(userJpaRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            if (containsConstraint(exception, USERNAME_UNIQUE_CONSTRAINT)) {
                throw new UsernameDuplicateException(exception);
            }
            throw exception;
        }
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
        // academyId는 사용하지 않는다 — 학원마다 별도 DB 스키마를 쓰는 배포 구조라 users 도메인은
        // 더 이상 academyId로 스코핑하지 않지만, messenger/workspace가 이 시그니처로 호출하고
        // 있어서(크로스 BC 파라미터) 그쪽 정리가 끝나기 전까지는 그대로 받아만 두고 무시한다.
        if (userIds.isEmpty()) {
            return Set.of();
        }
        return userJpaRepository.findActiveIdsByIdIn(userIds);
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
                entity.getId(), entity.getUsername(), entity.getPassword(), entity.getName(),
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

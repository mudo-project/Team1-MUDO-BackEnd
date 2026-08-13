package com.academy.mudogroupware.users.domain.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

import com.academy.mudogroupware.users.domain.model.User;

public interface UserRepository {

    boolean existsActiveByRoleId(Long roleId);

    void clearRoleId(Long roleId);

    void changeRole(Long userId, Long roleId);

    void updateProfile(Long userId, String name, String phone, String email, java.time.LocalDateTime joinedAt);

    void changeStatus(Long userId, com.academy.mudogroupware.users.domain.model.UserStatus status);

    void changePassword(Long userId, String encodedPassword);

    List<User> search(String keyword);

    List<User> findAll();

    Map<Long, Long> countActiveByRoleIds(Set<Long> roleIds);

    long countActiveUsers();

    User save(User user);

    boolean completePasswordSetup(Long userId, String newPasswordHash, String phone, String email);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<User> findById(Long id);

    Set<Long> findActiveUserIds(Long academyId, Set<Long> userIds);

    List<User> findAllById(Set<Long> ids);
}

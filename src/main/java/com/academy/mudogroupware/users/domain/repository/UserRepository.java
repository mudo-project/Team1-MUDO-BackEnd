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

    List<User> searchByAcademyId(Long academyId, String keyword);

    Map<Long, Long> countActiveByRoleIds(Set<Long> roleIds);

    User save(User user);

    boolean completePasswordSetup(Long userId, String newPasswordHash);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<User> findById(Long id);

    Set<Long> findActiveUserIds(Long academyId, Set<Long> userIds);

    List<User> findAllById(Set<Long> ids);
}

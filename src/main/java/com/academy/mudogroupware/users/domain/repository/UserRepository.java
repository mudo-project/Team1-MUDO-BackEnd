package com.academy.mudogroupware.users.domain.repository;

import java.util.List;
import java.util.Set;
import java.util.Optional;

import com.academy.mudogroupware.users.domain.model.User;

public interface UserRepository {

    boolean existsActiveByRoleId(Long roleId);

    void clearRoleId(Long roleId);

    Optional<User> findByUsername(String username);

    Optional<User> findById(Long id);

    Set<Long> findActiveUserIds(Long academyId, Set<Long> userIds);

    List<User> findAllById(Set<Long> ids);
}

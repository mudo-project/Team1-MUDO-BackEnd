package com.academy.mudogroupware.users.domain.repository;

import java.util.Optional;
import java.util.Set;

import com.academy.mudogroupware.users.domain.model.Role;

public interface RoleRepository {

    Role save(Role role);

    boolean existsByAcademyIdAndName(Long academyId, String name);

    Optional<Role> findById(Long id);

    void updatePermissions(Long roleId, Set<String> permissionCodes);
}

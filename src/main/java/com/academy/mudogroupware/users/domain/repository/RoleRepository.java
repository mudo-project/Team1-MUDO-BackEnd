package com.academy.mudogroupware.users.domain.repository;

import com.academy.mudogroupware.users.domain.model.Role;

public interface RoleRepository {

    Role save(Role role);

    boolean existsByAcademyIdAndName(Long academyId, String name);
}

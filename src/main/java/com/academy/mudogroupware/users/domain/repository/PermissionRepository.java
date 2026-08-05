package com.academy.mudogroupware.users.domain.repository;

import java.util.List;
import java.util.Set;

import com.academy.mudogroupware.users.domain.model.Permission;

public interface PermissionRepository {

    List<Permission> findAll();

    List<Permission> findAllByCodeIn(Set<String> codes);
}

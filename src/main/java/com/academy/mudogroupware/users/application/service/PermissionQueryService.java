package com.academy.mudogroupware.users.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.users.application.usecase.PermissionQueryUseCase;
import com.academy.mudogroupware.users.domain.model.Permission;
import com.academy.mudogroupware.users.domain.repository.PermissionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionQueryService implements PermissionQueryUseCase {

    private final PermissionRepository permissionRepository;

    @Override
    public List<Permission> getPermissions() {
        log.info("event=permission_catalog_list_시작");
        List<Permission> permissions = permissionRepository.findAll();
        log.info("event=permission_catalog_list_완료 count={}", permissions.size());
        return permissions;
    }
}

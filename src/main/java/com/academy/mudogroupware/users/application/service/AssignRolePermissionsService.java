package com.academy.mudogroupware.users.application.service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.users.application.command.AssignRolePermissionsCommand;
import com.academy.mudogroupware.users.application.usecase.AssignRolePermissionsUseCase;
import com.academy.mudogroupware.users.domain.exception.InvalidPermissionCodeException;
import com.academy.mudogroupware.users.domain.exception.RoleNotFoundException;
import com.academy.mudogroupware.users.domain.model.Permission;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.repository.PermissionRepository;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AssignRolePermissionsService implements AssignRolePermissionsUseCase {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public void assignPermissions(AssignRolePermissionsCommand command) {
        Role role = roleRepository.findById(command.roleId())
                .filter(r -> r.getAcademyId().equals(command.academyId()))
                .orElseThrow(RoleNotFoundException::new);

        Set<String> foundCodes = permissionRepository.findAllByCodeIn(command.permissionCodes()).stream()
                .map(Permission::code)
                .collect(Collectors.toSet());
        Set<String> missing = new HashSet<>(command.permissionCodes());
        missing.removeAll(foundCodes);
        if (!missing.isEmpty()) {
            throw new InvalidPermissionCodeException(missing);
        }

        roleRepository.updatePermissions(role.getId(), command.permissionCodes());
    }
}

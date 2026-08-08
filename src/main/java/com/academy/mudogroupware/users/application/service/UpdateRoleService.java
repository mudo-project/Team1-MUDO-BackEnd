package com.academy.mudogroupware.users.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.users.application.command.UpdateRoleCommand;
import com.academy.mudogroupware.users.application.usecase.UpdateRoleUseCase;
import com.academy.mudogroupware.users.domain.exception.RoleNameDuplicateException;
import com.academy.mudogroupware.users.domain.exception.RoleNotFoundException;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateRoleService implements UpdateRoleUseCase {

    private final RoleRepository roleRepository;

    @Override
    public void updateRole(UpdateRoleCommand command) {
        Role role = roleRepository.findById(command.roleId())
                .filter(r -> r.getAcademyId().equals(command.academyId()))
                .orElseThrow(RoleNotFoundException::new);

        if (roleRepository.existsByAcademyIdAndNameAndIdNot(command.academyId(), command.name(), role.getId())) {
            throw new RoleNameDuplicateException();
        }

        roleRepository.updateNameAndDescription(role.getId(), command.name(), command.description(), null);
    }
}

package com.academy.mudogroupware.users.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.users.application.command.DeleteRoleCommand;
import com.academy.mudogroupware.users.application.usecase.DeleteRoleUseCase;
import com.academy.mudogroupware.users.domain.exception.RoleInUseException;
import com.academy.mudogroupware.users.domain.exception.RoleNotFoundException;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteRoleService implements DeleteRoleUseCase {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Override
    public void deleteRole(DeleteRoleCommand command) {
        Role role = roleRepository.findById(command.roleId())
                .filter(r -> r.getAcademyId().equals(command.academyId()))
                .orElseThrow(RoleNotFoundException::new);

        if (userRepository.existsActiveByRoleId(role.getId())) {
            throw new RoleInUseException();
        }

        userRepository.clearRoleId(role.getId());
        roleRepository.deleteById(role.getId());
    }
}

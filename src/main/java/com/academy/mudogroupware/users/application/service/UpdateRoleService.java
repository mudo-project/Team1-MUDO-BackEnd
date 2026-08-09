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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateRoleService implements UpdateRoleUseCase {

    private final RoleRepository roleRepository;

    @Override
    public void updateRole(UpdateRoleCommand command) {
        log.info("event=role_update_시작 roleId={}, academyId={}", command.roleId(), command.academyId());
        try {
            Role role = roleRepository.findById(command.roleId())
                    .filter(r -> r.getAcademyId().equals(command.academyId()))
                    .orElseThrow(RoleNotFoundException::new);

            if (roleRepository.existsByAcademyIdAndNameAndIdNot(command.academyId(), command.name(), role.getId())) {
                throw new RoleNameDuplicateException();
            }

            roleRepository.updateNameAndDescription(role.getId(), command.name(), command.description(),
                    command.color());
            log.info("event=role_update_완료 roleId={}", role.getId());
        } catch (RuntimeException e) {
            log.warn("event=role_update_실패 roleId={}, academyId={}, reason={}", command.roleId(),
                    command.academyId(), e.getMessage());
            throw e;
        }
    }
}

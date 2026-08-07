package com.academy.mudogroupware.users.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.users.application.usecase.ListRolesUseCase;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListRolesService implements ListRolesUseCase {

    private final RoleRepository roleRepository;

    @Override
    public List<Role> listRoles(Long academyId) {
        return roleRepository.findAllByAcademyId(academyId);
    }
}

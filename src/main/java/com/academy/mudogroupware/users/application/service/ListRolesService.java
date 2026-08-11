package com.academy.mudogroupware.users.application.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.users.application.query.RoleView;
import com.academy.mudogroupware.users.application.usecase.ListRolesUseCase;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListRolesService implements ListRolesUseCase {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Override
    public List<RoleView> listRoles() {
        log.info("event=role_list_시작");
        List<Role> roles = roleRepository.findAll();
        Set<Long> roleIds = roles.stream().map(Role::getId).collect(Collectors.toSet());
        Map<Long, Long> memberCounts = userRepository.countActiveByRoleIds(roleIds);
        List<RoleView> result = roles.stream()
                .map(role -> new RoleView(role, memberCounts.getOrDefault(role.getId(), 0L)))
                .toList();
        log.info("event=role_list_완료 count={}", result.size());
        return result;
    }
}

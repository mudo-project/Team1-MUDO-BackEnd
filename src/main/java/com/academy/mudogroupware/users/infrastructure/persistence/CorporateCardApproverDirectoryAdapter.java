package com.academy.mudogroupware.users.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.corporatecard.application.port.CorporateCardApproverDirectoryPort;
import com.academy.mudogroupware.corporatecard.application.port.CorporateCardApproverDirectoryPort.ApproverInfo;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CorporateCardApproverDirectoryAdapter implements CorporateCardApproverDirectoryPort {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /** Consumer: corporatecard / Purpose: 법인카드 상세의 결재자 이름과 현재 역할명 조회 */
    @Override
    public Map<Long, ApproverInfo> getApprovers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<User> users = userRepository.findAllById(Set.copyOf(userIds));
        Set<Long> roleIds = users.stream()
                .map(User::getRoleId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> roleNames = roleIds.isEmpty() ? Map.of() : roleRepository.findAll().stream()
                .filter(role -> roleIds.contains(role.getId()))
                .collect(Collectors.toMap(Role::getId, Role::getName));

        return users.stream().map(user -> new ApproverInfo(
                        user.getId(), user.getName(), user.getRoleId() == null
                                ? null : roleNames.get(user.getRoleId())))
                .collect(Collectors.toMap(ApproverInfo::userId, Function.identity()));
    }
}

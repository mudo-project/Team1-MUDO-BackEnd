package com.academy.mudogroupware.users.application.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.users.application.result.MemberListItem;
import com.academy.mudogroupware.users.application.usecase.ListMembersUseCase;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListMembersService implements ListMembersUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public List<MemberListItem> list(Long academyId, String keyword) {
        log.info("event=member_list_시작 academyId={}, keywordPresent={}", academyId,
                keyword != null && !keyword.isBlank());

        Map<Long, String> roleNamesById = roleRepository.findAllByAcademyId(academyId).stream()
                .collect(Collectors.toMap(Role::getId, Role::getName));

        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);

        List<MemberListItem> result = userRepository.findAllByAcademyId(academyId).stream()
                .map(user -> toItem(user, roleNamesById.get(user.getRoleId())))
                .filter(item -> matchesKeyword(item, normalizedKeyword))
                .toList();

        log.info("event=member_list_완료 academyId={}, count={}", academyId, result.size());
        return result;
    }

    private MemberListItem toItem(User user, String roleName) {
        return new MemberListItem(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getRoleId(), roleName, user.getJoinedAt(), user.getStatus());
    }

    private boolean matchesKeyword(MemberListItem item, String normalizedKeyword) {
        if (normalizedKeyword.isEmpty()) {
            return true;
        }
        boolean nameMatches = item.name().toLowerCase(Locale.ROOT).contains(normalizedKeyword);
        boolean roleMatches = item.roleName() != null
                && item.roleName().toLowerCase(Locale.ROOT).contains(normalizedKeyword);
        return nameMatches || roleMatches;
    }
}

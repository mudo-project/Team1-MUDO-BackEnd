package com.academy.mudogroupware.users.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.users.application.port.MemberTodayAttendanceStatus;
import com.academy.mudogroupware.users.application.port.TodayAttendanceStatusPort;
import com.academy.mudogroupware.users.application.result.MemberListItem;
import com.academy.mudogroupware.users.application.usecase.ListMembersUseCase;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.model.UserStatus;
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
    private final TodayAttendanceStatusPort todayAttendanceStatusPort;

    @Override
    public PageResult<MemberListItem> list(Long academyId, String keyword, Long roleId, int page, int size) {
        log.info("event=member_list_시작 academyId={}, keywordPresent={}, roleId={}, page={}, size={}", academyId,
                keyword != null && !keyword.isBlank(), roleId, page, size);

        List<User> users = userRepository.findAllByAcademyId(academyId);

        Map<Long, String> roleNamesById = roleRepository.findAllByAcademyId(academyId).stream()
                .collect(Collectors.toMap(Role::getId, Role::getName));

        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);

        List<MemberListItem> filtered = users.stream()
                .map(user -> toItem(user, roleNamesById.get(user.getRoleId())))
                .filter(item -> matchesKeyword(item, normalizedKeyword))
                .filter(item -> matchesRole(item, roleId))
                .sorted(Comparator
                        .comparing(MemberListItem::roleName, Comparator.nullsLast(String::compareTo))
                        .thenComparing(MemberListItem::name)
                        .thenComparing(MemberListItem::userId))
                .toList();

        long offset = (long) page * size;
        int from = (int) Math.min(offset, (long) filtered.size());
        int to = (int) Math.min(offset + size, (long) filtered.size());
        List<MemberListItem> paged = filtered.subList(from, to);

        Map<Long, String> attendanceStatusByUserId = todayAttendanceStatusPort
                .findTodayStatusByUserIds(paged.stream().map(MemberListItem::userId).toList()).stream()
                .collect(Collectors.toMap(MemberTodayAttendanceStatus::userId, MemberTodayAttendanceStatus::status));

        List<MemberListItem> result = paged.stream()
                .map(item -> withAttendanceStatus(item, attendanceStatusByUserId))
                .toList();

        log.info("event=member_list_완료 academyId={}, count={}", academyId, result.size());
        return PageResult.of(result, page, size, to < filtered.size());
    }

    private MemberListItem toItem(User user, String roleName) {
        return new MemberListItem(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getRoleId(), roleName, user.getJoinedAt(), user.getStatus(), null);
    }

    private MemberListItem withAttendanceStatus(MemberListItem item, Map<Long, String> attendanceStatusByUserId) {
        String attendanceStatus = item.status() == UserStatus.ACTIVE
                ? attendanceStatusByUserId.get(item.userId())
                : null;
        return new MemberListItem(item.userId(), item.name(), item.email(), item.phone(), item.roleId(),
                item.roleName(), item.joinedAt(), item.status(), attendanceStatus);
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

    private boolean matchesRole(MemberListItem item, Long roleId) {
        return roleId == null || roleId.equals(item.roleId());
    }
}

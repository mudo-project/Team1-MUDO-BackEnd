package com.academy.mudogroupware.users.application.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public List<MemberListItem> list(Long academyId, String keyword) {
        log.info("event=member_list_시작 academyId={}, keywordPresent={}", academyId,
                keyword != null && !keyword.isBlank());

        List<User> users = userRepository.findAllByAcademyId(academyId);

        Map<Long, String> roleNamesById = roleRepository.findAllByAcademyId(academyId).stream()
                .collect(Collectors.toMap(Role::getId, Role::getName));

        Map<Long, String> attendanceStatusByUserId = todayAttendanceStatusPort
                .findTodayStatusByUserIds(users.stream().map(User::getId).toList()).stream()
                .collect(Collectors.toMap(MemberTodayAttendanceStatus::userId, MemberTodayAttendanceStatus::status));

        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);

        List<MemberListItem> result = users.stream()
                .map(user -> toItem(user, roleNamesById.get(user.getRoleId()), attendanceStatusByUserId))
                .filter(item -> matchesKeyword(item, normalizedKeyword))
                .toList();

        log.info("event=member_list_완료 academyId={}, count={}", academyId, result.size());
        return result;
    }

    private MemberListItem toItem(User user, String roleName, Map<Long, String> attendanceStatusByUserId) {
        String attendanceStatus = user.getStatus() == UserStatus.ACTIVE
                ? attendanceStatusByUserId.get(user.getId())
                : null;
        return new MemberListItem(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getRoleId(), roleName, user.getJoinedAt(), user.getStatus(), attendanceStatus);
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

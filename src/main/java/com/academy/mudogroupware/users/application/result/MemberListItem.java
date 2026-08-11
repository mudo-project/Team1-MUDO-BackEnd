package com.academy.mudogroupware.users.application.result;

import java.time.LocalDateTime;

import com.academy.mudogroupware.users.domain.model.UserStatus;

public record MemberListItem(
        Long userId,
        String name,
        String email,
        String phone,
        Long roleId,
        String roleName,
        LocalDateTime joinedAt,
        UserStatus status,
        String attendanceStatus) {
}

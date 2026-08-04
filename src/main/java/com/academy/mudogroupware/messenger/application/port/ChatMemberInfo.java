package com.academy.mudogroupware.messenger.application.port;

public record ChatMemberInfo(
        Long userId,
        String name,
        Long academyId
) {
}

package com.academy.mudogroupware.notice.application.port;

public record AuthorInfo(
        Long userId,
        String name,
        String role,
        Long academyId
) {
}

package com.academy.mudogroupware.users.domain.event;

public record AcademyApplicationApprovedEvent(Long academyId, Long userId, Long applicationId) {
}

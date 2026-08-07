package com.academy.mudogroupware.users.application.command;

public record RejectAcademyApplicationCommand(Long applicationId, Long reviewerId, String reason) {
}

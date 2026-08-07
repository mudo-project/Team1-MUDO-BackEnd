package com.academy.mudogroupware.google.application.command;

public record StartGoogleConnectionCommand(Long academyId, Long userId, boolean forceAccountSelection) {
}

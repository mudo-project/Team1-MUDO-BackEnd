package com.academy.mudogroupware.google.application.command;

public record StartGoogleConnectionCommand(Long userId, boolean forceAccountSelection) {
}

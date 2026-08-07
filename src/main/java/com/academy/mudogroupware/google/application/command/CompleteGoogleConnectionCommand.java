package com.academy.mudogroupware.google.application.command;

public record CompleteGoogleConnectionCommand(String authorizationCode, String state) {
}

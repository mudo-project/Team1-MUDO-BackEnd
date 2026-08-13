package com.academy.mudogroupware.resourceusage.application.command;

public record RecordSmsUsageCommand(
        String feature,
        long sentCount
) {
}

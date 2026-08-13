package com.academy.mudogroupware.platform.domain.model;

import java.time.Instant;

public record StorageUsage(
    String academyCode,
    long databaseBytes,
    long s3Bytes,
    Instant collectedAt) {}

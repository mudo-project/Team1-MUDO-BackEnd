package com.academy.mudogroupware.platform.presentation.api.response;

import com.academy.mudogroupware.platform.domain.model.StorageUsage;
import java.time.Instant;

public record StorageUsageResponse(
    String academyCode, long databaseBytes, long s3Bytes, Instant collectedAt) {
  public static StorageUsageResponse from(StorageUsage usage) {
    return new StorageUsageResponse(
        usage.academyCode(), usage.databaseBytes(), usage.s3Bytes(), usage.collectedAt());
  }
}

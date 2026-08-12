package com.academy.mudogroupware.platform.infrastructure.aws;

import com.academy.mudogroupware.platform.application.port.StorageUsagePort;
import com.academy.mudogroupware.platform.domain.exception.PlatformErrorCode;
import com.academy.mudogroupware.platform.domain.exception.PlatformException;
import com.academy.mudogroupware.platform.domain.model.AcademyRuntime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

@Component
@ConditionalOnProperty(prefix = "platform.dashboard", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class AwsStorageUsageAdapter implements StorageUsagePort {
  private final S3Client s3Client;

  @Override
  public long s3Bytes(AcademyRuntime academy) {
    try {
      long total = bytesInPrefix(academy.staffBucket(), academy.s3Prefix());
      if (!academy.financeBucket().equals(academy.staffBucket())) {
        total += bytesInPrefix(academy.financeBucket(), academy.s3Prefix());
      }
      return total;
    } catch (Exception exception) {
      throw new PlatformException(PlatformErrorCode.METRICS_UNAVAILABLE, exception);
    }
  }

  private long bytesInPrefix(String bucket, String prefix) {
    String continuationToken = null;
    long total = 0;
    do {
      var page = s3Client.listObjectsV2(ListObjectsV2Request.builder()
          .bucket(bucket).prefix(prefix).continuationToken(continuationToken).build());
      total += page.contents().stream().mapToLong(object -> object.size()).sum();
      continuationToken = page.nextContinuationToken();
    } while (continuationToken != null);
    return total;
  }
}

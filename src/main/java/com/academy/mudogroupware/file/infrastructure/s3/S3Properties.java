package com.academy.mudogroupware.file.infrastructure.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record S3Properties(String region, String bucket) {
  public S3Properties(
      @Value("${AWS_REGION:ap-northeast-2}") String region,
      @Value("${AWS_S3_BUCKET_NAME:academy-files}") String bucket) {
    this.region = region;
    this.bucket = bucket;
  }
}

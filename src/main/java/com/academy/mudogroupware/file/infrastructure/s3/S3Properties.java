package com.academy.mudogroupware.file.infrastructure.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public record S3Properties(String region, String staffBucket, String financeBucket) {
  @Autowired
  public S3Properties(
      @Value("${AWS_REGION:ap-northeast-2}") String region,
      @Value("${AWS_S3_STAFF_BUCKET_NAME:${AWS_S3_BUCKET_NAME:academy-files}}") String staffBucket,
      @Value("${AWS_S3_FINANCE_BUCKET_NAME:academy-finance}") String financeBucket) {
    this.region = region;
    this.staffBucket = staffBucket;
    this.financeBucket = financeBucket;
  }

  public S3Properties(String region, String staffBucket) {
    this(region, staffBucket, "academy-finance");
  }
}

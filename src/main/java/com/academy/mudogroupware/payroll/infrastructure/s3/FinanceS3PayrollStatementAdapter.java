package com.academy.mudogroupware.payroll.infrastructure.s3;

import com.academy.mudogroupware.file.infrastructure.s3.S3Properties;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementStoragePort;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Component
@Profile("!local")
@RequiredArgsConstructor
public class FinanceS3PayrollStatementAdapter implements PayrollStatementStoragePort {
  private final S3Client client;
  private final S3Presigner presigner;
  private final S3Properties properties;

  @Override public void upload(String key, byte[] content, String contentType) {
    client.putObject(PutObjectRequest.builder().bucket(properties.financeBucket()).key(key)
        .contentType(contentType).build(), RequestBody.fromBytes(content));
  }
  @Override public byte[] download(String key) {
    return client.getObjectAsBytes(GetObjectRequest.builder()
        .bucket(properties.financeBucket()).key(key).build()).asByteArray();
  }
  @Override public String generateDownloadUrl(String key, long expiresInSeconds) {
    return presigner.presignGetObject(GetObjectPresignRequest.builder()
        .signatureDuration(Duration.ofSeconds(expiresInSeconds))
        .getObjectRequest(b -> b.bucket(properties.financeBucket()).key(key)).build())
        .url().toString();
  }
}

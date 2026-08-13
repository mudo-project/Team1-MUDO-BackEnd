package com.academy.mudogroupware.payroll.infrastructure.s3;

import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementStoragePort;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalPayrollStatementStorageAdapter implements PayrollStatementStoragePort {
  private final Path root;

  public LocalPayrollStatementStorageAdapter(
      @Value("${app.payroll.statement.local-storage-path:build/local-payroll-statements}")
      String storagePath) {
    this.root = Path.of(storagePath).toAbsolutePath().normalize();
  }

  @Override
  public void upload(String key, byte[] content, String contentType) {
    Path target = resolve(key);
    try {
      Files.createDirectories(target.getParent());
      Files.write(target, content);
    } catch (IOException e) {
      throw new IllegalStateException("로컬 급여명세서를 저장할 수 없습니다.", e);
    }
  }

  @Override
  public byte[] download(String key) {
    try {
      return Files.readAllBytes(resolve(key));
    } catch (IOException e) {
      throw new IllegalStateException("로컬 급여명세서를 읽을 수 없습니다.", e);
    }
  }

  @Override
  public String generateDownloadUrl(String key, long expiresInSeconds) {
    return resolve(key).toUri().toString();
  }

  private Path resolve(String key) {
    Path target = root.resolve(key).normalize();
    if (!target.startsWith(root)) {
      throw new IllegalArgumentException("급여명세서 저장 경로가 올바르지 않습니다.");
    }
    return target;
  }
}

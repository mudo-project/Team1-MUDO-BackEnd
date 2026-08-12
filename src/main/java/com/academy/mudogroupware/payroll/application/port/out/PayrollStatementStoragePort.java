package com.academy.mudogroupware.payroll.application.port.out;

public interface PayrollStatementStoragePort {
  void upload(String objectKey, byte[] content, String contentType);
  byte[] download(String objectKey);
  String generateDownloadUrl(String objectKey, long expiresInSeconds);
}

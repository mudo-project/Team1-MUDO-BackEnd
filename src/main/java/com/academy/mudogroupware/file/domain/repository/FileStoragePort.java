package com.academy.mudogroupware.file.domain.repository;
public interface FileStoragePort { String generatePresignedUploadUrl(String objectKey,String contentType); String generatePresignedDownloadUrl(String objectKey); byte[] download(String objectKey); void delete(String objectKey); }

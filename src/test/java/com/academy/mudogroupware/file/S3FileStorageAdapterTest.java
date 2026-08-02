package com.academy.mudogroupware.file;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.academy.mudogroupware.file.infrastructure.s3.*;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class S3FileStorageAdapterTest {
  @Test
  void deletesByObjectKey() {
    S3Client client = mock(S3Client.class);
    S3FileStorageAdapter adapter =
        new S3FileStorageAdapter(
            client, mock(S3Presigner.class), new S3Properties("ap-northeast-2", "academy-files"));
    adapter.delete("documents/file.pdf");
    verify(client)
        .deleteObject(
            argThat(
                (DeleteObjectRequest r) ->
                    r.bucket().equals("academy-files") && r.key().equals("documents/file.pdf")));
  }
}

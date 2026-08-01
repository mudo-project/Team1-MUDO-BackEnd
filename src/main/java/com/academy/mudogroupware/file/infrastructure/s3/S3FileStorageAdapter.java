package com.academy.mudogroupware.file.infrastructure.s3;
import com.academy.mudogroupware.file.domain.repository.FileStoragePort; import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Component; import software.amazon.awssdk.services.s3.*; import software.amazon.awssdk.services.s3.model.*; import software.amazon.awssdk.services.s3.presigner.S3Presigner; import software.amazon.awssdk.services.s3.presigner.model.*; import java.time.Duration;
@Component @RequiredArgsConstructor public class S3FileStorageAdapter implements FileStoragePort { private final S3Client client; private final S3Presigner presigner; private final S3Properties p;
 public String generatePresignedUploadUrl(String k,String type){return presigner.presignPutObject(PutObjectPresignRequest.builder().signatureDuration(Duration.ofMinutes(15)).putObjectRequest(b->b.bucket(p.bucket()).key(k).contentType(type)).build()).url().toString();}
 public String generatePresignedDownloadUrl(String k){return presigner.presignGetObject(GetObjectPresignRequest.builder().signatureDuration(Duration.ofHours(1)).getObjectRequest(b->b.bucket(p.bucket()).key(k)).build()).url().toString();}
 public byte[] download(String k){try{return client.getObjectAsBytes(GetObjectRequest.builder().bucket(p.bucket()).key(k).build()).asByteArray();}catch(S3Exception e){throw new S3StorageException(e);}}
 public void delete(String k){try{client.deleteObject(DeleteObjectRequest.builder().bucket(p.bucket()).key(k).build());}catch(S3Exception e){throw new S3StorageException(e);}}
}

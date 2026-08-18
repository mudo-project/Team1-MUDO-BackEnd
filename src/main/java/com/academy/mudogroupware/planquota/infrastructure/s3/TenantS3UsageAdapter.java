package com.academy.mudogroupware.planquota.infrastructure.s3;

import com.academy.mudogroupware.file.infrastructure.s3.S3Properties;
import com.academy.mudogroupware.global.infrastructure.observability.InstanceMetadataProperties;
import com.academy.mudogroupware.planquota.application.port.TenantS3UsagePort;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

@Component
@RequiredArgsConstructor
public class TenantS3UsageAdapter implements TenantS3UsagePort {

    private final S3Client s3Client;
    private final S3Properties s3Properties;
    private final InstanceMetadataProperties instanceMetadataProperties;

    @Override
    public long currentBytes() {
        String prefix = "tenants/%s/files/".formatted(instanceMetadataProperties.getTenantId());
        String continuationToken = null;
        long total = 0;
        do {
            ListObjectsV2Response page = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(s3Properties.staffBucket())
                    .prefix(prefix)
                    .continuationToken(continuationToken)
                    .build());
            total += page.contents().stream().mapToLong(object -> object.size()).sum();
            continuationToken = page.nextContinuationToken();
        } while (continuationToken != null);
        return total;
    }
}

package com.academy.mudogroupware.file.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.file.application.command.GeneratePresignedUploadUrlCommand;
import com.academy.mudogroupware.file.application.result.PresignedUploadUrlResult;
import com.academy.mudogroupware.file.domain.repository.FileStoragePort;
import com.academy.mudogroupware.global.infrastructure.observability.InstanceMetadataProperties;

class GeneratePresignedUploadUrlServiceTest {

    private final FileStoragePort fileStoragePort = mock(FileStoragePort.class);
    private final InstanceMetadataProperties instanceMetadata = new InstanceMetadataProperties();
    private final GeneratePresignedUploadUrlService service =
            new GeneratePresignedUploadUrlService(fileStoragePort, instanceMetadata);

    @Test
    void generatesObjectKeyUnderTenantPrefixAndSanitizesFileName() {
        instanceMetadata.setTenantId("academy-a");
        when(fileStoragePort.generatePresignedUploadUrl(anyString(), eq("application/pdf")))
                .thenReturn("https://s3.example.com/presigned-put");

        PresignedUploadUrlResult result = service.generate(
                new GeneratePresignedUploadUrlCommand("../secret/휴가원.pdf", "application/pdf"));

        // 운영 Task Role은 tenants/{tenantId}/* 경로에만 s3:PutObject를 허용하므로,
        // objectKey가 반드시 이 prefix 안에 있어야 실제 S3 PUT이 AccessDenied 없이 성공한다.
        assertThat(result.objectKey()).startsWith("tenants/academy-a/files/");
        assertThat(result.objectKey()).endsWith("휴가원.pdf");
        // fileName의 경로 구분자는 전부 치환되어, tenants/{id}/files/ 뒤로 추가 "/"가 생기지 않는다
        // (tenants/ + tenantId/ + files/ = 3개, 접미사에 남은 슬래시는 없어야 한다)
        long slashCount = result.objectKey().chars().filter(c -> c == '/').count();
        assertThat(slashCount).isEqualTo(3);
        assertThat(result.uploadUrl()).isEqualTo("https://s3.example.com/presigned-put");
    }
}

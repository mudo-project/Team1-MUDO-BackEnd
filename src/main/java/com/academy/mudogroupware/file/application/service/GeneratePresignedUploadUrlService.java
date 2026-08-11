package com.academy.mudogroupware.file.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.academy.mudogroupware.file.application.command.GeneratePresignedUploadUrlCommand;
import com.academy.mudogroupware.file.application.result.PresignedUploadUrlResult;
import com.academy.mudogroupware.file.application.usecase.GeneratePresignedUploadUrlUseCase;
import com.academy.mudogroupware.file.domain.repository.FileStoragePort;
import com.academy.mudogroupware.global.infrastructure.observability.InstanceMetadataProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GeneratePresignedUploadUrlService implements GeneratePresignedUploadUrlUseCase {

    private final FileStoragePort fileStoragePort;
    private final InstanceMetadataProperties instanceMetadata;

    @Override
    public PresignedUploadUrlResult generate(GeneratePresignedUploadUrlCommand command) {
        String objectKey = buildObjectKey(command.fileName());
        String uploadUrl = fileStoragePort.generatePresignedUploadUrl(objectKey, command.contentType());
        return new PresignedUploadUrlResult(objectKey, uploadUrl);
    }

    // 운영 ECS Task Role은 tenants/{tenantId}/* 경로에만 s3:PutObject 등을 허용하도록
    // 구성돼 있다. 이 경로 밖으로 objectKey를 만들면 presigned URL 발급 자체는 성공해도
    // 실제 S3 PUT에서 AccessDenied가 난다. tenantId는 클라이언트 입력이 아니라 ECS Task에
    // 주입된 TENANT_ID(app.instance.tenant-id)에서 읽는다 — 클라이언트가 경로를 정하게 하면 안 된다.
    // 원본 파일명 그대로는 경로 구분자(/,\)로 임의 경로에 쓰일 수 있어 제거하고,
    // UUID로 겹치지 않는 고유 objectKey를 만든다.
    private String buildObjectKey(String fileName) {
        String sanitized = fileName.replaceAll("[\\\\/]+", "_");
        return "tenants/%s/files/%s-%s".formatted(instanceMetadata.getTenantId(), UUID.randomUUID(), sanitized);
    }
}

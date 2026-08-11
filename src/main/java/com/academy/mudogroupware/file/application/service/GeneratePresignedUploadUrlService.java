package com.academy.mudogroupware.file.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.academy.mudogroupware.file.application.command.GeneratePresignedUploadUrlCommand;
import com.academy.mudogroupware.file.application.result.PresignedUploadUrlResult;
import com.academy.mudogroupware.file.application.usecase.GeneratePresignedUploadUrlUseCase;
import com.academy.mudogroupware.file.domain.repository.FileStoragePort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GeneratePresignedUploadUrlService implements GeneratePresignedUploadUrlUseCase {

    private final FileStoragePort fileStoragePort;

    @Override
    public PresignedUploadUrlResult generate(GeneratePresignedUploadUrlCommand command) {
        String objectKey = buildObjectKey(command.fileName());
        String uploadUrl = fileStoragePort.generatePresignedUploadUrl(objectKey, command.contentType());
        return new PresignedUploadUrlResult(objectKey, uploadUrl);
    }

    // 원본 파일명 그대로는 경로 구분자(/,\)로 임의 경로에 쓰일 수 있어 제거하고,
    // UUID로 겹치지 않는 고유 objectKey를 만든다.
    private String buildObjectKey(String fileName) {
        String sanitized = fileName.replaceAll("[\\\\/]+", "_");
        return "uploads/%s-%s".formatted(UUID.randomUUID(), sanitized);
    }
}

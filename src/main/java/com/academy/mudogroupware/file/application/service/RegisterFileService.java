package com.academy.mudogroupware.file.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.file.application.command.RegisterFileCommand;
import com.academy.mudogroupware.file.application.usecase.RegisterFileUseCase;
import com.academy.mudogroupware.file.domain.repository.FileStoragePort;
import com.academy.mudogroupware.file.infrastructure.persistence.FileMetadataEntity;
import com.academy.mudogroupware.file.infrastructure.persistence.FileMetadataJpaRepository;
import com.academy.mudogroupware.planquota.application.service.CurrentPlanProvider;
import com.academy.mudogroupware.planquota.domain.exception.PlanLimitErrorCode;
import com.academy.mudogroupware.planquota.domain.exception.PlanLimitExceededException;
import com.academy.mudogroupware.resourceusage.application.command.RecordS3StorageUsageCommand;
import com.academy.mudogroupware.resourceusage.application.port.ResourceUsageQueryPort;
import com.academy.mudogroupware.resourceusage.application.port.ResourceUsageRecorder;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RegisterFileService implements RegisterFileUseCase {

    private final FileMetadataJpaRepository fileMetadataJpaRepository;
    private final FileStoragePort fileStoragePort;
    private final ResourceUsageQueryPort resourceUsageQueryPort;
    private final ResourceUsageRecorder resourceUsageRecorder;
    private final CurrentPlanProvider currentPlanProvider;

    @Override
    public Long register(RegisterFileCommand command) {
        long fileSize = fileStoragePort.headObjectSize(command.objectKey());
        long currentUsage = resourceUsageQueryPort.sumByType(ResourceUsageType.S3_STORAGE);
        long limit = currentPlanProvider.currentLimits().s3BytesLimit();
        if (currentUsage + fileSize > limit) {
            throw new PlanLimitExceededException(PlanLimitErrorCode.S3_LIMIT_EXCEEDED,
                    currentPlanProvider.currentPlan(), limit, currentUsage + fileSize);
        }

        FileMetadataEntity entity = FileMetadataEntity.create(command.objectKey(), command.contentType());
        Long fileId = fileMetadataJpaRepository.save(entity).getId();
        resourceUsageRecorder.recordS3Storage(new RecordS3StorageUsageCommand("file-register", fileSize));
        return fileId;
    }
}

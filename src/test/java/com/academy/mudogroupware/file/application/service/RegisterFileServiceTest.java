package com.academy.mudogroupware.file.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.file.application.command.RegisterFileCommand;
import com.academy.mudogroupware.file.domain.repository.FileStoragePort;
import com.academy.mudogroupware.file.infrastructure.persistence.FileMetadataEntity;
import com.academy.mudogroupware.file.infrastructure.persistence.FileMetadataJpaRepository;
import com.academy.mudogroupware.planquota.application.service.CurrentPlanProvider;
import com.academy.mudogroupware.planquota.domain.exception.PlanLimitExceededException;
import com.academy.mudogroupware.planquota.domain.model.Plan;
import com.academy.mudogroupware.planquota.domain.model.PlanLimits;
import com.academy.mudogroupware.resourceusage.application.command.RecordS3StorageUsageCommand;
import com.academy.mudogroupware.resourceusage.application.port.ResourceUsageQueryPort;
import com.academy.mudogroupware.resourceusage.application.port.ResourceUsageRecorder;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageType;

class RegisterFileServiceTest {

    private final FileMetadataJpaRepository fileMetadataJpaRepository = mock(FileMetadataJpaRepository.class);
    private final FileStoragePort fileStoragePort = mock(FileStoragePort.class);
    private final ResourceUsageQueryPort resourceUsageQueryPort = mock(ResourceUsageQueryPort.class);
    private final ResourceUsageRecorder resourceUsageRecorder = mock(ResourceUsageRecorder.class);
    private final CurrentPlanProvider currentPlanProvider = mock(CurrentPlanProvider.class);
    private final RegisterFileService service = new RegisterFileService(fileMetadataJpaRepository,
            fileStoragePort, resourceUsageQueryPort, resourceUsageRecorder, currentPlanProvider);

    @Test
    void registersFileMetadataAndRecordsUsageWhenWithinLimit() {
        when(fileStoragePort.headObjectSize("uploads/abc-file.pdf")).thenReturn(1024L);
        when(resourceUsageQueryPort.sumByType(ResourceUsageType.S3_STORAGE)).thenReturn(0L);
        when(currentPlanProvider.currentLimits()).thenReturn(PlanLimits.of(Plan.PAID));
        when(fileMetadataJpaRepository.save(any(FileMetadataEntity.class)))
                .thenReturn(FileMetadataEntity.restore(5L, "uploads/abc-file.pdf", "application/pdf"));

        Long fileId = service.register(new RegisterFileCommand("uploads/abc-file.pdf", "application/pdf"));

        assertThat(fileId).isEqualTo(5L);
        verify(resourceUsageRecorder).recordS3Storage(new RecordS3StorageUsageCommand("file-register", 1024L));
    }

    @Test
    void throwsWhenUploadWouldExceedS3Limit() {
        when(fileStoragePort.headObjectSize("key1")).thenReturn(400L * 1024 * 1024);
        when(resourceUsageQueryPort.sumByType(ResourceUsageType.S3_STORAGE)).thenReturn(150L * 1024 * 1024);
        when(currentPlanProvider.currentPlan()).thenReturn(Plan.FREE);
        when(currentPlanProvider.currentLimits()).thenReturn(PlanLimits.of(Plan.FREE));

        assertThatThrownBy(() -> service.register(new RegisterFileCommand("key1", "image/png")))
                .isInstanceOf(PlanLimitExceededException.class);

        verifyNoInteractions(fileMetadataJpaRepository);
        verifyNoInteractions(resourceUsageRecorder);
    }

    @Test
    void deletesOrphanedS3ObjectWhenUploadWouldExceedLimit() {
        when(fileStoragePort.headObjectSize("key1")).thenReturn(400L * 1024 * 1024);
        when(resourceUsageQueryPort.sumByType(ResourceUsageType.S3_STORAGE)).thenReturn(150L * 1024 * 1024);
        when(currentPlanProvider.currentPlan()).thenReturn(Plan.FREE);
        when(currentPlanProvider.currentLimits()).thenReturn(PlanLimits.of(Plan.FREE));

        assertThatThrownBy(() -> service.register(new RegisterFileCommand("key1", "image/png")))
                .isInstanceOf(PlanLimitExceededException.class);

        verify(fileStoragePort).delete("key1");
    }
}

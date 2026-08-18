package com.academy.mudogroupware.resourceusage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.resourceusage.application.command.RecordMailUsageCommand;
import com.academy.mudogroupware.resourceusage.application.command.RecordS3StorageUsageCommand;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageEvent;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageRepository;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageType;

@ExtendWith(MockitoExtension.class)
class ResourceUsageRecordServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 15, 10, 0);

    @Mock
    private ResourceUsageRepository resourceUsageRepository;

    private final Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(),
            ZoneId.of("Asia/Seoul"));

    @Test
    void recordsS3StorageEvent() {
        ResourceUsageRecordService service = new ResourceUsageRecordService(resourceUsageRepository, clock);
        when(resourceUsageRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.recordS3Storage(new RecordS3StorageUsageCommand("file-register", 1024L));

        ArgumentCaptor<ResourceUsageEvent> captor = ArgumentCaptor.forClass(ResourceUsageEvent.class);
        verify(resourceUsageRepository).save(captor.capture());
        assertThat(captor.getValue().getResourceType()).isEqualTo(ResourceUsageType.S3_STORAGE);
        assertThat(captor.getValue().getAmount()).isEqualTo(1024L);
    }

    @Test
    void recordsMailEvent() {
        ResourceUsageRecordService service = new ResourceUsageRecordService(resourceUsageRepository, clock);
        when(resourceUsageRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.recordMailUsage(new RecordMailUsageCommand("payroll-statement", 1L));

        ArgumentCaptor<ResourceUsageEvent> captor = ArgumentCaptor.forClass(ResourceUsageEvent.class);
        verify(resourceUsageRepository).save(captor.capture());
        assertThat(captor.getValue().getResourceType()).isEqualTo(ResourceUsageType.MAIL);
    }

    @Test
    void ignoresNullS3StorageCommand() {
        ResourceUsageRecordService service = new ResourceUsageRecordService(resourceUsageRepository, clock);

        service.recordS3Storage(null);

        verifyNoInteractions(resourceUsageRepository);
    }

    @Test
    void ignoresZeroOrNegativeS3StorageBytes() {
        ResourceUsageRecordService service = new ResourceUsageRecordService(resourceUsageRepository, clock);

        service.recordS3Storage(new RecordS3StorageUsageCommand("file-register", 0L));
        service.recordS3Storage(new RecordS3StorageUsageCommand("file-register", -1L));

        verifyNoInteractions(resourceUsageRepository);
    }

    @Test
    void ignoresNullMailUsageCommand() {
        ResourceUsageRecordService service = new ResourceUsageRecordService(resourceUsageRepository, clock);

        service.recordMailUsage(null);

        verifyNoInteractions(resourceUsageRepository);
    }

    @Test
    void ignoresZeroOrNegativeMailCount() {
        ResourceUsageRecordService service = new ResourceUsageRecordService(resourceUsageRepository, clock);

        service.recordMailUsage(new RecordMailUsageCommand("payroll-statement", 0L));
        service.recordMailUsage(new RecordMailUsageCommand("payroll-statement", -1L));

        verifyNoInteractions(resourceUsageRepository);
    }
}

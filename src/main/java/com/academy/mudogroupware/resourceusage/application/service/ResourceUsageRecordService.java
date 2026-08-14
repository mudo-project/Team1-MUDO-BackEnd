package com.academy.mudogroupware.resourceusage.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.resourceusage.application.command.RecordAiTokenUsageCommand;
import com.academy.mudogroupware.resourceusage.application.command.RecordMailUsageCommand;
import com.academy.mudogroupware.resourceusage.application.command.RecordS3StorageUsageCommand;
import com.academy.mudogroupware.resourceusage.application.command.RecordSmsUsageCommand;
import com.academy.mudogroupware.resourceusage.application.port.ResourceUsageRecorder;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageEvent;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResourceUsageRecordService implements ResourceUsageRecorder {

    private final ResourceUsageRepository resourceUsageRepository;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAiTokens(RecordAiTokenUsageCommand command) {
        if (command == null || command.totalTokens() <= 0) {
            return;
        }
        resourceUsageRepository.save(ResourceUsageEvent.aiTokens(
                command.feature(),
                command.provider(),
                command.modelName(),
                Math.max(command.promptTokens(), 0),
                Math.max(command.outputTokens(), 0),
                command.totalTokens(),
                now()));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSmsMessages(RecordSmsUsageCommand command) {
        if (command == null || command.sentCount() <= 0) {
            return;
        }
        resourceUsageRepository.save(ResourceUsageEvent.smsMessages(command.feature(), command.sentCount(), now()));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordS3Storage(RecordS3StorageUsageCommand command) {
        if (command == null || command.bytes() <= 0) {
            return;
        }
        resourceUsageRepository.save(ResourceUsageEvent.s3Storage(command.feature(), command.bytes(), now()));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordMailUsage(RecordMailUsageCommand command) {
        if (command == null || command.count() <= 0) {
            return;
        }
        resourceUsageRepository.save(ResourceUsageEvent.mail(command.feature(), command.count(), now()));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}

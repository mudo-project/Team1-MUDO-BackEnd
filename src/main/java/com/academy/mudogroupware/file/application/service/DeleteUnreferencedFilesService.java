package com.academy.mudogroupware.file.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.file.application.port.FileReferenceChecker;
import com.academy.mudogroupware.file.domain.repository.FileStoragePort;
import com.academy.mudogroupware.file.infrastructure.persistence.FileMetadataEntity;
import com.academy.mudogroupware.file.infrastructure.persistence.FileMetadataJpaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeleteUnreferencedFilesService {

    private final FileMetadataJpaRepository fileMetadataJpaRepository;
    private final FileStoragePort fileStoragePort;
    private final FileReferenceChecker fileReferenceChecker;

    @Transactional
    public int deleteUnreferencedFiles(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return 0;
        }

        int deletedCount = 0;
        for (FileMetadataEntity metadata : fileMetadataJpaRepository.findAllById(fileIds)) {
            if (fileReferenceChecker.isReferenced(metadata.getId())) {
                continue;
            }
            try {
                fileStoragePort.delete(metadata.getObjectKey());
                fileMetadataJpaRepository.delete(metadata);
                deletedCount++;
            } catch (RuntimeException exception) {
                log.warn("event=file_cleanup_failed fileId={}, reason={}",
                        metadata.getId(), exception.getMessage(), exception);
            }
        }
        return deletedCount;
    }
}
